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

import java.text.DecimalFormat;
import java.text.ParseException;

import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;
import org.apache.commons.lang3.StringUtils;

/**
 * Provides methods for parsing objects into numeric values and for formatting
 * numeric values as strings.
 *
 * @version $Revision: 4 $
 * @see DecimalFormat
 */
public class NumericFormatter
{
  private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink (NumericFormatter.class);

  private int radix = 10;
  private String pattern = null;
  private DecimalFormat formatter = null;

  /**
   * Creates a number formatter using the default numeric representation.
   */
  public NumericFormatter ()
  {
    this (null);
  }

  /**
   * Creates a number formatter using the default numeric representation in the specified radix.
   *
   * @param radix the radix to use while parsing numeric strings
   */
  public NumericFormatter (int radix)
  {
    this.radix = radix;
  }

  /**
   * Creates a number formatter/parser for numbers using the specified format pattern.
   *
   * @param pattern format pattern
   */
  public NumericFormatter (String pattern)
  {
    setPattern (pattern);
  }

  /**
   * Gets the radix used by this formatter.
   *
   * @return radix used for parsing numeric strings
   */
  public int getRadix ()
  {
    return radix;
  }

  /**
   * Sets the radix used by this formatter.
   *
   * @param radix the radix to use while parsing numeric strings
   */
  public void setRadix (int radix)
  {
    this.radix = radix;
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
   *                use default representation.
   */
  public void setPattern (String pattern)
  {
    this.pattern = pattern;
    formatter = StringUtils.isEmpty (pattern) ? null : new DecimalFormat (pattern);
  }

  /**
   * Formats the specified object using the defined pattern, or using the default
   * numeric formatting if no pattern was defined.
   *
   * @param value value to convert
   *
   * @return formatted value of field in required internal data type
   *
   * @throws ParseException if an error parsing the specified value based on the field
   *                        definition (e.g. does not match defined pattern, etc.)
   */
  public Number parse (Object value) throws ParseException
  {
    return parse (formatter, radix, value, 1.0);
  }

  /**
   * Formats the specified object using the defined pattern, or using the default
   * numeric formatting if no pattern was defined.
   *
   * @param value value to convert
   * @param scale value to multiply the formatted value by
   *
   * @return formatted value of field in required internal data type
   *
   * @throws ParseException if an error parsing the specified value based on the field
   *                        definition (e.g. does not match defined pattern, etc.)
   */
  public Number parse (Object value, Number scale) throws ParseException
  {
    return parse (formatter, radix, value, scale);
  }

  /**
   * Formats the specified object using the defined pattern, or using the default
   * numeric formatting if no pattern was defined.
   *
   * @param pattern number format pattern
   * @param value   value to convert
   * @param scale   value to multiply the formatted value by
   *
   * @return formatted value of field in required internal data type
   *
   * @throws ParseException if an error parsing the specified value based on the field
   *                        definition (e.g. does not match defined pattern, etc.)
   * @see java.text.DecimalFormat#DecimalFormat(String)
   */
  public static Number parse (String pattern, Object value, Number scale) throws ParseException
  {
    return parse (new DecimalFormat (pattern), 10, value, scale);
  }

  /**
   * Formats the specified object using the defined pattern, or using the default
   * numeric formatting if no pattern was defined.
   *
   * @param formatter formatter object to apply to value
   * @param radix     the radix to use while parsing numeric strings
   * @param value     value to convert
   * @param scale     value to multiply the formatted value by
   *
   * @return formatted value of field in required internal data type
   *
   * @throws ParseException if an error parsing the specified value based on the field
   *                        definition (e.g. does not match defined pattern, etc.)
   */
  private static Number parse (DecimalFormat formatter, int radix, Object value, Number scale) throws ParseException
  {
    if (value == null)
    {
      return null;
    }
    if (scale == null)
    {
      scale = 1.0;
    }
    try
    {
      Number numValue = null;
      if (formatter == null && value instanceof String)
      {
        String strValue = (String) value;
        if (strValue.startsWith ("0x") || strValue.startsWith ("0X"))
        {
          numValue = Long.parseLong (strValue.substring (2), 16);
        }
      }
      if (numValue == null)
      {
        if (formatter != null)
        {
          numValue = formatter.parse (value.toString ());
        }
        else if (radix != 10)
        {
          numValue = Long.parseLong (value.toString (), radix);
        }
        else
        {
          numValue = value instanceof Number ? (Number) value : Double.valueOf (value.toString ());
        }
      }
      return numValue.doubleValue () * scale.doubleValue ();
    }
    catch (NumberFormatException nfe)
    {
      throw new ParseException (nfe.getMessage (), 0);
    }
  }
}
