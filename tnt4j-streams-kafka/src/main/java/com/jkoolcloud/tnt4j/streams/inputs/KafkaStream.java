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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

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
 * This activity stream supports the following properties (in addition to those supported by
 * {@link TNTParseableInputStream}):
 * <ul>
 * <li>Topic - topic name to listen. (Required)</li>
 * <li>StartServer - flag indicating if stream has to start Kafka server on startup. Default value - {@code false}.
 * (Optional)</li>
 * <li>StartZookeeper - flag indicating if stream has to start Zookeeper server on startup. Default value -
 * {@code false}. (Optional)</li>
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

	private static final String PROP_SCOPE_COMMON = "common"; // NON-NLS
	private static final String PROP_SCOPE_SERVER = "server"; // NON-NLS
	private static final String PROP_SCOPE_CONSUMER = "consumer"; // NON-NLS

	private static final String ZK_PROP_FILE_KEY = "tnt4j.zookeeper.config"; // NON-NLS
	private static final String KS_PROP_FILE_KEY = "tnt4j.kafka.srv.config"; // NON-NLS

	private final AtomicBoolean closed = new AtomicBoolean(false);

	private ConsumerConnector consumer;
	private String topicName;

	private ServerCnxnFactory zkCnxnFactory;
	private boolean startZookeeper = false;
	private KafkaServer server;
	private boolean startServer = false;

	private Iterator<MessageAndMetadata<byte[], byte[]>> messageBuffer;

	private Map<String, Properties> userKafkaProps;

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
		userKafkaProps = new HashMap<String, Properties>(3);
		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
				topicName = value;
			} else if (StreamProperties.PROP_START_SERVER.equalsIgnoreCase(name)) {
				startServer = Boolean.parseBoolean(value);
			} else if (KafkaStreamConstants.PROP_START_ZOOKEEPER.equalsIgnoreCase(name)) {
				startZookeeper = Boolean.parseBoolean(value);
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
					addUserKafkaProperty(name, value);
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

		if (KafkaStreamConstants.PROP_START_ZOOKEEPER.equalsIgnoreCase(name)) {
			return startZookeeper;
		}

		Object prop = super.getProperty(name);
		if (prop == null) {
			prop = getUserKafkaProperty(name);
		}

		return prop;
	}

	private Object addUserKafkaProperty(String pName, String pValue) {
		if (StringUtils.isEmpty(pName)) {
			return null;
		}

		String[] pParts = tokenizePropertyName(pName);

		Properties sProps = userKafkaProps.get(pParts[0]);
		if (sProps == null) {
			sProps = new Properties();
			userKafkaProps.put(pParts[0], sProps);
		}

		return sProps.setProperty(pParts[1], pValue);
	}

	private String getUserKafkaProperty(String pName) {
		if (StringUtils.isEmpty(pName)) {
			return null;
		}

		String[] pParts = tokenizePropertyName(pName);
		Properties sProperties = userKafkaProps.get(pParts[0]);

		return sProperties == null ? null : sProperties.getProperty(pParts[1]);
	}

	private static String[] tokenizePropertyName(String pName) {
		if (StringUtils.isEmpty(pName)) {
			return null;
		}

		int sIdx = pName.indexOf(':');
		String[] pParts = new String[2];

		if (sIdx >= 0) {
			pParts[0] = pName.substring(0, sIdx);
			pParts[1] = pName.substring(sIdx + 1);
		} else {
			pParts[1] = pName;
		}

		if (StringUtils.isEmpty(pParts[0])) {
			pParts[0] = PROP_SCOPE_COMMON;
		}

		return pParts;
	}

	private Properties getScopeProps(String scope) {
		Properties allScopeProperties = new Properties();

		Properties sProperties = userKafkaProps.get(scope);
		if (sProperties != null) {
			allScopeProperties.putAll(sProperties);
		}

		sProperties = userKafkaProps.get(PROP_SCOPE_COMMON);
		if (sProperties != null) {
			allScopeProperties.putAll(sProperties);
		}

		return allScopeProperties;
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		if (StringUtils.isEmpty(topicName)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", StreamProperties.PROP_TOPIC_NAME));
		}

		if (startServer) {
			if (startZookeeper) {
				startZookeeper();
			}

			logger().log(OpLevel.DEBUG, StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
					"KafkaStream.server.starting"));

			Properties srvProp = getServerProperties(getScopeProps(PROP_SCOPE_SERVER));
			server = new KafkaServer(new KafkaConfig(srvProp), SystemTime$.MODULE$);
			server.startup();

			logger().log(OpLevel.DEBUG, StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
					"KafkaStream.server.started"));
		}

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME, "KafkaStream.consumer.starting"));

		consumer = Consumer.createJavaConsumerConnector(new ConsumerConfig(getScopeProps(PROP_SCOPE_CONSUMER)));
	}

	private void startZookeeper() throws Exception {
		logger().log(OpLevel.DEBUG, StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
				"KafkaStream.zookeeper.server.starting"));

		Properties zkProps = loadPropertiesFile(ZK_PROP_FILE_KEY);

		QuorumPeerConfig conf = new QuorumPeerConfig();
		conf.parseProperties(zkProps);

		ServerConfig sc = new ServerConfig();
		sc.readFrom(conf);

		ZooKeeperServer zkServer = new ZooKeeperServer(new File(sc.getDataLogDir()), new File(sc.getDataDir()),
				sc.getTickTime());
		zkServer.setMinSessionTimeout(sc.getMinSessionTimeout());
		zkServer.setMaxSessionTimeout(sc.getMaxSessionTimeout());
		zkCnxnFactory = ServerCnxnFactory.createFactory(sc.getClientPortAddress(), sc.getMaxClientCnxns());
		zkCnxnFactory.startup(zkServer);

		logger().log(OpLevel.DEBUG, StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
				"KafkaStream.zookeeper.server.started"));
	}

	private static Properties getServerProperties(Properties userDefinedProps) throws IOException {
		putIfAbsent(userDefinedProps, "zookeeper.connect", "localhost:2181/tnt4j_kafka"); // NON-NLS

		Properties fProps = loadPropertiesFile(KS_PROP_FILE_KEY);

		for (Map.Entry<?, ?> pe : fProps.entrySet()) {
			putIfAbsent(userDefinedProps, String.valueOf(pe.getKey()), pe.getValue());
		}

		return userDefinedProps;
	}

	private static boolean putIfAbsent(Properties props, String key, Object value) {
		if (StringUtils.isEmpty(props.getProperty(key))) {
			props.put(key, String.valueOf(value));

			return true;
		}

		return false;
	}

	private static Properties loadPropertiesFile(String propKey) throws IOException {
		String configFile = System.getProperty(propKey);
		Properties fProps = new Properties();

		InputStream is = null;
		try {
			is = new FileInputStream(new File(configFile));
			fProps.load(is);
		} finally {
			Utils.close(is);
		}

		return fProps;
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
		while (!closed.get() && !isHalted()) {
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
				logger().log(OpLevel.DEBUG, StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME,
						"KafkaStream.retrieving.messages.timeout"));
			}
		}
		logger().log(OpLevel.INFO,
				StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME, "KafkaStream.stopping"));
		return null;
	}

	@Override
	protected void cleanup() {
		if (server != null) {
			server.shutdown();
			server.awaitShutdown();
		}

		if (zkCnxnFactory != null) {
			zkCnxnFactory.shutdown();
		}

		closed.set(true);
		if (consumer != null) {
			consumer.shutdown();
		}

		super.cleanup();
	}
}
