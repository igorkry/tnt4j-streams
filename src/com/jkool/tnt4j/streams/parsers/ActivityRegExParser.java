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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

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
 * string of fields as defined by the specified regular expression, with the
 * value for each field being retrieved from either of the 1-based group
 * position, or match position.
 * </p>
 * <p>
 * This parser supports the following properties:
 * <ul>
 * <li>Pattern</li>
 * </ul>
 *
 * @version $Revision: 5 $
 */
public class ActivityRegExParser extends ActivityParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityRegExParser.class);

	/**
	 * Contains the regular expression pattern that each data item is assumed to
	 * match (set by {@code Pattern} property).
	 */
	protected Pattern pattern = null;

	/**
	 * Defines the mapping of activity fields to the regular expression group
	 * location(s) in the raw data from which to extract its value.
	 */
	protected final Map<ActivityField, List<ActivityFieldLocator>> groupMap = new HashMap<ActivityField, List<ActivityFieldLocator>>();

	/**
	 * Defines the mapping of activity fields to the regular expression match
	 * sequence(s) in the raw data from which to extract its value.
	 */
	protected final Map<ActivityField, List<ActivityFieldLocator>> matchMap = new HashMap<ActivityField, List<ActivityFieldLocator>>();

	/**
	 * Constructs an ActivityRegExParser.
	 */
	public ActivityRegExParser() {
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
			if (StreamsConfig.PROP_PATTERN.equalsIgnoreCase(name)) {
				if (!StringUtils.isEmpty(value)) {
					pattern = Pattern.compile(value);
					LOGGER.log(OpLevel.DEBUG,
							StreamsResources.getStringFormatted("ActivityParser.setting", name, value));
				}
			}
			LOGGER.log(OpLevel.TRACE, StreamsResources.getStringFormatted("ActivityParser.ignoring", name));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addField(ActivityField field) {
		List<ActivityFieldLocator> locations = field.getLocators();
		if (locations == null) {
			return;
		}
		List<ActivityFieldLocator> matchLocs = new ArrayList<ActivityFieldLocator>();
		List<ActivityFieldLocator> groupLocs = new ArrayList<ActivityFieldLocator>();
		for (ActivityFieldLocator locator : locations) {
			ActivityFieldLocatorType locType = ActivityFieldLocatorType.REGroupNum;
			try {
				locType = ActivityFieldLocatorType.valueOf(locator.getType());
			} catch (Exception e) {
			}
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getStringFormatted("ActivityParser.adding.field", field.toDebugString()));
			if (locType == ActivityFieldLocatorType.REMatchNum) {
				if (groupMap.containsKey(field)) {
					throw new IllegalArgumentException(
							StreamsResources.getStringFormatted("ActivityRegExParser.conflicting.mapping", field));
				}
				matchLocs.add(locator);
			} else {
				if (matchMap.containsKey(field)) {
					throw new IllegalArgumentException(
							StreamsResources.getStringFormatted("ActivityRegExParser.conflicting.mapping", field));
				}
				groupLocs.add(locator);
			}
		}
		if (!matchLocs.isEmpty()) {
			matchMap.put(field, matchLocs);
		}
		if (!groupLocs.isEmpty()) {
			groupMap.put(field, groupLocs);
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
		if (pattern == null || StringUtils.isEmpty(pattern.pattern())) {
			throw new IllegalStateException(StreamsResources.getString("ActivityRegExParser.no.regex.pattern"));
		}
		if (data == null) {
			return null;
		}
		String dataStr = getNextString(data);
		if (StringUtils.isEmpty(dataStr)) {
			return null;
		}
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("ActivityParser.parsing", dataStr));
		Matcher matcher = pattern.matcher(dataStr);
		if (matcher == null || !matcher.matches()) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("ActivityParser.input.not.match", getName()));
			return null;
		}
		ActivityInfo ai = new ActivityInfo();
		// save entire activity string as message data
		ActivityField field = new ActivityField(StreamFieldType.Message.name());
		applyFieldValue(stream, ai, field, dataStr);
		// apply fields for parser
		try {
			if (!matchMap.isEmpty()) {
				LOGGER.log(OpLevel.DEBUG,
						StreamsResources.getStringFormatted("ActivityRegExParser.applying.regex", matchMap.size()));
				ArrayList<String> matches = new ArrayList<String>();
				matches.add(""); // dummy entry to index array with match
									// locations
				while (matcher.find()) {
					String matchStr = matcher.group().trim();
					matches.add(matchStr);
					LOGGER.log(OpLevel.TRACE,
							StreamsResources.getStringFormatted("ActivityRegExParser.match", matches.size(), matchStr));
				}
				LOGGER.log(OpLevel.DEBUG,
						StreamsResources.getStringFormatted("ActivityRegExParser.found.matches", matches.size()));
				Object value;
				for (Map.Entry<ActivityField, List<ActivityFieldLocator>> fieldMapEntry : matchMap.entrySet()) {
					field = fieldMapEntry.getKey();
					List<ActivityFieldLocator> locations = fieldMapEntry.getValue();
					value = null;
					if (locations != null) {
						LOGGER.log(OpLevel.TRACE,
								StreamsResources.getStringFormatted("ActivityRegExParser.setting.field", field));
						if (locations.size() == 1) {
							value = getLocatorValue(stream, locations.get(0), ActivityFieldLocatorType.REMatchNum,
									matcher, matches);
						} else {
							Object[] values = new Object[locations.size()];
							for (int li = 0; li < locations.size(); li++) {
								values[li] = getLocatorValue(stream, locations.get(li),
										ActivityFieldLocatorType.REMatchNum, matcher, matches);
							}
							value = values;
						}
					}
					applyFieldValue(stream, ai, field, value);
				}
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(
					StreamsResources.getStringFormatted("ActivityRegExParser.failed.parsing.regex", field), 0);
			pe.initCause(e);
			throw pe;
		}
		try {
			Object value;
			for (Map.Entry<ActivityField, List<ActivityFieldLocator>> fieldMapEntry : groupMap.entrySet()) {
				field = fieldMapEntry.getKey();
				List<ActivityFieldLocator> locations = fieldMapEntry.getValue();
				value = null;
				if (locations != null) {
					LOGGER.log(OpLevel.TRACE,
							StreamsResources.getStringFormatted("ActivityRegExParser.setting.group.field", field));
					if (locations.size() == 1) {
						value = getLocatorValue(stream, locations.get(0), ActivityFieldLocatorType.REGroupNum, matcher,
								null);
					} else {
						Object[] values = new Object[locations.size()];
						for (int li = 0; li < locations.size(); li++) {
							values[li] = getLocatorValue(stream, locations.get(li), ActivityFieldLocatorType.REGroupNum,
									matcher, null);
						}
						value = values;
					}
				}
				applyFieldValue(stream, ai, field, value);
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(
					StreamsResources.getStringFormatted("ActivityRegExParser.failed.parsing.regex.group", field), 0);
			pe.initCause(e);
			throw pe;
		}
		return ai;
	}

	private static Object getLocatorValue(TNTInputStream stream, ActivityFieldLocator locator,
			ActivityFieldLocatorType locType, Matcher matcher, List<String> matches) throws ParseException {
		Object val = null;
		if (locator != null) {
			String locStr = locator.getLocator();
			if (!StringUtils.isEmpty(locStr)) {
				if (locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp) {
					val = stream.getProperty(locStr);
				} else {
					int loc = Integer.parseInt(locStr);
					if (locType == ActivityFieldLocatorType.REMatchNum) {
						if (loc <= matches.size()) {
							val = matches.get(loc);
						}
					} else {
						if (loc <= matcher.groupCount()) {
							val = matcher.group(loc);
						}
					}
				}
			}
			val = locator.formatValue(val);
		}
		return val;
	}
}
