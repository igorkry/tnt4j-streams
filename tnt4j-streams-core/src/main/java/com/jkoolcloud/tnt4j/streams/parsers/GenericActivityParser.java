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

import java.io.*;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.filters.StreamFiltersGroup;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.preparsers.ActivityDataPreParser;
import com.jkoolcloud.tnt4j.streams.utils.StreamsCache;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Generic class for common activity parsers. It provides some generic functionality witch is common to most activity
 * parsers.
 * <p>
 * This parser supports the following configuration properties:
 * <ul>
 * <li>UseActivityDataAsMessageForUnset - flag indicating weather RAW activity data shall be put into field 'Message' if
 * there is no mapping defined for that field in stream parser configuration or value was not resolved by parser from
 * RAW activity data. NOTE: it is recommended to use it for DEBUGGING purposes only. For a production version of your
 * software, remove this property form stream parser configuration. Default value - '{@code false}'. (Optional)</li>
 * </ul>
 *
 * @param <T>
 *            the type of handled activity data
 * @version $Revision: 2 $
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
	 * Flag indicating weather RAW activity data shall be put into field 'Message' if there is no mapping defined for
	 * that field in stream parser configuration or value was not resolved by parser from RAW activity data.
	 */
	protected boolean useActivityAsMessage = false;

	private StreamFiltersGroup<ActivityInfo> activityFilter;

	private List<ActivityDataPreParser<?>> preParsers;

	protected final Object NEXT_LOCK = new Object();
	protected final Object FILTER_LOCK = new Object();
	protected final Object PRE_PARSER_LOCK = new Object();

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();

			if (ParserProperties.PROP_USE_ACTIVITY_DATA_AS_MESSAGE_FOR_UNSET.equalsIgnoreCase(name)) {
				useActivityAsMessage = Boolean.parseBoolean(value);

				logger().log(OpLevel.DEBUG,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
						name, value);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see #isDataClassSupportedByParser(Object)
	 * @see #isDataClassSupportedByPreParser(Object)
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return isDataClassSupportedByParser(data) || isDataClassSupportedByPreParser(data);
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.lang.String}</li>
	 * <li>{@code byte[]}</li>
	 * <li>{@link java.nio.ByteBuffer}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.InputStream}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	protected boolean isDataClassSupportedByParser(Object data) {
		return String.class.isInstance(data) || byte[].class.isInstance(data) || ByteBuffer.class.isInstance(data)
				|| Reader.class.isInstance(data) || InputStream.class.isInstance(data);
	}

	/**
	 * Returns whether this parser pre-parsers supports the given format of the RAW activity data. This is used by
	 * activity streams to determine if the parser pre-parses can parse the data in the format that the stream has it.
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser pre-parsers can process data in the specified format, {@code false} -
	 *         otherwise
	 */
	protected boolean isDataClassSupportedByPreParser(Object data) {
		if (CollectionUtils.isNotEmpty(preParsers)) {
			for (ActivityDataPreParser<?> dp : preParsers) {
				if (dp.isDataClassSupported(data)) {
					return true;
				}
			}
		}

		return false;
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

		return Utils.toString(data);
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

		int idx = -1;
		for (int i = 0; i < fieldList.size(); i++) {
			ActivityField af = fieldList.get(i);
			if (af.hasCacheLocators() || af.hasActivityLocators() || af.hasActivityTransformations()) {
				idx = i;
				break;
			}
		}
		fieldList.add(idx >= 0 ? idx : fieldList.size(), field);
	}

	/**
	 * Removed an activity field definition from the set of fields supported by this parser.
	 *
	 * @param field
	 *            activity field to remove
	 */
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
	 * Sets stream filters group used to filter activity data evaluating multiple activity information fields values.
	 * 
	 * @param afg
	 *            activity filters group instance
	 */
	public void setActivityFilter(StreamFiltersGroup<ActivityInfo> afg) {
		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.adding.filter"), afg);

		activityFilter = afg;
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

		synchronized (NEXT_LOCK) {
			try {
				str = Utils.getNonEmptyLine(rdr);
			} catch (EOFException eof) {
				logger().log(OpLevel.DEBUG,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.data.end"),
						getActivityDataType(), eof);
			} catch (IOException ioe) {
				logger().log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityParser.error.reading"), getActivityDataType(), ioe);
			}
		}

		return str;
	}

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - TEXT
	 */
	@Override
	protected String getActivityDataType() {
		return "TEXT"; // NON-NLS
	}

	@Override
	public ActivityInfo parse(TNTInputStream<?, ?> stream, Object data) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.activity.raw.data"),
				getLogString(data));

		data = preParse(stream, data);

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.preparsed.data"),
				getLogString(data));

		ActivityContext cData = prepareItem(stream, data);

		if (cData == null || !cData.isValid()) {
			logger().log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityParser.nothing.to.parse"));
			return null;
		}

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.parsing.data"),
				getLogString(cData.getMessage()));

		ActivityInfo ai = parsePreparedItem(cData);
		fillInMessageData(stream, ai, cData.getMessage());
		postParse(cData);

		return ai;
	}

	/**
	 * Prepares RAW activity data to be parsed.
	 *
	 * @param stream
	 *            stream providing activity data
	 * @param data
	 *            raw activity data to prepare
	 * @return activity data context package
	 */
	@SuppressWarnings("unchecked")
	protected ActivityContext prepareItem(TNTInputStream<?, ?> stream, Object data) throws ParseException {
		T aData = (T) data;

		ActivityContext cData = new ActivityContext(stream, data, aData);
		cData.setMessage(getRawDataAsMessage(aData));

		return cData;
	}

	/**
	 * Performs pre-parse actions on RAW activity data, e.g., conversion using pre-parsers.
	 * 
	 * @param stream
	 *            stream providing activity data
	 * @param data
	 *            raw activity data
	 * @return pre-parsed activity data
	 */
	protected Object preParse(TNTInputStream<?, ?> stream, Object data) {
		try {
			data = preParseActivityData(data);
		} catch (Exception exc) {
			logger().log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityParser.pre.parsing.failed"), exc);
		}

		return data;
	}

	/**
	 * Performs post-parse actions on resolved activity fields data, e.g., filtering.
	 *
	 * @param cData
	 *            prepared activity data item context to parse
	 * @throws java.text.ParseException
	 *             if exception occurs applying field locator resolved cached value
	 */
	protected void postParse(ActivityContext cData) throws ParseException {
		if (cData == null || cData.getActivity() == null) {
			return;
		}

		ActivityInfo ai = cData.getActivity();

		try {
			filterActivity(ai);
		} catch (Exception exc) {
			logger().log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityParser.activity.filtering.failed"), ai, exc);
		}

		if (!ai.isFilteredOut()) {
			StreamsCache.cacheValues(ai, getName());
		}
	}

	/**
	 * Parse the specified prepared activity data, converting each field in prepared data to its corresponding value of
	 * activity info item.
	 *
	 * @param cData
	 *            prepared activity data item context to parse
	 * @return converted activity info, or {@code null} if activity data is {@code null}
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 */
	protected ActivityInfo parsePreparedItem(ActivityContext cData) throws ParseException {
		if (cData == null || cData.getData() == null) {
			return null;
		}

		ActivityInfo ai = new ActivityInfo();
		ActivityField field = null;
		cData.setActivity(ai);
		try {
			// apply fields for parser
			Object value;
			for (ActivityField aField : fieldList) {
				field = aField;
				cData.setField(aField);
				value = Utils.simplifyValue(parseLocatorValues(field, cData));

				applyFieldValue(field, value, cData);
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
	 * Fills in activity entity 'Message' field with activity RAW data when parser has not resolved value for that
	 * field.
	 * <p>
	 * Parser has to be configured to act this way using parser configuration property
	 * 'UseActivityDataAsMessageForUnset'.
	 * 
	 * @param stream
	 *            stream providing activity data
	 * @param ai
	 *            converted activity info
	 * @param dataStr
	 *            raw activity data string
	 * @throws ParseException
	 *             if an error parsing the specified value
	 */
	protected void fillInMessageData(TNTInputStream<?, ?> stream, ActivityInfo ai, String dataStr)
			throws ParseException {
		if (useActivityAsMessage && ai.getMessage() == null && dataStr != null) {
			// save entire activity string as message data
			ActivityField field = new ActivityField(StreamFieldType.Message.name());
			applyFieldValue(stream, ai, field, dataStr);
		}
	}

	/**
	 * Sets the value for the field in the specified activity entity. Depending on field definition if it is dynamic or
	 * resolved collection value has to be split, value is applied in dynamic manner using
	 * {@link #applyDynamicValue(com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext, com.jkoolcloud.tnt4j.streams.fields.ActivityField, Object)}
	 * method. In all other cases {@link #applyFieldValue(TNTInputStream, ActivityInfo, ActivityField, Object)} is
	 * invoked.
	 *
	 * @param field
	 *            field to apply value to
	 * @param value
	 *            value to apply for this field
	 * @param cData
	 *            parsing context data package
	 * @throws IllegalStateException
	 *             if parser has not been properly initialized
	 * @throws ParseException
	 *             if an error parsing the specified value
	 *
	 * @see #applyDynamicValue(com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext,
	 *      com.jkoolcloud.tnt4j.streams.fields.ActivityField, Object)
	 * @see #applyFieldValue(TNTInputStream, ActivityInfo, ActivityField, Object)
	 */
	protected void applyFieldValue(ActivityField field, Object value, ActivityContext cData) throws ParseException {
		if (field.isDynamic() || (field.isSplitCollection() && Utils.isCollection(value))) {
			applyDynamicValue(cData, field, value);
		} else {
			applyFieldValue(cData.getStream(), cData.getActivity(), field, value);
		}
	}

	/**
	 * Transforms activity data to be put to activity field
	 * {@link com.jkoolcloud.tnt4j.streams.fields.StreamFieldType#Message}. This is used when no field
	 * {@link com.jkoolcloud.tnt4j.streams.fields.StreamFieldType#Message} mapping defined in parser configuration.
	 * 
	 * @param data
	 *            activity data
	 * @return data to be used for activity field {@link com.jkoolcloud.tnt4j.streams.fields.StreamFieldType#Message}
	 */
	protected String getRawDataAsMessage(T data) {
		return Utils.toString(data);
	}

	// protected void postParse(ActivityInfo ai, TNTInputStream<?, ?> stream, T data) throws ParseException {
	// Object msgData = getRawDataAsMessage(data);
	// if (useActivityAsMessage && ai.getMessage() == null && msgData != null) {
	// // save entire activity string as message data
	// ActivityField field = new ActivityField(StreamFieldType.Message.name());
	// applyFieldValue(stream, ai, field, msgData);
	// }
	// }

	/**
	 * Sets the value for the dynamic fields in the specified activity entity.
	 * <p>
	 * If field has stacked parser defined, then field value is parsed into separate activity using stacked parser. If
	 * field can be parsed by stacked parser, can be merged or added as a child into specified (parent) activity
	 * depending on stacked parser reference 'aggregation' attribute value.
	 *
	 * @param cData
	 *            parsing context data package
	 * @param field
	 *            field to apply value to
	 * @param value
	 *            value to apply for dynamic fields
	 * @throws IllegalStateException
	 *             if parser has not been properly initialized
	 * @throws ParseException
	 *             if an error parsing the specified value
	 *
	 * @see #applyFieldValue(TNTInputStream, ActivityInfo, ActivityField, Object)
	 */
	protected void applyDynamicValue(ActivityContext cData, ActivityField field, Object value) throws ParseException {
		Map<String, Object> dValMap = parseDynamicValues(cData, field.getDynamicLocators());

		Object[] fValues = Utils.makeArray(value);

		List<ActivityField> tFieldsList = new ArrayList<>();
		for (int vi = 0; vi < fValues.length; vi++) {
			ActivityField tField = field.createTempField(dValMap, vi);
			tFieldsList.add(tField);
		}

		reviewTempFieldsNames(tFieldsList);

		ActivityField tField = null;
		try {
			for (int tfi = 0; tfi < tFieldsList.size(); tfi++) {
				tField = tFieldsList.get(tfi);
				Object fValue = fValues[tfi];

				applyFieldValue(cData.getStream(), cData.getActivity(), tField, Utils.simplifyValue(fValue));
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.parsing.failed", tField), 0);
			pe.initCause(e);
			throw pe;
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
	 * @param cData
	 *            parsing context data package
	 * @return field locators parsed values array
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 * @see #parseLocatorValues(java.util.List,
	 *      com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)
	 */
	protected Object[] parseLocatorValues(ActivityField field, ActivityContext cData) throws ParseException {
		return parseLocatorValues(field.getLocators(), cData);
	}

	/**
	 * Parses values array from prepared activity data item using provided locators list.
	 *
	 * @param locators
	 *            value locators list
	 * @param cData
	 *            parsing context data package
	 * @return locators parsed values array
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 */
	protected Object[] parseLocatorValues(List<ActivityFieldLocator> locators, ActivityContext cData)
			throws ParseException {
		if (locators != null) {
			Object[] values = new Object[locators.size()];
			for (int li = 0; li < locators.size(); li++) {
				values[li] = getLocatorValue(locators.get(li), cData);
			}
			return values;
		}

		return null;
	}

	private Map<String, Object> parseDynamicValues(ActivityContext cData,
			Map<String, ActivityFieldLocator> dynamicLocators) throws ParseException {
		Map<String, Object> dynamicValuesMap = new HashMap<>();
		if (dynamicLocators != null) {
			for (Map.Entry<String, ActivityFieldLocator> dLocator : dynamicLocators.entrySet()) {
				Object lValue = getLocatorValue(dLocator.getValue(), cData);
				final Object dynamicLocatorValue = Utils.simplifyValue(lValue);
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
	 * @see #getLocatorValue(com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator,
	 *      com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)
	 * @deprecated used by tests only. Use
	 *             {@link #getLocatorValue(com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator, com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser.ActivityContext)}
	 *             instead.
	 */
	@Deprecated
	protected Object getLocatorValue(TNTInputStream<?, ?> stream, ActivityFieldLocator locator, T data)
			throws ParseException {
		return getLocatorValue(locator, new ActivityContext(stream, null, data));
	}

	/**
	 * Gets field value from raw data location and formats it according locator definition.
	 *
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            parsing context data package
	 * @return value formatted based on locator definition or {@code null} if locator is not defined
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 * @see ActivityFieldLocator#formatValue(Object)
	 */
	protected Object getLocatorValue(ActivityFieldLocator locator, ActivityContext cData) throws ParseException {
		Object val = null;
		if (locator != null) {
			String locStr = locator.getLocator();
			AtomicBoolean formattingNeeded = new AtomicBoolean(true);
			if (StringUtils.isNotEmpty(locStr)) {
				if (locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp) {
					val = cData.getStream().getProperty(locStr);
				} else if (locator.getBuiltInType() == ActivityFieldLocatorType.Cache) {
					val = Utils.simplifyValue(StreamsCache.getValue(cData.getActivity(), locStr, getName()));
				} else if (locator.getBuiltInType() == ActivityFieldLocatorType.Activity) {
					val = cData.getActivity().getFieldValue(locator.getLocator());
				} else {
					val = resolveLocatorValue(locator, cData, formattingNeeded);
					// logger().log(val == null && !locator.isOptional() ? OpLevel.WARNING : OpLevel.TRACE,
					logger().log(OpLevel.TRACE, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ActivityParser.locator.resolved"), locStr, toString(val));
				}
			}

			if (formattingNeeded.get()) {
				val = locator.formatValue(val);
			}

			try {
				val = locator.transformValue(val, cData.getActivity());
			} catch (Exception exc) {
				logger().log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityParser.transformation.failed"), locStr, toString(val), exc);
			}

			try {
				boolean filteredOut = locator.filterValue(val, cData.getActivity());

				if (filteredOut) {
					val = null;
				}
			} catch (Exception exc) {
				logger().log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityParser.field.filtering.failed"), locStr, toString(val), exc);
			}
		}
		return val;
	}

	/**
	 * Gets field raw data value resolved by locator.
	 *
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            activity object context data package
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 *
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value
	 */
	protected abstract Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) throws ParseException;

	/**
	 * Makes string representation of data package to put into log.
	 *
	 * @param data
	 *            data package to be logged
	 * @return string representation of data package to be logged
	 */
	protected String getLogString(Object data) {
		if (data == null || data instanceof String) {
			return String.valueOf(data);
		}
		return logger().isSet(OpLevel.TRACE) ? toString(data) : data.getClass().getName();
	}

	/**
	 * Applies stream filters group defined filters on activity information data. If activity data matches at least one
	 * excluding filter, activity is marked as "filtered out".
	 * 
	 * @param ai
	 *            activity information data
	 *
	 * @see com.jkoolcloud.tnt4j.streams.fields.ActivityInfo#setFiltered(boolean)
	 */
	protected void filterActivity(ActivityInfo ai) throws Exception {
		if (activityFilter == null || ai == null) {
			return;
		}

		synchronized (FILTER_LOCK) {
			boolean filteredOut = activityFilter.doFilter(null, ai);
			ai.setFiltered(filteredOut);
		}
	}

	@Override
	public void addReference(Object refObject) {
		if (refObject instanceof ActivityDataPreParser) {
			if (preParsers == null) {
				preParsers = new ArrayList<>();
			}
			preParsers.add((ActivityDataPreParser<?>) refObject);
		}
	}

	/**
	 * Converts RAW activity data using defined set of pre-parsers. Converted activity data then is parsed by parser
	 * itself.
	 * 
	 * @param data
	 *            RAW activity data to pre-parse
	 * @return pre-parsers converted activity data package
	 * @throws java.lang.Exception
	 *             if RAW activity data pre-parsing fails
	 */
	protected Object preParseActivityData(Object data) throws Exception {
		if (CollectionUtils.isNotEmpty(preParsers)) {
			logger().log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityParser.data.before.pre.parsing"), getLogString(data));

			synchronized (PRE_PARSER_LOCK) {
				for (ActivityDataPreParser<?> preParser : preParsers) {
					boolean validData = preParser.isDataClassSupported(data);
					if (validData && getActivityDataType().equals(preParser.dataTypeReturned())) {
						logger().log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
								"ActivityParser.pre.parsing.data"), preParser.getClass().getSimpleName());
						data = preParser.preParse(data);
						logger().log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
								"ActivityParser.data.after.pre.parsing"), getLogString(data));
					}
				}
			}
		}

		return data;
	}

	/**
	 * Activity data context containing all data used by parsers to resolve field values.
	 */
	protected class ActivityContext extends HashMap<String, Object> {
		private static final String STREAM_KEY = "STREAM_DATA"; // NON-NLS
		private static final String RAW_DATA_KEY = "RAW_ACTIVITY_DATA"; // NON-NLS

		private static final String PREPARED_DATA_KEY = "PREPARED_ACTIVITY_DATA"; // NON-NLS
		private static final String ACTIVITY_DATA_KEY = "ACTIVITY_DATA"; // NON-NLS
		private static final String MESSAGE_DATA_KEY = "ACT_MESSAGE_DATA"; // NON-NLS

		private static final String FIELD_KEY = "PARSED_FIELD"; // NON-NLS

		private boolean valid = true;

		/**
		 * Constructs new activity data context.
		 * 
		 * @param stream
		 *            stream providing activity data
		 * @param rawData
		 *            stream provided RAW activity data
		 */
		public ActivityContext(TNTInputStream<?, ?> stream, Object rawData) {
			put(STREAM_KEY, stream);
			put(RAW_DATA_KEY, rawData);
		}

		/**
		 * Constructs new activity data context.
		 *
		 * @param stream
		 *            stream providing activity data
		 * @param rawData
		 *            stream provided RAW activity data
		 * @param preparedData
		 *            parser prepared activity data compatible to locate values
		 */
		public ActivityContext(TNTInputStream<?, ?> stream, Object rawData, T preparedData) {
			put(STREAM_KEY, stream);
			put(RAW_DATA_KEY, rawData);
			put(PREPARED_DATA_KEY, preparedData);
		}

		/**
		 * Invalidates this activity data context.
		 */
		public void invalidate() {
			valid = true;
		}

		/**
		 * Reruns flag indicating whether this activity data context is valid.
		 *
		 * @return {@code true} is context is valid, {@code false} - otherwise
		 */
		public boolean isValid() {
			return valid;
		}

		/**
		 * Returns stream provided RAW activity data.
		 *
		 * @return stream provided RAW activity data
		 */
		public Object getRawData() {
			return get(RAW_DATA_KEY);
		}

		/**
		 * Sets parser prepared activity data.
		 * 
		 * @param preparedData
		 *            parser prepared activity data
		 */
		public void setData(T preparedData) {
			put(PREPARED_DATA_KEY, preparedData);
		}

		/**
		 * Returns parser prepared activity data.
		 * 
		 * @return parser prepared activity data
		 */
		@SuppressWarnings("unchecked")
		public T getData() {
			return (T) get(PREPARED_DATA_KEY);
		}

		/**
		 * Returns instance of stream providing activity data.
		 *
		 * @return stream providing activity data
		 */
		public TNTInputStream<?, ?> getStream() {
			return (TNTInputStream<?, ?>) get(STREAM_KEY);
		}

		/**
		 * Sets resolved activity entity data.
		 *
		 * @param ai
		 *            resolved activity entity data
		 */
		public void setActivity(ActivityInfo ai) {
			put(ACTIVITY_DATA_KEY, ai);
		}

		/**
		 * Returns resolved activity entity data.
		 *
		 * @return resolved activity entity data
		 */
		public ActivityInfo getActivity() {
			return (ActivityInfo) get(ACTIVITY_DATA_KEY);
		}

		/**
		 * Sets activity data string representation to be used as 'Message' field data.
		 *
		 * @param message
		 *            activity data string representation to be used as 'Message' field data
		 */
		public void setMessage(String message) {
			put(MESSAGE_DATA_KEY, message);
		}

		/**
		 * Returns activity data string representation to be used as 'Message' field data.
		 * 
		 * @return activity data string representation to be used as 'Message' field data
		 */
		public String getMessage() {
			return (String) get(MESSAGE_DATA_KEY);
		}

		/**
		 * Sets currently parsed field instance.
		 *
		 * @param field
		 *            currently parsed field instance
		 */
		public void setField(ActivityField field) {
			put(FIELD_KEY, field);
		}

		/**
		 * Gets currently parsed field instance.
		 * 
		 * @return currently parsed field instance
		 */
		public ActivityField getField() {
			return (ActivityField) get(FIELD_KEY);
		}
	}
}
