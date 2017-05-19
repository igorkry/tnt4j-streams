/*
 * Copyright 2014-2017 JKOOL, LLC.
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
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements an activity data parser that assumes each activity data item is a string of fields as defined by the
 * specified regular expression, with the value for each field being retrieved from either of the 1-based group
 * position, or match position.
 * <p>
 * This parser supports the following properties (in addition to those supported by {@link GenericActivityParser}):
 * <ul>
 * <li>Pattern - contains the regular expression pattern that each data item is assumed to match. (Required)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityRegExParser extends GenericActivityParser<Object> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityRegExParser.class);

	private static final String MATCHES_KEY = "MATCHES_DATA"; // NON-NLS

	/**
	 * Contains the regular expression pattern that each data item is assumed to match (set by {@code Pattern}
	 * property).
	 */
	protected Pattern pattern = null;

	/**
	 * Defines the mapping of activity fields to the regular expression group location(s) in the raw data, cached
	 * activity data or stream properties from which to extract its value.
	 */
	protected final Map<ActivityField, List<ActivityFieldLocator>> groupMap = new HashMap<>();

	/**
	 * Defines the mapping of activity fields to the regular expression match sequence(s) in the raw data from which to
	 * extract its value.
	 */
	protected final Map<ActivityField, List<ActivityFieldLocator>> matchMap = new HashMap<>();

	/**
	 * Constructs a new ActivityRegExParser.
	 */
	public ActivityRegExParser() {
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

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();

			if (ParserProperties.PROP_PATTERN.equalsIgnoreCase(name)) {
				if (StringUtils.isNotEmpty(value)) {
					pattern = Pattern.compile(value);
					logger().log(OpLevel.DEBUG,
							StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
							name, value);
				}
			}
		}
	}

	@Override
	public void addField(ActivityField field) {
		List<ActivityFieldLocator> locators = field.getLocators();
		if (CollectionUtils.isEmpty(locators)) {
			return;
		}
		List<ActivityFieldLocator> matchLocs = new ArrayList<>();
		List<ActivityFieldLocator> groupLocs = new ArrayList<>();
		for (ActivityFieldLocator locator : locators) {
			ActivityFieldLocatorType locType = locator.getBuiltInType();
			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.adding.field"),
					field); // Utils.getDebugString(field));
			if (locType == ActivityFieldLocatorType.REMatchNum) {
				if (groupMap.containsKey(field)) {
					throw new IllegalArgumentException(StreamsResources.getStringFormatted(
							StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityRegExParser.conflicting.mapping", field));
				}
				matchLocs.add(locator);
			} else {
				if (matchMap.containsKey(field)) {
					throw new IllegalArgumentException(StreamsResources.getStringFormatted(
							StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityRegExParser.conflicting.mapping", field));
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

	@Override
	public ActivityInfo parse(TNTInputStream<?, ?> stream, Object data) throws IllegalStateException, ParseException {
		if (pattern == null || StringUtils.isEmpty(pattern.pattern())) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityRegExParser.no.regex.pattern"));
		}

		return super.parse(stream, data);
	}

	@Override
	protected ActivityContext prepareItem(TNTInputStream<?, ?> stream, Object data) throws ParseException {
		String dataStr = getNextActivityString(data);
		if (StringUtils.isEmpty(dataStr)) {
			return null;
		}
		Matcher matcher = pattern.matcher(dataStr);
		if (matcher == null || !matcher.matches()) {
			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.input.not.match"),
					getName());
			return null;
		}

		ActivityContext cData = new ActivityContext(stream, data, matcher);
		cData.setMessage(dataStr);

		return cData;
	}

	@Override
	protected ActivityInfo parsePreparedItem(ActivityContext cData) throws ParseException {
		if (cData == null || cData.getData() == null) {
			return null;
		}

		ActivityInfo ai = new ActivityInfo();
		ActivityField field = null;
		cData.setActivity(ai);
		Matcher matcher = (Matcher) cData.getData();
		// apply fields for parser
		try {
			if (!matchMap.isEmpty()) {
				logger().log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityRegExParser.applying.regex"), matchMap.size());
				ArrayList<String> matches = new ArrayList<>();
				matches.add(""); // dummy entry to index array with match
									// locations
				while (matcher.find()) {
					String matchStr = matcher.group().trim();
					matches.add(matchStr);
					logger().log(OpLevel.TRACE, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ActivityRegExParser.match"), matches.size(), matchStr);
				}
				logger().log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityRegExParser.found.matches"), matches.size());
				cData.put(MATCHES_KEY, matches);
				Object value;
				for (Map.Entry<ActivityField, List<ActivityFieldLocator>> fieldMapEntry : matchMap.entrySet()) {
					field = fieldMapEntry.getKey();
					cData.setField(field);
					List<ActivityFieldLocator> locations = fieldMapEntry.getValue();

					value = Utils.simplifyValue(parseLocatorValues(locations, cData));

					if (value != null) {
						logger().log(OpLevel.TRACE, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
								"ActivityRegExParser.setting.field"), field);
					}

					applyFieldValue(field, value, cData);
				}
				cData.remove(MATCHES_KEY);
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityRegExParser.failed.parsing.regex", field), 0);
			pe.initCause(e);
			throw pe;
		}
		try {
			Object value;
			for (Map.Entry<ActivityField, List<ActivityFieldLocator>> fieldMapEntry : groupMap.entrySet()) {
				field = fieldMapEntry.getKey();
				cData.setField(field);
				List<ActivityFieldLocator> locations = fieldMapEntry.getValue();

				value = Utils.simplifyValue(parseLocatorValues(locations, cData));

				if (value != null) {
					logger().log(OpLevel.TRACE, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ActivityRegExParser.setting.group.field"), field);
				}

				applyFieldValue(field, value, cData);
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityRegExParser.failed.parsing.regex.group", field), 0);
			pe.initCause(e);
			throw pe;
		}

		return ai;
	}

	/**
	 * Gets field raw data value resolved by locator.
	 *
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            RegEx data package - {@link Matcher} or {@link ArrayList} of matches
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) {
		Object val = null;
		String locStr = locator.getLocator();

		if (StringUtils.isNotEmpty(locStr)) {
			int loc = Integer.parseInt(locStr);

			if (cData.containsKey(MATCHES_KEY)) {
				ArrayList<String> matches = (ArrayList<String>) cData.get(MATCHES_KEY);

				if (loc >= 0 && loc < matches.size()) {
					val = matches.get(loc);
				}
			} else {
				Matcher matcher = (Matcher) cData.getData();

				if (loc >= 0 && loc <= matcher.groupCount()) {
					val = matcher.group(loc);
				}
			}
		}

		return val;
	}
}
