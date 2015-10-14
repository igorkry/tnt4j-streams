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

import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.fields.*;
import com.jkool.tnt4j.streams.inputs.ActivityFeeder;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * <p>Implements an activity data parser that assumes each activity data item is a string of fields
 * as defined by the specified regular expression, with the value for each field being retrieved
 * from either of the 1-based group position, or match position.</p>
 * <p>This parser supports the following properties:
 * <ul>
 * <li>Pattern</li>
 * </ul>
 * </p>
 *
 * @version $Revision: 5 $
 */
public class ActivityRegExParser extends ActivityParser
{
  private static final Logger logger = Logger.getLogger (ActivityRegExParser.class);

  /**
   * Contains the regular expression pattern that each data item is assumed to match
   * (set by {@code Pattern} property).
   */
  protected Pattern pattern = null;

  /**
   * Defines the mapping of activity fields to the regular expression group location(s)
   * in the raw data from which to extract its value.
   */
  protected HashMap<ActivityField, ArrayList<ActivityFieldLocator>> groupMap =
      new HashMap<ActivityField, ArrayList<ActivityFieldLocator>> ();

  /**
   * Defines the mapping of activity fields to the regular expression match sequence(s)
   * in the raw data from which to extract its value.
   */
  protected HashMap<ActivityField, ArrayList<ActivityFieldLocator>> matchMap =
      new HashMap<ActivityField, ArrayList<ActivityFieldLocator>> ();

  /**
   * Constructs an ActivityRegExParser.
   */
  public ActivityRegExParser ()
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setProperties (Collection<Entry<String, String>> props) throws Throwable
  {
    if (props == null)
    { return; }
    super.setProperties (props);
    for (Entry<String, String> prop : props)
    {
      String name = prop.getKey ();
      String value = prop.getValue ();
      if (StreamsConfig.PROP_PATTERN.equalsIgnoreCase (name))
      {
        if (!StringUtils.isEmpty (value))
        {
          pattern = Pattern.compile (value);
          if (logger.isDebugEnabled ())
          { logger.debug ("Setting " + name + " to '" + value + "'"); }
        }
      }
      else if (logger.isTraceEnabled ())
      {
        logger.trace ("Ignoring property " + name);
      }
    }
  }

  /* (non-Javadoc)
   * @see com.jkool.tnt4j.streams.parsers.ActivityParser#addField(com.jkool.tnt4j.streams.fields.ActivityField)
   */
  @Override
  public void addField (ActivityField field)
  {
    ArrayList<ActivityFieldLocator> locations = field.getLocators ();
    if (locations == null)
    { return; }
    ArrayList<ActivityFieldLocator> matchLocs = new ArrayList<ActivityFieldLocator> ();
    ArrayList<ActivityFieldLocator> groupLocs = new ArrayList<ActivityFieldLocator> ();
    for (ActivityFieldLocator locator : locations)
    {
      ActivityFieldLocatorType locType = ActivityFieldLocatorType.REGroupNum;
      try {locType = ActivityFieldLocatorType.valueOf (locator.getType ());} catch (Exception e) {}
      if (logger.isDebugEnabled ())
      { logger.debug ("Adding field " + field.toDebugString ()); }
      if (locType == ActivityFieldLocatorType.REMatchNum)
      {
        if (groupMap.containsKey (field))
        { throw new IllegalArgumentException ("Conflicting mapping for '" + field + "'"); }
        matchLocs.add (locator);
      }
      else
      {
        if (matchMap.containsKey (field))
        { throw new IllegalArgumentException ("Conflicting mapping for '" + field + "'"); }
        groupLocs.add (locator);
      }
    }
    if (matchLocs.size () > 0)
    { matchMap.put (field, matchLocs); }
    if (groupLocs.size () > 0)
    { groupMap.put (field, groupLocs); }
  }

