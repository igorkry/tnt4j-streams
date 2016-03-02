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

import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.jkool.tnt4j.streams.fields.ActivityField;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.fields.StreamFieldType;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Generic class for common activity parsers. It provides some generic
 * functionality witch is common to most activity parsers.
 *
 * @param <T>
 *            the type of handled activity data
 * @version $Revision: 1 $
 */
public abstract class GenericActivityParser<T> extends ActivityParser {

	/**
	 * Constant for default delimiter symbol used by parsers.
	 */
	protected static final String DEFAULT_DELIM = ","; // NON-NLS

	/**
	 * List of supported activity fields used to extract values from RAW
	 * activity data defined by field location(s).
	 */
	protected final List<ActivityField> fieldList = new ArrayList<ActivityField>();

	/**
	 * Creates a new GenericActivityParser.
	 *
	 * @param logger
	 *            logger used by activity parser
	 */
	protected GenericActivityParser(EventSink logger) {
		super(logger);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This parser supports the following class types (and all classes
	 * extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.lang.String}</li>
	 * <li>{@code byte[]}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.InputStream}</li>
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
	public void addField(ActivityField field) {
		logger.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
				"ActivityParser.adding.field", field.toDebugString()));
		fieldList.add(field);
	}

	/**
	 * Parse the specified prepared activity data, converting each field in
	 * prepared data to its corresponding value of activity info item.
	 *
	 * @param stream
	 *            parent stream
	 * @param dataStr
	 *            raw activity data string
	 * @param data
	 *            prepared activity data item to parse
	 *
	 * @return converted activity info, or {@code null} if activity data is
	 *         {@code null}
	 *
	 * @throws ParseException
	 *             if error applying locator format properties to specified
	 *             value
	 */
	protected ActivityInfo parsePreparedItem(TNTInputStream stream, String dataStr, T data) throws ParseException {
		if (data == null) {
			return null;
		}

		ActivityInfo ai = new ActivityInfo();
		ActivityField field = null;
		try {
			if (dataStr != null) {
				// save entire activity string as message data
				field = new ActivityField(StreamFieldType.Message.name());
				applyFieldValue(stream, ai, field, dataStr);
			}

			// apply fields for parser
			Object value;
			for (int i = 0; i < fieldList.size(); i++) {
				value = null;
				field = fieldList.get(i);
				List<ActivityFieldLocator> locations = field.getLocators();
				if (locations != null) {
					if (locations.size() == 1) {
						// field value is based on single raw data location, get
						// the value of this location
						value = getLocatorValue(stream, locations.get(0), data);
					} else {
						// field value is based on concatenation of several raw
						// data locations, build array to hold data from each
						// location
						Object[] values = new Object[locations.size()];
						for (int li = 0; li < locations.size(); li++) {
							values[li] = getLocatorValue(stream, locations.get(li), data);
						}
						value = values;
					}
				}
				applyFieldValue(stream, ai, field, value);
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_CORE, "ActivityParser.parsing.failed", field), 0);
			pe.initCause(e);
			throw pe;
		}

		return ai;
	}

	/**
	 * Gets field value from raw data location and formats it according locator
	 * definition.
	 *
	 * @param stream
	 *            parent stream
	 * @param locator
	 *            activity field locator
	 * @param data
	 *            activity object data
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
	protected abstract Object getLocatorValue(TNTInputStream stream, ActivityFieldLocator locator, T data)
			throws ParseException;
}
