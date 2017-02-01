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

import java.io.*;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
	protected final List<ActivityField> fieldList = new ArrayList<>();

	/**
	 * {@inheritDoc}
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.lang.String}</li>
	 * <li>{@code byte[]}</li>
	 * <li>{@link java.nio.ByteBuffer}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.InputStream}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return String.class.isInstance(data) || byte[].class.isInstance(data) || ByteBuffer.class.isInstance(data)
				|| Reader.class.isInstance(data) || InputStream.class.isInstance(data);
	}

	/**
	 * Returns the appropriate string representation for the specified object.
	 * <p>
	 * If {@code data} is byte array, HEX dump representation is returned.
	 * 
	 * @param data
	 *            object to convert to string representation
	 * @return string representation of object
	 */
	protected static String toString(Object data) {
		if (data instanceof byte[]) {
			return Utils.toHexDump((byte[]) data);
		}

		return String.valueOf(data);
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
			StreamFieldType fieldType = aField.getFieldType();
			if (aField.getFieldTypeName().equals(field.getFieldTypeName())
					&& !Utils.isCollectionType(fieldType == null ? null : fieldType.getDataType())) {
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
	 * Reads the next RAW activity data string (line) from the specified data input source.
	 *
	 * @param data
	 *            input source for activity data
	 * @return string, or {@code null} if end of input source has been reached
	 * @throws IllegalArgumentException
	 *             if the class of input source supplied is not supported.
	 */
	protected String getNextActivityString(Object data) {
		if (data == null) {
			return null;
		}
		if (data instanceof String) {
			return (String) data;
		} else if (data instanceof byte[]) {
			return Utils.getString((byte[]) data);
		} else if (data instanceof ByteBuffer) {
			return Utils.getString(((ByteBuffer) data).array());// Utils.getStringLine(data);
		}
		BufferedReader rdr;
		if (data instanceof BufferedReader) {
			rdr = (BufferedReader) data;
		} else if (data instanceof Reader) {
			rdr = new BufferedReader((Reader) data);
		} else if (data instanceof InputStream) {
			rdr = new BufferedReader(new InputStreamReader((InputStream) data));
		} else {
			throw new IllegalArgumentException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ActivityParser.data.unsupported", data.getClass().getName()));
		}

		return readNextActivity(rdr);
	}

	/**
	 * Reads RAW activity data string (line) from {@link BufferedReader}.
	 *
	 * @param rdr
	 *            reader to use for reading
	 * @return non empty RAW activity data text string, or {@code null} if the end of the stream has been reached
	 */
	protected String readNextActivity(BufferedReader rdr) {
		String str = null;
		try {
			str = Utils.getNonEmptyLine(rdr);
		} catch (EOFException eof) {
			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.data.end"),
					getActivityDataType(), eof);
		} catch (IOException ioe) {
			logger().log(OpLevel.WARNING,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.error.reading"),
					getActivityDataType(), ioe);
		}

		return str;
	}

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - TEXT
	 */
	protected String getActivityDataType() {
		return "TEXT"; // NON-NLS
	}

	@Override
	@SuppressWarnings("unchecked")
	public ActivityInfo parse(TNTInputStream<?, ?> stream, Object data) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.parsing"),
				getLogString(data));

		T aData = (T) data;

		return parsePreparedItem(stream, aData.toString(), aData);
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
	 *             if exception occurs applying locator format properties to specified value
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
			for (ActivityField aFieldList : fieldList) {
				field = aFieldList;
				value = Utils.simplifyValue(parseLocatorValues(field, stream, data));

				if (field.isDynamic() || (field.isSplitCollection() && Utils.isCollection(value))) {
					applyDynamicValue(stream, data, ai, field, value);
				} else {
					applyFieldValue(stream, ai, field, value);
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

	private void applyDynamicValue(TNTInputStream<?, ?> stream, T data, ActivityInfo ai, ActivityField field,
			Object value) throws ParseException {
		Map<String, Object> dValMap = parseDynamicValues(stream, data, field.getDynamicLocators());

		Object[] fValues = Utils.makeArray(value);

		List<ActivityField> tFieldsList = new ArrayList<>();
		for (int vi = 0; vi < fValues.length; vi++) {
			ActivityField tField = field.createTempField(dValMap, vi);
			tFieldsList.add(tField);
		}

		reviewTempFieldsNames(tFieldsList);

		for (int tfi = 0; tfi < tFieldsList.size(); tfi++) {
			ActivityField tField = tFieldsList.get(tfi);
			Object fValue = fValues[tfi];

			applyFieldValue(stream, ai, tField, Utils.simplifyValue(fValue));
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
	 * Parses values array from prepared activity data item using field bound locators.
	 *
	 * @param field
	 *            field instance to get locators
	 * @param stream
	 *            stream providing activity data
	 * @param data
	 *            prepared activity data item to parse
	 * @return field locators parsed values array
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 * @see #parseLocatorValues(List, TNTInputStream, Object)
	 */
	protected Object[] parseLocatorValues(ActivityField field, TNTInputStream<?, ?> stream, T data)
			throws ParseException {
		return parseLocatorValues(field.getLocators(), stream, data);
	}

	/**
	 * Parses values array from prepared activity data item using provided locators list.
	 *
	 * @param locations
	 *            value locators list
	 * @param stream
	 *            stream providing activity data
	 * @param data
	 *            prepared activity data item to parse
	 * @return locators parsed values array
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 */
	protected Object[] parseLocatorValues(List<ActivityFieldLocator> locations, TNTInputStream<?, ?> stream, T data)
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

	private Map<String, Object> parseDynamicValues(TNTInputStream<?, ?> stream, T data,
			Map<String, ActivityFieldLocator> dynamicLocators) throws ParseException {
		Map<String, Object> dynamicValuesMap = new HashMap<>();
		if (dynamicLocators != null) {
			for (Map.Entry<String, ActivityFieldLocator> dLocator : dynamicLocators.entrySet()) {
				final Object dynamicLocatorValue = Utils
						.simplifyValue(getLocatorValue(stream, dLocator.getValue(), data));
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
	 *             if exception occurs applying locator format properties to specified value
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

					// logger().log(val == null && !locator.isOptional() ? OpLevel.WARNING : OpLevel.TRACE,
					logger().log(OpLevel.TRACE, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ActivityParser.locator.resolved"), locStr, toString(val));
				}
			}

			if (formattingNeeded.get()) {
				val = locator.formatValue(val);
			}

			try {
				val = locator.transformValue(val);
			} catch (Exception exc) {
				logger().log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityParser.transformation.failed"), locStr, toString(val), exc);
			}
		}
		return val;
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
	 *             if exception occurs while resolving raw data value
	 */
	protected abstract Object resolveLocatorValue(ActivityFieldLocator locator, T data, AtomicBoolean formattingNeeded)
			throws ParseException;

	/**
	 * Makes string representation of data package to put into log.
	 *
	 * @param data
	 *            data package to be logged
	 * @return string representation of data package to be logged
	 */
	protected String getLogString(Object data) {
		return data instanceof String ? data.toString()
				: logger().isSet(OpLevel.TRACE) ? toString(data) : data.getClass().getName();
	}
}