  /**
   * {@inheritDoc}
   * <p> This parser supports the following class types
   * (and all classes extending/implementing any of these):</p>
   * <ul>
   * <li>{@code java.lang.String}</li>
   * <li>{@code java.io.Reader}</li>
   * <li>{@code java.io.InputStream}</li>
   * </ul>
   */
  @Override
  public boolean isDataClassSupported (Object data)
  {
    return (
        String.class.isInstance (data) ||
        Reader.class.isInstance (data) ||
        InputStream.class.isInstance (data));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ActivityInfo parse (ActivityFeeder feeder, Object data) throws IllegalStateException, ParseException
  {
    if (pattern == null || StringUtils.isEmpty (pattern.pattern ()))
    { throw new IllegalStateException ("ActivityRegExParser: regular expression pattern not specified or empty"); }
    if (data == null)
    { return null; }
    String dataStr = getNextString (data);
    if (StringUtils.isEmpty (dataStr))
    { return null; }
    if (logger.isDebugEnabled ())
    { logger.debug ("Parsing: " + dataStr); }
    Matcher m = pattern.matcher (dataStr);
    if (m == null || !m.matches ())
    {
      if (logger.isDebugEnabled ())
      { logger.debug ("Input does not match pattern"); }
      return null;
    }
    ActivityInfo ai = new ActivityInfo ();
    ActivityField field = null;
    Object value = null;
    // save entire activity string as message data
    field = new ActivityField (ActivityFieldType.ActivityData);
    applyFieldValue (ai, field, dataStr);
    // apply fields for parser
    try
    {
      if (matchMap.size () > 0)
      {
        if (logger.isDebugEnabled ())
        { logger.debug ("Applying RE Match mappings, count = " + matchMap.size ()); }
        ArrayList<String> matches = new ArrayList<String> ();
        matches.add ("");    // dummy entry to index array with match locations
        while (m.find ())
        {
          String matchStr = m.group ().trim ();
          matches.add (matchStr);
          if (logger.isTraceEnabled ())
          { logger.trace ("match " + matches.size () + " = " + matchStr); }
        }
        if (logger.isDebugEnabled ())
        { logger.debug ("Found " + matches.size () + " matches"); }
        for (Map.Entry<ActivityField, ArrayList<ActivityFieldLocator>> fieldMap : matchMap.entrySet ())
        {
          field = fieldMap.getKey ();
          ArrayList<ActivityFieldLocator> locations = fieldMap.getValue ();
          value = null;
          if (locations != null)
          {
            if (logger.isTraceEnabled ())
            { logger.trace ("Setting field " + field + " from match locations"); }
            if (locations.size () == 1)
            {
              value = getLocatorValue (feeder, locations.get (0), ActivityFieldLocatorType.REMatchNum, m, matches);
            }
            else
            {
              Object[] values = new Object[locations.size ()];
              for (int l = 0; l < locations.size (); l++)
              { values[l] = getLocatorValue (feeder, locations.get (l), ActivityFieldLocatorType.REMatchNum, m, matches); }
              value = values;
            }
          }
          applyFieldValue (ai, field, value);
        }
      }
    }
    catch (Exception e)
    {
      ParseException pe = new ParseException ("Failed parsing RE Match data for field " + field, 0);
      pe.initCause (e);
      throw pe;
    }
    try
    {
      for (Map.Entry<ActivityField, ArrayList<ActivityFieldLocator>> fieldMap : groupMap.entrySet ())
      {
        field = fieldMap.getKey ();
        ArrayList<ActivityFieldLocator> locations = fieldMap.getValue ();
        value = null;
        if (locations != null)
        {
          if (logger.isTraceEnabled ())
          { logger.trace ("Setting field " + field + " from group locations"); }
          if (locations.size () == 1)
          {
            value = getLocatorValue (feeder, locations.get (0), ActivityFieldLocatorType.REGroupNum, m, null);
          }
          else
          {
            Object[] values = new Object[locations.size ()];
            for (int l = 0; l < locations.size (); l++)
            { values[l] = getLocatorValue (feeder, locations.get (l), ActivityFieldLocatorType.REGroupNum, m, null); }
            value = values;
          }
        }
        applyFieldValue (ai, field, value);
      }
    }
    catch (Exception e)
    {
      ParseException pe = new ParseException ("Failed parsing RE Group data for field " + field, 0);
      pe.initCause (e);
      throw pe;
    }
    return ai;
  }

  private Object getLocatorValue (
      ActivityFeeder feeder, ActivityFieldLocator locator, ActivityFieldLocatorType locType, Matcher m, ArrayList<String> matches)
      throws ParseException
  {
    Object val = null;
    if (locator != null)
    {
      String locStr = locator.getLocator ();
      if (!StringUtils.isEmpty (locStr))
      {
        if (locator.getBuiltInType () == ActivityFieldLocatorType.FeederProp)
        {
          val = feeder.getProperty (locStr);
        }
        else
        {
          int loc = Integer.parseInt (locStr);
          if (locType == ActivityFieldLocatorType.REMatchNum)
          {
            if (loc <= matches.size ())
            { val = matches.get (loc); }
          }
          else
          {
            if (loc <= m.groupCount ())
            { val = m.group (loc); }
          }
        }
      }
      val = locator.formatValue (val);
    }
    return val;
  }
}
