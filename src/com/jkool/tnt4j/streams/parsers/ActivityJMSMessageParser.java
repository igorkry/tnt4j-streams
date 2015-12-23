/*
 * Copyright (c) 2015 jKool, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * jKool, LLC. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with jKool, LLC.
 *
 * JKOOL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. JKOOL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 *
 */

package com.jkool.tnt4j.streams.parsers;

import java.lang.IllegalStateException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Map;

import javax.jms.*;

import com.jkool.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * TODO
 */
public class ActivityJMSMessageParser extends GenericActivityParser<Message> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityJMSMessageParser.class);

	/**
	 * Constructs a new JMSMessageParser.
	 */
	public ActivityJMSMessageParser() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Throwable {
		if (props == null) {
			return;
		}
		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();

			// TODO:
		}
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
	 * {@inheritDoc}
	 */
	@Override
	public ActivityInfo parse(TNTInputStream stream, Object data) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("ActivityParser.parsing", data));

		Message message = (Message) data;

		ActivityInfo ai = new ActivityInfo();

		if (message instanceof TextMessage) {
			ai = parseTextMessage((TextMessage) message);
		} else if (message instanceof BytesMessage) {
			ai = parseBytesMessage((BytesMessage) message);
		} else if (message instanceof MapMessage) {
			ai = parseMapMessage((MapMessage) message);
		} else if (message instanceof StreamMessage) {
			ai = parseStreamMessage((StreamMessage) message);
		} else if (message instanceof ObjectMessage) {
			ai = parseObjectMessage((ObjectMessage) message);
		} else {
			ai = parseCustomMessage(message);
		}

		return ai;
	}

	/**
	 * Parse text message activity info.
	 *
	 * @param textMessage
	 *            the text message
	 * @return the activity info
	 */
	protected ActivityInfo parseTextMessage(TextMessage textMessage) {
		return null; // TODO
	}

	/**
	 * Parse bytes message activity info.
	 *
	 * @param bytesMessage
	 *            the bytes message
	 * @return the activity info
	 */
	protected ActivityInfo parseBytesMessage(BytesMessage bytesMessage) {
		return null; // TODO
	}

	/**
	 * Parse map message activity info.
	 *
	 * @param mapMessage
	 *            the map message
	 * @return the activity info
	 */
	protected ActivityInfo parseMapMessage(MapMessage mapMessage) {
		return null; // TODO
	}

	/**
	 * Parse stream message activity info.
	 *
	 * @param streamMessage
	 *            the stream message
	 * @return the activity info
	 */
	protected ActivityInfo parseStreamMessage(StreamMessage streamMessage) {
		return null; // TODO
	}

	/**
	 * Parse object message activity info.
	 *
	 * @param objMessage
	 *            the obj message
	 * @return the activity info
	 */
	protected ActivityInfo parseObjectMessage(ObjectMessage objMessage) {
		return null; // TODO
	}

	/**
	 * Parse custom message activity info.
	 *
	 * @param message
	 *            the message
	 * @return the activity info
	 */
	protected ActivityInfo parseCustomMessage(Message message) {
		return null; // TODO
	}

	/**
	 * Gets field value from raw data location and formats it according locator
	 * definition.
	 *
	 * @param stream
	 *            parent stream
	 * @param locator
	 *            activity field locator
	 * @param data
	 *            activity object data
	 *
	 * @return value formatted based on locator definition or {@code null} if
	 *         locator is not defined
	 *
	 * @throws ParseException
	 *             if error applying locator format properties to specified
	 *             value
	 *
	 * @see ActivityFieldLocator#formatValue(Object)
	 */
	protected Object getLocatorValue(TNTInputStream stream, ActivityFieldLocator locator, Message data)
			throws ParseException {
		return null; // TODO
	}
}
