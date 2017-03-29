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

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements an activity data parser that assumes each activity data item is an plain java {@link Object} data
 * structure, where each field is represented by declared class field and the field name is used to map each field onto
 * its corresponding activity field.
 * <p>
 * If field is complex object, subfields can be accessed using
 * '{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM}' as naming hierarchy separator: i.e.
 * 'header.author.name'.
 *
 * @version $Revision: 1 $
 */
public class ActivityJavaObjectParser extends GenericActivityParser<Object> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityJavaObjectParser.class);

	/**
	 * Constructs a new ActivityJavaObjectParser.
	 *
	 */
	public ActivityJavaObjectParser() {
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

		// for (Map.Entry<String, String> prop : props) {
		// String name = prop.getKey();
		// String value = prop.getValue();
		//
		// // no any additional properties are required yet.
		// if (false) {
		// logger().log(OpLevel.DEBUG,
		// StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
		// name, value);
		// }
		// }
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.lang.Object}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return Object.class.isInstance(data);
	}

	@Override
	public ActivityInfo parse(TNTInputStream<?, ?> stream, Object data) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}

		data = preParse(stream, data);

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.parsing"),
				getLogString(data));

		String dataStr = ToStringBuilder.reflectionToString(data, ToStringStyle.MULTI_LINE_STYLE);
		ActivityInfo ai = parsePreparedItem(stream, dataStr, data);
		postParse(ai, stream, data);

		return ai;
	}

	/**
	 * Gets field raw data value resolved by locator.
	 *
	 * @param locator
	 *            activity field locator
	 * @param dataObj
	 *            activity data carrier object
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, Object dataObj, AtomicBoolean formattingNeeded) {
		Object val = null;
		String locStr = locator.getLocator();
		String[] path = Utils.getNodePath(locStr, StreamsConstants.DEFAULT_PATH_DELIM);
		val = getFieldValue(path, dataObj, 0);

		return val;
	}

	private static Object getFieldValue(String[] path, Object dataObj, int i) {
		if (ArrayUtils.isEmpty(path) || dataObj == null) {
			return null;
		}

		try {
			Field f = dataObj.getClass().getDeclaredField(path[i]);
			Object obj = f.get(dataObj);

			if (i < path.length - 1 && !f.getType().isPrimitive()) {
				obj = getFieldValue(path, obj, ++i);
			}

			return obj;
		} catch (Exception exc) {
			LOGGER.log(OpLevel.WARNING,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ActivityJavaObjectParser.could.not.get.declared.field"),
					path[i], dataObj.getClass().getSimpleName(), toString(dataObj));
			return null;
		}
	}

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - OBJECT
	 */
	@Override
	protected String getActivityDataType() {
		return "OBJECT"; // NON-NLS
	}
}
