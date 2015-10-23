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

package com.jkool.tnt4j.streams.tnt4j;

import java.io.IOException;
import java.util.Map;

import com.jkool.jesl.tnt4j.sink.JKCloudEventSink;
import com.nastel.jkool.tnt4j.core.KeyValueStats;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.core.Snapshot;
import com.nastel.jkool.tnt4j.format.EventFormatter;
import com.nastel.jkool.tnt4j.sink.*;
import com.nastel.jkool.tnt4j.source.Source;
import com.nastel.jkool.tnt4j.tracker.TrackingActivity;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;

public class StreamsEventSink extends AbstractEventSink
{
  private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink (StreamsEventSink.class);

  private Sink outSink;

  public StreamsEventSink (String name, String url, String gwAccessToken, EventFormatter formatter)
  {
    super (name, formatter);
    if (url.startsWith (StreamsConstants.FILE_PREFIX))
    {
      String fileName = url.substring (StreamsConstants.FILE_PREFIX.length ());
      outSink = new FileSink (fileName, true, formatter);
    }
    else
    {
      outSink = new JKCloudEventSink (name, url, gwAccessToken, formatter, null);
    }
  }

  private void writeFormattedMsg (String msg) throws IOException, InterruptedException
  {
    if (isOpen ())
    {
      outSink.write (msg);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void _log (TrackingEvent event) throws Exception
  {
    writeFormattedMsg (getEventFormatter ().format (event));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void _log (TrackingActivity activity) throws Exception
  {
    writeFormattedMsg (getEventFormatter ().format (activity));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void _log (Snapshot snapshot) throws Exception
  {
    writeFormattedMsg (getEventFormatter ().format (snapshot));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void _log (
      long ttl, Source src, OpLevel sev, String msg, Object... args) throws Exception
  {
    writeFormattedMsg (getEventFormatter ().format (ttl, src, sev, msg, args));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void _write (Object msg, Object... args) throws IOException, InterruptedException
  {
    writeFormattedMsg (getEventFormatter ().format (msg, args));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSet (OpLevel sev)
  {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getSinkHandle ()
  {
    return outSink;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isOpen ()
  {
    return outSink != null && outSink.isOpen ();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void open () throws IOException
  {
    if (outSink != null)
    {
      outSink.open ();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close () throws IOException
  {
    try
    {
      if (outSink != null)
      {
        outSink.close ();
      }
    }
    finally
    {
      outSink = null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public KeyValueStats getStats (Map<String, Object> stats)
  {
    if (outSink instanceof EventSink)
    {
      ((KeyValueStats) outSink).getStats (stats);
    }
    return super.getStats (stats);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resetStats ()
  {
    super.resetStats ();
    if (outSink instanceof EventSink)
    {
      ((KeyValueStats) outSink).resetStats ();
    }
  }
}
