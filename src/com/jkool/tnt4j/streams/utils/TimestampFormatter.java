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

package com.jkool.tnt4j.streams.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.jkool.tnt4j.streams.fields.ActivityFieldUnitsType;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>Provides methods for parsing objects into timestamps and for formatting
 * timestamps as strings.</p>
 * <p>This is based on {@link SimpleDateFormat}, but extends its support to
 * recognize microsecond fractional seconds.  If number of fractional second
 * characters is greater than 3, then it's assumed to be microseconds.
 * Otherwise, it's assumed to be milliseconds (as this is the behavior of
 * {@link SimpleDateFormat}.
 *
 * @version $Revision: 6 $
 * @see SimpleDateFormat
 * @see StreamTimestamp
 */
public class TimestampFormatter
{
  private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink (TimestampFormatter.class);

  private String pattern = null;
  private String timeZone = null;
  private ActivityFieldUnitsType units = null;
  private DateFormat formatter = null;
  private String locale = null;

  /**
   * Creates a timestamp formatter/parser for numeric timestamps with the specified resolution.
   *
   * @param units resolution of timestamp values
   */
  public TimestampFormatter (ActivityFieldUnitsType units)
  {
    setUnits (units);
  }

  /**
   * Creates a timestamp formatter/parser for date/time expressions, using the specified
   * format pattern.
   *
   * @param pattern  format pattern, or {@code null} to use the default format
   * @param timeZone time zone ID, or {@code null} to use the default time zone or to assume
   *                 pattern contains time zone specification
   * @param locale   locale for date format to use.
   */
  public TimestampFormatter (String pattern, String timeZone, String locale)
  {
    setPattern (pattern, locale);
    this.timeZone = timeZone;
  }

  /**
   * Gets the format pattern string for this formatter.
   *
   * @return format pattern, or {@code null} if none specified
   */
  public String getPattern ()
  {
    return pattern;
  }

  /**
   * Sets the format pattern string for this formatter.
   *
   * @param pattern format pattern - can be set to {@code null} to
   *                use default format representation.
   * @param locale  locale for date format to use.
   */
  protected void setPattern (String pattern, String locale)
  {
    this.pattern = pattern;
    this.units = null;
    this.locale = locale;
    formatter = StringUtils.isEmpty (pattern) ? new SimpleDateFormat () : StringUtils.isEmpty (locale) ? new SimpleDateFormat (pattern)
                                                                                                       : new SimpleDateFormat (pattern,
                                                                                                                               Locale.forLanguageTag (
                                                                                                                                   locale));
  }

  /**
   * Gets the units for numeric timestamps.
   *
   * @return resolution of timestamp values
   */
  public ActivityFieldUnitsType getUnits ()
  {
    return units;
  }

  /**
   * Sets the units for numeric timestamps.
   *
   * @param units resolution of timestamp values
   */
  protected void setUnits (ActivityFieldUnitsType units)
  {
    this.units = units;
    this.pattern = null;
    this.formatter = null;
    this.locale = null;
  }

  /**
   * Gets the time zone ID that date/time strings are assumed to be in.
   *
   * @return time zone for date/time strings ({@code null} indicates
   * deriving from format string or default is being used if no
   * time zone specification in string)
   */
  public String getTimeZone ()
  {
    return timeZone;
  }

  /**
   * Sets the time zone ID that date/time strings are assumed to be in.
   *
   * @param timeZone time zone ID for time zone of date/time strings
   */
  public void setTimeZone (String timeZone)
  {
    this.timeZone = timeZone;
  }

  /**
   * Parses the value into a timestamp with microsecond accuracy based on the
   * timestamp pattern supported by this parser.
   *
   * @param value value to convert
   *
   * @return formatted value of timestamp
   *
   * @throws ParseException if an error parsing the specified value based
   *                        timestamp pattern supported by this parser;
   */
  public StreamTimestamp parse (Object value) throws ParseException
  {
    if (value instanceof StreamTimestamp)
    {
      return (StreamTimestamp) value;
    }
    if (value instanceof Date)
    {
      return new StreamTimestamp (((Date) value).getTime ());
    }
    if (value instanceof Calendar)
    {
      return new StreamTimestamp (((Calendar) value).getTimeInMillis ());
    }
    if (value instanceof String || value instanceof Number)
    {
      if (units != null)
      {
        return parse (units, value);
      }
      else if (pattern != null)
      {
        return parse (pattern, value, timeZone, locale);
      }
    }
    throw new ParseException ("Unsupported date/time pattern: " +
                              ", dataType=" + (value == null ? "null" : value.getClass ().getName ()) +
                              ", units=" + units +
                              ", pattern=" + pattern +
                              ", locale=" + locale, 0);
  }

