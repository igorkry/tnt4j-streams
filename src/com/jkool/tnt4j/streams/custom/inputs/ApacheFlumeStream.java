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

package com.jkool.tnt4j.streams.custom.inputs;

import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;
import java.util.Map;

import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.inputs.CharacterStream;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Extends default TNT4J-Streams {@code CharacterStream} to handle additional
 * metadata received from Apache Flume output event. Apache Flume output event
 * is treated as JSON format compliant data object.
 * </p>
 *
 * @version $Revision: 1 $
 * @see CharacterStream
 */
public class ApacheFlumeStream extends CharacterStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ApacheFlumeStream.class);

	private static final String MESSAGE_KEY = "body"; // NON-NLS

	/**
	 * Construct empty ApacheFlumeStream. Requires configuration settings to set
	 * input stream source.
	 */
	public ApacheFlumeStream() {
		super(LOGGER);
	}

	/**
	 * Constructs ApacheFlumeStream to obtain activity data from the specified
	 * InputStream.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	public ApacheFlumeStream(InputStream stream) {
		super(stream);
	}

	/**
	 * Constructs ApacheFlumeStream to obtain activity data from the specified
	 * Reader.
	 *
	 * @param reader
	 *            reader to read data from
	 */
	public ApacheFlumeStream(Reader reader) {
		super(reader);
	}

	/**
	 * Gets the next processed activity.
	 * <p>
	 * Default implementation simply calls {@link #getNextItem()} to get next
	 * activity data item and calls {@link #applyParsers(Object)} to process it.
	 *
	 * @return next activity item
	 *
	 * @throws Throwable
	 *             if error getting next activity data item or processing it
	 */
	protected ActivityInfo getNextActivity() throws Throwable {
		ActivityInfo ai = null;
		Object data = getNextItem();

		try {
			if (data == null) {
				halt(); // no more data items to process
			} else {
				Map<String, ?> jsonMap = Utils.fromJsonToMap(data);

				if (jsonMap != null && !jsonMap.isEmpty()) {
					Object msgData = jsonMap.get(MESSAGE_KEY);

					if (msgData == null) {
						String jsonLine = String.valueOf(jsonMap.get(Utils.JSON_DATA_KEY));
						LOGGER.log(OpLevel.DEBUG,
								StreamsResources.getStringFormatted("CustomStream.no.activity.data", jsonLine));
					} else {
						ai = applyParsers(Utils.cleanActivityData(msgData));
					}

					if (ai != null) {
						jsonMap.remove(MESSAGE_KEY);
						jsonMap.remove(Utils.JSON_DATA_KEY);

						for (Map.Entry<String, ?> jme : jsonMap.entrySet()) {
							ai.addActivityProperty(jme.getKey(), jme.getValue());
						}
					}
				} else {
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("CustomStream.no.json.data"));
					halt();
				}
			}
		} catch (ParseException e) {
			int position = getActivityPosition();
			ParseException pe = new ParseException(
					StreamsResources.getStringFormatted("TNTInputStream.failed.to.process", position), position);
			pe.initCause(e);
			throw pe;
		}

		return ai;
	}

	// TODO: handle JSON containing already parsed data
}
