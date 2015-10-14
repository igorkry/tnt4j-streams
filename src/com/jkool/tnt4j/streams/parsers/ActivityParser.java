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

package com.jkool.tnt4j.streams.parsers;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import com.jkool.tnt4j.streams.fields.ActivityField;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.inputs.ActivityFeeder;
import org.apache.log4j.Logger;

/**
 * Base class that all activity parsers must extend.  It provides some base functionality
 * useful for all activity parsers.
 *
 * @version $Revision: 2 $
 */
public abstract class ActivityParser
{
  private static Logger logger = Logger.getLogger (ActivityParser.class);

  /**
   * Initializes ActivityParser.
   */
  protected ActivityParser ()
  {
  }

  /**
   * Defines the mapping of activity fields to the location(s) in the raw data
   * from which to extract their values.
   */
  protected HashMap<ActivityField, ArrayList<ActivityFieldLocator>> fieldMap =
      new HashMap<ActivityField, ArrayList<ActivityFieldLocator>> ();

  /**
   * Set properties for the parser.
   * <p>This method is called during the parsing of the configuration when all
   * specified properties in the configuration have been loaded.  In general,
   * parsers should ignore properties that they do not recognize, since they
   * may be valid for a subclass of the parser.  If extending an existing
   * parser subclass, the method from the base class should be called so that
   * it can process any properties it requires.</p>
   *
   * @param props properties to set
   *
   * @throws Throwable indicates error with properties
   */
  public void setProperties (Collection<Entry<String, String>> props) throws Throwable
  {
  }

  /**
   * Add an activity field definition to the set of fields supported by this parser.
   *
   * @param field activity field to add
   */
  public void addField (ActivityField field)
  {
    if (logger.isDebugEnabled ())
    { logger.debug ("Adding field " + field.toDebugString ()); }
    fieldMap.put (field, field.getLocators ());
  }

  /**
   * Parse the specified raw activity data, converting each field in raw data
   * to its corresponding value for passing to jKool Cloud Service.
   *
   * @param feeder parent feeder
   * @param data   raw activity data to parse
   *
   * @return converted activity info, or {@code null} if raw data string does not
   * match format for this parser
   *
   * @throws IllegalStateException if parser has not been properly initialized
   * @throws ParseException        if an error parsing raw data string
   * @see #isDataClassSupported(Object)
   */
  public abstract ActivityInfo parse (ActivityFeeder feeder, Object data) throws IllegalStateException, ParseException;

  /**
   * Returns whether this parser supports the given format of the activity data.
   * This is used by activity feeders to determine if the parser can parse the data
   * in the format that the feeder has it.
   *
   * @param data data object whose class is to be verified
   *
   * @return {@code true} if this parser can process data in the
   * specified format, {@code false} otherwise
   */
  public abstract boolean isDataClassSupported (Object data);

  /**
   * Reads the next string (line) from the specified data input source.
   *
   * @param data input source for activity data
   *
   * @return string, or {@code null} if end of input source has been reached
   *
   * @throws UnsupportedOperationException if the class of input source supplied
   *                                       is not supported.
   */
  protected String getNextString (Object data)
  {
    String str = null;
    BufferedReader rdr = null;
    if (data == null)
    { return null; }
    if (data instanceof String)
    { return (String) data; }
    if (data instanceof BufferedReader)
    {
      rdr = (BufferedReader) data;
    }
    else if (data instanceof Reader)
    {
      rdr = new BufferedReader ((Reader) data);
    }
    else if (data instanceof InputStream)
    {
      rdr = new BufferedReader (new InputStreamReader ((InputStream) data));
    }
    else
    {
      throw new UnsupportedOperationException ("data in the format of a " + data.getClass ().getName () + " is not supported");
    }
    try
    {
      str = rdr.readLine ();
    }
    catch (EOFException eof)
    {
      if (logger.isDebugEnabled ())
      { logger.debug ("Reached end of data stream", eof); }
    }
    catch (IOException ioe)
    {
      logger.warn ("Error reading from data stream", ioe);
    }
    return str;
  }

  /**
   * Sets the value for the field in the specified activity.
   *
   * @param ai    activity object whose field is to be set
   * @param field field to apply value to
   * @param value value to apply for this field
   *
   * @throws ParseException if an error parsing the specified value
   */
  protected void applyFieldValue (ActivityInfo ai, ActivityField field, Object value) throws ParseException
  {
    ai.applyField (field, value);
  }
}
