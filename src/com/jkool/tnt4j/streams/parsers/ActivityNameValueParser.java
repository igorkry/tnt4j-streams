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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrMatcher;
import org.apache.commons.lang3.text.StrTokenizer;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
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
 * token-separated string of fields, where each field is represented by a
 * name/value pair and the name is used to map each field onto its corresponding
 * activity field. The field-separator and the name/value separator can both be
 * customized.
 * </p>
 * <p>
 * This parser supports the following properties:
 * <ul>
 * <li>FieldDelim</li>
 * <li>ValueDelim</li>
 * <li>Pattern</li>
 * <li>StripQuotes</li>
 * </ul>
 *
 * @version $Revision: 5 $
 */
public class ActivityNameValueParser extends GenericActivityParser<Map<String, String>> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityNameValueParser.class);

	/**
	 * Contains the field separator (set by {@code FieldDelim} property) -
	 * Default: ","
	 */
	protected StrMatcher fieldDelim = StrMatcher.charSetMatcher(DEFAULT_DELIM);

	/**
	 * Contains the name/value separator (set by {@code ValueDelim} property) -
	 * Default: "="
	 */
	protected String valueDelim = "="; // NON-NLS

	/**
	 * Contains the pattern used to determine which types of activity data
	 * string this parser supports (set by {@code Pattern} property). When
	 * {@code null}, all strings are assumed to match the format supported by
	 * this parser.
	 */
	protected Pattern pattern = null;

	/**
	 * Indicates whether surrounding double quotes should be stripped from
	 * extracted data values (set by {@code StripQuotes} property) - default:
	 * {@code true}
	 */
	protected boolean stripQuotes = true;

	/**
	 * Constructs an ActivityNameValueParser.
	 */
	public ActivityNameValueParser() {
		super(LOGGER);
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
			if (StreamsConfig.PROP_FLD_DELIM.equals(name)) {
				fieldDelim = StringUtils.isEmpty(value) ? null : StrMatcher.charSetMatcher(value);
				LOGGER.log(OpLevel.DEBUG,
						StreamsResources.getStringFormatted("ActivityParser.setting", name, fieldDelim));
			} else if (StreamsConfig.PROP_VAL_DELIM.equals(name)) {
				valueDelim = value;
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("ActivityParser.setting", name, value));
			} else if (StreamsConfig.PROP_PATTERN.equals(name)) {
				if (!StringUtils.isEmpty(value)) {
					pattern = Pattern.compile(value);
					LOGGER.log(OpLevel.DEBUG,
							StreamsResources.getStringFormatted("ActivityParser.setting", name, value));
				}
			} else if (StreamsConfig.PROP_STRIP_QUOTES.equals(name)) {
				stripQuotes = Boolean.parseBoolean(value);
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("ActivityParser.setting", name, value));
			}
			LOGGER.log(OpLevel.TRACE, StreamsResources.getStringFormatted("ActivityParser.ignoring", name));
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
	 * <li>{@code byte[]}</li>
	 * <li>{@code java.io.Reader}</li>
	 * <li>{@code java.io.InputStream}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return String.class.isInstance(data) || byte[].class.isInstance(data) || Reader.class.isInstance(data)
				|| InputStream.class.isInstance(data);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActivityInfo parse(TNTInputStream stream, Object data) throws IllegalStateException, ParseException {
		if (fieldDelim == null) {
			throw new IllegalStateException(StreamsResources.getString("ActivityNameValueParser.no.field.delimiter"));
		}
		if (valueDelim == null) {
			throw new IllegalStateException(StreamsResources.getString("ActivityNameValueParser.no.value.delimiter"));
		}
		if (data == null) {
			return null;
		}
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
		Map<String, String> nameValues = new HashMap<String, String>(fields.length);
		for (String field : fields) {
			if (field != null) {
				String[] nv = field.split(valueDelim);
				if (ArrayUtils.isNotEmpty(nv)) {
					nameValues.put(nv[0], nv.length > 1 ? nv[1].trim() : "");
				}
				LOGGER.log(OpLevel.TRACE, StreamsResources.getStringFormatted("ActivityNameValueParser.found", field));
			}
		}

		return parsePreparedItem(stream, dataStr, nameValues);
	}

	/**
	 * Gets field value from raw data location and formats it according locator
	 * definition.
	 *
	 * @param stream
	 *            parent stream
	 * @param locator
	 *            activity field locator
	 * @param nameValues
	 *            activity object name/value pairs map
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
	protected Object getLocatorValue(TNTInputStream stream, ActivityFieldLocator locator,
			Map<String, String> nameValues) throws ParseException {
		Object val = null;
		if (locator != null) {
			String locStr = locator.getLocator();
			if (!StringUtils.isEmpty(locStr)) {
				val = locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp ? stream.getProperty(locStr)
						: nameValues.get(locStr);
			}
			val = locator.formatValue(val);
		}
		return val;
	}
}
