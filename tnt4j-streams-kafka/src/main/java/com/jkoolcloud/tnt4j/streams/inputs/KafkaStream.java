/*
 * Copyright 2014-2016 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser;
import com.jkoolcloud.tnt4j.streams.utils.KafkaStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerTimeoutException;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.SystemTime$;

/**
 * Implements a Kafka topics transmitted activity stream, where each message body is assumed to represent a single
 * activity or event which should be recorded. Topic to listen is defined using "Topic" property in stream
 * configuration.
 * <p>
 * This activity stream requires parsers that can support {@link Map} data. On message reception message data is packed
 * into {@link Map} filling these entries:
 * <ul>
 * <li>ActivityTopic - topic name message with activity data was received.</li>
 * <li>ActivityData - raw activity data as {@code byte[]} retrieved from message.</li>
 * <li>ActivityTransport - activity transport definition: 'Kafka'.</li>
 * </ul>
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>Topic - topic name to listen. (Required)</li>
 * <li>StartServer - flag indicating if stream has to start Kafka server on startup. Default value - {@code false}.
 * (Optional)</li>
 * <li>List of properties used by Kafka API. i.e zookeeper.connect, group.id. See {@link kafka.consumer.ConsumerConfig}
 * for more details on Kafka consumer properties. @see <a href="https://kafka.apache.org/08/configuration.html">Kafka
 * configuration reference</a></li>.
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see ActivityMapParser
 * @see kafka.consumer.ConsumerConfig
 * @see kafka.server.KafkaServer
 */