  /**
   * Formats the given object representing a date/time as a string.
   *
   * @param value date/time to format
   *
   * @return date/time formatted as a string
   */
  public String format (Object value)
  {
    return formatter.format (value);
  }

  /**
   * Parses the value into a timestamp with microsecond accuracy based on the
   * specified units.
   *
   * @param units units that value is in
   * @param value value to convert
   *
   * @return microsecond timestamp
   *
   * @throws ParseException if an error parsing the specified value
   */
  public static StreamTimestamp parse (ActivityFieldUnitsType units, Object value) throws ParseException
  {
    StreamTimestamp ts;
    try
    {
      long time;
      if (value instanceof Date)
      {
        time = ((Date) value).getTime ();
        units = ActivityFieldUnitsType.Milliseconds;
      }
      else if (value instanceof Calendar)
      {
        time = ((Calendar) value).getTimeInMillis ();
        units = ActivityFieldUnitsType.Milliseconds;
      }
      else
      {
        time = value instanceof Number ? ((Number) value).longValue () : Long.parseLong (value.toString ());
      }
      switch (units)
      {
        case Microseconds:
          long mSecs = time / 1000L;
          long uSecs = time - mSecs * 1000L;
          ts = new StreamTimestamp (mSecs, uSecs);
          break;
        case Seconds:
          ts = new StreamTimestamp (time * 1000L);
          break;
        default:
          ts = new StreamTimestamp (time);
          break;
      }
    }
    catch (NumberFormatException nfe)
    {
      ParseException pe = new ParseException ("Failed to parse date/time value '" + value + "': " + nfe.getMessage (), 0);
      pe.initCause (nfe);
      throw pe;
    }
    return ts;
  }

  /**
   * Parses the value into a timestamp with microsecond accuracy based on the
   * timestamp pattern supported by this parser.
   *
   * @param pattern    pattern value is in
   * @param value      value to convert
   * @param timeZoneId time zone that timeStampStr represents. This is only needed when formatStr does not include
   *                   time zone specification and timeStampStr does not represent a string in local time zone.
   * @param locale     locale for date format to use.
   *
   * @return microsecond timestamp
   *
   * @throws ParseException if an error parsing the specified value based on pattern
   * @see java.util.TimeZone
   */
  public static StreamTimestamp parse (String pattern, Object value, String timeZoneId, String locale) throws ParseException
  {
    String dateStr = value.toString ();
    return new StreamTimestamp (dateStr, pattern, timeZoneId, locale);
  }

  /**
   * Formats the given object representing a date/time as a string
   * using the specified pattern.
   *
   * @param pattern format pattern
   * @param value   date/time to format
   * @param locale  locale for date format to use.
   *
   * @return date/time formatted as a string
   */
  public static String format (String pattern, Object value, String locale)
  {
    TimestampFormatter formatter = new TimestampFormatter (pattern, null, locale);
    return formatter.format (value);
  }

  /**
   * Converts the specified numeric timestamp from one precision to another.
   *
   * @param timestamp numeric timestamp to convert
   * @param fromUnits precision units timestamp is in
   * @param toUnits   precision units to convert timestamp to
   *
   * @return converted numeric timestamp in precision units specified by toUnits
   *
   * @throws ParseException if an error parsing or converting the timestamp
   */
  public static Number convert (Number timestamp, ActivityFieldUnitsType fromUnits, ActivityFieldUnitsType toUnits) throws ParseException
  {
    double scale = 1.0;
    if (fromUnits != null && toUnits != null && fromUnits != toUnits)
    {
      switch (fromUnits)
      {
        case Microseconds:
          switch (toUnits)
          {
            case Microseconds:
              scale = 1.0;
              break;
            case Milliseconds:
              scale = 0.001;
              break;
            case Seconds:
              scale = 0.000001;
              break;
          }
          break;
        case Milliseconds:
          switch (toUnits)
          {
            case Microseconds:
              scale = 1000.0;
              break;
            case Milliseconds:
              scale = 1.0;
              break;
            case Seconds:
              scale = 0.001;
              break;
          }
          break;
        case Seconds:
          switch (toUnits)
          {
            case Microseconds:
              scale = 1000000.0;
              break;
            case Milliseconds:
              scale = 1000.0;
              break;
            case Seconds:
              scale = 1.0;
              break;
          }
          break;
      }
    }
    return NumericFormatter.parse (null, timestamp, scale);
  }
}
