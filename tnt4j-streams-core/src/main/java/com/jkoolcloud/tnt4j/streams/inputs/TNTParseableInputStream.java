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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.outputs.JKCloudActivityOutput;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Base class that all activity streams performing RAW activity data parsing must extend. It maps RAW activities data to
 * related parsers and controls generic parsing process.
 * <p>
 * This activity stream supports the following properties (in addition to those supported by {@link TNTInputStream}):
 * <ul>
 * <li>HaltIfNoParser - if set to {@code true}, stream will halt if none of the parsers can parse activity object RAW
 * data. If set to {@code false} - puts log entry and continues. Default value - {@code false}. (Optional)</li>
 * </ul>
 *
 * @param <T>
 *            the type of handled RAW activity data
 *
 * @version $Revision: 1 $
 */
public abstract class TNTParseableInputStream<T> extends TNTInputStream<T, ActivityInfo> {

	/**
	 * Map of parsers being used by stream.
	 */
	protected final Map<String, List<ActivityParser>> parsersMap = new LinkedHashMap<>();

	private boolean haltIfNoParser = false;

	@Override
	protected void setDefaultStreamOutput() {
		setOutput(new JKCloudActivityOutput());
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
			if (StreamProperties.PROP_HALT_ON_PARSER.equalsIgnoreCase(name)) {
				haltIfNoParser = Boolean.parseBoolean(value);
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_HALT_ON_PARSER.equals(name)) {
			return haltIfNoParser;
		}

		return super.getProperty(name);
	}

	@Override
	public void addReference(Object refObject) throws IllegalStateException {
		if (refObject instanceof ActivityParser) {
			ActivityParser ap = (ActivityParser) refObject;
			addParser(ap);
		}

		super.addReference(refObject);
	}

	/**
	 * Adds the specified parser to the list of parsers being used by this stream.
	 *
	 * @param parser
	 *            parser to add
	 * @throws IllegalStateException
	 *             if parser can't be added to stream
	 */
	public void addParser(ActivityParser parser) throws IllegalStateException {
		if (parser == null) {
			return;
		}

		String[] tags = parser.getTags();

		if (ArrayUtils.isNotEmpty(tags)) {
			for (String tag : tags) {
				addTaggedParser(tag, parser);
			}
		} else {
			addTaggedParser(parser.getName(), parser);
		}
	}

	/**
	 * Adds specified parsers array to the list of parsers being used by this stream.
	 *
	 * @param parsers
	 *            array of parsers to add
	 * @throws IllegalStateException
	 *             if parser can't be added to stream
	 */
	public void addParser(ActivityParser... parsers) throws IllegalStateException {
		if (parsers == null) {
			return;
		}

		addParsers(Arrays.asList(parsers));
	}

	/**
	 * Adds specified parsers collection to the list of parsers being used by this stream.
	 *
	 * @param parsers
	 *            collection of parsers to add
	 * @throws IllegalArgumentException
	 *             if parser can't be added to stream
	 */
	public void addParsers(Iterable<ActivityParser> parsers) throws IllegalArgumentException {
		if (parsers == null) {
			return;
		}

		for (ActivityParser parser : parsers) {
			addParser(parser);
		}
	}

	private void addTaggedParser(String tag, ActivityParser parser) {
		List<ActivityParser> tpl = parsersMap.get(tag);

		if (tpl == null) {
			tpl = new ArrayList<>();
			parsersMap.put(tag, tpl);
		}

		tpl.add(parser);
	}

	/**
	 * Applies all defined parsers for this stream that support the format that the raw activity data is in the order
	 * added until one successfully matches the specified activity data item.
	 *
	 * @param data
	 *            activity data item to process
	 * @return processed activity data item, or {@code null} if activity data item does not match rules for any parsers
	 * @throws IllegalStateException
	 *             if parser fails to run
	 * @throws ParseException
	 *             if any parser encounters an error parsing the activity data
	 *
	 * @see #getDataTags(Object)
	 * @see #applyParsers(Object, String...)
	 */
	protected ActivityInfo applyParsers(Object data) throws IllegalStateException, ParseException {
		return applyParsers(data, getDataTags(data));
	}

