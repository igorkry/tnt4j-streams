/*
 * Copyright 2014-2018 JKOOL, LLC.
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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Provides methods for parsing objects into numeric values and for formatting numeric values as strings.
 *
 * @version $Revision: 1 $
 *
 * @see DecimalFormat
 */
public class NumericFormatter {

	private int radix = 10;
	private FormatterContext formatter = null;

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
	 * <p>
	 * Pattern also can be one of number types enumerators: {@code "integer"},
	 * {@code "long"},{@code "double"},{@code "float"},{@code "short"} and {@code "byte"}.
	 *
	 * @param pattern
	 *            format pattern - can be set to {@code null} to use default representation
	 * @param locale
	 *            locale for decimal format to use, or {@code null} if default locale shall be used
	 *
	 * @see #setPattern(String, String)
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
		return formatter == null ? null : formatter.pattern;
	}

	/**
	 * Gets the locale definition string for this formatter.
	 *
	 * @return formatter used locale, or {@code null} if none specified
	 */
	public String getLocale() {
		return formatter == null ? null : formatter.locale;
	}

	/**
	 * Sets the format pattern string for this formatter.
	 * <p>
	 * Pattern also can be one of number types enumerators: {@code "integer"},
	 * {@code "long"},{@code "double"},{@code "float"},{@code "short"} and {@code "byte"}.
	 *
	 * @param pattern
	 *            format pattern - can be set to {@code null} to use default representation
	 * @param locale
	 *            locale for decimal format to use, or {@code null} if default locale shall be used
	 */
	public void setPattern(String pattern, String locale) {
		formatter = new FormatterContext(pattern, locale);
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
	 *
	 * @see #parse(com.jkoolcloud.tnt4j.streams.utils.NumericFormatter.FormatterContext, int, Object, Number)
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
	 *
	 * @see #parse(com.jkoolcloud.tnt4j.streams.utils.NumericFormatter.FormatterContext, int, Object, Number)
	 */
	public Number parse(Object value, Number scale) throws ParseException {
		return parse(formatter, radix, value, scale);
	}

	/**
	 * Formats the specified object using the defined pattern, or using the default numeric formatting if no pattern was
	 * defined.
	 * <p>
	 * Pattern also can be one of number types enumerators: {@code "integer"},
	 * {@code "long"},{@code "double"},{@code "float"},{@code "short"} and {@code "byte"}.
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
	 *
	 * @see #parse(String, Object, Number, String)
	 */
	public static Number parse(String pattern, Object value, Number scale) throws ParseException {
		return parse(pattern, value, scale, null);
	}

	/**
	 * Formats the specified object using the defined pattern, or using the default numeric formatting if no pattern was
	 * defined.
	 * <p>
	 * Pattern also can be one of number types enumerators: {@code "integer"},
	 * {@code "long"},{@code "double"},{@code "float"},{@code "short"} and {@code "byte"}.
	 *
	 * @param pattern
	 *            number format pattern
	 * @param value
	 *            value to convert
	 * @param scale
	 *            value to multiply the formatted value by
	 * @param locale
	 *            locale for decimal format to use, or {@code null} if default locale shall be used
	 * @return formatted value of field in required internal data type
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             pattern, etc.)
	 *
	 * @see #parse(com.jkoolcloud.tnt4j.streams.utils.NumericFormatter.FormatterContext, int, Object, Number)
	 */
	public static Number parse(String pattern, Object value, Number scale, String locale) throws ParseException {
		return parse(new FormatterContext(pattern, locale), 10, value, scale);
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
	private static Number parse(FormatterContext formatter, int radix, Object value, Number scale)
			throws ParseException {
		if (value == null) {
			return null;
		}
		Number numValue = null;
		if (value instanceof Number) {
			numValue = (Number) value;

			if (formatter != null) {
				if (FormatterContext.INT.equalsIgnoreCase(formatter.pattern)) {
					numValue = numValue.intValue();
				} else if (FormatterContext.LONG.equalsIgnoreCase(formatter.pattern)) {
					numValue = numValue.longValue();
				} else if (FormatterContext.DOUBLE.equalsIgnoreCase(formatter.pattern)) {
					numValue = numValue.doubleValue();
				} else if (FormatterContext.FLOAT.equalsIgnoreCase(formatter.pattern)) {
					numValue = numValue.floatValue();
				} else if (FormatterContext.SHORT.equalsIgnoreCase(formatter.pattern)) {
					numValue = numValue.shortValue();
				} else if (FormatterContext.BYTE.equalsIgnoreCase(formatter.pattern)) {
					numValue = numValue.byteValue();
				}
			}
		} else {
			String strValue = Utils.toString(value).trim();
			if (StringUtils.isEmpty(strValue)) {
				return null;
			}

			Exception nfe;
			if (formatter != null && formatter.format != null) {
				try {
					numValue = formatter.format.parse(strValue);
					nfe = null;
				} catch (ParseException exc) {
					nfe = exc;
				}
			} else {
				if (radix != 10) {
					try {
						numValue = Long.parseLong(value.toString(), radix);
						nfe = null;
					} catch (NumberFormatException exc) {
						nfe = exc;
					}
				} else {
					try {
						numValue = strToNumber(strValue);
						nfe = null;
					} catch (NumberFormatException exc) {
						nfe = exc;
					}

					if (numValue == null && formatter != null) {
						try {
							numValue = formatter.getGPFormat().parse(strValue);
							nfe = null;
						} catch (ParseException exc) {
							nfe = exc;
						}
					}
				}
			}

			if (nfe != null) {
				ParseException pe = new ParseException(nfe.getLocalizedMessage(), 0);
				pe.initCause(nfe);

				throw pe;
			}
		}

		return scaleNumber(numValue, scale);
	}

	/**
	 * Resolves number value from provided string.
	 *
	 * @param str
	 *            string defining numeric value
	 * @return number value built from provided {@code str}, or {@code null} if {@code str} is {@code null} or empty
	 */
	public static Number strToNumber(String str) {
		if (StringUtils.isEmpty(str)) {
			return null;
		}

		try {
			return Integer.valueOf(str, 10);
		} catch (Exception exc) {
			return NumberUtils.createNumber(str);
		}
	}

	/**
	 * Scales number value <tt>numValue</tt> by <tt>scale</tt> factor.
	 *
	 * @param numValue
	 *            number value to scale
	 * @param scale
	 *            scale factor
	 * @return scaled number value
	 */
	private static Number scaleNumber(Number numValue, Number scale) {
		if (numValue == null || scale == null) {
			return numValue;
		}

		Number scaledValue = numValue.doubleValue() * scale.doubleValue();
		return Utils.castNumber(scaledValue, numValue.getClass());
	}

	/**
	 * Number formatting context values.
	 */
	private static class FormatterContext {
		private static String INT = "integer"; // NON-NLS
		private static String LONG = "long"; // NON-NLS
		private static String DOUBLE = "double"; // NON-NLS
		private static String FLOAT = "float"; // NON-NLS
		private static String SHORT = "short"; // NON-NLS
		private static String BYTE = "byte"; // NON-NLS

		// private static final String GENERIC_NUMBER_PATTERN = "###,###.###"; // NON-NLS

		private String pattern;
		private String locale;
		private NumberFormat format;

		/**
		 * Creates a number formatter context using defined format <tt>pattern</tt></> and default locale.
		 * <p>
		 * Pattern also can be one of number types enumerators: {@code "integer"},
		 * {@code "long"},{@code "double"},{@code "float"},{@code "short"} and {@code "byte"}.
		 *
		 * @param pattern
		 *            format pattern - can be set to {@code null} to use default representation.
		 */
		private FormatterContext(String pattern) {
			this(pattern, null);
		}

		/**
		 * Creates a number formatter context using defined format <tt>pattern</tt></> and <tt>locale</tt>.
		 * <p>
		 * Pattern also can be one of number types enumerators: {@code "integer"},
		 * {@code "long"},{@code "double"},{@code "float"},{@code "short"} and {@code "byte"}.
		 *
		 * @param pattern
		 *            format pattern - can be set to {@code null} to use default representation.
		 * @param locale
		 *            locale for decimal format to use, or {@code null} if default locale shall be used
		 */
		private FormatterContext(String pattern, String locale) {
			this.pattern = pattern;
			this.locale = locale;

			Locale loc = Utils.getLocale(locale);

			if (INT.equalsIgnoreCase(pattern) || LONG.equalsIgnoreCase(pattern) || BYTE.equalsIgnoreCase(pattern)
					|| SHORT.equalsIgnoreCase(pattern)) {
				format = loc == null ? NumberFormat.getIntegerInstance() : NumberFormat.getIntegerInstance(loc);
			} else if (DOUBLE.equalsIgnoreCase(pattern) || FLOAT.equalsIgnoreCase(pattern)) {
				format = loc == null ? NumberFormat.getNumberInstance() : NumberFormat.getNumberInstance(loc);
			} else {
				format = StringUtils.isEmpty(pattern) ? null : loc == null ? new DecimalFormat(pattern)
						: new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(loc));
			}
		}

		/**
		 * Builds number format instance to parse number from string using general-purpose number format for default or
		 * specified {@code locale}.
		 *
		 * @return general-purpose number format
		 *
		 * @see #getGPFormat(String)
		 */
		private NumberFormat getGPFormat() {
			return getGPFormat(locale);
		}

		/**
		 * Builds number format instance to parse number from string using general-purpose number format for default or
		 * specified <tt>locale</tt>.
		 *
		 * @param locale
		 *            locale for decimal format to use, or {@code null} if default locale shall be used
		 *
		 * @return general-purpose number format
		 */
		private static NumberFormat getGPFormat(String locale) {
			// return StringUtils.isEmpty(locale) ? new DecimalFormat(GENERIC_NUMBER_PATTERN)
			// : new DecimalFormat(GENERIC_NUMBER_PATTERN, new DecimalFormatSymbols(Utils.getLocale(locale)));

			return StringUtils.isEmpty(locale) ? NumberFormat.getNumberInstance()
					: NumberFormat.getNumberInstance(Utils.getLocale(locale));
		}
	}
}
