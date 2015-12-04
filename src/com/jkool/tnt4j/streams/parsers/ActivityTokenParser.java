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

import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrMatcher;
import org.apache.commons.lang3.text.StrTokenizer;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.fields.*;
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
 * </p>
 * <p>
 * This parser supports the following properties:
 * <ul>
 * <li>FieldDelim</li>
 * <li>Pattern</li>
 * <li>StripQuotes</li>
 * </ul>
 *
 * @version $Revision: 5 $
 */
public class ActivityTokenParser extends ActivityParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityTokenParser.class);

	/**
	 * Contains the field separator (set by {@code FieldDelim} property) -
	 * Default: ","
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
	 * Constructs an ActivityTokenParser.
	 */
	public ActivityTokenParser() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Throwable {
		if (props == null) {
			return;
		}
		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamsConfig.PROP_FLD_DELIM.equalsIgnoreCase(name)) {
				fieldDelim = StringUtils.isEmpty(value) ? null : StrMatcher.charSetMatcher(value);
				LOGGER.log(OpLevel.DEBUG,
						StreamsResources.getStringFormatted("ActivityParser.setting", name, fieldDelim));
			} else if (StreamsConfig.PROP_PATTERN.equalsIgnoreCase(name)) {
				if (!StringUtils.isEmpty(value)) {
					pattern = Pattern.compile(value);
					LOGGER.log(OpLevel.DEBUG,
							StreamsResources.getStringFormatted("ActivityParser.setting", name, value));
				}
			} else if (StreamsConfig.PROP_STRIP_QUOTES.equalsIgnoreCase(name)) {
				stripQuotes = Boolean.parseBoolean(value);
				LOGGER.log(OpLevel.TRACE, StreamsResources.getStringFormatted("ActivityParser.setting", name, value));
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This parser supports the following class types (and all classes
	 * extending/implementing any of these):
	 * </p>
	 * <ul>
	 * <li>{@code java.lang.String}</li>
	 * <li>{@code java.io.Reader}</li>
	 * <li>{@code java.io.InputStream}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return String.class.isInstance(data) || Reader.class.isInstance(data) || InputStream.class.isInstance(data);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActivityInfo parse(TNTInputStream stream, Object data) throws IllegalStateException, ParseException {
		if (fieldDelim == null) {
			throw new IllegalStateException(StreamsResources.getString("ActivityTokenParser.no.field.delimiter"));
		}
		if (data == null) {
			return null;
		}
		// Get next string to parse
		String dataStr = getNextString(data);
		if (StringUtils.isEmpty(dataStr)) {
			return null;
		}
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("ActivityParser.parsing", dataStr));
		if (pattern != null) {
			Matcher matcher = pattern.matcher(dataStr);
			if (matcher == null || !matcher.matches()) {
				LOGGER.log(OpLevel.DEBUG,
						StreamsResources.getStringFormatted("ActivityParser.input.not.match", getName()));
				return null;
			}
		}
		StrTokenizer tk = stripQuotes ? new StrTokenizer(dataStr, fieldDelim, StrMatcher.doubleQuoteMatcher())
				: new StrTokenizer(dataStr, fieldDelim);
		tk.setIgnoreEmptyTokens(false);
		String[] fields = tk.getTokenArray();
		if (ArrayUtils.isEmpty(fields)) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("ActivityParser.not.find"));
			return null;
		}
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("ActivityParser.split", fields.length));
		ActivityInfo ai = new ActivityInfo();
		ActivityField field = null;
		try {
			// save entire activity string as message data
			field = new ActivityField(StreamFieldType.Message.name());
			applyFieldValue(ai, field, dataStr);
			// apply fields for parser
			Object value;
			for (Map.Entry<ActivityField, List<ActivityFieldLocator>> fieldEntry : fieldMap.entrySet()) {
				value = null;
				field = fieldEntry.getKey();
				List<ActivityFieldLocator> locations = fieldEntry.getValue();
				if (locations != null) {
					if (locations.size() == 1) {
						// field value is based on single raw data location, get
						// the value of this location
						value = getLocatorValue(stream, locations.get(0), fields);
					} else {
						// field value is based on concatenation of several raw
						// data locations,
						// build array to hold data from each location
						Object[] values = new Object[locations.size()];
						for (int li = 0; li < locations.size(); li++) {
							values[li] = getLocatorValue(stream, locations.get(li), fields);
						}
						value = values;
					}
				}
				applyFieldValue(ai, field, value);
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(
					StreamsResources.getStringFormatted("ActivityParser.parsing.failed", field), 0);
			pe.initCause(e);
			throw pe;
		}
		return ai;
	}

	private static Object getLocatorValue(TNTInputStream stream, ActivityFieldLocator locator, String[] fields)
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
