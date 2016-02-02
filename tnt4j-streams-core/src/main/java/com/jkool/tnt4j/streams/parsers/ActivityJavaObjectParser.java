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

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang3.StringUtils;

import com.jkool.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements default activity data parser that assumes each activity data item
 * is an plain java {@code Object} data structure, where each field is
 * represented by declared class field and the field name is used to map each
 * field onto its corresponding activity field.
 * <p>
 * If field is complex object, subfields can be accessed using '.' as naming
 * hierarchy separator: i.e. 'header.author.name'.
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

		// for (Map.Entry<String, String> prop : props) {
		// String name = prop.getKey();
		// String value = prop.getValue();
		//
		// // no any additional properties are required yet.
		// }
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This parser supports the following class types (and all classes
	 * extending/implementing any of these):
	 * <ul>
	 * <li>{@code java.lang.Object}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return Object.class.isInstance(data);
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

		String dataStr = ToStringBuilder.reflectionToString(data, ToStringStyle.MULTI_LINE_STYLE);
		return parsePreparedItem(stream, dataStr, data);
	}

	/**
	 * Gets field value from raw data location and formats it according locator
	 * definition.
	 *
	 * @param stream
	 *            parent stream
	 * @param locator
	 *            activity field locator
	 * @param dataObj
	 *            activity data carrier object
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
	@Override
	protected Object getLocatorValue(TNTInputStream stream, ActivityFieldLocator locator, Object dataObj)
			throws ParseException {
		Object val = null;
		if (locator != null) {
			String locStr = locator.getLocator();
			if (!StringUtils.isEmpty(locStr)) {
				if (locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp) {
					val = stream.getProperty(locStr);
				} else {
					String[] path = getNodePath(locStr);
					val = getFieldValue(path, dataObj, 0);
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
		} catch (Throwable exc) {
			LOGGER.log(OpLevel.WARNING,
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ActivityJavaObjectParser.could.not.get.declared.field"),
					path[i], dataObj.getClass().getSimpleName(), exc);
			return null;
		}
	}
}
