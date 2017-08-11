/*
 * Copyright 2014-2017 JKOOL, LLC.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.KafkaStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * TODO
 *
 * @version $Revision: 1 $
 */
public class KafkaStreamC extends AbstractBufferedStream<ConsumerRecord<?, ?>> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(KafkaStreamC.class);

	/**
	 * Kafka server/consumer properties scope mapping key.
	 */
	protected static final String PROP_SCOPE_COMMON = "common"; // NON-NLS
	/**
	 * Kafka consumer properties scope mapping key.
	 */
	protected static final String PROP_SCOPE_CONSUMER = "consumer"; // NON-NLS

	private String topicName;

	private Map<String, Properties> userKafkaProps;

	private KafkaDataReceiver kafkaDataReceiver;

	/**
	 * Constructs a new KafkaStreamC.
	 */
	public KafkaStreamC() {
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
		userKafkaProps = new HashMap<>(3);
		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
				topicName = value;
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

		Object prop = super.getProperty(name);
		if (prop == null) {
			prop = getUserKafkaProperty(name);
		}

		return prop;
	}

	/**
	 * Adds Kafka configuration property to user defined (from stream configuration) properties map.
	 *
	 * @param pName
	 *            fully qualified property name
	 * @param pValue
	 *            property value
	 * @return the previous value of the specified property in user's Kafka configuration property list, or {@code null}
	 *         if it did not have one
	 */
	protected Object addUserKafkaProperty(String pName, String pValue) {
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

	/**
	 * Gets user defined (from stream configuration) Kafka server configuration property value.
	 *
	 * @param pName
	 *            fully qualified property name
	 * @return property value, or {@code null} if property is not set
	 */
	protected String getUserKafkaProperty(String pName) {
		if (StringUtils.isEmpty(pName)) {
			return null;
		}

		String[] pParts = tokenizePropertyName(pName);
		Properties sProperties = userKafkaProps.get(pParts[0]);

		return sProperties == null ? null : sProperties.getProperty(pParts[1]);
	}

	/**
	 * Splits fully qualified property name to property scope and name.
	 *
	 * @param pName
	 *            fully qualified property name
	 * @return string array containing property scope and name
	 */
	protected static String[] tokenizePropertyName(String pName) {
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

	/**
	 * Returns scope defined properties set.
	 *
	 * @param scope
	 *            properties scope key
	 * @return scope defined properties
	 */
	protected Properties getScopeProps(String scope) {
		Properties allScopeProperties = new Properties();

		Properties sProperties = userKafkaProps.get(scope);
		if (sProperties != null) {
			allScopeProperties.putAll(sProperties);
		}

		if (!PROP_SCOPE_COMMON.equals(scope)) {
			sProperties = userKafkaProps.get(PROP_SCOPE_COMMON);
			if (sProperties != null) {
				allScopeProperties.putAll(sProperties);
			}
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

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME, "KafkaStream.consumer.starting"));

		kafkaDataReceiver = new KafkaDataReceiver();
		kafkaDataReceiver.initialize(getScopeProps(PROP_SCOPE_CONSUMER), Collections.singleton(topicName));
	}

	@Override
	protected void start() throws Exception {
		super.start();

		kafkaDataReceiver.start();

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.stream.start"),
				getClass().getSimpleName(), getName());
	}

	@Override
	protected long getActivityItemByteSize(ConsumerRecord<?, ?> activityItem) {
		int size = Math.max(activityItem.serializedKeySize(), 0) + Math.max(activityItem.serializedValueSize(), 0);

		return size;
	}

	@Override
	public boolean isInputEnded() {
		return kafkaDataReceiver.isInputEnded();
	}

	@Override
	protected void cleanup() {
		if (kafkaDataReceiver != null) {
			kafkaDataReceiver.shutdown();
		}

		super.cleanup();
	}

	private class KafkaDataReceiver extends InputProcessor {

		private Consumer<?, ?> consumer;
		private Collection<String> topics;

		private KafkaDataReceiver() {
			super("KafkaStreamC.KafkaDataReceiver");
		}

		/**
		 * Input data receiver initialization - Kafka consumer configuration.
		 *
		 * @param params
		 *            initialization parameters array
		 *
		 * @throws Exception
		 *             if fails to initialize data receiver and configure Kafka consumer
		 */
		@Override
		@SuppressWarnings("unchecked")
		protected void initialize(Object... params) throws Exception {
			Properties cProperties = (Properties) params[0];
			topics = (Collection<String>) params[1];

			consumer = new KafkaConsumer<>(cProperties);
		}

		/**
		 * Starts Kafka consumer client to receive incoming data. Shuts down this data receiver if exception occurs.
		 */
		@Override
		public void run() {
			if (consumer != null) {
				try {
					consumer.subscribe(topics);

					while (!isHalted()) {
						ConsumerRecords<?, ?> records = consumer.poll(Long.MAX_VALUE);
						for (ConsumerRecord<?, ?> record : records) {
							String msgData = Utils.toString(record.value());
							logger().log(OpLevel.DEBUG, StreamsResources.getString(
									KafkaStreamConstants.RESOURCE_BUNDLE_NAME, "KafkaStream.next.message"), msgData);

							addInputToBuffer(record);
						}
					}
				} catch (WakeupException exc) {
				} finally {
					// consumer.unsubscribe();
					consumer.close();
				}
			}
		}

		/**
		 * Closes Kafka consume.
		 */
		@Override
		void closeInternals() {
			if (consumer != null) {
				consumer.wakeup();
			}
		}
	}
}
