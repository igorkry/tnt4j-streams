/*
 * Copyright 2014-2016 JKOOL, LLC.
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class for abstract activity data parser that assumes each activity data item can be transformed into an
 * {@link Map} data structure, where each field is represented by a key/value pair and the name is used to map each
 * field onto its corresponding activity field.
 * <p>
 * If map entry value is inner map, entries of that map can be accessed using
 * '{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM}' as naming hierarchy delimiter: i.e.
 * 'headers.auth.name'. Locator path delimiter value can be configured over parser 'LocPathDelim' property.
 * <p>
 * This parser supports the following properties:
 * <ul>
 * <li>LocPathDelim - locator path in map delimiter. Empty value means locator value should not be delimited into path
 * elements. Default value - '{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM}'.
 * (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractActivityMapParser extends GenericActivityParser<Map<String, ?>> {

	private String nodePathDelim = StreamsConstants.DEFAULT_PATH_DELIM;

	/**
	 * {@inheritDoc}
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.util.Map}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return Map.class.isInstance(data);
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();

			if (ParserProperties.PROP_LOC_PATH_DELIM.equalsIgnoreCase(name)) {
				nodePathDelim = value;

				logger().log(OpLevel.DEBUG,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
						name, value);
			}
		}
	}

	@Override
	public ActivityInfo parse(TNTInputStream<?, ?> stream, Object data) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}
		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.parsing"),
				getLogString(data));

		Map<String, ?> dataMap = getDataMap(data);
		if (MapUtils.isEmpty(dataMap)) {
			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.not.find"));
			return null;
		}

		return parsePreparedItem(stream, dataMap.toString(), dataMap);
	}

	/**
	 * Makes map data package containing data of specified activity object.
	 *
	 * @param data
	 *            activity object data
	 * @return activity object data map
	 */
	protected abstract Map<String, ?> getDataMap(Object data);

	/**
	 * Gets field raw data value resolved by locator.
	 * 
	 * @param locator
	 *            activity field locator
	 * @param dataMap
	 *            activity object data map
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, Map<String, ?> dataMap,
			AtomicBoolean formattingNeeded) {
		Object val = null;
		String locStr = locator.getLocator();
		String[] path = Utils.getNodePath(locStr, nodePathDelim);
		val = getNode(path, dataMap, 0);

		return val;
	}

	@SuppressWarnings("unchecked")
	private static Object getNode(String[] path, Map<String, ?> dataMap, int i) {
		if (ArrayUtils.isEmpty(path) || dataMap == null) {
			return null;
		}

		Object val = dataMap.get(path[i]);

		if (i < path.length - 1 && val instanceof Map) {
			val = getNode(path, (Map<String, ?>) val, ++i);
		} else if (i < path.length - 2 && val instanceof List) {
			try {
				int lii = Integer.parseInt(getItemIndexStr(path[i + 1]));
				val = getNode(path, (Map<String, ?>) ((List<?>) val).get(lii), i + 2);
			} catch (NumberFormatException exc) {
			}
		}

		return val;
	}

	private static String getItemIndexStr(String indexToken) {
		return indexToken.replaceAll("\\D+", ""); // NON-NLS
	}

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - MAP
	 */
	@Override
	protected String getActivityDataType() {
		return "MAP"; // NON-NLS
	}
}
