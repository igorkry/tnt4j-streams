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

import com.jkool.tnt4j.streams.utils.StreamsResources;

/**
 * Provides list of valid Message Types.
 *
 * @version $Revision: 7 $
 */
public enum MessageType {
	/**
	 * Unknown message type.
	 */
	UNKNOWN(0),

	/**
	 * Request message type.
	 */
	REQUEST(1),

	/**
	 * Reply message type.
	 */
	REPLY(2),

	/**
	 * Report message type.
	 */
	REPORT(4),

	/**
	 * Datagram message type.
	 */
	DATAGRAM(8),

	/**
	 * Acknowledgement message type.
	 */
	ACKNOWLEDGEMENT(12);

	MessageType(int enumValue) {
		this.enumValue = enumValue;
	}

	/**
	 * Gets numeric value of message type representation.
	 *
	 * @return message type member value
	 */
	public int value() {
		return enumValue;
	}

	/**
	 * Converts the specified value to a member of the enumeration.
	 *
	 * @param value
	 *            enumeration value to convert
	 *
	 * @return enumeration member
	 *
	 * @throws IllegalArgumentException
	 *             if there is no member of the enumeration with the specified
	 *             value
	 */
	public static MessageType valueOf(int value) {
		if (value == UNKNOWN.value()) {
			return UNKNOWN;
		}
		if (value == REQUEST.value()) {
			return REQUEST;
		}
		if (value == REPLY.value()) {
			return REPLY;
		}
		if (value == REPORT.value()) {
			return REPORT;
		}
		if (value == DATAGRAM.value()) {
			return DATAGRAM;
		}
		if (value == ACKNOWLEDGEMENT.value()) {
			return ACKNOWLEDGEMENT;
		}
		throw new IllegalArgumentException(StreamsResources.getStringFormatted("MessageType.illegal.num.value", value));
	}

	private final int enumValue;

	/**
	 * Converts the specified object to a member of the enumeration.
	 *
	 * @param value
	 *            object to convert
	 *
	 * @return enumeration member
	 *
	 * @throws IllegalArgumentException
	 *             if value is {@code null} or object cannot be matched to a
	 *             member of the enumeration
	 */
	public static MessageType valueOf(Object value) {
		if (value == null) {
			throw new IllegalArgumentException(StreamsResources.getString("MessageType.null.object"));
		}
		if (value instanceof Number) {
			return valueOf(((Number) value).intValue());
		}
		if (value instanceof String) {
			return valueOf(value.toString());
		}
		throw new IllegalArgumentException(
				StreamsResources.getStringFormatted("MessageType.illegal.obj.value", value.getClass().getName()));
	}
}
