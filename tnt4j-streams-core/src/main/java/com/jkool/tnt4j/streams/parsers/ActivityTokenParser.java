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

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrMatcher;
import org.apache.commons.lang3.text.StrTokenizer;

import com.jkool.tnt4j.streams.configure.ParserProperties;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements an activity data parser that assumes each activity data item is a
 * token-separated string of fields, with the value for each field being
 * retrieved from a specific 1-based numeric token position. The field-separator
 * can be customized.
 * <p>
 * This parser supports the following properties:
 * <ul>
 * <li>FieldDelim - fields separator. (Optional)</li>
 * <li>Pattern - pattern used to determine which types of activity data string
 * this parser supports. When {@code null}, all strings are assumed to match the
 * format supported by this parser. (Optional)</li>
 * <li>StripQuotes - whether surrounding double quotes should be stripped from
 * extracted data values. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityTokenParser extends GenericActivityParser<String[]> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityTokenParser.class);

	/**
	 * Contains the field separator (set by {@code FieldDelim} property) -
	 * default: ","
	 */
	protected StrMatcher fieldDelim = StrMatcher.charSetMatcher(DEFAULT_DELIM);

	/**
	 * Indicates whether surrounding double quotes should be stripped from
	 * extracted data values (set by {@code StripQuotes} property) - default:
	 * {@code true}
	 */
	protected boolean stripQuotes = true;

	/**
	 * Contains the pattern used to determine which types of activity data
	 * string this parser supports (set by {@code Pattern} property). When
	 * {@code null}, all strings are assumed to match the format supported by
	 * this parser.
	 */
	protected Pattern pattern = null;

	/**
	 * Constructs a new ActivityTokenParser.
	 */
	public ActivityTokenParser() {
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
			if (ParserProperties.PROP_FLD_DELIM.equalsIgnoreCase(name)) {
				fieldDelim = StringUtils.isEmpty(value) ? null : StrMatcher.charSetMatcher(value);
				LOGGER.log(OpLevel.DEBUG,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "ActivityParser.setting"),
						name, fieldDelim);
			} else if (ParserProperties.PROP_PATTERN.equalsIgnoreCase(name)) {
				if (!StringUtils.isEmpty(value)) {
					pattern = Pattern.compile(value);
					LOGGER.log(OpLevel.DEBUG,
							StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "ActivityParser.setting"),
							name, value);
				}
			} else if (ParserProperties.PROP_STRIP_QUOTES.equalsIgnoreCase(name)) {
				stripQuotes = Boolean.parseBoolean(value);
				LOGGER.log(OpLevel.TRACE,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "ActivityParser.setting"),
						name, value);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActivityInfo parse(TNTInputStream stream, Object data) throws IllegalStateException, ParseException {
		if (fieldDelim == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
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
		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "ActivityParser.parsing"), dataStr);
		if (pattern != null) {
			Matcher matcher = pattern.matcher(dataStr);
			if (matcher == null || !matcher.matches()) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
						"ActivityParser.input.not.match"), getName());
				return null;
			}
		}
		StrTokenizer tk = stripQuotes ? new StrTokenizer(dataStr, fieldDelim, StrMatcher.doubleQuoteMatcher())
				: new StrTokenizer(dataStr, fieldDelim);
		tk.setIgnoreEmptyTokens(false);
		String[] fields = tk.getTokenArray();
		if (ArrayUtils.isEmpty(fields)) {
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "ActivityParser.not.find"));
			return null;
		}
		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "ActivityParser.split"),
				fields.length);

		return parsePreparedItem(stream, dataStr, fields);
	}

	/**
	 * Gets field value from raw data location and formats it according locator
	 * definition.
	 *
	 * @param stream
	 *            parent stream
	 * @param locator
	 *            activity field locator
	 * @param fields
	 *            activity object data fields array
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
	@Override
	protected Object getLocatorValue(TNTInputStream stream, ActivityFieldLocator locator, String[] fields)
			throws ParseException {
		Object val = null;
		if (locator != null) {
			String locStr = locator.getLocator();
			if (!StringUtils.isEmpty(locStr)) {
				if (locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp) {
					val = stream.getProperty(locStr);
				} else {
					int loc = Integer.parseInt(locStr);
					if (loc >= 0 && loc <= fields.length) {
						val = fields[loc - 1].trim();
					}
				}
			}
			val = locator.formatValue(val);
		}
		return val;
	}
}
