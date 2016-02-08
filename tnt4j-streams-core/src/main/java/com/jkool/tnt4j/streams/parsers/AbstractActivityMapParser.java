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

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkool.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Base class for abstract activity data parser that assumes each activity data
 * item can be transformed into an {@code Map} data structure, where each field
 * is represented by a key/value pair and the name is used to map each field
 * onto its corresponding activity field.
 * <p>
 * If map entry value is inner map, entries of that map can be accessed using
 * '.' as naming hierarchy separator: i.e. 'headers.auth.name'.
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractActivityMapParser extends GenericActivityParser<Map<String, ?>> {

	/**
	 * Constructs a new AbstractActivityMapParser.
	 * 
	 * @param logger
	 *            logger used by activity parser
	 */
	protected AbstractActivityMapParser(EventSink logger) {
		super(logger);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		// for (Map.Entry<String, String> prop : props) {
		// String name = prop.getKey();
		// String value = prop.getValue();
		//
		// // no any additional properties are required yet.
		// }
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActivityInfo parse(TNTInputStream stream, Object data) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}
		logger.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
				"ActivityParser.parsing", data));

		Map<String, ?> dataMap = getDataMap(data);
		if (dataMap == null || dataMap.isEmpty()) {
			logger.log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "ActivityParser.not.find"));
			return null;
		}

		return parsePreparedItem(stream, dataMap.toString(), dataMap);
	}

	/**
	 * Makes map data package containing data of specified activity object.
	 *
	 * @param data
	 *            activity object data
	 *
	 * @return activity object data map
	 */
	protected abstract Map<String, ?> getDataMap(Object data);

	/**
	 * Gets field value from raw data location and formats it according locator
	 * definition.
	 * 
	 * @param stream
	 *            parent stream
	 * @param locator
	 *            activity field locator
	 * @param dataMap
	 *            activity object data map
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
	protected Object getLocatorValue(TNTInputStream stream, ActivityFieldLocator locator, Map<String, ?> dataMap)
			throws ParseException {
		Object val = null;
		if (locator != null) {
			String locStr = locator.getLocator();
			if (!StringUtils.isEmpty(locStr)) {
				if (locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp) {
					val = stream.getProperty(locStr);
				} else {
					String[] path = getNodePath(locStr);
					val = getNode(path, dataMap, 0);
				}
			}
			val = locator.formatValue(val);
		}

		return val;
	}

	private static String[] getNodePath(String locStr) {
		if (StringUtils.isNotEmpty(locStr)) {
			return locStr.split("\\."); // NON-NLS
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private static Object getNode(String[] path, Map<String, ?> dataMap, int i) {
		if (ArrayUtils.isEmpty(path) || dataMap == null) {
			return null;
		}

		Object val = dataMap.get(path[i]);

		if (i < path.length - 1 && val instanceof Map) {
			val = getNode(path, (Map<String, ?>) val, ++i);
		}

		return val;
	}
}
