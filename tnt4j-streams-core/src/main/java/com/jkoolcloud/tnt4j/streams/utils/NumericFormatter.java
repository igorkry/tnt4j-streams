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

package com.jkoolcloud.tnt4j.streams.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;

import org.apache.commons.lang3.StringUtils;

/**
 * Provides methods for parsing objects into numeric values and for formatting numeric values as strings.
 *
 * @version $Revision: 1 $
 *
 * @see DecimalFormat
 */
public class NumericFormatter {

	private int radix = 10;
	private String pattern = null;
	private DecimalFormat formatter = null;

	/**
	 * Creates a number formatter using the default numeric representation.
	 */
	public NumericFormatter() {
		this(null, null);
	}

	/**
	 * Creates a number formatter using the default numeric representation in the specified radix.
	 *
	 * @param radix
	 *            the radix to use while parsing numeric strings
	 */
	public NumericFormatter(int radix) {
		this.radix = radix;
	}

	/**
	 * Creates a number formatter/parser for numbers using the specified format pattern.
	 *
	 * @param pattern
	 *            format pattern
	 * @param locale
	 *            locale for numeric formatter to use.
	 */
	public NumericFormatter(String pattern, String locale) {
		setPattern(pattern, locale);
	}

	/**
	 * Gets the radix used by this formatter.
	 *
	 * @return radix used for parsing numeric strings
	 */
	public int getRadix() {
		return radix;
	}

	/**
	 * Sets the radix used by this formatter.
	 *
	 * @param radix
	 *            the radix to use while parsing numeric strings
	 */
	public void setRadix(int radix) {
		this.radix = radix;
	}

	/**
	 * Gets the format pattern string for this formatter.
	 *
	 * @return format pattern, or {@code null} if none specified
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * Sets the format pattern string for this formatter.
	 *
	 * @param pattern
	 *            format pattern - can be set to {@code null} to use default representation.
	 * @param locale
	 *            locale for decimal format to use.
	 */
	public void setPattern(String pattern, String locale) {
		this.pattern = pattern;
		formatter = StringUtils.isEmpty(pattern) ? null : StringUtils.isEmpty(locale) ? new DecimalFormat(pattern)
				: new DecimalFormat(pattern, new DecimalFormatSymbols(Utils.getLocale(locale)));
	}

	/**
	 * Formats the specified object using the defined pattern, or using the default numeric formatting if no pattern was
	 * defined.
	 *
	 * @param value
	 *            value to convert
	 * @return formatted value of field in required internal data type
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             pattern, etc.)
	 */
	public Number parse(Object value) throws ParseException {
		return parse(formatter, radix, value, 1.0);
	}

	/**
	 * Formats the specified object using the defined pattern, or using the default numeric formatting if no pattern was
	 * defined.
	 *
	 * @param value
	 *            value to convert
	 * @param scale
	 *            value to multiply the formatted value by
	 * @return formatted value of field in required internal data type
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             pattern, etc.)
	 */
	public Number parse(Object value, Number scale) throws ParseException {
		return parse(formatter, radix, value, scale);
	}

	/**
	 * Formats the specified object using the defined pattern, or using the default numeric formatting if no pattern was
	 * defined.
	 *
	 * @param pattern
	 *            number format pattern
	 * @param value
	 *            value to convert
	 * @param scale
	 *            value to multiply the formatted value by
	 * @return formatted value of field in required internal data type
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             pattern, etc.)
	 * @see DecimalFormat#DecimalFormat(String)
	 */
	public static Number parse(String pattern, Object value, Number scale) throws ParseException {
		return parse(Utils.isEmpty(pattern) ? null : new DecimalFormat(pattern), 10, value, scale);
	}

	/**
	 * Formats the specified object using the defined pattern, or using the default numeric formatting if no pattern was
	 * defined.
	 *
	 * @param formatter
	 *            formatter object to apply to value
	 * @param radix
	 *            the radix to use while parsing numeric strings
	 * @param value
	 *            value to convert
	 * @param scale
	 *            value to multiply the formatted value by
	 *
	 * @return formatted value of field in required internal data type
	 *
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             pattern, etc.)
	 */
	private static Number parse(DecimalFormat formatter, int radix, Object value, Number scale) throws ParseException {
		if (value == null) {
			return null;
		}
		if (scale == null) {
			scale = 1.0;
		}
		try {
			Number numValue = null;
			if (formatter == null && value instanceof String) {
				String strValue = (String) value;
				if (strValue.startsWith("0x") || strValue.startsWith("0X")) { // NON-NLS
					numValue = Long.parseLong(strValue.substring(2), 16);
				}
			}
			if (numValue == null) {
				if (formatter != null) {
					numValue = formatter.parse(value.toString());
				} else if (radix != 10) {
					numValue = Long.parseLong(value.toString(), radix);
				} else {
					numValue = value instanceof Number ? (Number) value : Double.valueOf(value.toString());
				}
			}
			Number scaledValue = numValue.doubleValue() * scale.doubleValue();
			return Utils.castNumber(scaledValue, numValue.getClass());
		} catch (NumberFormatException nfe) {
			throw new ParseException(nfe.getLocalizedMessage(), 0);
		}
	}
}
