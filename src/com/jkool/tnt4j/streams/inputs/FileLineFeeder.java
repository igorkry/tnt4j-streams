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

package com.jkool.tnt4j.streams.inputs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.Map.Entry;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
import org.apache.log4j.Logger;

/**
 * <p>Implements a file activity feeder, where each line of the file is
 * assumed to represent a single activity or event which should be recorded.</p>
 * <p>This activity feeder requires parsers that can support {@code String} data.</p>
 * <p>This activity feeder supports the following properties:
 * <ul>
 * <li>FileName</li>
 * </ul>
 * </p>
 *
 * @version $Revision: 3 $
 * @see ActivityParser#isDataClassSupported(Object)
 */
public class FileLineFeeder extends ActivityFeeder
{
  private static final Logger logger = Logger.getLogger (FileLineFeeder.class);

  private String fileName;
  private File activityFile;
  private LineNumberReader lineReader;
  private int lineNumber;

  /**
   * Constructs an FileLineFeeder.
   */
  public FileLineFeeder ()
  {
    super (logger);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getProperty (String name)
  {
    if (StreamsConfig.PROP_FILENAME.equalsIgnoreCase (name))
    { return fileName; }
    return super.getProperty (name);
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
      if (StreamsConfig.PROP_FILENAME.equalsIgnoreCase (name))
      { fileName = value; }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize () throws Throwable
  {
    super.initialize ();
    if (fileName == null)
    { throw new IllegalStateException ("FileLineFeeder: File name not defined"); }
    if (logger.isDebugEnabled ())
    { logger.debug ("Opening file: " + fileName); }
    activityFile = new File (fileName);
    lineReader = new LineNumberReader (new FileReader (activityFile));
  }

  /**
   * {@inheritDoc}
   * <p>This method returns a string containing the contents of the next line in the file.</p>
   */
  @Override
  public Object getNextItem () throws Throwable
  {
    if (lineReader == null)
    { throw new IllegalStateException ("FileLineFeeder: File is not opened for reading"); }
    String line = lineReader.readLine ();
    lineNumber = lineReader.getLineNumber ();
    return line;
  }

  /**
   * {@inheritDoc}
   * <p>This method returns line number of the file last read.</p>
   */
  @Override
  public int getActivityPosition ()
  {
    return lineNumber;
  }

  /**
   * {@inheritDoc}
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
