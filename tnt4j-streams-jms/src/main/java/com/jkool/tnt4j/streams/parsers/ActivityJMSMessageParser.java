/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jkool.tnt4j.streams.parsers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.*;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.jkool.tnt4j.streams.utils.StreamsConstants;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements default activity data parser that assumes each activity data item
 * is an JMS message data structure. Message payload data is put into map entry
 * using key defined in {@code StreamsConstants.ACTIVITY_DATA_KEY}. This parser
 * supports JMS messages of those types:
 * <ul>
 * <li>TextMessage - activity data is message text</li>
 * <li>BytesMessage - activity data is string made from message bytes</li>
 * <li>MapMessage - activity data is message map entries</li>
 * <li>StreamMessage - activity data is string made from message bytes</li>
 * <li>ObjectMessage - activity data is message serializable object</li>
 * </ul>
 * </p>
 * </p>
 * Custom messages parsing not implemented and puts just log entry.
 * </p>
 *
 * @version $Revision: 1 $
 */
public class ActivityJMSMessageParser extends AbstractActivityMapParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityJMSMessageParser.class);

	private static final int BYTE_BUFFER_LENGTH = 1024;

	/**
	 * Constructs a new ActivityJMSMessageParser.
	 */
	public ActivityJMSMessageParser() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This parser supports the following class types (and all classes
	 * extending/implementing any of these):
	 * </p>
	 * <ul>
	 * <li>{@code javax.jms.Message}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return Message.class.isInstance(data);
	}

	/**
	 * Makes map object containing activity object data collected from JMS
	 * message payload data.
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
		Map<String, Object> dataMap = new HashMap<String, Object>();

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
		} catch (JMSException exc) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getString("ActivityJMSMessageParser.payload.data.error"), exc);
		}

		if (!dataMap.isEmpty()) {
			dataMap.put(StreamsConstants.TRANSPORT_KEY, StreamsConstants.TRANSPORT_JMS);
		}

		return dataMap;
	}

	/**
	 * Parse JMS {@code TextMessage} activity info into activity data map.
	 *
	 * @param textMessage
	 *            JMS text message
	 * @param dataMap
	 *            activity data map collected from JMS {@code TextMessage}
	 *
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
	 * Parse JMS {@code BytesMessage} activity info into activity data map.
	 *
	 * @param bytesMessage
	 *            JMS bytes message
	 * @param dataMap
	 *            activity data map collected from JMS {@code BytesMessage}
	 *
	 * @throws JMSException
	 *             if JMS exception occurs while reading bytes from message.
	 */
	protected void parseBytesMessage(BytesMessage bytesMessage, Map<String, Object> dataMap) throws JMSException {
		byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
		bytesMessage.readBytes(bytes);

		if (ArrayUtils.isNotEmpty(bytes)) {
			dataMap.put(StreamsConstants.ACTIVITY_DATA_KEY, Utils.getString(bytes));
		}
	}

	/**
	 * Parse JMS {@code MapMessage} activity info into activity data map.
	 *
	 * @param mapMessage
	 *            JMS map message
	 * @param dataMap
	 *            activity data map collected from JMS {@code MapMessage}
	 *
	 * @throws JMSException
	 *             if JMS exception occurs while getting map entries from
	 *             message.
	 */
	protected void parseMapMessage(MapMessage mapMessage, Map<String, Object> dataMap) throws JMSException {
		Enumeration<String> en = mapMessage.getMapNames();
		while (en.hasMoreElements()) {
			String key = en.nextElement();
			dataMap.put(key, mapMessage.getObject(key));
		}
	}

	/**
	 * Parse JMS {@code StreamMessage} activity info into activity data map.
	 *
	 * @param streamMessage
	 *            JMS stream message
	 * @param dataMap
	 *            activity data map collected from JMS {@code StreamMessage}
	 *
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
			LOGGER.log(OpLevel.ERROR, StreamsResources.getString("ActivityJMSMessageParser.bytes.buffer.error"), exc);
		}

		byte[] bytes = baos.toByteArray();
		Utils.close(baos);

		if (ArrayUtils.isNotEmpty(bytes)) {
			dataMap.put(StreamsConstants.ACTIVITY_DATA_KEY, Utils.getString(bytes));
		}
	}

	/**
	 * Parse JMS {@code ObjectMessage} activity info into activity data map.
	 *
	 * @param objMessage
	 *            JMS object message
	 * @param dataMap
	 *            activity data map collected from JMS {@code ObjectMessage}
	 *
	 * @throws JMSException
	 *             if JMS exception occurs while getting {@code Serializable}
	 *             object from message.
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
	 *
	 * @throws JMSException
	 *             if any JMS exception occurs while parsing message.
	 */
	protected void parseCustomMessage(Message message, Map<String, Object> dataMap) throws JMSException {
		LOGGER.log(OpLevel.WARNING, StreamsResources.getString("ActivityJMSMessageParser.parsing.custom.jms.message"));
	}
}
