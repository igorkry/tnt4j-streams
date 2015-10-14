package com.jkool.tnt4j.streams.tnt4j;

import java.io.IOException;
import java.util.Map;

import com.jkool.jesl.tnt4j.sink.JKCloudEventSink;
import com.nastel.jkool.tnt4j.core.KeyValueStats;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.core.Snapshot;
import com.nastel.jkool.tnt4j.format.EventFormatter;
import com.nastel.jkool.tnt4j.sink.AbstractEventSink;
import com.nastel.jkool.tnt4j.sink.EventSink;
import com.nastel.jkool.tnt4j.sink.FileSink;
import com.nastel.jkool.tnt4j.sink.Sink;
import com.nastel.jkool.tnt4j.source.Source;
import com.nastel.jkool.tnt4j.tracker.TrackingActivity;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;

/**
 * @author akausinis
 * @version 1.0
 * @created 2015-10-08 11:11
 */
public class StreamsEventSink extends AbstractEventSink
{
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
    return (outSink != null && outSink.isOpen ());
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
      ((EventSink) outSink).getStats (stats);
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
      ((EventSink) outSink).resetStats ();
    }
  }
}
