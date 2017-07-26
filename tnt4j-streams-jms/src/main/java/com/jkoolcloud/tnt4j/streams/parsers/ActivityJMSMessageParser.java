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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.*;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.JMSParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.StreamFieldType;
import com.jkoolcloud.tnt4j.streams.utils.JMSStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements an activity data parser that assumes each activity data item is an JMS message data structure. Message
 * payload data is put into map entry using key defined in {@link StreamsConstants#ACTIVITY_DATA_KEY}. This parser
 * supports JMS messages of those types:
 * <ul>
 * <li>{@link TextMessage} - activity data is message text</li>
 * <li>{@link BytesMessage} - activity data is message {@code byte[]} or string made from message {@code byte[]}
 * depending on property 'ConvertToString' value</li>
 * <li>{@link MapMessage} - activity data is message map entries</li>
 * <li>{@link StreamMessage} - activity data is message {@code byte[]} or string made from message {@code byte[]}
 * depending on property 'ConvertToString' value</li>
 * <li>{@link ObjectMessage} - activity data is message serializable object</li>
 * </ul>
 * <p>
 * NOTE: Custom messages parsing not implemented and puts just log entry.
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link AbstractActivityMapParser}):
 * <ul>
 * <li>ConvertToString - flag indicating whether to convert message payload {@code byte[]} data to string. Applicable to
 * {@link BytesMessage} and {@link StreamMessage}. Default value - 'false'. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityJMSMessageParser extends AbstractActivityMapParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityJMSMessageParser.class);

	private static final int BYTE_BUFFER_LENGTH = 1024;

	private boolean convertToString = false;

	/**
	 * Constructs a new ActivityJMSMessageParser.
	 */
	public ActivityJMSMessageParser() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link javax.jms.Message}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return Message.class.isInstance(data);
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();

			if (JMSParserProperties.PROP_CONV_TO_STRING.equalsIgnoreCase(name)) {
				convertToString = Boolean.parseBoolean(value);

				logger().log(OpLevel.DEBUG,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
						name, value);
			}
		}
	}

	/**
	 * Makes map object containing activity object data collected from JMS message payload data.
	 *
	 * @param data
	 *            activity object data object - JMS message
	 *
	 * @return activity object data map
	 */
	@Override
	protected Map<String, ?> getDataMap(Object data) {
		if (data == null) {
			return null;
		}

		Message message = (Message) data;
		Map<String, Object> dataMap = new HashMap<>();

		try {
			if (message instanceof TextMessage) {
				parseTextMessage((TextMessage) message, dataMap);
			} else if (message instanceof BytesMessage) {
				parseBytesMessage((BytesMessage) message, dataMap);
			} else if (message instanceof MapMessage) {
				parseMapMessage((MapMessage) message, dataMap);
			} else if (message instanceof StreamMessage) {
				parseStreamMessage((StreamMessage) message, dataMap);
			} else if (message instanceof ObjectMessage) {
				parseObjectMessage((ObjectMessage) message, dataMap);
			} else {
				parseCustomMessage(message, dataMap);
			}

			dataMap.put(StreamFieldType.Correlator.name(), message.getJMSCorrelationID());
		} catch (JMSException exc) {
			logger().log(OpLevel.ERROR, StreamsResources.getString(JMSStreamConstants.RESOURCE_BUNDLE_NAME,
					"ActivityJMSMessageParser.payload.data.error"), exc);
		}

		if (!dataMap.isEmpty()) {
			dataMap.put(StreamsConstants.TRANSPORT_KEY, JMSStreamConstants.TRANSPORT_JMS);
		}

		return dataMap;
	}

	/**
	 * Parse JMS {@link TextMessage} activity info into activity data map.
	 *
	 * @param textMessage
	 *            JMS text message
	 * @param dataMap
	 *            activity data map collected from JMS {@link TextMessage}
	 * @throws JMSException
	 *             if JMS exception occurs while getting text from message.
	 */
	protected void parseTextMessage(TextMessage textMessage, Map<String, Object> dataMap) throws JMSException {
		String text = textMessage.getText();
		if (StringUtils.isNotEmpty(text)) {
			dataMap.put(StreamsConstants.ACTIVITY_DATA_KEY, text);
		}
	}

	/**
	 * Parse JMS {@link BytesMessage} activity info into activity data map.
	 *
	 * @param bytesMessage
	 *            JMS bytes message
	 * @param dataMap
	 *            activity data map collected from JMS {@link BytesMessage}
	 * @throws JMSException
	 *             if JMS exception occurs while reading bytes from message.
	 */
	protected void parseBytesMessage(BytesMessage bytesMessage, Map<String, Object> dataMap) throws JMSException {
		byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
		bytesMessage.readBytes(bytes);

		if (ArrayUtils.isNotEmpty(bytes)) {
			dataMap.put(StreamsConstants.ACTIVITY_DATA_KEY, convertToString ? Utils.getString(bytes) : bytes);
		}
	}

	/**
	 * Parse JMS {@link MapMessage} activity info into activity data map.
	 *
	 * @param mapMessage
	 *            JMS map message
	 * @param dataMap
	 *            activity data map collected from JMS {@link MapMessage}
	 * @throws JMSException
	 *             if JMS exception occurs while getting map entries from message.
	 */
	@SuppressWarnings("unchecked")
	protected void parseMapMessage(MapMessage mapMessage, Map<String, Object> dataMap) throws JMSException {
		Enumeration<String> en = (Enumeration<String>) mapMessage.getMapNames();
		while (en.hasMoreElements()) {
			String key = en.nextElement();
			dataMap.put(key, mapMessage.getObject(key));
		}
	}

	/**
	 * Parse JMS {@link StreamMessage} activity info into activity data map.
	 *
	 * @param streamMessage
	 *            JMS stream message
	 * @param dataMap
	 *            activity data map collected from JMS {@link StreamMessage}
	 * @throws JMSException
	 *             if JMS exception occurs while reading bytes from message.
	 */
	protected void parseStreamMessage(StreamMessage streamMessage, Map<String, Object> dataMap) throws JMSException {
		streamMessage.reset();

		byte[] buffer = new byte[BYTE_BUFFER_LENGTH];

		int bytesRead = 0;
		ByteArrayOutputStream baos = new ByteArrayOutputStream(buffer.length);

		try {
			do {
				bytesRead = streamMessage.readBytes(buffer);

				baos.write(buffer);
			} while (bytesRead != 0);
		} catch (IOException exc) {
			logger().log(OpLevel.ERROR, StreamsResources.getString(JMSStreamConstants.RESOURCE_BUNDLE_NAME,
					"ActivityJMSMessageParser.bytes.buffer.error"), exc);
		}

		byte[] bytes = baos.toByteArray();
		Utils.close(baos);

		if (ArrayUtils.isNotEmpty(bytes)) {
			dataMap.put(StreamsConstants.ACTIVITY_DATA_KEY, convertToString ? Utils.getString(bytes) : bytes);
		}
	}

	/**
	 * Parse JMS {@link ObjectMessage} activity info into activity data map.
	 *
	 * @param objMessage
	 *            JMS object message
	 * @param dataMap
	 *            activity data map collected from JMS {@link ObjectMessage}
	 * @throws JMSException
	 *             if JMS exception occurs while getting {@link Serializable} object from message.
	 */
	protected void parseObjectMessage(ObjectMessage objMessage, Map<String, Object> dataMap) throws JMSException {
		Serializable serializableObj = objMessage.getObject();
		if (serializableObj != null) {
			dataMap.put(StreamsConstants.ACTIVITY_DATA_KEY, serializableObj);
		}
	}

	/**
	 * Parse custom message activity info into activity data map.
	 *
	 * @param message
	 *            custom JMS message
	 * @param dataMap
	 *            activity data map collected from custom JMS message
	 * @throws JMSException
	 *             if any JMS exception occurs while parsing message.
	 */
	protected void parseCustomMessage(Message message, Map<String, Object> dataMap) throws JMSException {
		logger().log(OpLevel.WARNING, StreamsResources.getString(JMSStreamConstants.RESOURCE_BUNDLE_NAME,
				"ActivityJMSMessageParser.parsing.custom.jms.message"));
	}

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - JMS MESSAGE
	 */
	@Override
	protected String getActivityDataType() {
		return "JMS MESSAGE"; // NON-NLS
	}
}
