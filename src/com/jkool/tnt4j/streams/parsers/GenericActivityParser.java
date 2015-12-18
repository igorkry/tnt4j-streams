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

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	 * Defines the mapping of activity fields to the location(s) in the raw data
	 * from which to extract their values.
	 */
	protected final Map<ActivityField, List<ActivityFieldLocator>> fieldMap = new HashMap<ActivityField, List<ActivityFieldLocator>>();

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
	 */
	public void addField(ActivityField field) {
		logger.log(OpLevel.DEBUG,
				StreamsResources.getStringFormatted("ActivityParser.adding.field", field.toDebugString()));
		fieldMap.put(field, field.getLocators());
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
			for (Map.Entry<ActivityField, List<ActivityFieldLocator>> fieldEntry : fieldMap.entrySet()) {
				value = null;
				field = fieldEntry.getKey();
				List<ActivityFieldLocator> locations = fieldEntry.getValue();
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
			ParseException pe = new ParseException(
					StreamsResources.getStringFormatted("ActivityParser.parsing.failed", field), 0);
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
