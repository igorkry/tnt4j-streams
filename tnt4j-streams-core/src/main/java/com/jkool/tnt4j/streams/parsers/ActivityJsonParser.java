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

import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements an activity data parser that assumes each activity data item is an
 * JSON format string, where each field is represented by a key/value pair and
 * the name is used to map each field onto its corresponding activity field.
 *
 * @version $Revision: 2 $
 */
// TODO: now works when one activity item is one JSON data package. No activity
// arrays parsing supported.
public class ActivityJsonParser extends AbstractActivityMapParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityJsonParser.class);

	/**
	 * Constructs a new ActivityJsonParser.
	 */
	public ActivityJsonParser() {
		super(LOGGER);
	}

	/**
	 * Makes map object containing activity object data parsed from JSON.
	 * 
	 * @param data
	 *            activity object data object
	 * 
	 * @return activity object data map
	 */
	@Override
	protected Map<String, ?> getDataMap(Object data) {
		return Utils.fromJsonToMap(data);
	}
}
