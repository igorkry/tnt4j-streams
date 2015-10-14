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

package com.jkool.tnt4j.streams.configure;

import java.io.*;
import java.util.HashMap;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.jkool.tnt4j.streams.inputs.ActivityFeeder;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class will load the specified Feed configuration.
 *
 * @version $Revision: 5 $
 */
public class StreamsConfig
{
  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_DATETIME = "DateTime";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_PATTERN = "Pattern";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_FLD_DELIM = "FieldDelim";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_VAL_DELIM = "ValueDelim";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_STRIP_QUOTES = "StripQuotes";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_FILENAME = "FileName";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_HOST = "Host";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_PORT = "Port";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_SIG_DELIM = "SignatureDelim";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_REQUIRE_ALL = "RequireDefault";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_QMGR_NAME = "QueueManager";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_QUEUE_NAME = "Queue";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_TOPIC_NAME = "Topic";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_SUB_NAME = "Subscription";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_TOPIC_STRING = "TopicString";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_CHANNEL_NAME = "Channel";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_STRIP_HEADERS = "StripHeaders";

  /**
   * Constant for name of built-in {@value} property.
   */
  public static final String PROP_NAMESPACE = "Namespace";

  /**
   * Name of default configuration file name ({@value})
   */
  public static final String DFLT_CFG_FILE_NAME = "tw-direct-feed-probe.xml";
  //TODO: maybe change to something like "tnt4j-streams-probe"???

  private static final String DFLT_CONFIG_FILE_PATH = "config" + File.separator + DFLT_CFG_FILE_NAME;

  private SAXParserFactory parserFactory = SAXParserFactory.newInstance ();

  private HashMap<String, ActivityParser> parsers = null;
  private HashMap<String, ActivityFeeder> feeders = null;

  /**
   * Creates a new TNT4J-Streams Configuration loader, using the default
   * configuration file ({@value #DFLT_CFG_FILE_NAME}), which is assumed to be
   * in the classpath.
   *
   * @throws SAXException                 if there was an error parsing the file
   * @throws FileNotFoundException        if the file cannot be located
   * @throws ParserConfigurationException if there is an inconsistency in the configuration
   * @throws IOException                  if there is an error reading the file
   */
  public StreamsConfig () throws SAXException, FileNotFoundException, ParserConfigurationException, IOException
  {
    InputStream config = null;
    try {config = new FileInputStream (DFLT_CONFIG_FILE_PATH);} catch (FileNotFoundException e) {}
    // if could not locate file on file system, try classpath
    if (config == null)
    { config = Thread.currentThread ().getContextClassLoader ().getResourceAsStream (DFLT_CONFIG_FILE_PATH); }
    if (config == null)
    { throw new FileNotFoundException ("Could not find configuration file '" + DFLT_CONFIG_FILE_PATH + "'"); }
    load (new InputStreamReader (config));
  }

  /**
   * Creates a new TNT4J-Streams Configuration loader for the file with the specified file name.
   *
   * @param configFileName name of configuration file
   *
   * @throws SAXException                 if there was an error parsing the file
   * @throws FileNotFoundException        if the file cannot be located
   * @throws ParserConfigurationException if there is an inconsistency in the configuration
   * @throws IOException                  if there is an error reading the file
   */
  public StreamsConfig (String configFileName) throws SAXException, FileNotFoundException, ParserConfigurationException, IOException
  {
    load (new FileReader (configFileName));
  }

  /**
   * Creates a new TNT4J-Streams Configuration loader for the specified file.
   *
   * @param configFile configuration file
   *
   * @throws SAXException                 if there was an error parsing the file
   * @throws FileNotFoundException        if the file cannot be located
   * @throws ParserConfigurationException if there is an inconsistency in the configuration
   * @throws IOException                  if there is an error reading the file
   */
  public StreamsConfig (File configFile) throws SAXException, FileNotFoundException, ParserConfigurationException, IOException
  {
    load (new FileReader (configFile));
  }

  /**
   * Creates a new TNT4J-Streams Configuration loader, using the specified Reader to obtain the configuration data.
   *
   * @param configReader Reader to get configuration data from
   *
   * @throws SAXException                 if there was an error parsing the configuration
   * @throws ParserConfigurationException if there is an inconsistency in the configuration
   * @throws IOException                  if there is an error reading the configuration data
   */
  public StreamsConfig (Reader configReader) throws SAXException, ParserConfigurationException, IOException
  {
    load (configReader);
  }

  /**
   * Loads the configuration and invokes the (SAX-based) parser to parse the configuration file.
   *
   * @param config Reader to get configuration data from
   *
   * @throws SAXException                 if there was an error parsing the configuration
   * @throws ParserConfigurationException if there is an inconsistency in the configuration
   * @throws IOException                  if there is an error reading the configuration data
   */
  protected void load (Reader config) throws SAXException, ParserConfigurationException, IOException
  {
    SAXParser parser = parserFactory.newSAXParser ();
    ConfigParserHandler hndlr = new ConfigParserHandler ();
    parser.parse (new InputSource (config), hndlr);
    feeders = hndlr.getFeeders ();
    parsers = hndlr.getParsers ();
  }

  /**
   * Returns the feeder with the specified name.
   *
   * @param feederName name of feeder, as specified in configuration file
   *
   * @return feeder with specified name, or {@code null} if no such feeder
   */
  public ActivityFeeder getFeeder (String feederName)
  {
    return (feeders == null ? null : feeders.get (feederName));
  }

  /**
   * Returns the set of feeders found in the configuration.
   *
   * @return set of feeders found
   */
  public HashMap<String, ActivityFeeder> getFeeders ()
  {
    return feeders;
  }

  /**
   * Returns the set of parsers found in the configuration.
   *
   * @return set of parsers found
   */
  public HashMap<String, ActivityParser> getParsers ()
  {
    return parsers;
  }

  /**
   * Returns the parser with the specified name.
   *
   * @param parserName name of parser, as specified in configuration file
   *
   * @return parser with specified name, or {@code null} if no such parser
   */
  public ActivityParser getParser (String parserName)
  {
    return (parsers == null ? null : parsers.get (parserName));
  }
}
