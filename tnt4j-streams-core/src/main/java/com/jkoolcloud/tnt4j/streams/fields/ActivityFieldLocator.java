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

package com.jkoolcloud.tnt4j.streams.fields;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.UsecTimestamp;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Represents the locator rules for a specific activity data item field, defining how to locate a particular raw
 * activity data item field for its corresponding activity item value, as well as any transformations that are
 * necessary.
 *
 * @version $Revision: 1 $
 */
public class ActivityFieldLocator implements Cloneable {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityFieldLocator.class);

	private String type = null;
	private String locator = null;
	private ActivityFieldDataType dataType = ActivityFieldDataType.String;
	private int radix = 10;
	private String units = null;
	private String format = null;
	private String locale = null;
	private String timeZone = null;
	private Object cfgValue = null;
	private String requiredVal = ""; /* string to allow no value */
	private String id = null;

	private ActivityFieldLocatorType builtInType = null;
	private ActivityFieldFormatType builtInFormat = null;
	private TimeUnit builtInUnits = null;
	private Map<Object, Object> valueMap = null;
	private Object mapCatchAll = null;

	private NumericFormatter numberParser = null;
	private TimestampFormatter timeParser = null;

	/**
	 * Constructs a new activity field locator for either a built-in type or a custom type.
	 *
	 * @param type
	 *            type of locator - can be one of predefined values from {@link ActivityFieldLocatorType} or a custom
	 *            type
	 * @param locator
	 *            key to use to locate raw data value - interpretation of this value depends on locator type
	 * @throws IllegalArgumentException
	 *             if locator type is a numeric value and is not a positive number
	 */
	public ActivityFieldLocator(String type, String locator) {
		this.type = type;
		this.locator = locator;
		try {
			builtInType = ActivityFieldLocatorType.valueOf(this.type);
		} catch (Exception e) {
		}
		if (builtInType != ActivityFieldLocatorType.Label && builtInType != ActivityFieldLocatorType.StreamProp) {
			int loc = Integer.parseInt(locator);
			if (loc <= 0) {
				throw new IllegalArgumentException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityFieldLocator.numeric.locator.positive"));
			}
		}
	}

	/**
	 * Constructs a new activity field locator for a built-in type.
	 *
	 * @param type
	 *            type of locator
	 * @param locator
	 *            key to use to locate raw data value - interpretation of this value depends on locator type
	 */
	public ActivityFieldLocator(ActivityFieldLocatorType type, String locator) {
		this.type = type.name();
		this.locator = locator;
		this.builtInType = type;
	}

	/**
	 * Constructs a new activity field locator that simply uses the specified value as the value for this locator.
	 *
	 * @param value
	 *            constant value for locator
	 */
	public ActivityFieldLocator(Object value) {
		this.cfgValue = value;
	}

	/**
	 * Constructs a new "hidden" activity field locator that is used to format final field value made from multiple
	 * locators values.
	 */
	ActivityFieldLocator() {
	}

	/**
	 * Gets the type of this locator that indicates how to interpret the locator to find the value in the raw activity
	 * data. This value can be one of the predefined types, or it can be a custom type.
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field locator is always a specific type.
	 *
	 * @return the label representing the type of locator
	 */
	public String getType() {
		return type;
	}

	/**
	 * Gets the enumeration value for this locator if it implements one of the built-in locator types.
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field locator is always a specific type.
	 *
	 * @return the builtInType built-in locator type, or {@code null} if this locator is a custom one.
	 */
	public ActivityFieldLocatorType getBuiltInType() {
		return builtInType;
	}

	/**
	 * Gets the locator to find the value of this field in the raw activity data. This is generally a numeric position
	 * or a string label.
	 *
	 * @return the locator for data value
	 */
	public String getLocator() {
		return locator;
	}

	/**
	 * Get the radix that raw data field values are interpreted in. Only relevant for numeric fields and will be ignored
	 * by those fields to which it does not apply.
	 *
	 * @return radix for field values
	 */
	public int getRadix() {
		return radix;
	}

	/**
	 * Set the radix used to interpret the raw data field values. Only relevant for numeric fields and will be ignored
	 * by those fields to which it does not apply.
	 *
	 * @param radix
	 *            radix of field values
	 */
	public void setRadix(int radix) {
		this.radix = radix;
	}

	/**
	 * Gets the data type indicating how to treat the raw data field value.
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field value is always a specific data type.
	 *
	 * @return the data type for raw data field
	 */
	public ActivityFieldDataType getDataType() {
		return dataType;
	}

	/**
	 * Sets the data type indicating how to treat the raw data field value.
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field value is always a specific data type.
	 *
	 * @param dataType
	 *            the data type for raw data field
	 */
	public void setDataType(ActivityFieldDataType dataType) {
		this.dataType = dataType;
	}

	/**
	 * Gets the units represented by the raw data field value. This value can be one of the predefined units, or it can
	 * be a custom unit type.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields.
	 *
	 * @return the units the raw data value represents
	 */
	public String getUnits() {
		return units;
	}

	/**
	 * Gets the enumeration value for this locator's units if it implements one of the built-in units types.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 *
	 * @return the builtInUnits built-in format type, or {@code null} if this units specification is a custom one.
	 */
	public TimeUnit getBuiltInUnits() {
		return builtInUnits;
	}

	/**
	 * Sets the units represented by the raw data field value. This value can be one of the predefined units, or it can
	 * be a custom unit type.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 *
	 * @param units
	 *            the units the raw data value represents
	 */
	public void setUnits(String units) {
		this.units = units;
		builtInUnits = null;
		try {
			if (units != null) {
				builtInUnits = TimeUnit.valueOf(units.toUpperCase());
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Gets the format string defining how to interpret the raw data field value.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 *
	 * @return the format string for interpreting raw data value
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * Gets locale representation string used by formatter.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 *
	 * @return the locale representation string used by formatter.
	 */
	public String getLocale() {
		return locale;
	}

	/**
	 * Gets the enumeration value for this locator's format if it implements one of the built-in format types.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 *
	 * @return the builtInFormat built-in format type, or {@code null} if this format is either a format string, or a
	 *         custom one.
	 */
	public ActivityFieldFormatType getBuiltInFormat() {
		return builtInFormat;
	}

	/**
	 * Sets the format string defining how to interpret the raw data field value.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 *
	 * @param format
	 *            the format string for interpreting raw data value
	 * @param locale
	 *            locale for formatter to use
	 */
	public void setFormat(String format, String locale) {
		this.format = format;
		this.locale = locale;
		builtInFormat = null;
		try {
			builtInFormat = ActivityFieldFormatType.valueOf(this.format);
		} catch (Exception e) {
		}
	}

	/**
	 * Gets the time zone ID that the date/time string is assumed to be in when parsed.
	 *
	 * @return time zone ID
	 */
	public String getTimeZone() {
		return timeZone;
	}

	/**
	 * Sets the time zone ID that the date/time string is assumed to represent.
	 *
	 * @param timeZone
	 *            the timeZone to set
	 */
	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * Sets the required option flag to indicator if locator is optional
	 *
	 * @param requiredVal
	 *            {@code true}/{@code false} string
	 */
	public void setRequired(String requiredVal) {
		this.requiredVal = requiredVal;
	}

	/**
	 * Determines whether value resolution by locator is optional.
	 *
	 * @return flag indicating value resolution by locator is optional
	 */
	public boolean isOptional() {
		return "false".equalsIgnoreCase(requiredVal); // NON-NLS
	}

	/**
	 * Determines whether value resolution by locator is required.
	 *
	 * @return flag indicating value resolution by locator is required
	 */
	public boolean isRequired() {
		return "true".equalsIgnoreCase(requiredVal); // NON-NLS
	}

	/**
	 * Determines whether value resolution by locator is required depending on stream context. For example XML/JSON
	 * parser context may require all locators to resolve non {@code null} values by default.
	 *
	 * @return flag indicating value resolution by locator is required depending on stream context
	 */
	public boolean isDefaultRequire() {
		return StringUtils.isEmpty(requiredVal); // NON-NLS
	}

	/**
	 * Gets field locator identifier.
	 *
	 * @return field locator identifier
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets field locator identifier.
	 *
	 * @param id
	 *            field locator identifier
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Adds a mapping to translate a raw data value to the corresponding converted data value.
	 *
	 * @param source
	 *            raw data value
	 * @param target
	 *            value to translate raw value to
	 * @return instance of this locator object
	 */
	public ActivityFieldLocator addValueMap(String source, String target) {
		return addValueMap(source, target, null);
	}

	/**
	 * Adds a mapping to translate a raw data value to the corresponding converted data value.
	 *
	 * @param source
	 *            raw data value
	 * @param target
	 *            value to translate raw value to
	 * @param mapType
	 *            type of values mapping
	 * @return instance of this locator object
	 */
	public ActivityFieldLocator addValueMap(String source, String target, ActivityFieldMappingType mapType) {
		if (StringUtils.isEmpty(source)) {
			mapCatchAll = target;
		} else {
			if (valueMap == null) {
				valueMap = new ValueMap<Object, Object>();
			}
			try {
				if (mapType == null) {
					mapType = ActivityFieldMappingType.Value;
				}

				switch (mapType) {
				case Range:
					valueMap.put(DoubleRange.getRange(source), target);
					break;
				case Calc:
					valueMap.put(getCalcKey(source), target);
					break;
				default:
					valueMap.put(source, target);
				}
			} catch (Exception exc) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityFieldLocator.mapping.add.error"), source, target, mapType);
			}
		}

		return this;
	}

	/**
	 * Translates the specified raw data value to its corresponding converted data value.
	 *
	 * @param source
	 *            raw data value
	 * @return converted value
	 */
	protected Object getMappedValue(Object source) {
		if (valueMap == null && mapCatchAll == null) {
			return source;
		}
		Object target = null;
		if (source == null) {
			target = mapCatchAll;
		} else {
			String srcString = String.valueOf(source);
			if (valueMap != null) {
				target = valueMap.get(srcString);
			}
			if (target == null) {
				LOGGER.log(OpLevel.TRACE, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityFieldLocator.mapped.default"), type);
				target = mapCatchAll != null ? mapCatchAll : source;
			}
		}
		LOGGER.log(OpLevel.TRACE,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityFieldLocator.mapped.result"),
				source, target, type);
		return target;
	}

	/**
	 * Formats the specified value based on the locator's formatting properties.
	 *
	 * @param value
	 *            value to format
	 * @return value formatted based on locator definition
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 */
	public Object formatValue(Object value) throws ParseException {
		if (cfgValue != null) {
			return cfgValue;
		}
		if (value == null) {
			return null;
		}
		switch (dataType) {
		case String:
			return getMappedValue(value);
		case Number:
			return getMappedValue(formatNumericValue(value));
		case Binary:
			if (builtInFormat == ActivityFieldFormatType.base64Binary) {
				value = Utils.base64Decode(value.toString().getBytes());
			} else if (builtInFormat == ActivityFieldFormatType.hexBinary) {
				value = Utils.decodeHex(value.toString());
			} else if (builtInFormat == ActivityFieldFormatType.string) {
				value = value.toString().getBytes();
			}
			break;
		case DateTime:
		case Timestamp:
			value = formatDateValue(value);
			break;
		default:
			break;
		}
		return value;
	}

	/**
	 * Formats the value for the specified numeric field based on the definition of the field.
	 *
	 * @param value
	 *            raw value of field
	 * @return formatted value of field in required internal data type
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             format, etc.)
	 */
	protected Number formatNumericValue(Object value) throws ParseException {
		if (numberParser == null) {
			numberParser = new NumericFormatter(format, locale);
		}

		Object val = value;

		if (value instanceof String) {
			val = value.toString().trim();

			// TODO: make empty value handling configurable: null, exception,
			// default
			if (StringUtils.isEmpty(val.toString())) {
				return null;
			}
		}

		return numberParser.parse(val, 1.0);
	}

	/**
	 * Formats the value for the specified date/time field based on the definition of the field.
	 *
	 * @param value
	 *            raw value of field
	 * @return formatted value of field in required internal data type
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             format, etc.)
	 */
	protected UsecTimestamp formatDateValue(Object value) throws ParseException {
		if (value instanceof UsecTimestamp) {
			return (UsecTimestamp) value;
		}
		if (timeParser == null) {
			ActivityFieldDataType fDataType = dataType == null ? ActivityFieldDataType.DateTime : dataType;
			TimeUnit fUnits = TimeUnit.MILLISECONDS;
			if (units != null) {
				try {
					fUnits = TimeUnit.valueOf(units.toUpperCase());
				} catch (Exception e) {
				}
			}
			timeParser = fDataType == ActivityFieldDataType.Timestamp || fDataType == ActivityFieldDataType.Number
					? new TimestampFormatter(fUnits) : new TimestampFormatter(format, timeZone, locale);
		}
		return timeParser.parse(value);
	}

	/**
	 * Returns string representing field locator by type and locator key.
	 *
	 * @return a string representing field locator.
	 */
	@Override
	public String toString() {
		return String.format("%s::%s", type, locator); // NON-NLS
	}

	/**
	 * Makes clone copy of activity field locator.
	 *
	 * @return clone copy of activity field locator
	 */
	@Override
	public ActivityFieldLocator clone() {
		try {
			ActivityFieldLocator cafl = (ActivityFieldLocator) super.clone();
			cafl.type = type;
			cafl.locator = locator;
			cafl.dataType = dataType;
			cafl.radix = radix;
			cafl.units = units;
			cafl.format = format;
			cafl.locale = locale;
			cafl.timeZone = timeZone;
			cafl.cfgValue = cfgValue;
			cafl.requiredVal = requiredVal;
			cafl.id = id;

			cafl.builtInType = builtInType;
			cafl.builtInFormat = builtInFormat;
			cafl.builtInUnits = builtInUnits;
			cafl.valueMap = valueMap;
			cafl.mapCatchAll = mapCatchAll;

			return cafl;
		} catch (CloneNotSupportedException exc) {

		}

		return null;
	}

	private static Calc getCalcKey(String source) throws IllegalArgumentException {
		return new Calc(ActivityFieldMappingCalc.valueOf(source.toUpperCase()));
	}

	private static class Calc {
		private ActivityFieldMappingCalc function;

		private Calc(ActivityFieldMappingCalc functionName) {
			this.function = functionName;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Number) {
				return match(((Number) obj).doubleValue());
			}

			if (obj instanceof String) {
				try {
					return match(Double.parseDouble((String) obj));
				} catch (NumberFormatException exc) {
				}
			}

			return super.equals(obj);
		}

		private boolean match(Double num) {
			switch (function) {
			case ODD:
				return num % 2 != 0;
			case EVEN:
				return num % 2 == 0;
			default:
				return false;
			}
		}

		@Override
		public String toString() {
			return String.valueOf(function);
		}
	}

	private class ValueMap<K, V> extends HashMap<K, V> {
		private static final long serialVersionUID = 2566002253449435488L;

		private ValueMap() {
			super();
		}

		private ValueMap(int ic) {
			super(ic);
		}

		@Override
		public V get(Object key) {
			V e = super.get(key);

			if (e == null) {
				e = getCompared(key);
			}

			return e;
		}

		private V getCompared(Object key) {
			Iterator<Map.Entry<K, V>> i = entrySet().iterator();
			if (key == null) {
				while (i.hasNext()) {
					Map.Entry<K, V> e = i.next();
					if (e.getKey() == null)
						return e.getValue();
				}
			} else {
				while (i.hasNext()) {
					Map.Entry<K, V> e = i.next();
					if (e.getKey() != null && e.getKey().equals(key))
						return e.getValue();
				}
			}
			return null;
		}
	}
}
