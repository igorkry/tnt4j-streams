/*
 * Copyright 2014-2018 JKOOL, LLC.
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.matcher.StringMatcher;
import org.apache.commons.text.matcher.StringMatcherFactory;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements an activity data parser that assumes each activity data item is a token-separated string of fields, where
 * each field is represented by a name/value pair and the name is used to map each field into its corresponding activity
 * field. The field-separator and the name/value separator can both be customized.
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link GenericActivityParser}):
 * <ul>
 * <li>FieldDelim - fields separator. (Optional)</li>
 * <li>ValueDelim - value delimiter. (Optional)</li>
 * <li>Pattern - pattern used to determine which types of activity data string this parser supports. When {@code null},
 * all strings are assumed to match the format supported by this parser. (Optional)</li>
 * <li>StripQuotes - whether surrounding double quotes should be stripped from extracted data values. (Optional)</li>
 * <li>EntryPattern - pattern used to to split data into name/value pairs. It should define two RegEx groups named
 * {@code "key"} and {@code "value"} used to map data contained values to name/value pair. NOTE: this parameter takes
 * preference on {@code "FieldDelim"} and {@code "ValueDelim"} properties. (Optional)</li>
 * </ul>
 * <p>
 * This activity parser supports those activity field locator types:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Label}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#StreamProp}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Cache}</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Activity}</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityNameValueParser extends GenericActivityParser<Map<String, String>> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(ActivityNameValueParser.class);

	/**
	 * Contains the field separator (set by {@code FieldDelim} property) - Default:
	 * {@value com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser#DEFAULT_DELIM}
	 */
	protected StringMatcher fieldDelim = StringMatcherFactory.INSTANCE.charSetMatcher(DEFAULT_DELIM);

	/**
	 * Contains the name/value separator (set by {@code ValueDelim} property) - Default: "="
	 */
	protected String valueDelim = "="; // NON-NLS

	/**
	 * Contains the pattern used to determine which types of activity data string this parser supports (set by
	 * {@code Pattern} property). When {@code null}, all strings are assumed to match the format supported by this
	 * parser.
	 */
	protected Pattern pattern = null;

	/**
	 * Indicates whether surrounding double quotes should be stripped from extracted data values (set by
	 * {@code StripQuotes} property) - default: {@code true}
	 */
	protected boolean stripQuotes = true;

	/**
	 * Contains the pattern used to to split data into name/value pairs. It should define two RegEx groups named
	 * {@code "key"} and {@code "value"} used to map data contained values to name/value pair. NOTE: this parameter
	 * takes preference on {@link #fieldDelim} and {@link #valueDelim} parameters.
	 */
	protected Pattern entryPattern = null;

	/**
	 * Constructs a new ActivityNameValueParser.
	 */
	public ActivityNameValueParser() {
		super(ActivityFieldDataType.String);
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		if (ParserProperties.PROP_FLD_DELIM.equalsIgnoreCase(name)) {
			fieldDelim = StringUtils.isEmpty(value) ? null : StringMatcherFactory.INSTANCE.charSetMatcher(value);
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.setting", name, value);
		} else if (ParserProperties.PROP_VAL_DELIM.equalsIgnoreCase(name)) {
			valueDelim = value;
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.setting", name, value);
		} else if (ParserProperties.PROP_PATTERN.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				pattern = Pattern.compile(value);
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.setting", name, value);
			}
		} else if (ParserProperties.PROP_STRIP_QUOTES.equalsIgnoreCase(name)) {
			stripQuotes = Utils.toBoolean(value);
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.setting", name, value);
		} else if (ParserProperties.PROP_ENTRY_PATTERN.equalsIgnoreCase(name)) {
			if (StringUtils.isNotEmpty(value)) {
				entryPattern = Pattern.compile(value);
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.setting", name, value);
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (ParserProperties.PROP_FLD_DELIM.equalsIgnoreCase(name)) {
			return fieldDelim;
		}
		if (ParserProperties.PROP_VAL_DELIM.equalsIgnoreCase(name)) {
			return valueDelim;
		}
		if (ParserProperties.PROP_PATTERN.equalsIgnoreCase(name)) {
			return pattern;
		}
		if (ParserProperties.PROP_STRIP_QUOTES.equalsIgnoreCase(name)) {
			return stripQuotes;
		}
		if (ParserProperties.PROP_ENTRY_PATTERN.equalsIgnoreCase(name)) {
			return entryPattern;
		}

		return super.getProperty(name);
	}

	@Override
	protected ActivityInfo parse(TNTInputStream<?, ?> stream, Object data, ActivityParserContext cData)
			throws IllegalStateException, ParseException {
		if (fieldDelim == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityNameValueParser.no.field.delimiter"));
		}
		if (valueDelim == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityNameValueParser.no.value.delimiter"));
		}

		return super.parse(stream, data, cData);
	}

	@Override
	protected ActivityContext prepareItem(TNTInputStream<?, ?> stream, Object data) throws ParseException {
		String dataStr = getNextActivityString(data);
		if (StringUtils.isEmpty(dataStr)) {
			return null;
		}
		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityParser.splitting.string", dataStr);
		if (pattern != null) {
			Matcher matcher = pattern.matcher(dataStr);
			if (matcher == null || !matcher.matches()) {
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityParser.input.not.match", getName(), pattern.pattern());
				return null;
			}
		}

		Map<String, String> nameValues = entryPattern == null ? delimit(dataStr) : regex(dataStr);
		ActivityContext cData = new ActivityContext(stream, data, nameValues);
		// cData.setMessage(getRawDataAsMessage(nameValues));

		return cData;
	}

	private Map<String, String> delimit(String dataStr) {
		StringTokenizer tk = stripQuotes
				? new StringTokenizer(dataStr, fieldDelim, StringMatcherFactory.INSTANCE.doubleQuoteMatcher())
				: new StringTokenizer(dataStr, fieldDelim);
		tk.setIgnoreEmptyTokens(false);
		String[] fields = tk.getTokenArray();
		if (ArrayUtils.isEmpty(fields)) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.no.fields");
			return null;
		}
		logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityParser.split", fields.length);
		Map<String, String> nameValues = new HashMap<>(fields.length);
		for (String field : fields) {
			if (field != null) {
				String[] nv = field.split(Pattern.quote(valueDelim));
				if (ArrayUtils.isNotEmpty(nv)) {
					nameValues.put(nv[0], nv.length > 1 ? nv[1].trim() : "");
				}
				logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"ActivityNameValueParser.found.delim", field);
			}
		}

		return nameValues;
	}

	private Map<String, String> regex(String dataStr) {
		Matcher matcher = entryPattern.matcher(dataStr);
		Map<String, String> nameValues = new HashMap<>();
		while (matcher.find()) {
			String key = matcher.group("key"); // NON-NLS
			String value = matcher.group("value"); // NON-NLS
			nameValues.put(key, value == null ? "" : value.trim());
			logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityNameValueParser.found.regex", key, value);
		}

		return nameValues;
	}

	/**
	 * Gets field raw data value resolved by locator.
	 *
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            activity object name/value pairs map
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) {
		Object val = null;
		String locStr = locator.getLocator();
		val = cData.getData().get(locStr); // NOTE: locStr == null?

		return val;
	}

	@SuppressWarnings("deprecation")
	private static final EnumSet<ActivityFieldLocatorType> UNSUPPORTED_LOCATOR_TYPES = EnumSet
			.of(ActivityFieldLocatorType.Index, ActivityFieldLocatorType.Range, ActivityFieldLocatorType.REMatchId);

	/**
	 * {@inheritDoc}
	 * <p>
	 * Unsupported activity locator types are:
	 * <ul>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Index}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#Range}</li>
	 * <li>{@link com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType#REMatchId}</li>
	 * </ul>
	 */
	@Override
	protected EnumSet<ActivityFieldLocatorType> getUnsupportedLocatorTypes() {
		return UNSUPPORTED_LOCATOR_TYPES;
	}
}
