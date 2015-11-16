/*
 * Copyright (c) 2015 jKool, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * jKool, LLC. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with jKool, LLC.
 *
 * JKOOL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. JKOOL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 *
 */

package com.jkool.tnt4j.streams.fields;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jkool.tnt4j.streams.utils.NumericFormatter;
import com.jkool.tnt4j.streams.utils.StreamTimestamp;
import com.jkool.tnt4j.streams.utils.TimestampFormatter;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Represents the locator rules for a specific activity data item field,
 * defining how to locate a particular raw activity data item field for its
 * corresponding activity item value, as well as any transformations that are
 * necessary.
 *
 * @version $Revision: 7 $
 */
public class ActivityFieldLocator {
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
	private String requiredVal = "";

	private ActivityFieldLocatorType builtInType = null;
	private ActivityFieldFormatType builtInFormat = null;
	private ActivityFieldUnitsType builtInUnits = null;
	private Map<Object, Object> map = null;
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
			int loc = Integer.valueOf(locator);
			if (loc <= 0) {
				throw new IllegalArgumentException("Numeric locator must be > 0");
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
	 * </p>
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field
	 * locator is always a specific type.
	 * </p>
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
	 * </p>
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field
	 * locator is always a specific type.
	 * </p>
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
	 * </p>
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
	 * </p>
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
	 * </p>
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
	 * </p>
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field
	 * value is always a specific data type.
	 * </p>
	 *
	 * @return the data type for raw data field
	 */
	public ActivityFieldDataType getDataType() {
		return dataType;
	}

	/**
	 * <p>
	 * Sets the data type indicating how to treat the raw data field value.
	 * </p>
	 * <p>
	 * Note: Some activity fields will ignore this and assume that the field
	 * value is always a specific data type.
	 * </p>
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
	 * </p>
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields.
	 * </p>
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
	 * </p>
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
	 * </p>
	 *
	 * @return the builtInUnits built-in format type, or {@code null} if this
	 *         units specification is a custom one.
	 */
	public ActivityFieldUnitsType getBuiltInUnits() {
		return builtInUnits;
	}

	/**
	 * <p>
	 * Sets the units represented by the raw data field value. This value can be
	 * one of the predefined units, or it can be a custom unit type.
	 * </p>
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
	 * </p>
	 *
	 * @param units
	 *            the units the raw data value represents
	 */
	public void setUnits(String units) {
		this.units = units;
		builtInUnits = null;
		try {
			builtInUnits = ActivityFieldUnitsType.valueOf(this.format);
		} catch (Exception e) {
		}
	}

	/**
	 * <p>
	 * Gets the format string defining how to interpret the raw data field
	 * value.
	 * </p>
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
	 * </p>
	 *
	 * @return the format string for interpreting raw data value
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * <p>
	 * Gets locale representation string used by formatter.
	 * </p>
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
	 * </p>
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
	 * </p>
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
	 * </p>
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
	 * </p>
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
	 * </p>
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
	 *            true/false string
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
		if (StringUtils.isEmpty(source)) {
			mapCatchAll = target;
		} else {
			if (map == null) {
				map = new HashMap<Object, Object>();
			}
			map.put(source, target);
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
		if (map == null && mapCatchAll == null) {
			return source;
		}
		Object target = null;
		if (source == null) {
			target = mapCatchAll;
		} else {
			String srcString = source instanceof Number ? String.valueOf(((Number) source).longValue())
					: source.toString();
			if (map != null) {
				target = map.get(srcString);
			}
			if (target == null) {
				LOGGER.log(OpLevel.TRACE, "Applying default mapping for locator type \"{0}\"", type);
				target = mapCatchAll != null ? mapCatchAll : source;
			}
		}
		LOGGER.log(OpLevel.TRACE, "Mapped value \"{0}\" to \"{1}\" for locator type \"{2}\"", source, target, type);
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
			ActivityFieldDataType fDataType = this.dataType == null ? ActivityFieldDataType.DateTime : this.dataType;
			ActivityFieldUnitsType fUnits = ActivityFieldUnitsType.Milliseconds;
			if (this.units != null) {
				try {
					fUnits = ActivityFieldUnitsType.valueOf(this.units);
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
		return type + "::" + locator;
	}

	/**
	 * Gets a string representation of this object for use in debugging, which
	 * includes the value of each data member.
	 *
	 * @return debugging string representation
	 */
	public String toDebugString() {
		return "{type='" + type + "' " + "locator='" + locator + "' " + "dataType='" + dataType + "' " + "format='"
				+ format + "' " + "locale='" + locale + "' " + "units='" + units + "' " + "cfgValue='" + cfgValue + "' "
				+ "required='" + requiredVal + "'}";
	}
}
