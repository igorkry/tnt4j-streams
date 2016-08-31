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

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.CollectionUtils;
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
import kafka.consumer.Whitelist;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.SystemTime$;

/**
 * <p>
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
 * <li>Topic - regex of topic name to listen. (Required)</li>
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
	private ConsumerConfig kafkaProperties;
	private String topicNameRegex;

	private KafkaServer server;
	private boolean startServer = false;

	private Iterator<MessageAndMetadata<byte[], byte[]>> messageBuffer;

	/**
	 * Constructs a new KafkaStream.
	 */
	public KafkaStream() {
		super(LOGGER);
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}
		Properties properties = new Properties();
		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
				topicNameRegex = value;
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
					properties.put(name, value);
				}
			}
		}
		kafkaProperties = new ConsumerConfig(properties);
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
			return topicNameRegex;
		}

		if (StreamProperties.PROP_START_SERVER.equalsIgnoreCase(name)) {
			return startServer;
		}

		Object prop = super.getProperty(name);
		if (prop == null) {
			prop = kafkaProperties.props().getProperty(name);
		}

		return prop;
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();
		if (StringUtils.isEmpty(topicNameRegex)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", StreamProperties.PROP_TOPIC_NAME));
		}

		if (startServer) {
			// TODO: properties should come from stream or separate config file.
			Properties serverProps = new Properties();

			serverProps.setProperty("broker.id", String.valueOf(969696));
			// serverProps.setProperty("host.name", InetAddress.getLocalHost().getHostName());
			serverProps.setProperty("port", String.valueOf(6667));

			// serverProps.setProperty("log.dir", new File(".").getAbsolutePath());
			serverProps.setProperty("log.flush.interval.messages", String.valueOf(1));
			serverProps.setProperty("log.flush.scheduler.interval.ms", String.valueOf(1));
			serverProps.setProperty("log.flush.interval.ms", String.valueOf(1));

			serverProps.setProperty("message.max.bytes", String.valueOf(50 * 1024 * 1024));
			serverProps.setProperty("replica.fetch.max.bytes", String.valueOf(50 * 1024 * 1024));

			serverProps.setProperty("zookeeper.connect", "127.0.0.1:2181");

			// for CI stability, increase zookeeper session timeout
			serverProps.setProperty("zookeeper.session.timeout.ms", String.valueOf(20000));

			server = new KafkaServer(new KafkaConfig(serverProps), SystemTime$.MODULE$);
			server.startup();
		}

		// TODO: if server has started then consumer should listen to it.
		consumer = Consumer.createJavaConsumerConnector(kafkaProperties);
		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME, "KafkaStream.stream.ready"));
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
			if (messageBuffer == null || !messageBuffer.hasNext()) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
						"KafkaStream.empty.messages.buffer"));
				final List<kafka.consumer.KafkaStream<byte[], byte[]>> streams = consumer
						.createMessageStreamsByFilter(new Whitelist(topicNameRegex));
				if (CollectionUtils.isNotEmpty(streams)) {
					kafka.consumer.KafkaStream<byte[], byte[]> stream = streams.get(0);
					messageBuffer = stream.iterator();
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
							"KafkaStream.retrieved.new.messages"), stream.size());
				} else {
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
							"KafkaStream.retrieved.no.new.messages"));
				}
			}

			if (messageBuffer != null && messageBuffer.hasNext()) {
				MessageAndMetadata<byte[], byte[]> msg = messageBuffer.next();
				byte[] msgPayload = msg.message();
				String msgData = Utils.getString(msgPayload);

				LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
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
		}
		LOGGER.log(OpLevel.ERROR,
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
