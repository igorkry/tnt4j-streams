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

import java.util.Map;

import com.jkool.tnt4j.streams.utils.StreamsConstants;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements default activity data parser that assumes each activity data item
 * is an {@code Map} data structure, where each field is represented by a
 * key/value pair and the name is used to map each field onto its corresponding
 * activity field.
 * </p>
 * <p>
 * Additionally this parser makes activity data transformation from
 * {@code byte[]} to {@code String}.
 * </p>
 *
 * @version $Revision: 1 $
 */
public class ActivityMapParser extends AbstractActivityMapParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityMapParser.class);

	/**
	 * Constructs an ActivityMapParser.
	 */
	public ActivityMapParser() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This parser supports the following class types (and all classes
	 * extending/implementing any of these):
	 * </p>
	 * <ul>
	 * <li>{@code java.util.Map}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return Map.class.isInstance(data);
	}

	/**
	 * Casts specified data object to map and applies default activity data
	 * transformation from {@code byte[]} to {@code String}.
	 *
	 * @param data
	 *            activity object data object
	 *
	 * @return activity object data map
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Map<String, ?> getDataMap(Object data) {
		Map<String, Object> map = (Map<String, Object>) data;

		Object activityData = map.get(StreamsConstants.ACTIVITY_DATA_KEY);
		if (activityData instanceof byte[]) {
			String activityDataStr = Utils.getString((byte[]) activityData);
			map.put(StreamsConstants.ACTIVITY_DATA_KEY, Utils.cleanActivityData(activityDataStr));
		}

		return map;
	}
}
