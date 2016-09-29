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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrMatcher;
import org.apache.commons.lang3.text.StrTokenizer;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Implements an activity data parser that assumes each activity data item is a token-separated string of fields, with
 * the value for each field being retrieved from a specific 1-based numeric token position. The field-separator can be
 * customized.
 * <p>
 * This parser supports the following properties:
 * <ul>
 * <li>FieldDelim - fields separator. (Optional)</li>
 * <li>Pattern - pattern used to determine which types of activity data string this parser supports. When {@code null},
 * all strings are assumed to match the format supported by this parser. (Optional)</li>
 * <li>StripQuotes - whether surrounding double quotes should be stripped from extracted data values. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityTokenParser extends GenericActivityParser<String[]> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityTokenParser.class);

	/**
	 * Contains the field separator (set by {@code FieldDelim} property) - default:
	 * "{@value com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser#DEFAULT_DELIM}"
	 */
	protected StrMatcher fieldDelim = StrMatcher.charSetMatcher(DEFAULT_DELIM);

	/**
	 * Indicates whether surrounding double quotes should be stripped from extracted data values (set by
	 * {@code StripQuotes} property) - default: {@code true}
	 */
	protected boolean stripQuotes = true;

	/**
	 * Contains the pattern used to determine which types of activity data string this parser supports (set by
	 * {@code Pattern} property). When {@code null}, all strings are assumed to match the format supported by this
	 * parser.
	 */
	protected Pattern pattern = null;

	/**
	 * Constructs a new ActivityTokenParser.
	 */
	public ActivityTokenParser() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}
		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (ParserProperties.PROP_FLD_DELIM.equalsIgnoreCase(name)) {
				fieldDelim = StringUtils.isEmpty(value) ? null : StrMatcher.charSetMatcher(value);
				logger().log(OpLevel.DEBUG,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
						name, fieldDelim);
			} else if (ParserProperties.PROP_PATTERN.equalsIgnoreCase(name)) {
				if (StringUtils.isNotEmpty(value)) {
					pattern = Pattern.compile(value);
					logger().log(OpLevel.DEBUG,
							StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
							name, value);
				}
			} else if (ParserProperties.PROP_STRIP_QUOTES.equalsIgnoreCase(name)) {
				stripQuotes = Boolean.parseBoolean(value);
				logger().log(OpLevel.TRACE,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
						name, value);
			}
		}
	}

	@Override
	public ActivityInfo parse(TNTInputStream<?, ?> stream, Object data) throws IllegalStateException, ParseException {
		if (fieldDelim == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityTokenParser.no.field.delimiter"));
		}
		if (data == null) {
			return null;
		}
		// Get next string to parse
		String dataStr = getNextString(data);
		if (StringUtils.isEmpty(dataStr)) {
			return null;
		}
		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.parsing"), dataStr);
		if (pattern != null) {
			Matcher matcher = pattern.matcher(dataStr);
			if (matcher == null || !matcher.matches()) {
				logger().log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityParser.input.not.match"), getName());
				return null;
			}
		}
		StrTokenizer tk = stripQuotes ? new StrTokenizer(dataStr, fieldDelim, StrMatcher.doubleQuoteMatcher())
				: new StrTokenizer(dataStr, fieldDelim);
		tk.setIgnoreEmptyTokens(false);
		String[] fields = tk.getTokenArray();
		if (ArrayUtils.isEmpty(fields)) {
			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.not.find"));
			return null;
		}
		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.split"),
				fields.length);

		return parsePreparedItem(stream, dataStr, fields);
	}

	/**
	 * Gets field value from raw data location and formats it according locator definition.
	 *
	 * @param locator
	 *            activity field locator
	 * @param fields
	 *            activity object data fields array
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return value formatted based on locator definition or {@code null} if locator is not defined
	 *
	 * @throws ParseException
	 *             if error applying locator format properties to specified value
	 *
	 * @see ActivityFieldLocator#formatValue(Object)
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, String[] fields, AtomicBoolean formattingNeeded)
			throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();
		int loc = Integer.parseInt(locStr);
		if (loc > 0 && loc <= fields.length) {
			val = fields[loc - 1].trim();
		}

		return val;
	}
}
