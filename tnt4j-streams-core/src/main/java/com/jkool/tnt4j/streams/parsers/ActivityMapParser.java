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
	 * Constructs a new ActivityMapParser.
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
