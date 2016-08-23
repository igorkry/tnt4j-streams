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
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

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
	 * Constructs a new GenericActivityParser.
	 *
	 * @param logger
	 *            logger used by activity parser
	 */
	protected GenericActivityParser(EventSink logger) {
		super(logger);
	}

	/**
	 * {@inheritDoc} This parser supports the following class types (and all classes extending/implementing any of
	 * these):
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
		logger.log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.adding.field"),
				field); // Utils.getDebugString(field));
		fieldList.add(field);
	}

	/**
	 * Parse the specified prepared activity data, converting each field in prepared data to its corresponding value of
	 * activity info item.
	 *
	 * @param stream
	 *            parent stream
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
			// name locators handling
			List<ActivityField> fieldListCopy = new ArrayList<ActivityField>(fieldList);
			// remove old first
			for (ActivityField aField2 : fieldListCopy) {
				if (aField2 instanceof DynamicActivityField) {
					if (((DynamicActivityField) aField2).isSynthetic()) {
						fieldList.remove(aField2);
					} else {
						continue;
					}
				}
			}
			fieldListCopy = new ArrayList<ActivityField>(fieldList);
			// remove old first
			for (ActivityField aField : fieldListCopy) {
				List<DynamicActivityField> names = null;
				final List<ActivityFieldLocator> nameLocators;
				if (aField instanceof DynamicActivityField && !((DynamicActivityField) aField).isSynthetic()) {
					nameLocators = ((DynamicActivityField) aField).getNameLocators();
					if (nameLocators == null) {
						continue;
					}
					resolveFieldNames(stream, data, names, nameLocators, aField);
				}
			}

			// apply fields for parser
			Object value;
			for (ActivityField aFieldList : fieldList) {
				value = null;
				field = aFieldList;
				String valueType = null;
				List<ActivityFieldLocator> locations = field.getLocators();
				if (field.getValueTypeLocator() != null) {
					Object valueTypeValue = wrapValue(getLocatorValue(stream, field.getValueTypeLocator(), data));
					if (valueTypeValue.getClass().isArray()) {
						valueTypeValue = ((Object[]) valueTypeValue)[0];
					}
					valueType = valueTypeValue.toString();
				}
				if (locations != null) {
					// For dynamic naming of activities field properties
					// Originally used for collectD
					if (aFieldList instanceof DynamicActivityField) {
						value = wrapValue(getLocatorValue(stream, locations.get(0), data));
						if (value instanceof Object[]) {
							Object[] values = (Object[]) value;
							try {
								value = wrapValue(values[((DynamicActivityField) aFieldList).getIndex()]);
							} catch (IndexOutOfBoundsException ex) {
								throw new ParseException(StreamsResources.getStringFormatted(
										StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.fields.failed"), 0);
							}
						}
						applyFieldValue(stream, ai, field, value, valueType);
						continue;
					}
					Object[] values = new Object[locations.size()];
					for (int li = 0; li < locations.size(); li++) {
						values[li] = getLocatorValue(stream, locations.get(li), data);
					}
					value = values;
				}
				applyFieldValue(stream, ai, field, wrapValue(value), valueType);
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.parsing.failed", field), 0);
			pe.initCause(e);
			throw pe;
		}

		return ai;
	}

	/**
	 * Gets all name locators specified and return's single name for a field
	 *
	 * @param stream
	 *            consumable stream
	 * @param data
	 *            parseable data
	 * @param names
	 *            List of fields to resolve
	 * @param nameLocators
	 *            name Locators
	 * @param aField
	 *            activity field
	 * @return
	 * @throws ParseException
	 */
	private List<DynamicActivityField> resolveFieldNames(TNTInputStream<?, ?> stream, T data,
			List<DynamicActivityField> names, final List<ActivityFieldLocator> nameLocators, ActivityField aField)
			throws ParseException {

		int nameLocatorIndex = 1;
		for (ActivityFieldLocator locator : nameLocators) {
			// get all field locators name values
			final Object nameLocatorValue = wrapValue(getLocatorValue(stream, locator, data));
			if (nameLocatorValue == null) {
				continue;
			}
			if (nameLocatorValue instanceof Object[]) {
				// If locator contains multiple fields, build names array
				Object[] nameLocatorValues = (Object[]) nameLocatorValue;
				names = createOrAppend(names, aField, nameLocatorIndex, nameLocatorValues);
			} else {
				// Field contains single locator
				if (names == null) {
					names = new ArrayList<DynamicActivityField>(1);
					final DynamicActivityField dynamicallyNamedField = new DynamicActivityField(aField,
							nameLocatorIndex);
					dynamicallyNamedField.appendName(nameLocatorValue.toString(), 0);
					names.add(dynamicallyNamedField);
					fieldList.add(dynamicallyNamedField);
				} else {
					// append
					names.get(0).appendName(nameLocatorValue.toString(), 0);
				}

			}
			nameLocatorIndex++;
		}
		return names;
	}

	/**
	 * Creates or append activity field names. In case of multiple name locators - resolves activity name based on name
	 * template.
	 *
	 * @param names
	 * @param aField
	 * @param nameLocatorIndex
	 * @param namLocatorValues
	 *
	 * @return
	 * @throws ParseException
	 */

	private List<DynamicActivityField> createOrAppend(List<DynamicActivityField> names, ActivityField aField,
			int nameLocatorIndex, Object[] namLocatorValues) throws ParseException {
		if (names == null) {
			// if new, create one
			names = new ArrayList<DynamicActivityField>(namLocatorValues.length);
			for (int i = 0; i < namLocatorValues.length; i++) {
				final Object fieldNamePart = wrapValue(namLocatorValues[i]);
				final DynamicActivityField field = new DynamicActivityField(aField, i);
				field.appendName(fieldNamePart.toString(), i);
				names.add(field);
				fieldList.add(field);
			}
		} else {
			// if already available - append
			for (DynamicActivityField name : names) {
				Object namePart = wrapValue(namLocatorValues[name.getIndex()]);
				name.appendName(namePart.toString(), nameLocatorIndex);
			}
		}
		return names;
	}

	/**
	 * Gets field value from raw data location and formats it according locator definition.
	 *
	 * @param stream
	 *            parent stream
	 * @param locator
	 *            activity field locator
	 * @param data
	 *            activity object data
	 * @return value formatted based on locator definition or {@code null} if locator is not defined
	 * @throws ParseException
	 *             if error applying locator format properties to specified value
	 * @see ActivityFieldLocator#formatValue(Object)
	 */
	protected abstract Object getLocatorValue(TNTInputStream<?, ?> stream, ActivityFieldLocator locator, T data)
			throws ParseException;

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
}
