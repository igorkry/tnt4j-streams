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

import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Generic class for common activity parsers. It provides some generic functionality witch is common to most activity
 * parsers.
 *
 * @param <T>
 *            the type of handled activity data
 * @version $Revision: 1 $
 */
public abstract class GenericActivityParser<T> extends ActivityParser {

	/**
	 * Constant for default delimiter symbol used by parsers.
	 */
	protected static final String DEFAULT_DELIM = ","; // NON-NLS

	/**
	 * List of supported activity fields used to extract values from RAW activity data defined by field location(s).
	 */
	protected final List<ActivityField> fieldList = new ArrayList<ActivityField>();

	/**
	 * {@inheritDoc}
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.lang.String}</li>
	 * <li>{@code byte[]}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.InputStream}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return String.class.isInstance(data) || byte[].class.isInstance(data) || Reader.class.isInstance(data)
				|| InputStream.class.isInstance(data);
	}

	@Override
	public void addField(ActivityField field) {
		if (field == null) {
			return;
		}

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.adding.field"),
				field); // Utils.getDebugString(field));

		for (ActivityField aField : fieldList) {
			if (aField.getFieldTypeName().equals(field.getFieldTypeName())
					&& !Utils.isCollectionType(aField.getFieldType().getDataType())) {
				throw new IllegalArgumentException(
						StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
								"ActivityParser.duplicate.field", getName(), aField.getFieldTypeName()));
			}
		}

		fieldList.add(field);
	}

	protected void removeField(ActivityField field) {
		if (field == null) {
			return;
		}

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.removing.field"),
				field); // Utils.getDebugString(field));
		fieldList.remove(field);
	}

	/**
	 * Parse the specified prepared activity data, converting each field in prepared data to its corresponding value of
	 * activity info item.
	 *
	 * @param stream
	 *            stream providing activity data
	 * @param dataStr
	 *            raw activity data string
	 * @param data
	 *            prepared activity data item to parse
	 * @return converted activity info, or {@code null} if activity data is {@code null}
	 * @throws ParseException
	 *             if error applying locator format properties to specified value
	 */
	protected ActivityInfo parsePreparedItem(TNTInputStream<?, ?> stream, String dataStr, T data)
			throws ParseException {
		if (data == null) {
			return null;
		}

		ActivityInfo ai = new ActivityInfo();
		ActivityField field = null;
		try {
			if (dataStr != null) {
				// save entire activity string as message data
				field = new ActivityField(StreamFieldType.Message.name());
				applyFieldValue(stream, ai, field, dataStr);
			}

			// apply fields for parser
			Object value;
			for (int fi = 0; fi < fieldList.size(); fi++) {
				field = fieldList.get(fi);
				value = wrapValue(resolveLocatorValues(field, stream, data));

				if (field.isDynamic() || (field.isSplitCollection() && Utils.isCollection(value))) {
					handleDynamicValues(stream, data, ai, field, value);
				} else {
					applyFieldValue(stream, ai, field, value, field.getValueType());
				}
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.parsing.failed", field), 0);
			pe.initCause(e);
			throw pe;
		}

		return ai;
	}

	private void handleDynamicValues(TNTInputStream<?, ?> stream, T data, ActivityInfo ai, ActivityField field,
			Object value) throws ParseException {
		Map<String, Object> dValMap = resolveDynamicValues(stream, data, field.getDynamicLocators());

		Object[] fValues = Utils.makeArray(value);

		List<ActivityField> tFieldsList = new ArrayList<ActivityField>();
		for (int vi = 0; vi < fValues.length; vi++) {
			ActivityField tField = field.createTempField(dValMap, vi);
			tFieldsList.add(tField);
		}

		reviewTempFieldsNames(tFieldsList);

		for (int tfi = 0; tfi < tFieldsList.size(); tfi++) {
			ActivityField tField = tFieldsList.get(tfi);
			Object fValue = fValues[tfi];

			applyFieldValue(stream, ai, tField, wrapValue(fValue), tField.getValueType());
		}
	}

	private static void reviewTempFieldsNames(List<ActivityField> tFieldsList) {
		if (tFieldsList != null) {
			int tid = 0;
			for (int tfi = 0; tfi < tFieldsList.size() - 1; tfi++) {
				ActivityField tField = tFieldsList.get(tfi);
				String tFieldName = tField.getFieldTypeName();
				String newName = null;

				for (int ntfi = tfi + 1; ntfi < tFieldsList.size(); ntfi++) {
					ActivityField ntField = tFieldsList.get(ntfi);
					String ntFieldName = ntField.getFieldTypeName();
					if (tFieldName.equals(ntFieldName)) {
						if (newName == null) {
							newName = tFieldName + (tid++);
						}

						ntField.setFieldTypeName(ntFieldName + (tid++));
					}
				}

				if (StringUtils.isNotEmpty(newName)) {
					tField.setFieldTypeName(newName);
				}
			}
		}
	}

	/**
	 * Resolves field values from prepared activity data item using field bound locators.
	 *
	 * @param field
	 *            field instance to resolve values
	 * @param stream
	 *            stream providing activity data
	 * @param data
	 *            prepared activity data item to parse
	 * @return resolved values array
	 * @throws ParseException
	 *             if error applying locator format properties to specified value
	 */
	protected Object[] resolveLocatorValues(ActivityField field, TNTInputStream<?, ?> stream, T data)
			throws ParseException {
		return resolveLocatorValues(field.getLocators(), stream, data);
	}

	/**
	 * Resolves field values from prepared activity data item using field bound locators.
	 *
	 * @param locations
	 *            value locators list
	 * @param stream
	 *            stream providing activity data
	 * @param data
	 *            prepared activity data item to parse
	 * @return resolved values array
	 * @throws ParseException
	 *             if error applying locator format properties to specified value
	 */
	protected Object[] resolveLocatorValues(List<ActivityFieldLocator> locations, TNTInputStream<?, ?> stream, T data)
			throws ParseException {
		if (locations != null) {
			Object[] values = new Object[locations.size()];
			for (int li = 0; li < locations.size(); li++) {
				values[li] = getLocatorValue(stream, locations.get(li), data);
			}
			return values;
		}

		return null;
	}

	private Map<String, Object> resolveDynamicValues(TNTInputStream<?, ?> stream, T data,
			Map<String, ActivityFieldLocator> dynamicLocators) throws ParseException {
		Map<String, Object> dynamicValuesMap = new HashMap<String, Object>();
		if (dynamicLocators != null) {
			for (Map.Entry<String, ActivityFieldLocator> dLocator : dynamicLocators.entrySet()) {
				final Object dynamicLocatorValue = wrapValue(getLocatorValue(stream, dLocator.getValue(), data));
				if (dynamicLocatorValue != null) {
					dynamicValuesMap.put(dLocator.getKey(), dynamicLocatorValue);
				}
			}
		}

		return dynamicValuesMap;
	}

	/**
	 * Gets field value from raw data location and formats it according locator definition.
	 *
	 * @param stream
	 *            stream providing activity data
	 * @param locator
	 *            activity field locator
	 * @param data
	 *            activity object data
	 * @return value formatted based on locator definition or {@code null} if locator is not defined
	 * @throws ParseException
	 *             if error applying locator format properties to specified value
	 * @see ActivityFieldLocator#formatValue(Object)
	 */
	protected Object getLocatorValue(TNTInputStream<?, ?> stream, ActivityFieldLocator locator, T data)
			throws ParseException {
		Object val = null;
		if (locator != null) {
			String locStr = locator.getLocator();
			AtomicBoolean formattingNeeded = new AtomicBoolean(true);
			if (StringUtils.isNotEmpty(locStr)) {
				if (locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp) {
					val = stream.getProperty(locStr);
				} else {
					val = resolveLocatorValue(locator, data, formattingNeeded);

					logger().log(val == null && !locator.isOptional() ? OpLevel.WARNING : OpLevel.TRACE,
							StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
									"ActivityParser.locator.resolved"),
							locStr, String.valueOf(val));
				}
			}

			if (formattingNeeded.get()) {
				val = locator.formatValue(val);
			}
		}
		return val;
	}

