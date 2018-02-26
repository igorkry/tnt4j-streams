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

/**
 * Producer/Consumer interceptors intercepted messages reporter sending JKool Cloud events containing intercepted
 * message payload data, metadata and context data.
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

	private KafkaMsgTraceStream stream;
	private Map<String, TraceCommandDeserializer.TopicTraceCommand> traceConfig = new HashMap<>();
	private final Timer pollTimer = new Timer();

	/**
	 * Constructs a new MsgTraceReporter.
	 */
	public MsgTraceReporter() {
		stream = new KafkaMsgTraceStream();
		StreamsAgent.runFromAPI(stream);
		TimerTask mrt = new TimerTask() {
			@Override
			public void run() {
				Map<String, Map<String, ?>> consumersCfg = InterceptionsManager.getInstance()
						.getInterceptorsConfig(TNTKafkaCInterceptor.class);
				Map<String, ?> cConfig = MapUtils.isEmpty(consumersCfg) ? null
						: consumersCfg.entrySet().iterator().next().getValue();

				pollConfigQueue(cConfig, traceConfig);
			}
		};
		long period = TimeUnit.SECONDS.toMillis(POOL_TIME_SECONDS);
		pollTimer.scheduleAtFixedRate(mrt, period, period);
	}

	protected static void pollConfigQueue(Map<String, ?> config,
			Map<String, TraceCommandDeserializer.TopicTraceCommand> traceConfig) {
		Properties props = new Properties();
		if (config != null) {
			props.putAll(config);
		}
		if (!props.isEmpty()) {
			props.put(ConsumerConfig.CLIENT_ID_CONFIG, "kafka-x-ray-message-trace-reporter-config-listener"); // NON-NLS
			props.remove(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG);
			KafkaConsumer<String, TraceCommandDeserializer.TopicTraceCommand> consumer = new KafkaConsumer<>(props,
					new StringDeserializer(), new TraceCommandDeserializer());
			TopicPartition topic = new TopicPartition(MsgTraceReporter.TNT_TRACE_CONFIG_TOPIC, 0);
			consumer.assign(Collections.singletonList(topic));
			while (true) {
				ConsumerRecords<String, TraceCommandDeserializer.TopicTraceCommand> records = consumer.poll(100);
				if (records.count() > 0) {
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
							"MsgTraceReporter.polled.commands", records.count());
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

	protected Boolean shouldSendTrace(String topic, boolean count) {
		TraceCommandDeserializer.TopicTraceCommand topicTraceConfig = traceConfig.get(topic);
		if (topicTraceConfig == null) {
			topicTraceConfig = traceConfig.get(TraceCommandDeserializer.MASTER_CONFIG);
		}
		if (topic == null || topicTraceConfig == null) {
			return false;
		} else {
			return topicTraceConfig.match(topic, count);
		}
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

				stream.getOutput().logItem(ai);
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
				ai.setFieldValue(new ActivityField(StreamFieldType.StartTime.name()), recordMetadata.timestamp());
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

				stream.getOutput().logItem(ai);
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
		ActivityInfo ai;
		for (ConsumerRecord<Object, Object> cr : consumerRecords) {
			if (cr == null) {
				continue;
			}
			if (shouldSendTrace(cr.topic(), true)) {
				try {
					ai = new ActivityInfo();
					ai.setFieldValue(new ActivityField(StreamFieldType.EventType.name()), OpType.RECEIVE);
					ai.setFieldValue(new ActivityField(StreamFieldType.EventName.name()),
							"Kafka_Consumer_Consume_Record"); // NON-NLS
					ai.setFieldValue(new ActivityField("Topic"), cr.topic()); // NON-NLS
					ai.setFieldValue(new ActivityField("Partition"), cr.partition()); // NON-NLS
					ai.setFieldValue(new ActivityField("Offset"), cr.offset()); // NON-NLS
					ai.setFieldValue(new ActivityField(StreamFieldType.StartTime.name()), cr.timestamp());
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

					stream.getOutput().logItem(ai);
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
		ActivityInfo ai;
		for (Map.Entry<TopicPartition, OffsetAndMetadata> me : map.entrySet()) {
			if (me == null) {
				continue;
			}
			if (shouldSendTrace(me.getKey().topic(), false)) {
				try {
					ai = new ActivityInfo();
					ai.setFieldValue(new ActivityField(StreamFieldType.EventType.name()), OpType.EVENT);
					ai.setFieldValue(new ActivityField(StreamFieldType.EventName.name()),
							"Kafka_Consumer_Commit_Entry"); // NON-NLS
					ai.setFieldValue(new ActivityField("Partition"), me.getKey().partition()); // NON-NLS
					ai.setFieldValue(new ActivityField("Topic"), me.getKey().topic()); // NON-NLS
					ai.setFieldValue(new ActivityField("Offset"), me.getValue().offset()); // NON-NLS
					ai.setFieldValue(new ActivityField("Metadata"), me.getValue().metadata()); // NON-NLS
					// ai.setFieldValue(new ActivityField(StreamFieldType.TrackingId.name()),
					// calcSignature(me.getKey().topic(), me.getKey().partition(), me.getValue().offset()));
					ai.addCorrelator(me.getKey().topic(), String.valueOf(me.getValue().offset()));

					stream.getOutput().logItem(ai);
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
