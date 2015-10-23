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
import java.util.Map;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>Implements a file activity stream, where each line of the file is
 * assumed to represent a single activity or event which should be recorded.</p>
 * <p>This activity stream requires parsers that can support {@code String} data.</p>
 * <p>This activity stream supports the following properties:
 * <ul>
 * <li>FileName</li>
 * </ul>
 *
 * @version $Revision: 3 $
 * @see ActivityParser#isDataClassSupported(Object)
 */
public class FileLineStream extends TNTInputStream
{
  private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink (FileLineStream.class);

  private String fileName = null;
  private File activityFile = null;
  private LineNumberReader lineReader = null;
  private int lineNumber = 0;

  /**
   * Constructs an FileLineStream.
   */
  public FileLineStream ()
  {
    super (LOGGER);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getProperty (String name)
  {
    if (StreamsConfig.PROP_FILENAME.equalsIgnoreCase (name))
    {
      return fileName;
    }
    return super.getProperty (name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setProperties (Collection<Map.Entry<String, String>> props)
  {
    if (props == null)
    {
      return;
    }
    for (Map.Entry<String, String> prop : props)
    {
      String name = prop.getKey ();
      String value = prop.getValue ();
      if (StreamsConfig.PROP_FILENAME.equalsIgnoreCase (name))
      {
        fileName = value;
      }
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
    {
      throw new IllegalStateException ("FileLineStream: File name not defined");
    }
    LOGGER.log (OpLevel.DEBUG, "Opening file: {0}", fileName);
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
    {
      throw new IllegalStateException ("FileLineStream: File is not opened for reading");
    }
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
      try
      {
        lineReader.close ();
      }
      catch (IOException e)
      {
      }
      lineReader = null;
      activityFile = null;
    }
    super.cleanup ();
  }
}