	/**
	 * Wraps locator prepared value if it is list or array.
	 *
	 * @param value
	 *            locator prepared value
	 * @return extracts actual object if list/array contains single item, array of values if list/array contains more
	 *         than one item, {@code null} if list/array is {@code null} or empty.
	 */
	protected static Object wrapValue(Object value) {
		if (value instanceof List) {
			return wrapValue((List<?>) value);
		}
		if (value instanceof Object[]) {
			return wrapValue((Object[]) value);
		}

		return value;
	}

	private static Object wrapValue(List<?> valuesList) {
		if (CollectionUtils.isEmpty(valuesList)) {
			return null;
		}

		return valuesList.size() == 1 ? valuesList.get(0) : valuesList.toArray();
	}

	private static Object wrapValue(Object[] valuesArray) {
		if (ArrayUtils.isEmpty(valuesArray)) {
			return null;
		}

		return valuesArray.length == 1 ? valuesArray[0] : valuesArray;
	}

	/**
	 * Gets field raw data value resolved by locator.
	 *
	 * @param locator
	 *            activity field locator
	 * @param data
	 *            activity object data
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 *
	 * @throws ParseException
	 *             if error appears while resolving raw data value
	 *
	 * @see ActivityFieldLocator#formatValue(Object)
	 */
	protected abstract Object resolveLocatorValue(ActivityFieldLocator locator, T data, AtomicBoolean formattingNeeded)
			throws ParseException;
}
