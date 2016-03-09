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

import java.util.Collection;
import java.util.Map;

import com.jkool.tnt4j.streams.configure.ParserProperties;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements an activity data parser that assumes each activity data item is an
 * JSON format string, where each field is represented by a key/value pair and
 * the name is used to map each field onto its corresponding activity field.
 * <p>
 * This parser supports the following properties:
 * <ul>
 * <li>ReadLines - indicates that complete JSON data package is single line.
 * (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
// TODO: now works when one activity item is one JSON data package. No activity
// arrays parsing supported.
public class ActivityJsonParser extends AbstractActivityMapParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityJsonParser.class);

	private boolean jsonAsLine = true;

	/**
	 * Constructs a new ActivityJsonParser.
	 */
	public ActivityJsonParser() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();

			if (ParserProperties.PROP_READ_LINES.equalsIgnoreCase(name)) {
				jsonAsLine = Boolean.parseBoolean(value);
			}
		}
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
		return Utils.fromJsonToMap(data, jsonAsLine);
	}
}
