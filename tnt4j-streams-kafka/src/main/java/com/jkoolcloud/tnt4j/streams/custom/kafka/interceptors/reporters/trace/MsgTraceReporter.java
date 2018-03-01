/*
 * Copyright 2014-2018 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.trace;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.MapUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.StreamsAgent;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.InterceptionsManager;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.InterceptionsReporter;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.fields.StreamFieldType;
import com.jkoolcloud.tnt4j.streams.utils.KafkaStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.uuid.DefaultUUIDFactory;

/**
 * Producer/Consumer interceptors intercepted messages reporter sending JKoolCloud events containing intercepted message
 * payload data, metadata and context data.
 * <p>
 * JKool Event types sent on consumer/producer interceptions:
 * <ul>
 * <li>send - 1 {@link com.jkoolcloud.tnt4j.core.OpType#SEND} type event.</li>
 * <li>acknowledge - 1 {@link com.jkoolcloud.tnt4j.core.OpType#EVENT} type event.</li>
 * <li>consume - n {@link com.jkoolcloud.tnt4j.core.OpType#RECEIVE} type events.</li>
 * <li>commit - n {@link com.jkoolcloud.tnt4j.core.OpType#EVENT} type events.</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class MsgTraceReporter implements InterceptionsReporter {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(MsgTraceReporter.class);

	public static final String TNT_TRACE_CONFIG_TOPIC = "TNT_TRACE_CONFIG_TOPIC"; // NON-NLS
	public static final int POOL_TIME_SECONDS = 3;
	public static final String TRACER_PROPERTY_PREFIX = "messages.tracer.kafka."; // NON-NLS

	private KafkaMsgTraceStream stream;
	private Map<String, TraceCommandDeserializer.TopicTraceCommand> traceConfig = new HashMap<>();
	private final Timer pollTimer = new Timer();

	private static KafkaConsumer<String, TraceCommandDeserializer.TopicTraceCommand> consumer;

	/**
	 * Constructs a new MsgTraceReporter.
	 */
	public MsgTraceReporter(final Properties kafkaProperties) {
		stream = new KafkaMsgTraceStream();
		StreamsAgent.runFromAPI(stream);
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"MsgTraceReporter.stream.started", stream.getName());
		TimerTask mrt = new TimerTask() {
			@Override
			public void run() {
				Map<String, Map<String, ?>> consumersCfg = InterceptionsManager.getInstance()
						.getInterceptorsConfig(TNTKafkaCInterceptor.class);
				Map<String, ?> cConfig = MapUtils.isEmpty(consumersCfg) ? null
						: consumersCfg.entrySet().iterator().next().getValue();
				pollConfigQueue(cConfig, kafkaProperties, traceConfig);
			}
		};
		traceConfig.put(TraceCommandDeserializer.MASTER_CONFIG, new TraceCommandDeserializer.TopicTraceCommand());
		long period = TimeUnit.SECONDS.toMillis(POOL_TIME_SECONDS);
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"MsgTraceReporter.schedule.commands.polling", TNT_TRACE_CONFIG_TOPIC, period, period);
		pollTimer.scheduleAtFixedRate(mrt, period, period);
	}

	protected static void pollConfigQueue(Map<String, ?> config, Properties kafkaProperties,
			Map<String, TraceCommandDeserializer.TopicTraceCommand> traceConfig) {
		Properties props = new Properties();
		if (config != null) {
			props.putAll(config);
		}
		if (kafkaProperties != null) {
			props.putAll(extractKafkaProperties(kafkaProperties));
		}
		if (!props.isEmpty()) {
			props.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafka-x-ray-message-trace-reporter-config-listener"); // NON-NLS
			props.remove(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG);
			KafkaConsumer<String, TraceCommandDeserializer.TopicTraceCommand> consumer = getKafkaConsumer(props);
			while (true) {
				ConsumerRecords<String, TraceCommandDeserializer.TopicTraceCommand> records = consumer.poll(100);
				if (records.count() > 0) {
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
							"MsgTraceReporter.polled.commands", records.count(), records.iterator().next());
					for (ConsumerRecord<String, TraceCommandDeserializer.TopicTraceCommand> record : records) {
						if (record.value() != null) {
							traceConfig.put(record.value().topic, record.value());
						}
					}
					break;
				}
			}
		}
	}

	private static KafkaConsumer<String, TraceCommandDeserializer.TopicTraceCommand> getKafkaConsumer(
			Properties props) {
		if (consumer != null) {
			return consumer;
		}
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"MsgTraceReporter.creating.command.consumer", props);
		consumer = new KafkaConsumer<>(props, new StringDeserializer(), new TraceCommandDeserializer());
		TopicPartition topic = new TopicPartition(MsgTraceReporter.TNT_TRACE_CONFIG_TOPIC, 0);
		consumer.assign(Collections.singletonList(topic));
		return consumer;
	}

	protected static Properties extractKafkaProperties(Properties kafkaProperties) {
		Properties props = new Properties();
		for (String key : kafkaProperties.stringPropertyNames()) {
			if (key.startsWith(TRACER_PROPERTY_PREFIX)) {
				props.put(key.substring(TRACER_PROPERTY_PREFIX.length()), kafkaProperties.getProperty(key));
			}
		}
		return props;
	}

	protected Boolean shouldSendTrace(String topic, boolean count) {
		TraceCommandDeserializer.TopicTraceCommand topicTraceConfig = traceConfig.get(topic);
		if (topicTraceConfig == null) {
			topicTraceConfig = traceConfig.get(TraceCommandDeserializer.MASTER_CONFIG);
		}

		boolean send = (topic == null || topicTraceConfig == null) ? false : topicTraceConfig.match(topic, count);
		StackTraceElement callMethodTrace = Utils.getStackFrame(2);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"MsgTraceReporter.should.trace", callMethodTrace.getMethodName(), topic, count, topicTraceConfig, send);

		return send;
	}

	@Override
	public void send(TNTKafkaPInterceptor interceptor, ProducerRecord<Object, Object> producerRecord) {
		if (producerRecord == null) {
			return;
		}
		if (shouldSendTrace(producerRecord.topic(), true)) {
			try {
				ActivityInfo ai = new ActivityInfo();
				ai.setFieldValue(new ActivityField(StreamFieldType.EventType.name()), OpType.SEND);
				ai.setFieldValue(new ActivityField(StreamFieldType.EventName.name()), "Kafka_Producer_Send"); // NON-NLS
				ai.setFieldValue(new ActivityField("Partition"), producerRecord.partition()); // NON-NLS
				ai.setFieldValue(new ActivityField("Topic"), producerRecord.topic()); // NON-NLS
				ai.setFieldValue(new ActivityField("Key"), producerRecord.key()); // NON-NLS
				ai.setFieldValue(new ActivityField("Value"), producerRecord.value()); // NON-NLS
				ai.setFieldValue(new ActivityField(StreamFieldType.StartTime.name()), producerRecord.timestamp());
				ai.addCorrelator(producerRecord.topic());

				stream.addInputToBuffer(ai);
			} catch (Exception exc) {
				Utils.logThrowable(LOGGER, OpLevel.ERROR,
						StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
						"MsgTraceReporter.send.failed", exc);
			}
		}
	}

	@Override
	public void acknowledge(TNTKafkaPInterceptor interceptor, RecordMetadata recordMetadata, Exception e,
			ClusterResource clusterResource) {
		if (recordMetadata == null) {
			return;
		}
		if (shouldSendTrace(recordMetadata.topic(), false)) {
			try {
				ActivityInfo ai = new ActivityInfo();
				ai.setFieldValue(new ActivityField(StreamFieldType.EventType.name()), OpType.EVENT);
				ai.setFieldValue(new ActivityField(StreamFieldType.EventName.name()), "Kafka_Producer_Acknowledge"); // NON-NLS
				ai.setFieldValue(new ActivityField("Offset"), recordMetadata.offset()); // NON-NLS
				ai.setFieldValue(new ActivityField(StreamFieldType.StartTime.name()),
						TimeUnit.MILLISECONDS.toMicros(recordMetadata.timestamp()));
				ai.setFieldValue(new ActivityField("Checksum"), recordMetadata.checksum()); // NON-NLS
				ai.setFieldValue(new ActivityField("Topic"), recordMetadata.topic()); // NON-NLS
				ai.setFieldValue(new ActivityField("Partition"), recordMetadata.partition()); // NON-NLS
				if (e != null) {
					ai.setFieldValue(new ActivityField(StreamFieldType.Exception.name()),
							Utils.getExceptionMessages(e));
				}
				if (clusterResource != null) {
					ai.setFieldValue(new ActivityField("ClusterId"), clusterResource.clusterId()); // NON-NLS
				}

				int size = Math.max(recordMetadata.serializedKeySize(), 0)
						+ Math.max(recordMetadata.serializedValueSize(), 0);

				ai.setFieldValue(new ActivityField("Size"), size); // NON-NLS
				ai.setFieldValue(new ActivityField(StreamFieldType.TrackingId.name()),
						calcSignature(recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset()));
				ai.addCorrelator(recordMetadata.topic());

				stream.addInputToBuffer(ai);
			} catch (Exception exc) {
				Utils.logThrowable(LOGGER, OpLevel.ERROR,
						StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
						"MsgTraceReporter.acknowledge.failed", exc);
			}
		}
	}

	@Override
	public void consume(TNTKafkaCInterceptor interceptor, ConsumerRecords<Object, Object> consumerRecords,
			ClusterResource clusterResource) {
		if (consumerRecords == null) {
			return;
		}
		String tid = null;
		ActivityInfo ai;
		try {
			ai = new ActivityInfo();
			ai.setFieldValue(new ActivityField(StreamFieldType.EventType.name()), OpType.ACTIVITY);
			ai.setFieldValue(new ActivityField(StreamFieldType.EventName.name()), "Kafka_Consumer_Consume"); // NON-NLS
			ai.setFieldValue(new ActivityField(StreamFieldType.TrackingId.name()),
					DefaultUUIDFactory.getInstance().newUUID());
			stream.addInputToBuffer(ai);

			tid = ai.getTrackingId();
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR,
					StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MsgTraceReporter.consume.failed", exc);
		}
		for (ConsumerRecord<Object, Object> cr : consumerRecords) {
			if (cr == null) {
				continue;
			}
			if (shouldSendTrace(cr.topic(), true)) {
				try {
					ai = new ActivityInfo();
					if (tid != null) {
						ai.setFieldValue(new ActivityField(StreamFieldType.ParentId.name()), tid);
					}
					ai.setFieldValue(new ActivityField(StreamFieldType.EventType.name()), OpType.RECEIVE);
					ai.setFieldValue(new ActivityField(StreamFieldType.EventName.name()),
							"Kafka_Consumer_Consume_Record"); // NON-NLS
					ai.setFieldValue(new ActivityField("Topic"), cr.topic()); // NON-NLS
					ai.setFieldValue(new ActivityField("Partition"), cr.partition()); // NON-NLS
					ai.setFieldValue(new ActivityField("Offset"), cr.offset()); // NON-NLS
					ai.setFieldValue(new ActivityField(StreamFieldType.StartTime.name()),
							TimeUnit.MILLISECONDS.toMicros(cr.timestamp()));
					ai.setFieldValue(new ActivityField("TimestampType"), cr.timestampType()); // NON-NLS
					ai.setFieldValue(new ActivityField("Key"), cr.key()); // NON-NLS
					ai.setFieldValue(new ActivityField("Value"), cr.value()); // NON-NLS
					ai.setFieldValue(new ActivityField("Checksum"), cr.checksum()); // NON-NLS

					int size = Math.max(cr.serializedKeySize(), 0) + Math.max(cr.serializedValueSize(), 0);
					long latency = System.currentTimeMillis() - cr.timestamp();

					ai.setFieldValue(new ActivityField("Size"), size); // NON-NLS
					ai.setFieldValue(new ActivityField("Latency"), latency); // NON-NLS

					if (clusterResource != null) {
						ai.setFieldValue(new ActivityField("ClusterId"), clusterResource.clusterId()); // NON-NLS
					}

					ai.setFieldValue(new ActivityField(StreamFieldType.TrackingId.name()),
							calcSignature(cr.topic(), cr.partition(), cr.offset()));
					ai.addCorrelator(cr.topic(), String.valueOf(cr.offset()));

					stream.addInputToBuffer(ai);
				} catch (Exception exc) {
					Utils.logThrowable(LOGGER, OpLevel.ERROR,
							StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
							"MsgTraceReporter.consume.failed", exc);
				}
			}
		}
	}

	@Override
	public void commit(TNTKafkaCInterceptor interceptor, Map<TopicPartition, OffsetAndMetadata> map) {
		if (map == null) {
			return;
		}
		String tid = null;
		ActivityInfo ai;
		try {
			ai = new ActivityInfo();
			ai.setFieldValue(new ActivityField(StreamFieldType.EventType.name()), OpType.ACTIVITY);
			ai.setFieldValue(new ActivityField(StreamFieldType.EventName.name()), "Kafka_Consumer_Commit"); // NON-NLS
			ai.setFieldValue(new ActivityField(StreamFieldType.TrackingId.name()),
					DefaultUUIDFactory.getInstance().newUUID());
			stream.addInputToBuffer(ai);

			tid = ai.getTrackingId();
		} catch (Exception exc) {
			Utils.logThrowable(LOGGER, OpLevel.ERROR,
					StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MsgTraceReporter.commit.failed", exc);
		}
		for (Map.Entry<TopicPartition, OffsetAndMetadata> me : map.entrySet()) {
			if (me == null) {
				continue;
			}
			if (shouldSendTrace(me.getKey().topic(), false)) {
				try {
					ai = new ActivityInfo();
					if (tid != null) {
						ai.setFieldValue(new ActivityField(StreamFieldType.ParentId.name()), tid);
					}
					ai.setFieldValue(new ActivityField(StreamFieldType.EventType.name()), OpType.EVENT);
					ai.setFieldValue(new ActivityField(StreamFieldType.EventName.name()),
							"Kafka_Consumer_Commit_Entry"); // NON-NLS
					ai.setFieldValue(new ActivityField("Partition"), me.getKey().partition()); // NON-NLS
					ai.setFieldValue(new ActivityField("Topic"), me.getKey().topic()); // NON-NLS
					ai.setFieldValue(new ActivityField("Offset"), me.getValue().offset()); // NON-NLS
					ai.setFieldValue(new ActivityField("Metadata"), me.getValue().metadata()); // NON-NLS
					ai.setFieldValue(new ActivityField(StreamFieldType.TrackingId.name()),
							calcSignature(me.getKey().topic(), me.getKey().partition(), me.getValue().offset()));
					ai.addCorrelator(me.getKey().topic(), String.valueOf(me.getValue().offset()));

					stream.addInputToBuffer(ai);
				} catch (Exception exc) {
					Utils.logThrowable(LOGGER, OpLevel.ERROR,
							StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
							"MsgTraceReporter.commit.failed", exc);
				}
			}
		}
	}

	@Override
	public void shutdown() {
		if (stream != null) {
			stream.markEnded();
		}
	}

	private static final MessageDigest MSG_DIGEST = Utils.getMD5Digester();

	private static String calcSignature(String topic, int partition, long offset) {
		synchronized (MSG_DIGEST) {
			MSG_DIGEST.reset();
			if (topic != null) {
				MSG_DIGEST.update(topic.getBytes());
			}
			MSG_DIGEST.update(ByteBuffer.allocate(4).putInt(partition).array());
			MSG_DIGEST.update(ByteBuffer.allocate(8).putLong(offset).array());

			return Utils.base64EncodeStr(MSG_DIGEST.digest());
		}
	}
}
