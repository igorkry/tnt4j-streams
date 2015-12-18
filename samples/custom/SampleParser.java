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

package com.jkool.tnt4j.streams.samples.custom;

import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.parsers.GenericActivityParser;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Sample custom parser.
 */
public class SampleParser extends GenericActivityParser<String[]> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(SampleParser.class);

	/**
	 * Defines field separator.
	 */
	protected String fieldDelim = DEFAULT_DELIM;

	/**
	 * Constructs an SampleParser.
	 */
	public SampleParser() {
		super(LOGGER);
	}

	/**
	 * Sets custom properties for this parser
	 *
	 * @param props
	 *            properties to set
	 *
	 * @throws Throwable
	 *             indicates error with properties
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Throwable {
		if (props == null) {
			return;
		}
		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			LOGGER.log(OpLevel.DEBUG, "Setting {0} to ''{1}''", name, value);
			if (StreamsConfig.PROP_FLD_DELIM.equalsIgnoreCase(name)) {
				fieldDelim = value;
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
			throw new IllegalStateException("SampleParser: field delimiter not specified or empty");
		}
		if (data == null) {
			return null;
		}
		// Get next string to parse
		String dataStr = getNextString(data);
		if (StringUtils.isEmpty(dataStr)) {
			return null;
		}
		LOGGER.log(OpLevel.DEBUG, "Parsing: {0}", dataStr);
		String[] fields = dataStr.split(fieldDelim);
		if (ArrayUtils.isEmpty(fields)) {
			LOGGER.log(OpLevel.DEBUG, "Did not find any fields in input string");
			return null;
		}
		LOGGER.log(OpLevel.DEBUG, "Split input into {0} fields", fields.length);

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
			if (locStr != null && locStr.length() > 0) {
				if (locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp) {
					val = stream.getProperty(locStr);
				} else {
					int loc = Integer.parseInt(locStr);
					if (loc <= fields.length) {
						val = fields[loc - 1].trim();
					}
				}
			}
			val = locator.formatValue(val);
		}
		return val;
	}
}
