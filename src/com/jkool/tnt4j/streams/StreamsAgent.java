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

import java.util.HashMap;
import java.util.Map;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.inputs.ActivityFeeder;
import com.jkool.tnt4j.streams.inputs.FeederThread;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Main class for jKool LLC TNT4J-Streams application.
 *
 * @version $Revision: 4 $
 */
public final class StreamsAgent
{
  private static final Logger logger = Logger.getLogger (StreamsAgent.class);
  private static Level logLevel = Level.INFO;
  private static String cfgFileName = null;

  /**
   * @param args command-line arguments. Supported arguments:
   *             <table>
   *             <tr><td>&nbsp;&nbsp;</td>
   *             <td>&nbsp;-f:&lt;cfg_file_name&gt;</td>
   *             <td>(optional) Load direct feed probe configuration from &lt;cfg_file_name&gt;</td>
   *             </tr>
   *             <tr><td>&nbsp;&nbsp;</td>
   *             <td>&nbsp;-h | -?</td>
   *             <td>(optional) Print usage</td>
   *             </tr>
   *             <tr><td>&nbsp;&nbsp;</td>
   *             <td>&nbsp;-d</td>
   *             <td>(optional) Enable debug-level logging</td>
   *             </tr>
   *             <tr><td>&nbsp;&nbsp;</td>
   *             <td>&nbsp;-t</td>
   *             <td>(optional) Enable trace-level logging</td>
   *             </tr>
   *             </table>
   */
  public static void main (String[] args)
  {
    logger.info ("jKool TNT4J Streams Probe starting ...");
    ThreadGroup feederThreads = new ThreadGroup (StreamsAgent.class.getName () + "Threads");
    try
    {
      processArgs (args);
      setLogLevel (logLevel);
      StreamsConfig cfg = (StringUtils.isEmpty (cfgFileName) ? new StreamsConfig () : new StreamsConfig (cfgFileName));
      HashMap<String, ActivityFeeder> feederMap = cfg.getFeeders ();
      if (feederMap == null || feederMap.isEmpty ())
      { throw new IllegalStateException ("No Activity Feeders found in configuration"); }
      for (Map.Entry<String, ActivityFeeder> f : feederMap.entrySet ())
      {
        String feederName = f.getKey ();
        ActivityFeeder feeder = f.getValue ();
        FeederThread ft = new FeederThread (feederThreads, feeder, feeder.getClass ().getSimpleName () + ":" + feederName);
        ft.start ();
      }
    }
    catch (Throwable t)
    {
      logger.error (t.getMessage (), t);
    }
  }

  /**
   * Set log4j logging level for use by TNT4J-Streams classes.
   * Useful for overriding default log level to enable troubleshooting.
   *
   * @param logLevel log4j logging level to use
   */
  public static void setLogLevel (Level logLevel)
  {
    Logger probeLogger = Logger.getLogger ("com.jkool.tnt4j.streams");
    if (probeLogger != null)
    {
      if (logLevel == null)
      { logLevel = Level.INFO; }
      if (probeLogger.getLevel () != logLevel)
      {
        logger.info ("Changing logging level to " + logLevel);
        probeLogger.setLevel (logLevel);
      }
      else
      {
        logger.debug ("Using logging level of " + logLevel);
      }
      logger.setLevel (logLevel);
      StreamsAgent.logLevel = logLevel;
    }
  }

  /**
   * Gets the current log4j logging level.
   *
   * @return current log4j logging level
   */
  public static Level getLogLevel ()
  {
    return logLevel;
  }

  private static void processArgs (String[] args)
  {
    for (int i = 0; i < args.length; i++)
    {
      String arg = args[i];
      if (arg == null)
      { continue; }
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
      else if (arg.equals ("-d"))
      {
        if (logLevel.isGreaterOrEqual (Level.DEBUG))
        { logLevel = Level.DEBUG; }
      }
      else if (arg.equals ("-t"))
      {
        if (logLevel.isGreaterOrEqual (Level.TRACE))
        { logLevel = Level.TRACE; }
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

  private static void printUsage ()
  {
    System.out.println ("\nValid arguments:\n");
    System.out.println ("    [-f:<cfg_file_name>] [-h|-?]");
    System.out.println ("where:");
    System.out.println ("    -f      -  Load direct feed probe configuration from <cfg_file_name>");
    System.out.println ("    -d      -  Enable debug-level logging");
    System.out.println ("    -t      -  Enable trace-level logging");
    System.out.println ("    -h, -?  -  Print usage");
  }
}
