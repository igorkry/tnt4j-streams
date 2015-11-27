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

import org.apache.commons.lang3.StringUtils;

import com.jkool.tnt4j.streams.fields.ActivityField;
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
 * JSON format string, where each field is represented by a key/value pair and
 * the name is used to map each field onto its corresponding activity field.
 * </p>
 *
 * @version $Revision: 1 $
 */
// TODO: now works when one activity item is one JSON data package. No activity
// arrays parsing supported.
public class ActivityJsonParser extends ActivityParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityJsonParser.class);

	/**
	 * Constructs an ActivityJsonParser.
	 */
	public ActivityJsonParser() {

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

			// TODO:
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
	 * <li>{@code com.google.gson.stream.JsonReader}</li>
	 * <li>{@code com.google.gson.JsonElement}</li>
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
		if (data == null) {
			return null;
		}
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("ActivityParser.parsing"), data);

		ActivityInfo ai = new ActivityInfo();
		ActivityField field = null;
		try {
			Map<String, ?> jsonMap = Utils.fromJsonToMap(data);
			if (jsonMap == null || jsonMap.isEmpty()) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("ActivityParser.not.find"));
				return null;
			}

			// // save entire activity string as message data
			// field = new ActivityField(StreamFieldType.Message.name());
			// applyFieldValue(ai, field, dataStr); //TODO

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
						value = getLocatorValue(stream, locations.get(0), jsonMap);
					} else {
						// field value is based on concatenation of several raw
						// data locations, build array to hold data from each
						// location
						Object[] values = new Object[locations.size()];
						for (int li = 0; li < locations.size(); li++) {
							values[li] = getLocatorValue(stream, locations.get(li), jsonMap);
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

	private Object getLocatorValue(TNTInputStream stream, ActivityFieldLocator locator, Map<String, ?> jsonMap)
			throws ParseException {
		Object val = null;
		if (locator != null) {
			String locStr = locator.getLocator();
			if (!StringUtils.isEmpty(locStr)) {
				if (locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp) {
					val = stream.getProperty(locStr);
				} else {
					val = jsonMap.get(locStr);
				}
			}
			val = locator.formatValue(val);
		}

		return val;
	}
}
