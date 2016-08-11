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

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.fields.DynamicNameActivityField;
import com.jkoolcloud.tnt4j.streams.fields.StreamFieldType;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Generic class for common activity parsers. It provides some generic
 * functionality witch is common to most activity parsers.
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
	 * List of supported activity fields used to extract values from RAW
	 * activity data defined by field location(s).
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
	 * {@inheritDoc}
	 * <p>
	 * This parser supports the following class types (and all classes
	 * extending/implementing any of these):
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
	 * Parse the specified prepared activity data, converting each field in
	 * prepared data to its corresponding value of activity info item.
	 *
	 * @param stream
	 *            parent stream
	 * @param dataStr
	 *            raw activity data string
	 * @param data
	 *            prepared activity data item to parse
	 * @return converted activity info, or {@code null} if activity data is
	 *         {@code null}
	 * @throws ParseException
	 *             if error applying locator format properties to specified
	 *             value
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
			final List<ActivityField> fieldListCopy = new ArrayList<ActivityField>(fieldList);
			for (ActivityField aField : fieldListCopy) {
				List<DynamicNameActivityField> names = null;
				final List<ActivityFieldLocator> nameLocators;
				if (aField instanceof DynamicNameActivityField) {
					nameLocators = ((DynamicNameActivityField) aField).getNameLocators();
					if (aField instanceof DynamicNameActivityField && ((DynamicNameActivityField) aField).isCreated() ) {
						fieldList.remove(aField);
					}
					if (nameLocators == null) continue;
				} else continue;
				
				resolveFieldNames(stream, data, names, nameLocators, aField);
			}
			
			// apply fields for parser
			Object value;
			for (ActivityField aFieldList : fieldList) {
				value = null;
				field = aFieldList;
				List<ActivityFieldLocator> locations = field.getLocators();
				if (locations != null) {
					// For dynamic naming of activities field properties Originally used for collectD
					if (aFieldList instanceof DynamicNameActivityField) {
						value = getLocatorValue(stream, locations.get(0), data);
						if (value.getClass().isArray()) {
							Object[] values = (Object[]) value;
							try {
								value = wrapValue(values[((DynamicNameActivityField) aFieldList).index]);
								if (value.getClass().isArray()) {
									value = wrapValue((((Object[]) value)[((DynamicNameActivityField) aFieldList).dept]));
								}
							} catch (IndexOutOfBoundsException ex) {
								throw new ParseException(StreamsResources.getStringFormatted(
										StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.fields.failed"), 0);
							}
						}
						applyFieldValue(stream, ai, field, value);
						continue;
					}
					if (locations.size() == 1) {
						// field value is based on single raw data location, get
						// the value of this location
						value = getLocatorValue(stream, locations.get(0), data);
					} else {
						// field value is based on concatenation of several raw
						// data locations, build array to hold data from each
						// location
						Object[] values = new Object[locations.size()];
						for (int li = 0; li < locations.size(); li++) {
							values[li] = getLocatorValue(stream, locations.get(li), data);
						}
						value = values;
					}
				}
				applyFieldValue(stream, ai, field, value);
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.parsing.failed", field), 0);
			pe.initCause(e);
			throw pe;
		}

		return ai;
	}



	/** Gets all name locators specified and return's single name for a field
	 *  
	 * @param stream consumable stream
	 * @param data 
	 * 				parsable data
	 * @param names	
	 * 				List of fields to resolve
	 * @param nameLocators 
	 * 				name Locators
	 * @param aField 
	 * 				activity field
	 * @return
	 * @throws ParseException
	 */
	
	private List<DynamicNameActivityField> resolveFieldNames(TNTInputStream<?, ?> stream, T data, List<DynamicNameActivityField> names,
			final List<ActivityFieldLocator> nameLocators, ActivityField aField) throws ParseException {
		
		int index = 1;
		for (ActivityFieldLocator locator : nameLocators) {
			// get all field locators name values
			final Object nameLocatorValue = getLocatorValue(stream, locator, data);
			if (nameLocatorValue == null)
				continue;
			
			if (nameLocatorValue.getClass().isArray()) {
				// If locator contains multiple fields, build names array
				Object[] nameLocatorValues = (Object[]) nameLocatorValue;
				names = createOrAppend(names, aField, index, nameLocatorValues, 0);
			} else {
				if (names == null) {
					names = new ArrayList<DynamicNameActivityField>(1);
					final DynamicNameActivityField dinamicallyNamedField = new DynamicNameActivityField(aField.getFieldTypeName(), aField.getLocators(), 0, 0);
					names.add(dinamicallyNamedField);
				} else {
					//append
					names.get(0).appendName(nameLocatorValue.toString(), 0);
				}

			}
			index++;
		}
		return names;
	}

	/**
	 * 	Creates or append activity field names. In case of multiple name locators - resolves activity name based on name template.
	 * 
	 * @param names
	 * @param aField
	 * @param index
	 * @param namLocatorValues
	 * @param depth
	 * @return
	 * @throws ParseException
	 */
	
	private List<DynamicNameActivityField> createOrAppend(List<DynamicNameActivityField> names, ActivityField aField,
			int index, Object[] namLocatorValues, int depth) throws ParseException {
		 if (names == null) {
			// if its new create one
			names = new ArrayList<DynamicNameActivityField>(namLocatorValues.length);
			for (int i = 0; i < namLocatorValues.length ; i++) {
				final Object fieldNamePart = wrapValue(namLocatorValues[i]);
				if (fieldNamePart.getClass().isArray()) {
					Object[] fieldNamePart2 = (Object[]) fieldNamePart;
					//TODO recursive multiple levels
					for (int j = 0; j < fieldNamePart2.length; j++) {
						Object elementFieldNamePart = fieldNamePart2[j];
						final DynamicNameActivityField field = new DynamicNameActivityField(aField.getFieldTypeName(), aField.getLocators(), 0, 1);
						field.appendName(elementFieldNamePart.toString(), 1);
						names.add(field);
					}
				} else {
					final DynamicNameActivityField field = new DynamicNameActivityField(aField.getFieldTypeName(), aField.getLocators(), 0, 1);
					field.appendName(fieldNamePart.toString(), 1);
					names.add(field);
				}
			}
		} else {
			// if its already building append
			for (DynamicNameActivityField name : names) {
				Object namePart = wrapValue(namLocatorValues[name.index]);
				if (namePart.getClass().isArray()) {
					Object[] namepart2 = (Object[]) namePart;
					namePart = namepart2[name.dept];
				}
				name.appendName(namePart.toString() , index);
			}
		}
		return names;
	}

	/**
	 * Gets field value from raw data location and formats it according locator
	 * definition.
	 *
	 * @param stream
	 *            parent stream
	 * @param locator
	 *            activity field locator
	 * @param data
	 *            activity object data
	 * @return value formatted based on locator definition or {@code null} if
	 *         locator is not defined
	 * @throws ParseException
	 *             if error applying locator format properties to specified
	 *             value
	 * @see ActivityFieldLocator#formatValue(Object)
	 */
	protected abstract Object getLocatorValue(TNTInputStream<?, ?> stream, ActivityFieldLocator locator, T data)
			throws ParseException;

	/**
	 * Wraps list of locator prepared values.
	 *
	 * @param valuesList
	 *            list of values
	 * @return extracts actual object if list contains single item, array of
	 *         values if list contains more than one item, {@code null} if list
	 *         is empty or {@code valuesList == null}.
	 */
	protected static Object wrapValue(List<Object> valuesList) {
		if (CollectionUtils.isEmpty(valuesList)) {
			return null;
		}

		return valuesList.size() == 1 ? valuesList.get(0) : valuesList.toArray();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected static Object wrapValue(Object value) {
		if (value instanceof List) {
			return wrapValue((List<Object>) value);
		}
		if (!value.getClass().isArray())
			return value;
		Object[] valuesList = (Object[]) value; 
		if (valuesList == null || valuesList.length ==0) {
			return null;
		}

		return valuesList.length == 1 ? valuesList[0] : valuesList;
	}
}