	/**
	 * Applies all defined parsers for this stream that support the format that the raw activity data is in the order
	 * added until one successfully matches the specified activity data item.
	 *
	 * @param data
	 *            activity data item to process
	 * @param tags
	 *            array of tag strings to map activity data with parsers. Can be {@code null}.
	 * @return processed activity data item, or {@code null} if activity data item does not match rules for any parsers
	 * @throws IllegalStateException
	 *             if parser fails to run
	 * @throws ParseException
	 *             if any parser encounters an error parsing the activity data
	 */
	protected ActivityInfo applyParsers(Object data, String... tags) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}

		Set<ActivityParser> parsers = getParsersFor(tags);
		for (ActivityParser parser : parsers) {
			if (parser.isDataClassSupported(data)) {
				ActivityInfo ai = parser.parse(this, data);
				if (ai != null) {
					// NOTE: TNT4J API fails if operation name is null
					if (StringUtils.isEmpty(ai.getEventName())) {
						ai.setEventName(getName() == null ? parser.getName() : getName());
					}

					return ai;
				}
			}
		}
		return null;
	}

	/**
	 * Resolves RAW activity data tag strings array to be used for activity data and parsers mapping.
	 *
	 * @param data
	 *            activity data item to get tags
	 * @return array of activity data found tag strings
	 *
	 * @see #applyParsers(Object, String...)
	 */
	protected String[] getDataTags(Object data) {
		return null;
	}

	private Set<ActivityParser> getParsersFor(String[] tags) {
		Set<ActivityParser> parsersSet = new LinkedHashSet<>();

		if (ArrayUtils.isNotEmpty(tags)) {
			for (String tag : tags) {
				List<ActivityParser> tpl = parsersMap.get(tag);

				if (tpl != null) {
					parsersSet.addAll(tpl);
				}
			}
		}

		if (parsersSet.isEmpty()) {
			Collection<List<ActivityParser>> allParsers = parsersMap.values();

			for (List<ActivityParser> tpl : allParsers) {
				if (tpl != null) {
					parsersSet.addAll(tpl);
				}
			}
		}

		return parsersSet;
	}

	/**
	 * Makes activity information {@link ActivityInfo} object from raw activity data item.
	 * <p>
	 * Default implementation simply calls {@link #applyParsers(Object)} to process raw activity data item.
	 *
	 * @param data
	 *            raw activity data item.
	 * @return activity information object
	 * @throws Exception
	 *             if exception occurs while parsing raw activity data item
	 */
	protected ActivityInfo makeActivityInfo(T data) throws Exception {
		ActivityInfo ai = null;
		if (data != null) {
			try {
				ai = applyParsers(data);
			} catch (ParseException exc) {
				int position = getActivityPosition();
				ParseException pe = new ParseException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.failed.to.process", position), position);
				pe.initCause(exc);
				throw pe;
			}
		}
		return ai;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Performs parsing of raw activity data to {@link ActivityInfo} data package, which can be transformed to
	 * {@link com.jkoolcloud.tnt4j.core.Trackable} object and sent to JKool Cloud using TNT4J and JESL APIs.
	 */
	@Override
	protected void processActivityItem(T item, AtomicBoolean failureFlag) throws Exception {
		notifyProgressUpdate(incrementCurrentActivitiesCount(), getTotalActivities());

		ActivityInfo ai = makeActivityInfo(item);
		if (ai == null) {
			logger().log(OpLevel.WARNING,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.no.parser"),
					item);
			incrementSkippedActivitiesCount();
			if (haltIfNoParser) {
				failureFlag.set(true);
				notifyFailed(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"TNTInputStream.no.parser", item), null, null);
				halt(false);
			} else {
				notifyStreamEvent(OpLevel.WARNING, StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.could.not.parse.activity", item), item);
			}
		} else {
			if (!ai.isFilteredOut()) {
				getOutput().logItem(ai);
			} else {
				logger().log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"TNTInputStream.activity.filtered.out"), ai);
			}
		}
	}
}
