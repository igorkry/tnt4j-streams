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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.jkoolcloud.tnt4j.core.UsecTimestamp;

/**
 * Provides methods for parsing objects into timestamps and for formatting timestamps as strings.
 * <p>
 * This is based on {@link SimpleDateFormat}, but extends its support to recognize microsecond fractional seconds. If
 * number of fractional second characters is greater than 3, then it's assumed to be microseconds. Otherwise, it's
 * assumed to be milliseconds (as this is the behavior of {@link SimpleDateFormat}.
 *
 * @version $Revision: 1 $
 *
 * @see SimpleDateFormat
 * @see UsecTimestamp
 */
public class TimestampFormatter {

	private String pattern = null;
	private String timeZone = null;
	private TimeUnit units = null;
	private DateFormat formatter = null;
	private String locale = null;

	/**
	 * Creates a timestamp formatter/parser for numeric timestamps with the specified resolution.
	 *
	 * @param units
	 *            resolution of timestamp values
	 */
	public TimestampFormatter(TimeUnit units) {
		setUnits(units);
	}

	/**
	 * Creates a timestamp formatter/parser for date/time expressions, using the specified format pattern.
	 *
	 * @param pattern
	 *            format pattern, or {@code null} to use the default format
	 * @param timeZone
	 *            time zone ID, or {@code null} to use the default time zone or to assume pattern contains time zone
	 *            specification
	 * @param locale
	 *            locale for date format to use.
	 */
	public TimestampFormatter(String pattern, String timeZone, String locale) {
		setPattern(pattern, locale);
		this.timeZone = timeZone;
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
	 *            format pattern - can be set to {@code null} to use default format representation.
	 * @param locale
	 *            locale for date format to use.
	 */
	protected void setPattern(String pattern, String locale) {
		this.pattern = pattern;
		this.units = null;
		this.locale = locale;
		formatter = StringUtils.isEmpty(pattern) ? new SimpleDateFormat() : StringUtils.isEmpty(locale)
				? new SimpleDateFormat(pattern) : new SimpleDateFormat(pattern, Utils.getLocale(locale));
	}

	/**
	 * Gets the units for numeric timestamps.
	 *
	 * @return resolution of timestamp values
	 */
	public TimeUnit getUnits() {
		return units;
	}

	/**
	 * Sets the units for numeric timestamps.
	 *
	 * @param units
	 *            resolution of timestamp values
	 */
	protected void setUnits(TimeUnit units) {
		this.units = units;
		this.pattern = null;
		this.formatter = null;
		this.locale = null;
	}

	/**
	 * Gets the time zone ID that date/time strings are assumed to be in.
	 *
	 * @return time zone for date/time strings {@code null} indicates deriving from format string or default is being
	 *         used if no time zone specification in string)
	 */
	public String getTimeZone() {
		return timeZone;
	}

	/**
	 * Sets the time zone ID that date/time strings are assumed to be in.
	 *
	 * @param timeZone
	 *            time zone ID for time zone of date/time strings
	 */
	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * Parses the value into a timestamp with microsecond accuracy based on the timestamp pattern supported by this
	 * parser.
	 *
	 * @param value
	 *            value to convert
	 * @return formatted value of timestamp
	 * @throws ParseException
	 *             if an error parsing the specified value based timestamp pattern supported by this parser;
	 * @see #parse(TimeUnit, Object)
	 */
	public UsecTimestamp parse(Object value) throws ParseException {
		if (value instanceof UsecTimestamp) {
			return (UsecTimestamp) value;
		}
		if (value instanceof Date) {
			return new UsecTimestamp((Date) value);
		}
		if (value instanceof Calendar) {
			return new UsecTimestamp(((Calendar) value).getTimeInMillis(), 0);
		}
		if (value instanceof String || value instanceof Number) {
			if (units != null) {
				return parse(units, value);
			} else if (pattern != null) {
				return parse(pattern, value, timeZone, locale);
			}
		}
		throw new ParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
				"TimestampFormatter.unsupported.pattern", (value == null ? "null" : value.getClass().getName()), units,
				pattern, locale), 0);
	}

	/**
	 * Formats the given object representing a date/time as a string.
	 *
	 * @param value
	 *            date/time to format
	 * @return date/time formatted as a string
	 */
	public String format(Object value) {
		return formatter.format(value);
	}

	/**
	 * Parses the value into a timestamp with microsecond accuracy based on the specified units.
	 * <p>
	 * If {@code value} represents decimal number (as {@link Number} or {@link String}, fraction gets preserved by
	 * scaling down {@code value} in {@code units} until numeric value expression gets with low (epsilon is
	 * {@code 0.001}) or without fraction or {@code units} gets set to {@link TimeUnit#NANOSECONDS}.
	 *
	 * @param units
	 *            units that value is in
	 * @param value
	 *            value to convert
	 * @return microsecond timestamp
	 * @throws ParseException
	 *             if an error parsing the specified value
	 *
	 * @see #scale(double, TimeUnit)
	 */
	public static UsecTimestamp parse(TimeUnit units, Object value) throws ParseException {
		UsecTimestamp ts;
		try {
			long time;
			if (value instanceof Date) {
				time = ((Date) value).getTime();
				units = TimeUnit.MILLISECONDS;
			} else if (value instanceof Calendar) {
				time = ((Calendar) value).getTimeInMillis();
				units = TimeUnit.MILLISECONDS;
			} else {
				if (units == null) {
					units = TimeUnit.MILLISECONDS;
				}

				double dTime = value instanceof Number ? ((Number) value).doubleValue()
						: Double.parseDouble(value.toString());

				Pair<Double, TimeUnit> sTimePair = scale(dTime, units);
				dTime = sTimePair.getLeft();
				units = sTimePair.getRight();

				time = (long) dTime;
			}

			switch (units) {
			case NANOSECONDS:
				long scale = 1000000L;
				long mSecs = time / scale;
				long uSecs = (time - mSecs * scale) / 1000L;
				ts = new UsecTimestamp(mSecs, uSecs);
				break;
			case MICROSECONDS:
				scale = 1000L;
				mSecs = time / scale;
				uSecs = time - mSecs * scale;
				ts = new UsecTimestamp(mSecs, uSecs);
				break;
			default:
				ts = new UsecTimestamp(units.toMicros(time));
				break;
			}
		} catch (NumberFormatException nfe) {
			ParseException pe = new ParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"TimestampFormatter.failed.parsing", value, nfe.getLocalizedMessage()),
					0);
			pe.initCause(nfe);
			throw pe;
		}
		return ts;
	}

	/**
	 * Scales decimal timestamp value and value units to preserve fractional part of the value.
	 * <p>
	 * Scaling is performed until numeric value expression gets with low (epsilon is {@code 0.001}) or without fraction
	 * or {@code units} gets set to {@link TimeUnit#NANOSECONDS}.
	 *
	 * @param dTime
	 *            numeric timestamp value to scale
	 * @param units
	 *            timestamp value units
	 * @return pair of scaled timestamp value and units
	 */
	public static Pair<Double, TimeUnit> scale(double dTime, TimeUnit units) {
		double fraction = dTime % 1;

		if (!Utils.equals(fraction, 0.0, 0.001)) {
			switch (units) {
			case DAYS:
				dTime = dTime * 24L;
				break;
			case HOURS:
			case MINUTES:
				dTime = dTime * 60L;
				break;
			case SECONDS:
			case MILLISECONDS:
			case MICROSECONDS:
				dTime = dTime * 1000L;
				break;
			case NANOSECONDS:
			default:
				dTime = Math.round(dTime);
				break;
			}

			units = shiftDown(units);

			return scale(dTime, units);
		}

		return new ImmutablePair<>(dTime, units);
	}

	/**
	 * Shifts {@link TimeUnit} enum value to next lower scale value, e.g., {@link TimeUnit#SECONDS} ->
	 * {@link TimeUnit#MILLISECONDS}.
	 * 
	 * @param units
	 *            units to shift down
	 * @return scale down shifted time units value
	 */
	public static TimeUnit shiftDown(TimeUnit units) {
		int nui = units.ordinal() - 1;

		return nui >= 0 ? TimeUnit.values()[nui] : units;
	}

	/**
	 * Shifts {@link TimeUnit} enum value to next upper scale value, e.g., {@link TimeUnit#SECONDS} ->
	 * {@link TimeUnit#MINUTES}.
	 * 
	 * @param units
	 *            units to shift up
	 * @return scaled up shifted time units value
	 */
	public static TimeUnit shiftUp(TimeUnit units) {
		int nui = units.ordinal() + 1;

		return nui < TimeUnit.values().length ? TimeUnit.values()[nui] : units;
	}

	/**
	 * Parses the value into a timestamp with microsecond accuracy based on the timestamp pattern supported by this
	 * parser.
	 *
	 * @param pattern
	 *            pattern value is in
	 * @param value
	 *            value to convert
	 * @param timeZoneId
	 *            time zone that timeStampStr represents. This is only needed when formatStr does not include time zone
	 *            specification and timeStampStr does not represent a string in local time zone.
	 * @param locale
	 *            locale for date format to use.
	 * @return microsecond timestamp
	 * @throws ParseException
	 *             if an error parsing the specified value based on pattern
	 * @see java.util.TimeZone
	 */
	public static UsecTimestamp parse(String pattern, Object value, String timeZoneId, String locale)
			throws ParseException {
		String dateStr = String.valueOf(value);
		return new UsecTimestamp(dateStr, pattern, timeZoneId, locale);
	}

	/**
	 * Formats the given object representing a date/time as a string using the specified pattern.
	 *
	 * @param pattern
	 *            format pattern
	 * @param value
	 *            date/time to format
	 * @param locale
	 *            locale for date format to use.
	 * @return date /time formatted as a string
	 */
	public static String format(String pattern, Object value, String locale) {
		TimestampFormatter formatter = new TimestampFormatter(pattern, null, locale);
		return formatter.format(value);
	}

	/**
	 * Converts the specified numeric timestamp from one precision to another.
	 *
	 * @param timestamp
	 *            numeric timestamp to convert
	 * @param fromUnits
	 *            precision units timestamp is in
	 * @param toUnits
	 *            precision units to convert timestamp to
	 * @return converted numeric timestamp in precision units specified by toUnits
	 */
	public static long convert(Number timestamp, TimeUnit fromUnits, TimeUnit toUnits) {
		return toUnits.convert(timestamp == null ? 0 : timestamp.longValue(), fromUnits);
	}
}
