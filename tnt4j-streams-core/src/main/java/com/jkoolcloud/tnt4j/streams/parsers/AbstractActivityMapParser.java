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
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class for abstract activity data parser that assumes each activity data item can be transformed into an
 * {@link Map} data structure, where each field is represented by a key/value pair and the name is used to map each
 * field into its corresponding activity field.
 * <p>
 * If map entry value is inner map, entries of that map can be accessed using
 * '{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM}' as naming hierarchy delimiter:
 * e.g., 'headers.auth.name'. Locator path delimiter value can be configured over parser 'LocPathDelim' property.
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link GenericActivityParser}):
 * <ul>
 * <li>LocPathDelim - locator path in map delimiter. Empty value means locator value should not be delimited into path
 * elements. Default value - '{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM}'.
 * (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractActivityMapParser extends GenericActivityParser<Map<String, ?>> {
	/**
	 * Constant for map entry locator path delimiter.
	 */
	protected String nodePathDelim = StreamsConstants.DEFAULT_PATH_DELIM;

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.util.Map}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return Map.class.isInstance(data);
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		super.setProperties(props);

		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				String name = prop.getKey();
				String value = prop.getValue();

				if (ParserProperties.PROP_LOC_PATH_DELIM.equalsIgnoreCase(name)) {
					nodePathDelim = value;

					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"ActivityParser.setting", name, value);
				}
			}
		}
	}

	@Override
	protected ActivityContext prepareItem(TNTInputStream<?, ?> stream, Object data) throws ParseException {
		Map<String, ?> dataMap = getDataMap(data);
		if (MapUtils.isEmpty(dataMap)) {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"ActivityParser.no.fields");
			return null;
		}

		ActivityContext cData = new ActivityContext(stream, data, dataMap);
		cData.setMessage(getRawDataAsMessage(dataMap));

		return cData;
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
	 * @param cData
	 *            activity object data map
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) {
		Object val = null;
		String locStr = locator.getLocator();
		val = Utils.getMapValueByPath(locStr, nodePathDelim, cData.getData());

		return val;
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
