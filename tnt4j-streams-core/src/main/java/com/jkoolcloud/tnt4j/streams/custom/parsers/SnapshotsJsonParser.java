/*
 * Copyright 2014-2016 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.custom.parsers;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Implements an activity data parser that assumes each activity data item is an JSON format string. JSON parsing is
 * performed using {@link JsonPath} API. Activity fields locator values are treated as JsonPath expressions. See
 * <a href="https://github.com/jayway/JsonPath">JsonPath API</a> for more details.
 * <p>
 * This parser supports the following properties:
 * <ul>
 * <li>ChildrenField - field name referencing JSON indicates that complete JSON data package is single line. (Optional)
 * </li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class SnapshotsJsonParser extends ActivityJsonParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(SnapshotsJsonParser.class);

	private String snapshotsFieldName = null;
	private ActivityField snapshotsField;

	private boolean snapshotsFieldInitialized = false;

	/**
	 * Constructs a new SnapshotsJsonParser.
	 */
	public SnapshotsJsonParser() {
		super(LOGGER);
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

			if (ParserProperties.PROP_CHILDREN_FIELD.equalsIgnoreCase(name)) {
				snapshotsFieldName = value;
			}
		}
	}

	private void initSnapshotsField() {
		if (StringUtils.isNotEmpty(snapshotsFieldName)) {
			for (ActivityField field : fieldList) {
				if (field.getFieldTypeName().equalsIgnoreCase(snapshotsFieldName)) {
					snapshotsField = field;
				}
			}

			if (snapshotsField != null) {
				fieldList.remove(snapshotsField);
			}
		}

		snapshotsFieldInitialized = true;
	}

	@Override
	protected ActivityInfo parsePreparedItem(TNTInputStream<?, ?> stream, String dataStr, DocumentContext data)
			throws ParseException {
		if (!snapshotsFieldInitialized) {
			initSnapshotsField();
		}

		ActivityInfo ai = super.parsePreparedItem(stream, dataStr, data);

		if (snapshotsField != null) {
			List<ActivityFieldLocator> locations = snapshotsField.getLocators();
			Object value = null;
			if (locations.size() == 1) {
				value = getLocatorValue(stream, locations.get(0), data);
			}

			if (!(value instanceof Object[])) {
				throw new ParseException(
						StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
								"SnapshotsJsonParser.illegal.parsed.value", value == null ? value : value.getClass()),
						0);
			}

			Object[] values = (Object[]) value;
			for (Object resolvedValue : values) {
				for (ActivityParser parser : snapshotsField.getStackedParsers()) {
					ActivityInfo parsedItem = parser.parse(stream, resolvedValue);
					ai.addChild(parsedItem);
				}
			}
		}

		return ai;
	}
}