public class KafkaStream extends TNTParseableInputStream<Map<String, ?>> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(KafkaStream.class);

	private final AtomicBoolean closed = new AtomicBoolean(false);

	private ConsumerConnector consumer;
	private String topicName;

	private KafkaServer server;
	private boolean startServer = false;

	private Iterator<MessageAndMetadata<byte[], byte[]>> messageBuffer;

	private Properties userKafkaProps;

	/**
	 * Constructs a new KafkaStream.
	 */
	public KafkaStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}
		userKafkaProps = new Properties();
		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
				topicName = value;
			} else if (StreamProperties.PROP_START_SERVER.equalsIgnoreCase(name)) {
				startServer = Boolean.parseBoolean(value);
			} else {
				Field[] propFields = StreamProperties.class.getDeclaredFields();

				boolean streamsProperty = false;
				for (Field pf : propFields) {
					pf.setAccessible(true);
					if (pf.get(StreamProperties.class).toString().equalsIgnoreCase(name)) {
						streamsProperty = true;
						break;
					}
				}

				if (!streamsProperty) {
					userKafkaProps.put(name, value);
				}
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
			return topicName;
		}

		if (StreamProperties.PROP_START_SERVER.equalsIgnoreCase(name)) {
			return startServer;
		}

		Object prop = super.getProperty(name);
		if (prop == null) {
			prop = userKafkaProps.getProperty(name);
		}

		return prop;
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		if (StringUtils.isEmpty(topicName)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", StreamProperties.PROP_TOPIC_NAME));
		}

		if (startServer) {
			logger().log(OpLevel.DEBUG, StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
					"KafkaStream.server.starting"));

			Properties srvProp = getServerProperties(userKafkaProps);
			server = new KafkaServer(new KafkaConfig(srvProp), SystemTime$.MODULE$);
			server.startup();

			logger().log(OpLevel.DEBUG, StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
					"KafkaStream.server.started"));
		}

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME, "KafkaStream.consumer.starting"));

		consumer = Consumer.createJavaConsumerConnector(new ConsumerConfig(userKafkaProps));
	}

	private static Properties getServerProperties(Properties userDefinedProps) throws IOException {
		File kafkaLogDir = new File("kafka-logs");
		FileUtils.deleteDirectory(kafkaLogDir);

		Properties prop = userDefinedProps;
		putIfAbsent(prop, "log.dir", kafkaLogDir.getAbsolutePath()); // NON-NLS
		putIfAbsent(prop, "port", String.valueOf(9092)); // NON-NLS
		putIfAbsent(prop, "broker.id", String.valueOf(684231)); // NON-NLS
		putIfAbsent(prop, "host.name", "localhost"); // NON-NLS
		putIfAbsent(prop, "log.retention.hours", String.valueOf(24)); // NON-NLS
		putIfAbsent(prop, "log.flush.interval.messages", String.valueOf(1)); // NON-NLS
		putIfAbsent(prop, "log.flush.interval.ms", String.valueOf(1000)); // NON-NLS
		putIfAbsent(prop, "zookeeper.connect", "localhost:2181/tnt4j_kafka"); // NON-NLS
		putIfAbsent(prop, "advertised.host.name", "localhost"); // NON-NLS
		// Set the connection timeout to relatively short time (3 seconds).
		// It is only used by the org.I0Itec.zkclient.ZKClient inside KafkaServer
		// to block and wait for ZK connection goes into SyncConnected state.
		//
		// However, due to race condition described in TWILL-139 in the ZK client library used by Kafka,
		// when ZK authentication is enabled, the ZK client may hang until connection timeout.
		// Setting it to lower value allow the AM to retry multiple times if race happens.
		putIfAbsent(prop, "zookeeper.connection.timeout.ms", String.valueOf(3000)); // NON-NLS

		return prop;
	}

	private static boolean putIfAbsent(Properties props, String key, Object value) {
		if (StringUtils.isEmpty(props.getProperty(key))) {
			props.put(key, value);

			return true;
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns a map structured content of next raw activity data item received over Kafka consumer.
	 * Returned {@link Map} contains:
	 * <ul>
	 * <li>{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#TOPIC_KEY}</li>
	 * <li>{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#ACTIVITY_DATA_KEY}</li>
	 * <li>{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#TRANSPORT_KEY}</li>
	 * </ul>
	 */
	@Override
	public Map<String, ?> getNextItem() throws Exception {
		while (!closed.get()) {
			try {
				if (messageBuffer == null || !messageBuffer.hasNext()) {
					logger().log(OpLevel.DEBUG, StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
							"KafkaStream.empty.messages.buffer"));
					Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
					topicCountMap.put(topicName, 1);

					Map<String, List<kafka.consumer.KafkaStream<byte[], byte[]>>> streams = consumer
							.createMessageStreams(topicCountMap);

					if (MapUtils.isNotEmpty(streams)) {
						kafka.consumer.KafkaStream<byte[], byte[]> stream = streams.get(topicName).get(0);
						messageBuffer = stream.iterator();
						logger().log(OpLevel.DEBUG,
								StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
										"KafkaStream.retrieved.new.messages"),
								stream.size());
					} else {
						logger().log(OpLevel.DEBUG, StreamsResources.getString(
								KafkaStreamConstants.RESOURCE_BUNDLE_NAME, "KafkaStream.retrieved.no.new.messages"));
					}
				}

				if (messageBuffer != null && messageBuffer.hasNext()) {
					MessageAndMetadata<byte[], byte[]> msg = messageBuffer.next();
					byte[] msgPayload = msg.message();
					String msgData = Utils.getString(msgPayload);

					logger().log(OpLevel.DEBUG, StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
							"KafkaStream.next.message"), msgData);

					Map<String, Object> msgDataMap = new HashMap<String, Object>();

					if (ArrayUtils.isNotEmpty(msgPayload)) {
						msgDataMap.put(StreamsConstants.TOPIC_KEY, msg.topic());
						msgDataMap.put(StreamsConstants.ACTIVITY_DATA_KEY, msgPayload);
						msgDataMap.put(StreamsConstants.TRANSPORT_KEY, KafkaStreamConstants.TRANSPORT_KAFKA);

						addStreamedBytesCount(msgPayload.length);
					}

					return msgDataMap;
				}
			} catch (ConsumerTimeoutException e) {
				logger().log(OpLevel.INFO, StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
						"KafkaStream.retrieving.messages.timeout"));
			}
		}
		logger().log(OpLevel.ERROR,
				StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME, "KafkaStream.failed.consumer"));
		return null;
	}

	@Override
	protected void cleanup() {
		if (server != null) {
			server.shutdown();
			server.awaitShutdown();
		}

		closed.set(true);
		if (consumer != null) {
			consumer.shutdown();
		}

		super.cleanup();
	}
}
