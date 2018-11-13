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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.collections4.MapUtils;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.ProducerConfig;
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
import com.jkoolcloud.tnt4j.streams.configure.sax.ConfigParserHandler;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.InterceptionsManager;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.InterceptionsReporter;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.fields.StreamFieldType;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.KafkaStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.uuid.DefaultUUIDFactory;

/**
 * Producer/Consumer interceptors intercepted messages reporter sending jKoolCloud events containing intercepted message
 * payload data, metadata and context data.
 * <p>
 * jKool Event types sent on consumer/producer interceptions:
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

	/**
	 * Constant defining tracing configuration dedicated topic name.
	 */
	public static final String TNT_TRACE_CONFIG_TOPIC = "TNT_TRACE_CONFIG_TOPIC"; // NON-NLS
	/**
	 * Constant defining tracing configuration dedicated topic polling interval in seconds.
	 */
	public static final int POOL_TIME_SECONDS = 3;
	/**
	 * Constant defining interceptor message tracing configuration properties prefix.
	 */
	public static final String TRACER_PROPERTY_PREFIX = "messages.tracer.kafka."; // NON-NLS
	/**
	 * Constant defining interceptor parsers configuration file name.
	 */
	public static final String DEFAULT_PARSER_CONFIG_FILE = "tnt-data-source-interceptor.xml"; // NON-NLS

	private ActivityParser mainParser;

	private KafkaObjTraceStream<ActivityInfo> stream;
	private Map<String, TraceCommandDeserializer.TopicTraceCommand> traceConfig = new HashMap<>();
	private Timer pollTimer;

	private static KafkaConsumer<String, TraceCommandDeserializer.TopicTraceCommand> consumer;

	/**
	 * Constructs a new MsgTraceReporter.
	 */
	public MsgTraceReporter(Properties kafkaProperties) {
		this(new KafkaObjTraceStream<ActivityInfo>(), kafkaProperties, true);
	}

	/**
	 * Constructs a new MsgTraceReporter.
	 *
	 * @param stream
	 *            trace stream instance
	 * @param interceptorProperties
	 *            Kafka interceptor configuration properties
	 * @param enableCfgPolling
	 *            flag indicating whether to enable tracing configuration pooling from dedicated Kafka topic
	 */
	MsgTraceReporter(KafkaObjTraceStream<ActivityInfo> stream, final Properties interceptorProperties,
			boolean enableCfgPolling) {
		this.stream = stream;
		mainParser = getParser("KafkaTraceParser"); // NON-NLS

		StreamsAgent.runFromAPI(stream);
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"MsgTraceReporter.stream.started", stream.getName());

		if (enableCfgPolling) {
			TimerTask mrt = new TimerTask() {
				@Override
				public void run() {
					Map<String, Map<String, ?>> consumersCfg = InterceptionsManager.getInstance()
							.getInterceptorsConfig(TNTKafkaCInterceptor.class);
					Map<String, ?> cConfig = MapUtils.isEmpty(consumersCfg) ? null
							: consumersCfg.entrySet().iterator().next().getValue();
					pollConfigQueue(cConfig, interceptorProperties, traceConfig);
				}
			};
			traceConfig.put(TraceCommandDeserializer.MASTER_CONFIG, new TraceCommandDeserializer.TopicTraceCommand());
			long period = TimeUnit.SECONDS.toMillis(POOL_TIME_SECONDS);
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MsgTraceReporter.schedule.commands.polling", TNT_TRACE_CONFIG_TOPIC, period, period);
			pollTimer = new Timer();
			pollTimer.scheduleAtFixedRate(mrt, period, period);
		}
	}

	/**
	 * Loads parser name having provided {@code name} from interceptor parsers configuration file.
	 *
	 * @param name
	 *            parser name
	 *
	 * @return parser instance having provided name
	 */
	protected static ActivityParser getParser(String name) {
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			ConfigParserHandler hndlr = new ConfigParserHandler();

			InputStream is;

			if (new File(DEFAULT_PARSER_CONFIG_FILE).exists()) {
				is = new FileInputStream(DEFAULT_PARSER_CONFIG_FILE);
			} else {
				is = Utils.getResourceAsStream(MsgTraceReporter.class, DEFAULT_PARSER_CONFIG_FILE);
			}
			parser.parse(is, hndlr);
			return hndlr.getStreamsConfigData().getParser(name);
		} catch (Exception e) {
			throw new RuntimeException("Can't start interceptor: " + e.getMessage() + " cause: " + e.getCause());
		}
	}

	/**
	 * Merges Kafka consumer, messages interceptor file and topic provided properties and puts them all into complete
	 * tracing configuration map {@code traceConfig}.
	 *
	 * @param config
	 *            Kafka consumer interceptor configuration properties map
	 * @param interceptorProperties
	 *            Kafka interceptor configuration properties
	 * @param traceConfig
	 *            complete message tracing configuration properties
	 */
	protected static void pollConfigQueue(Map<String, ?> config, Properties interceptorProperties,
			Map<String, TraceCommandDeserializer.TopicTraceCommand> traceConfig) {
		Properties props = new Properties();
		if (config != null) {
			props.putAll(config);
		}
		if (interceptorProperties != null) {
			props.putAll(extractKafkaProperties(interceptorProperties));
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

	/**
	 * Extracts message tracing specific configuration properties from interceptor configuration.
	 *
	 * @param interceptorProperties
	 *            Kafka interceptor configuration properties
	 *
	 * @return interceptor message tracing configuration properties
	 */
	protected static Properties extractKafkaProperties(Properties interceptorProperties) {
		Properties props = new Properties();
		for (String key : interceptorProperties.stringPropertyNames()) {
			if (key.startsWith(TRACER_PROPERTY_PREFIX)) {
				props.put(key.substring(TRACER_PROPERTY_PREFIX.length()), interceptorProperties.getProperty(key));
			}
		}
		return props;
	}

	/**
	 * Checks tracing configuration whether message lifecycle event shall be traced by interceptor.
	 *
	 * @param topic
	 *            topic name event received from
	 * @param count
	 *            events counts
	 *
	 * @return {@code true} if message should be traced, {@code false} - otherwise
	 */
	protected boolean shouldSendTrace(String topic, boolean count) {
		TraceCommandDeserializer.TopicTraceCommand topicTraceConfig = traceConfig.get(topic);
		if (topicTraceConfig == null) {
			topicTraceConfig = traceConfig.get(TraceCommandDeserializer.MASTER_CONFIG);
		}

		boolean send = (topic != null && topicTraceConfig != null) && topicTraceConfig.match(topic, count);
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
				KafkaTraceEventData kafkaTraceData = new KafkaTraceEventData(producerRecord,
						MapUtils.getString(interceptor.getConfig(), ProducerConfig.CLIENT_ID_CONFIG));

				stream.addInputToBuffer(mainParser.parse(stream, kafkaTraceData));
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
				KafkaTraceEventData kafkaTraceData = new KafkaTraceEventData(recordMetadata, e, clusterResource,
						MapUtils.getString(interceptor.getConfig(), ProducerConfig.CLIENT_ID_CONFIG));
				kafkaTraceData.setSignature(
						calcSignature(recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset()));
				stream.addInputToBuffer(mainParser.parse(stream, kafkaTraceData));
			} catch (Exception exc) {
				Utils.logThrowable(LOGGER, OpLevel.ERROR,
						StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
						"MsgTraceReporter.acknowledge.failed", exc);
				exc.printStackTrace();
			}
		}
	}

	private static String createCorrelator(String topic, long offset) {
		return topic + "_" + offset; // NON-NLS
	}

	/**
	 * Appends resource related fields to provided activity info instance {@code ai}.
	 *
	 * @param ai
	 *            activity info instance
	 * @param topic
	 *            topic name
	 * @param appName
	 *            application name
	 *
	 * @throws ParseException
	 *             if fails to set activity info value
	 */
	protected static void appendResourceFields(ActivityInfo ai, String topic, String appName) throws ParseException {
		ai.setFieldValue(new ActivityField(StreamFieldType.ResourceName.name()), "QUEUE=" + topic); // NON-NLS
		ai.setFieldValue(new ActivityField(StreamFieldType.ApplName.name()), appName);
	}

	@Override
	public void consume(TNTKafkaCInterceptor interceptor, ConsumerRecords<Object, Object> consumerRecords,
			ClusterResource clusterResource) {
		if (consumerRecords == null) {
			return;
		}
		String tid = null;
		ActivityInfo ai = null;
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
					KafkaTraceEventData kafkaTraceData = new KafkaTraceEventData(cr,
							MapUtils.getString(interceptor.getConfig(), ProducerConfig.CLIENT_ID_CONFIG));
					kafkaTraceData.setParentId(tid);
					kafkaTraceData.setSignature(calcSignature(cr.topic(), cr.partition(), cr.offset()));
					stream.addInputToBuffer(mainParser.parse(stream, kafkaTraceData));
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
		if (map == null || map.isEmpty()) {
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
					KafkaTraceEventData kafkaTraceData = new KafkaTraceEventData(me.getKey(), me.getValue(),
							MapUtils.getString(interceptor.getConfig(), ProducerConfig.CLIENT_ID_CONFIG));
					kafkaTraceData.setParentId(tid);
					stream.addInputToBuffer(mainParser.parse(stream, kafkaTraceData));
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

		if (pollTimer != null) {
			pollTimer.cancel();
		}
	}

	private final MessageDigest MSG_DIGEST = Utils.getMD5Digester();

	/**
	 * Generates a new unique message event signature.
	 *
	 * @param topic
	 *            topic name
	 * @param partition
	 *            partition index
	 * @param offset
	 *            offset index
	 *
	 * @return unique message event signature
	 */
	protected String calcSignature(String topic, int partition, long offset) {
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
