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

package com.jkool.tnt4j.streams.fields;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.jkool.tnt4j.streams.utils.*;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Represents the locator rules for a specific activity data item field,
 * defining how to locate a particular raw activity data item field for its
 * corresponding activity item value, as well as any transformations that are
 * necessary.
 *
 * @version $Revision: 1 $
 */
public class ActivityFieldLocator implements Cloneable {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityFieldLocator.class);

	private static final Pattern RANGE_PATTERN = Pattern.compile("(-?(\\d+)*(\\.\\d*)?)");
	private static final String RANGE_SEPARATOR = ":";

	private String type = null;
	private String locator = null;
	private ActivityFieldDataType dataType = ActivityFieldDataType.String;
	private int radix = 10;
	private String units = null;
	private String format = null;
	private String locale = null;
	private String timeZone = null;
	private Object cfgValue = null;
	private String requiredVal = "";

	private ActivityFieldLocatorType builtInType = null;
	private ActivityFieldFormatType builtInFormat = null;
	private TimeUnit builtInUnits = null;
	private Map<Object, Object> valueMap = null;
	private Object mapCatchAll = null;

	private NumericFormatter numberParser = null;
	private TimestampFormatter timeParser = null;

	/**
	 * Constructs a new activity field locator for either a built-in type or a
	 * custom type.
	 *
	 * @param type
	 *            type of locator - can be one of predefined values from
	 *            {@link ActivityFieldLocatorType} or a custom type
	 * @param locator
	 *            key to use to locate raw data value - interpretation of this
	 *            value depends on locator type
	 *
	 * @throws IllegalArgumentException
	 *             if locator type is a numeric value and is not a positive
	 *             number
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
				throw new IllegalArgumentException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
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
	 *            key to use to locate raw data value - interpretation of this
	 *            value depends on locator type
	 */
	public ActivityFieldLocator(ActivityFieldLocatorType type, String locator) {
		this.type = type.toString();
		this.locator = locator;
		this.builtInType = type;
	}

	/**
	 * Constructs a new activity field locator that simply uses the specified
	 * value as the value for this locator.
	 *
	 * @param value
	 *            constant value for locator
	 */
	public ActivityFieldLocator(Object value) {
		this.cfgValue = value;
	}

	/**
	 * <p>
	 * Gets the type of this locator that indicates how to interpret the locator
	 * to find the value in the raw activity data. This value can be one of the
	 * predefined types, or it can be a custom type.
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field
	 * locator is always a specific type.
	 *
	 * @return the label representing the type of locator
	 */
	public String getType() {
		return type;
	}

	/**
	 * <p>
	 * Gets the enumeration value for this locator if it implements one of the
	 * built-in locator types.
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field
	 * locator is always a specific type.
	 *
	 * @return the builtInType built-in locator type, or {@code null} if this
	 *         locator is a custom one.
	 */
	public ActivityFieldLocatorType getBuiltInType() {
		return builtInType;
	}

	/**
	 * <p>
	 * Gets the locator to find the value of this field in the raw activity
	 * data. This is generally a numeric position or a string label.
	 *
	 * @return the locator for data value
	 */
	public String getLocator() {
		return locator;
	}

	/**
	 * <p>
	 * Get the radix that raw data field values are interpreted in. Only
	 * relevant for numeric fields and will be ignored by those fields to which
	 * it does not apply.
	 *
	 * @return radix for field values
	 */
	public int getRadix() {
		return radix;
	}

	/**
	 * <p>
	 * Set the radix used to interpret the raw data field values. Only relevant
	 * for numeric fields and will be ignored by those fields to which it does
	 * not apply.
	 *
	 * @param radix
	 *            radix of field values
	 */
	public void setRadix(int radix) {
		this.radix = radix;
	}

	/**
	 * <p>
	 * Gets the data type indicating how to treat the raw data field value.
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field
	 * value is always a specific data type.
	 *
	 * @return the data type for raw data field
	 */
	public ActivityFieldDataType getDataType() {
		return dataType;
	}

	/**
	 * <p>
	 * Sets the data type indicating how to treat the raw data field value.
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field
	 * value is always a specific data type.
	 *
	 * @param dataType
	 *            the data type for raw data field
	 */
	public void setDataType(ActivityFieldDataType dataType) {
		this.dataType = dataType;
	}

	/**
	 * <p>
	 * Gets the units represented by the raw data field value. This value can be
	 * one of the predefined units, or it can be a custom unit type.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields.
	 *
	 * @return the units the raw data value represents
	 */
	public String getUnits() {
		return units;
	}

	/**
	 * <p>
	 * Gets the enumeration value for this locator's units if it implements one
	 * of the built-in units types.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
	 *
	 * @return the builtInUnits built-in format type, or {@code null} if this
	 *         units specification is a custom one.
	 */
	public TimeUnit getBuiltInUnits() {
		return builtInUnits;
	}

	/**
	 * <p>
	 * Sets the units represented by the raw data field value. This value can be
	 * one of the predefined units, or it can be a custom unit type.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
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
	 * <p>
	 * Gets the format string defining how to interpret the raw data field
	 * value.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
	 *
	 * @return the format string for interpreting raw data value
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * <p>
	 * Gets locale representation string used by formatter.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
	 *
	 * @return the locale representation string used by formatter.
	 */
	public String getLocale() {
		return locale;
	}

	/**
	 * <p>
	 * Gets the enumeration value for this locator's format if it implements one
	 * of the built-in format types.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
	 *
	 * @return the builtInFormat built-in format type, or {@code null} if this
	 *         format is either a format string, or a custom one.
	 */
	public ActivityFieldFormatType getBuiltInFormat() {
		return builtInFormat;
	}

	/**
	 * <p>
	 * Sets the format string defining how to interpret the raw data field
	 * value.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
	 *
	 * @param format
	 *            the format string for interpreting raw data value
	 * @param locale
	 *            locale for formatter to use.
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
	 * Gets the time zone ID that the date/time string is assumed to be in when
	 * parsed.
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
	 * Gets the required option flag indicating whether locator is required or
	 * optional.
	 *
	 * @return flag indicating whether locator is required or optional
	 */
	public String getRequired() {
		return requiredVal;
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
	 * Adds a mapping to translate a raw data value to the corresponding
	 * converted data value.
	 *
	 * @param source
	 *            raw data value
	 * @param target
	 *            value to translate raw value to
	 */
	public void addValueMap(String source, String target) {
		addValueMap(source, target, null);
	}

	/**
	 * Adds a mapping to translate a raw data value to the corresponding
	 * converted data value.
	 *
	 * @param source
	 *            raw data value
	 * @param target
	 *            value to translate raw value to
	 * @param mapType
	 *            type of values mapping
	 */
	public void addValueMap(String source, String target, ActivityFieldMappingType mapType) {
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
					valueMap.put(getRangeKey(source), target);
					break;
				case Calc:
					valueMap.put(getCalcKey(source), target);
					break;
				default:
					valueMap.put(source, target);
				}
			} catch (Exception exc) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
						"ActivityFieldLocator.mapping.add.error", source, target, mapType));
			}
		}
	}

	/**
	 * Translates the specified raw data value to its corresponding converted
	 * data value.
	 *
	 * @param source
	 *            raw data value
	 *
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
				LOGGER.log(OpLevel.TRACE, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
						"ActivityFieldLocator.mapped.default", type));
				target = mapCatchAll != null ? mapCatchAll : source;
			}
		}
		LOGGER.log(OpLevel.TRACE, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
				"ActivityFieldLocator.mapped.result", source, target, type));
		return target;
	}

	/**
	 * Formats the specified value based on the locator's formatting properties.
	 *
	 * @param value
	 *            value to format
	 *
	 * @return value formatted based on locator definition
	 *
	 * @throws ParseException
	 *             if error applying locator format properties to specified
	 *             value
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
			} else {
				value = builtInFormat == ActivityFieldFormatType.hexBinary ? Utils.decodeHex(value.toString())
						: value.toString();
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
	 * Formats the value for the specified numeric field based on the definition
	 * of the field.
	 *
	 * @param value
	 *            raw value of field
	 *
	 * @return formatted value of field in required internal data type
	 *
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field
	 *             definition (e.g. does not match defined format, etc.)
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
	 * Formats the value for the specified date/time field based on the
	 * definition of the field.
	 *
	 * @param value
	 *            raw value of field
	 *
	 * @return formatted value of field in required internal data type
	 *
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field
	 *             definition (e.g. does not match defined format, etc.)
	 */
	protected StreamTimestamp formatDateValue(Object value) throws ParseException {
		if (value instanceof StreamTimestamp) {
			return (StreamTimestamp) value;
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
	 * Gets a string representation of this object for use in debugging, which
	 * includes the value of each data member.
	 *
	 * @return debugging string representation
	 */
	public String toDebugString() {
		final StringBuilder sb = new StringBuilder("ActivityFieldLocator{"); // NON-NLS
		sb.append("type='").append(type).append('\''); // NON-NLS
		sb.append(", locator='").append(locator).append('\''); // NON-NLS
		sb.append(", dataType=").append(dataType); // NON-NLS
		sb.append(", radix=").append(radix); // NON-NLS
		sb.append(", units='").append(units).append('\''); // NON-NLS
		sb.append(", format='").append(format).append('\''); // NON-NLS
		sb.append(", locale='").append(locale).append('\''); // NON-NLS
		sb.append(", timeZone='").append(timeZone).append('\''); // NON-NLS
		sb.append(", cfgValue=").append(cfgValue); // NON-NLS
		sb.append(", requiredVal='").append(requiredVal).append('\''); // NON-NLS
		sb.append('}');
		return sb.toString();
	}

	/**
	 * Makes clone copy of activity field locator.
	 *
	 * @return clone copy of activity field locator
	 */
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
			cafl.timeZone = timeZone;

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

	private static Range getRangeKey(String source) throws Exception {
		String cs = StringUtils.trimToNull(source);
		if (StringUtils.isEmpty(cs)) {
			throw new IllegalArgumentException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ActivityFieldLocator.range.string.empty"));
		}
		int rCharIdx = cs.indexOf(RANGE_SEPARATOR);

		String[] numStrs = new String[2];
		int si = 0;
		Matcher m = RANGE_PATTERN.matcher(cs);
		int pos = 0;
		while (m.find()) {
			String g = m.group();
			if (StringUtils.isNotEmpty(g)) {
				numStrs[si++] = g;
			}
			pos = m.end();
		}

		String fromStr = null;
		String toStr = null;

		if (rCharIdx == -1) { // no range separator symbol found - unary range
			fromStr = numStrs.length > 0 ? numStrs[0] : null;
			toStr = fromStr;
		} else {
			if (rCharIdx == 0) { // unbound low range
				toStr = numStrs.length > 0 ? numStrs[0] : null;
			} else if (rCharIdx == cs.length()) { // unbound high range
				fromStr = numStrs.length > 0 ? numStrs[0] : null;
			} else { // bounded range
				fromStr = numStrs.length > 0 ? numStrs[0] : null;
				toStr = numStrs.length > 1 ? numStrs[1] : null;
			}
		}

		Double from = StringUtils.isEmpty(fromStr) ? -Double.MAX_VALUE : NumberUtils.createDouble(fromStr);
		Double to = StringUtils.isEmpty(toStr) ? Double.MAX_VALUE : NumberUtils.createDouble(toStr);

		return new Range(from, to);
	}

	private static Calc getCalcKey(String source) throws IllegalArgumentException {
		return new Calc(ActivityFieldMappingCalc.valueOf(source.toUpperCase()));
	}

	private static class Range {
		private Double from;
		private Double to;

		Range(Double from, Double to) {
			this.from = from;
			this.to = to;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Number) {
				return inRange(((Number) obj).doubleValue());
			}

			if (obj instanceof String) {
				try {
					return inRange(NumberUtils.createDouble((String) obj));
				} catch (NumberFormatException exc) {
				}
			}

			return super.equals(obj);
		}

		private boolean inRange(Double num) {
			int compareMin = from.compareTo(num);
			int compareMax = to.compareTo(num);

			return compareMin <= 0 && compareMax >= 0;
		}

		@Override
		public String toString() {
			return String.valueOf(from) + RANGE_SEPARATOR + String.valueOf(to);
		}

		@Override
		public int hashCode() {
			int result = from.hashCode();
			result = 31 * result + to.hashCode();
			return result;
		}
	}

	private static class Calc {
		private ActivityFieldMappingCalc function;

		Calc(ActivityFieldMappingCalc functionName) {
			this.function = functionName;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Number) {
				return match(((Number) obj).doubleValue());
			}

			if (obj instanceof String) {
				try {
					return match(NumberUtils.createDouble((String) obj));
				} catch (NumberFormatException exc) {
				}
			}

			return super.equals(obj);
		}

		public boolean match(Double num) {
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
		public ValueMap() {
			super();
		}

		public ValueMap(int ic) {
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
