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

package com.jkool.tnt4j.streams;

import java.util.Map;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.inputs.StreamThread;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;
import org.apache.commons.lang.StringUtils;

/**
 * Main class for jKool LLC TNT4J-Streams standalone application.
 *
 * @version $Revision: 4 $
 */
public final class StreamsAgent
{
  private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink (StreamsAgent.class);
  private static String cfgFileName = null;

  private StreamsAgent ()
  {
  }

  /**
   * Main entry point for running as a standalone application.
   *
   * @param args command-line arguments. Supported arguments:
   *             <table summary="TNT4J-Streams agent command line arguments">
   *             <tr><td>&nbsp;&nbsp;</td>
   *             <td>&nbsp;-f:&lt;cfg_file_name&gt;</td>
   *             <td>(optional) Load TNT4J Streams data source configuration from &lt;cfg_file_name&gt;</td>
   *             </tr>
   *             <tr><td>&nbsp;&nbsp;</td>
   *             <td>&nbsp;-h | -?</td>
   *             <td>(optional) Print usage</td>
   *             </tr>
   *             </table>
   */
  public static void main (String... args)
  {
    LOGGER.log (OpLevel.INFO, "jKool TNT4J Streams session starting ...");
    ThreadGroup streamThreads = new ThreadGroup (StreamsAgent.class.getName () + "Threads");
    try
    {
      processArgs (args);
      StreamsConfig cfg = StringUtils.isEmpty (cfgFileName) ? new StreamsConfig () : new StreamsConfig (cfgFileName);
      Map<String, TNTInputStream> streamsMap = cfg.getStreams ();
      if (streamsMap == null || streamsMap.isEmpty ())
      {
        throw new IllegalStateException ("No Activity Streams found in configuration");
      }
      StreamThread ft;
      for (Map.Entry<String, TNTInputStream> streamEntry : streamsMap.entrySet ())
      {
        String streamName = streamEntry.getKey ();
        TNTInputStream stream = streamEntry.getValue ();
        ft = new StreamThread (streamThreads, stream, stream.getClass ().getSimpleName () + ":" + streamName);
        ft.start ();
      }
    }
    catch (Throwable t)
    {
      LOGGER.log (OpLevel.ERROR, t.getMessage (), t);
    }
  }

  /**
   * Process and interprets command-line arguments.
   *
   * @param args command-line arguments.
   */
  private static void processArgs (String... args)
  {
    for (String arg : args)
    {
      if (StringUtils.isEmpty (arg))
      {
        continue;
      }
      if (arg.startsWith ("-f:"))
      {
        cfgFileName = arg.substring (3);
        if (StringUtils.isEmpty (cfgFileName))
        {
          System.out.println ("Missing <cfg_file_name> for '-f' argument");
          printUsage ();
          System.exit (1);
        }
      }
      else if (arg.equals ("-h") || arg.equals ("-?"))
      {
        printUsage ();
        System.exit (1);
      }
      else
      {
        System.out.println ("Invalid argument: " + arg);
        printUsage ();
        System.exit (1);
      }
    }
  }

  /**
   * Prints short standalone application usage manual.
   */
  private static void printUsage ()
  {
    System.out.println ("\nValid arguments:\n");
    System.out.println ("    [-f:<cfg_file_name>] [-h|-?]");
    System.out.println ("where:");
    System.out.println ("    -f      -  Load TNT4J Streams data source configuration from <cfg_file_name>");
    System.out.println ("    -h, -?  -  Print usage");
  }
}
