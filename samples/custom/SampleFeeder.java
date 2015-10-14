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

package com.jkool.tnt4j.streams.samples.custom;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.Map;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.inputs.ActivityFeeder;
import org.apache.log4j.Logger;

/**
 * Sample custom feeder.
 */
public class SampleFeeder extends ActivityFeeder
{
  private static final Logger logger = Logger.getLogger (SampleFeeder.class);

  private String fileName;
  private File activityFile;
  private LineNumberReader lineReader;
  private int lineNumber;

  private SampleFeeder ()
  {
    super (logger);
  }

  /**
   * Get value of specified property.
   *
   * @param name name of property whose value is to be retrieved
   *
   * @return value for property, or {@code null} if property does not exist
   */
  @Override
  public Object getProperty (String name)
  {
    if (StreamsConfig.PROP_FILENAME.equalsIgnoreCase (name))
    { return fileName; }
    return super.getProperty (name);
  }

  /**
   * Set properties for activity feeder.
   *
   * @param props properties to set
   *
   * @throws Throwable indicates error with properties
   */
  @Override
  public void setProperties (Collection<Map.Entry<String, String>> props) throws Throwable
  {
    if (props == null)
    { return; }
    super.setProperties (props);
    for (Map.Entry<String, String> prop : props)
    {
      String name = prop.getKey ();
      String value = prop.getValue ();
      if (StreamsConfig.PROP_FILENAME.equalsIgnoreCase (name))
      { fileName = value; }
    }
  }

  /**
   * Initialize the feeder.
   *
   * @throws Throwable indicates that feeder is not configured properly and
   *                   cannot continue.
   */
  @Override
  public void initialize () throws Throwable
  {
    super.initialize ();
    if (fileName == null)
    { throw new IllegalStateException ("SampleFeeder: File name not defined"); }
    if (logger.isDebugEnabled ())
    { logger.debug ("Opening file: " + fileName); }
    activityFile = new File (fileName);
    lineReader = new LineNumberReader (new FileReader (activityFile));
  }

  /**
   * Returns the next line in the file.
   *
   * @returns string containing next line in file.
   */
  @Override
  public Object getNextItem () throws Throwable
  {
    if (lineReader == null)
    { throw new IllegalStateException ("SampleFeeder: File is not opened for reading"); }
    String line = lineReader.readLine ();
    lineNumber = lineReader.getLineNumber ();
    return line;
  }

  /**
   * Returns line number of the file last read.
   */
  @Override
  public int getActivityPosition ()
  {
    return lineNumber;
  }

  /**
   * Closes file being read.
   */
  @Override
  protected void cleanup ()
  {
    if (lineReader != null)
    {
      try {lineReader.close ();} catch (IOException e) {}
      lineReader = null;
      activityFile = null;
    }
    super.cleanup ();
  }
}
