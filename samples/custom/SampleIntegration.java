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

import java.util.Map;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.inputs.StreamThread;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Sample integration of TNT4J-Streams into an application.
 *
 * @version $Revision: 2 $
 */
public final class SampleIntegration
{
  private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink (SampleIntegration.class);

  /**
   * Configure streams and parsers, and run each stream in its own thread.
   *
   * @param cfgFileName configuration file name
   */
  public static void loadConfigAndRun (String cfgFileName)
  {
    ThreadGroup streamThreads = new ThreadGroup ("Streams");
    try
    {
      StreamsConfig cfg;
      if (cfgFileName == null || cfgFileName.length () == 0)
      {
        cfg = new StreamsConfig ();
      }
      else
      {
        cfg = new StreamsConfig (cfgFileName);
      }
      Map<String, TNTInputStream> streamsMap = cfg.getStreams ();
      if (streamsMap == null || streamsMap.size () == 0)
      {
        throw new IllegalStateException ("No Activity Streams found in configuration");
      }
      for (Map.Entry<String, TNTInputStream> f : streamsMap.entrySet ())
      {
        String streamName = f.getKey ();
        TNTInputStream stream = f.getValue ();
        StreamThread ft = new StreamThread (streamThreads, stream, streamName);
        ft.start ();
      }
    }
    catch (Throwable t)
    {
      LOGGER.log (OpLevel.ERROR, t.getMessage (), t);
    }
  }

  /**
   * The following can be used if using the default configuration file
   * with a single stream.
   *
   * @param cfgFileName configuration file name
   */
  public static void simpleConfigAndRun (String cfgFileName)
  {
    try
    {
      StreamsConfig cfg = new StreamsConfig ();
      TNTInputStream stream = cfg.getStream ("StreamName");
      StreamThread ft = new StreamThread (stream);
      ft.start ();
    }
    catch (Throwable t)
    {
      LOGGER.log (OpLevel.ERROR, t.getMessage (), t);
    }
  }
}
