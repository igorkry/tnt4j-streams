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
import java.util.regex.Pattern;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Base class for abstract activity data parser that assumes each activity data item can be transformed into an
 * {@link Map} data structure, where each field is represented by a key/value pair and the name is used to map each
 * field onto its corresponding activity field.
 * <p>
 * If map entry value is inner map, entries of that map can be accessed using '.' as naming hierarchy delimiter: i.e.
 * 'headers.auth.name'. Locator path delimiter value can be configured over parser 'LocPathDelim' property.
 * <p>
 * This parser supports the following properties:
 * <ul>
 * <li>LocPathDelim - locator path in map delimiter. Empty value means locator value should not be delimited into path
 * elements. Default value - '.'. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractActivityMapParser extends GenericActivityParser<Map<String, ?>> {

	private static final String DEFAULT_NODE_PATH_DELIM = "."; // NON-NLS

	private String nodePathDelim = DEFAULT_NODE_PATH_DELIM;

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
			}
		}
	}

	@Override
	public ActivityInfo parse(TNTInputStream<?, ?> stream, Object data) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}
		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.parsing"), data);

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
	 * Gets field value from raw data location and formats it according locator definition.
	 * 
	 * @param stream
	 *            parent stream
	 * @param locator
	 *            activity field locator
	 * @param dataMap
	 *            activity object data map
	 *
	 * @return value formatted based on locator definition or {@code null} if locator is not defined
	 *
	 * @throws ParseException
	 *             if error applying locator format properties to specified value
	 *
	 * @see ActivityFieldLocator#formatValue(Object)
	 */
	@Override
	protected Object getLocatorValue(TNTInputStream<?, ?> stream, ActivityFieldLocator locator, Map<String, ?> dataMap)
			throws ParseException {
		Object val = null;
		if (locator != null) {
			String locStr = locator.getLocator();
			if (!StringUtils.isEmpty(locStr)) {
				if (locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp) {
					val = stream.getProperty(locStr);
				} else {
					String[] path = getNodePath(locStr, nodePathDelim);
					val = getNode(path, dataMap, 0);
				}
			}
			val = locator.formatValue(val);
		}

		return val;
	}

	private static String[] getNodePath(String locStr, String nps) {
		if (StringUtils.isNotEmpty(locStr)) {
			// Pattern.quote(nps);
			return StringUtils.isEmpty(nps) ? new String[] { locStr } : locStr.split(Pattern.quote(nps));
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
}
