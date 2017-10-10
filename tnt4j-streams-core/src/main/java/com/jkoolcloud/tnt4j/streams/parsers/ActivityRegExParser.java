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

import java.lang.reflect.Method;
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
 * position, match position, or group name.
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link GenericActivityParser}):
 * <ul>
 * <li>Pattern - contains the regular expression pattern that each data item is assumed to match. (Required)</li>
 * <li>MatchStrategy - defines <tt>pattern</tt> created <tt>matcher</tt> comparison strategy used against input data
 * string. Value can be one of: {@code "MATCH"} - pattern should match complete input string, or {@code "FIND"} -
 * pattern has to match subsequence within input string. Default value - '{@code MATCH}'. (Optional)</li>
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
	 * Defines strategy used to verify if {@link #pattern} created {@link Matcher} matches input data string.
	 */
	protected Strategy matchStrategy = Strategy.MATCH;

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
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		super.setProperties(props);

		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				String name = prop.getKey();
				String value = prop.getValue();

				if (ParserProperties.PROP_PATTERN.equalsIgnoreCase(name)) {
					if (StringUtils.isNotEmpty(value)) {
						pattern = Pattern.compile(value);
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ActivityParser.setting", name, value);
					}
				} else if (ParserProperties.PROP_MATCH_STRATEGY.equalsIgnoreCase(name)) {
					if (StringUtils.isNotEmpty(value)) {
						matchStrategy = Strategy.valueOf(value.toUpperCase());
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ActivityParser.setting", name, value);
					}
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
		List<ActivityFieldLocator> matchLocs = new ArrayList<>(5);
		List<ActivityFieldLocator> groupLocs = new ArrayList<>(5);
		for (ActivityFieldLocator locator : locators) {
			ActivityFieldLocatorType locType = locator.getBuiltInType();
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.adding.field", field); // Utils.getDebugString(field));
			if (locType == ActivityFieldLocatorType.REMatchId) {
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
		if (matcher == null || !isMatching(matcher)) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.input.not.match", getName(), pattern.pattern());
			return null;
		}

		ActivityContext cData = new ActivityContext(stream, data, matcher);
		cData.setMessage(dataStr);

		return cData;
	}

	/**
	 * Checks if <tt>matcher</tt> matches input data string using parser defined matching strategy.
	 * 
	 * @param matcher
	 *            matcher instance to use
	 * @return {@code true} if matcher matches input data string using parser defined matching strategy
	 *
	 * @see java.util.regex.Matcher#find()
	 * @see java.util.regex.Matcher#matches()
	 */
	protected boolean isMatching(Matcher matcher) {
		boolean match;

		if (matchStrategy == Strategy.FIND) {
			match = matcher.find();
		} else {
			match = matcher.matches();
		}

		return match;
	}

	@Override
	protected ActivityInfo parsePreparedItem(ActivityContext cData) throws ParseException {
		if (cData == null || cData.getData() == null) {
			return null;
		}

		ActivityInfo ai = new ActivityInfo();
		cData.setActivity(ai);
		// apply fields for parser
		if (!matchMap.isEmpty()) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityRegExParser.applying.regex", matchMap.size());
			Map<String, String> matches = findMatches((Matcher) cData.getData());

			cData.put(MATCHES_KEY, matches);
			resolveLocatorsValues(matchMap, cData);
			cData.remove(MATCHES_KEY);
		}

		resolveLocatorsValues(groupMap, cData);

		return ai;
	}

	/**
	 * Resolves and applies <tt>locMap</tt> defined locators mapped RegEx values.
	 * 
	 * @param locMap
	 *            regex locators map
	 * @param cData
	 *            prepared activity data item parsing context
	 * @throws ParseException
	 *             if exception occurs while resolving regex locators values
	 *
	 * @see #parseLocatorValues(java.util.List,
	 *      com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)
	 * @see #applyFieldValue(com.jkoolcloud.tnt4j.streams.fields.ActivityField, Object,
	 *      com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)
	 */
	protected void resolveLocatorsValues(Map<ActivityField, List<ActivityFieldLocator>> locMap, ActivityContext cData)
			throws ParseException {
		ActivityField field = null;
		try {
			Object value;
			for (Map.Entry<ActivityField, List<ActivityFieldLocator>> fieldMapEntry : locMap.entrySet()) {
				field = fieldMapEntry.getKey();
				cData.setField(field);
				List<ActivityFieldLocator> locations = fieldMapEntry.getValue();

				value = Utils.simplifyValue(parseLocatorValues(locations, cData));

				if (value != null) {
					logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"ActivityRegExParser.setting.field", field, value);
				}

				applyFieldValue(field, value, cData);
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityRegExParser.failed.parsing.regex", field), 0);
			pe.initCause(e);
			throw pe;
		}
	}

	/**
	 * Resolves all available RegEx matches found in activity data.
	 * <p>
	 * Found matches map key is match group index or name, value is matched activity data substring.
	 * 
	 * @param matcher
	 *            regex matcher to be used to find matches
	 * @return map of found matches
	 * @throws ParseException
	 *             if exception occurs while finding RegEx matches
	 *
	 * @see java.util.regex.Matcher#group(int)
	 */
	protected Map<String, String> findMatches(Matcher matcher) throws ParseException {
		try {
			Map<String, Integer> namedGroupsMap = getNamedGroups(pattern);
			Map<String, String> matches = new HashMap<>(10);

			matcher.reset();
			while (matcher.find()) {
				int gc = matcher.groupCount();
				for (int gi = 1; gi <= gc; gi++) {
					String matchStr = matcher.group(gi);
					if (matchStr != null) {
						addMatchEntry(matches, String.valueOf(gi), matchStr);

						if (namedGroupsMap != null) {
							for (Map.Entry<String, Integer> namedGroup : namedGroupsMap.entrySet()) {
								if (gi == namedGroup.getValue()) {
									addMatchEntry(matches, namedGroup.getKey(), matchStr);
								}
							}
						}
					}
				}
			}
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityRegExParser.found.matches", matches.size());

			return matches;
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityRegExParser.failed.parsing.matches"), 0);
			pe.initCause(e);
			throw pe;
		}
	}

	private void addMatchEntry(Map<String, String> matches, String matchKey, String matchStr) {
		matches.put(matchKey, matchStr);
		logger().log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"ActivityRegExParser.match", matchKey, matchStr);
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
	 *
	 * @see java.util.regex.Matcher#group(int)
	 * @see java.util.regex.Matcher#group(String)
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) {
		Object val = null;
		String locStr = locator.getLocator();

		if (StringUtils.isNotEmpty(locStr)) {
			if (cData.containsKey(MATCHES_KEY)) {
				Map<String, String> matches = (Map<String, String>) cData.get(MATCHES_KEY);
				val = matches.get(locStr);
			} else {
				Matcher matcher = (Matcher) cData.getData();
				ActivityFieldLocatorType locType = locator.getBuiltInType();

				if (locType != null && locType.getDataType() == Integer.class) {
					val = groupIndex(locStr, matcher);
				} else if (locType != null && locType.getDataType() == String.class) {
					val = groupName(locStr, matcher);
				} else {
					try {
						val = groupIndex(locStr, matcher);
					} catch (NumberFormatException exc) {
						val = groupName(locStr, matcher);
					}
				}
			}
		}

		return val;
	}

	private static String groupIndex(String locStr, Matcher matcher) {
		int loc = Integer.parseInt(locStr);
		if (loc >= 0 && loc <= matcher.groupCount()) {
			return matcher.group(loc);
		}

		return null;
	}

	private static String groupName(String locStr, Matcher matcher) {
		return matcher.group(locStr);
	}

	/**
	 * Retrieves named groups map from <tt>regexPattern</tt>.
	 * <p>
	 * Named groups map key is group name, value is group index.
	 * 
	 * @param regexPattern
	 *            pattern to get group names
	 * @return map of named groups, or {@code null} if no named groups defined
	 * @throws java.lang.Exception
	 *             if error occurs while retrieving named groups map from pattern
	 */
	@SuppressWarnings("unchecked")
	protected static Map<String, Integer> getNamedGroups(Pattern regexPattern) throws Exception {
		Method namedGroupsMethod = Pattern.class.getDeclaredMethod("namedGroups");
		namedGroupsMethod.setAccessible(true);

		Map<String, Integer> namedGroups = (Map<String, Integer>) namedGroupsMethod.invoke(regexPattern);

		return namedGroups == null ? null : Collections.unmodifiableMap(namedGroups);
	}

	/**
	 * Strategies used to verify if pattern matches input data string.
	 */
	protected enum Strategy {
		/**
		 * Complete input data string matches defined pattern.
		 */
		MATCH,

		/**
		 * Part of input string matches defined pattern.
		 */
		FIND
	}
}
