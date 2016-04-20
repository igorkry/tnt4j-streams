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

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jkool.tnt4j.streams.configure.ParserProperties;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements an activity data parser that assumes each activity data item is an
 * JSON format string. JSON parsing is performed using {@link JsonPath} API.
 * Activity fields locator values are treated as JsonPath expressions.
 * <p>
 * See <a href="https://github.com/jayway/JsonPath">JsonPath API</a> for more
 * details.
 * <p>
 * This parser supports the following properties:
 * <ul>
 * <li>ReadLines - indicates that complete JSON data package is single line.
 * (Optional)</li>
 * </ul>
 *
 * @version $Revision: 2 $
 */
public class ActivityJsonParser extends GenericActivityParser<DocumentContext> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityJsonParser.class);

	private static final String JSON_PATH_ROOT = "$.";// NON-NLS

	private boolean jsonAsLine = true;

	/**
	 * Constructs a new ActivityJsonParser.
	 */
	public ActivityJsonParser() {
		super(LOGGER);
	}

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

	@Override
	public boolean canHaveDelimitedLocators() {
		return false;
	}

	@Override
	public ActivityInfo parse(TNTInputStream stream, Object data) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}
		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "ActivityParser.parsing"), data);

		DocumentContext jsonDoc = null;
		String jsonString = null;
		try {
			if (data instanceof DocumentContext) {
				jsonDoc = (DocumentContext) data;
			} else {
				jsonString = getNextJSONString(data, jsonAsLine);
				if (StringUtils.isEmpty(jsonString)) {
					return null;
				}
				jsonDoc = JsonPath.parse(jsonString);
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ActivityJsonParser.jsonDocument.parse.error"), 0);
			pe.initCause(e);

			throw pe;
		}

		if (jsonString == null) {
			jsonString = jsonDoc.jsonString();
		}

		return parsePreparedItem(stream, jsonString, jsonDoc);
	}

	/**
	 * Reads the next complete JSON document string from the specified data
	 * input source and returns it as a string.
	 *
	 * @param data
	 *            input source for activity data
	 * @param jsonAsLine
	 *            if {@code true} indicates complete JSON package is line, if
	 *            {@code false} - whole data available to read
	 * @return JSON document string, or {@code null} if end of input source has
	 *         been reached
	 * @throws IllegalArgumentException
	 *             if the class of input source supplied is not supported.
	 */
	protected String getNextJSONString(Object data, boolean jsonAsLine) {
		if (data == null) {
			return null;
		}
		if (data instanceof String) {
			return (String) data;
		} else if (data instanceof byte[]) {
			return Utils.getString((byte[]) data);
		}
		BufferedReader rdr;
		if (data instanceof BufferedReader) {
			rdr = (BufferedReader) data;
		} else if (data instanceof Reader) {
			rdr = new BufferedReader((Reader) data);
		} else if (data instanceof InputStream) {
			rdr = new BufferedReader(new InputStreamReader((InputStream) data));
		} else {
			throw new IllegalArgumentException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ActivityParser.data.unsupported", data.getClass().getName()));
		}
		StringBuilder jsonStringBuilder = new StringBuilder();
		String line;

		try {
			while ((line = rdr.readLine()) != null) {
				jsonStringBuilder.append(line);
				if (jsonAsLine) {
					break;
				}
			}
		} catch (EOFException eof) {
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "ActivityJsonParser.data.end"),
					eof);
		} catch (IOException ioe) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ActivityJsonParser.error.reading"), ioe);
		}

		return jsonStringBuilder.toString();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Object getLocatorValue(TNTInputStream stream, ActivityFieldLocator locator, DocumentContext data)
			throws ParseException {
		Object val = null;
		if (locator != null) {
			boolean format = true;
			String locStr = locator.getLocator();
			if (!StringUtils.isEmpty(locStr)) {
				if (locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp) {
					val = stream.getProperty(locStr);
				} else {
					if (!locStr.startsWith(JSON_PATH_ROOT)) {
						locStr = JSON_PATH_ROOT + locStr;
					}
					Object jsonValue = data.read(locStr);

					List<Object> jsonValuesList;
					if (jsonValue instanceof List) {
						jsonValuesList = (List<Object>) jsonValue;
					} else {
						jsonValuesList = new ArrayList<Object>(1);
						jsonValuesList.add(jsonValue);
					}

					if (CollectionUtils.isNotEmpty(jsonValuesList)) {
						List<Object> valuesList = new ArrayList<Object>(jsonValuesList.size());
						for (Object jsonValues : jsonValuesList) {
							valuesList.add(locator.formatValue(jsonValues));
						}

						val = wrapValue(valuesList);
						format = false;
					}
				}
			}

			if (format) {
				val = locator.formatValue(val);
			}
		}

		return val;
	}
}
