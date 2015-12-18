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
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * This class will load the specified stream configuration.
 *
 * @version $Revision: 5 $
 */
public class StreamsConfig {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(StreamsConfig.class);
	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_DATETIME = "DateTime"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_PATTERN = "Pattern"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_FLD_DELIM = "FieldDelim"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_VAL_DELIM = "ValueDelim"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_STRIP_QUOTES = "StripQuotes"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_FILENAME = "FileName"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_HOST = "Host"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_PORT = "Port"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_SIG_DELIM = "SignatureDelim"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_REQUIRE_ALL = "RequireDefault"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_QMGR_NAME = "QueueManager"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_QUEUE_NAME = "Queue"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_TOPIC_NAME = "Topic"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_SUB_NAME = "Subscription"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_TOPIC_STRING = "TopicString"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_CHANNEL_NAME = "Channel"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_STRIP_HEADERS = "StripHeaders"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_NAMESPACE = "Namespace"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_START_FROM_LATEST = "StartFromLatest"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_FILE_READ_DELAY = "FileReadDelay"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_HALT_ON_PARSER = "HaltIfNoParser"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_USE_EXECUTOR_SERVICE = "UseExecutors"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_EXECUTOR_THREADS_QTY = "ExecutorThreadsQuantity"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_EXECUTOR_REJECTED_TASK_OFFER_TIMEOUT = "ExecutorRejectedTaskOfferTimeout"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_EXECUTORS_TERMINATION_TIMEOUT = "ExecutorsTerminationTimeout"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_EXECUTORS_BOUNDED = "ExecutorsBoundedModel"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_KEYSTORE = "Keystore"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_KEYSTORE_PASS = "KeystorePass"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_KEY_PASS = "KeyPass"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_JNDI_FACTORY = "JNDIFactory"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_JMS_FACTORY = "JMSFactory"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_SERVER_URI = "ServerURI"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_USERNAME = "UserName"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_PASSWORD = "Password"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_USE_SSL = "UseSSL"; // NON-NLS

	public static final String PROP_READ_LINES = "ReadLines"; // NON-NLS

	/**
	 * Name of default configuration file name ({@value})
	 */
	public static final String DFLT_CFG_FILE_NAME = "tnt-data-source.xml"; // NON-NLS

	private static final String DFLT_CONFIG_FILE_PATH = "config" + File.separator + DFLT_CFG_FILE_NAME; // NON-NLS

	private final SAXParserFactory parserFactory = SAXParserFactory.newInstance();

	private Map<String, ActivityParser> parsers = null;
	private Map<String, TNTInputStream> streams = null;

	/**
	 * Creates a new TNT4J-Streams Configuration loader, using the default
	 * configuration file ({@value #DFLT_CFG_FILE_NAME}), which is assumed to be
	 * in the classpath.
	 *
	 * @throws SAXException
	 *             if there was an error parsing the file
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the file
	 */
	public StreamsConfig() throws SAXException, ParserConfigurationException, IOException {
		InputStream config = null;
		try {
			config = new FileInputStream(DFLT_CONFIG_FILE_PATH);
		} catch (FileNotFoundException e) {
		}
		// if could not locate file on file system, try classpath
		if (config == null) {
			config = Thread.currentThread().getContextClassLoader().getResourceAsStream(DFLT_CONFIG_FILE_PATH);
		}
		if (config == null) {
			throw new FileNotFoundException(
					StreamsResources.getStringFormatted("StreamsConfig.file.not.found", DFLT_CONFIG_FILE_PATH));
		}
		load(new InputStreamReader(config));
	}

	/**
	 * Creates a new TNT4J-Streams Configuration loader for the file with the
	 * specified file name.
	 *
	 * @param configFileName
	 *            name of configuration file
	 *
	 * @throws SAXException
	 *             if there was an error parsing the file
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the file
	 */
	public StreamsConfig(String configFileName) throws SAXException, ParserConfigurationException, IOException {
		load(new FileReader(configFileName));
	}

	/**
	 * Creates a new TNT4J-Streams Configuration loader for the specified file.
	 *
	 * @param configFile
	 *            configuration file
	 *
	 * @throws SAXException
	 *             if there was an error parsing the file
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the file
	 */
	public StreamsConfig(File configFile) throws SAXException, ParserConfigurationException, IOException {
		load(new FileReader(configFile));
	}

	/**
	 * Creates a new TNT4J-Streams Configuration loader, using the specified
	 * Reader to obtain the configuration data.
	 *
	 * @param configReader
	 *            Reader to get configuration data from
	 *
	 * @throws SAXException
	 *             if there was an error parsing the configuration
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the configuration data
	 */
	public StreamsConfig(Reader configReader) throws SAXException, ParserConfigurationException, IOException {
		load(configReader);
	}

	/**
	 * Loads the configuration and invokes the (SAX-based) parser to parse the
	 * configuration file.
	 *
	 * @param config
	 *            Reader to get configuration data from
	 *
	 * @throws SAXException
	 *             if there was an error parsing the configuration
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the configuration data
	 */
	protected void load(Reader config) throws SAXException, ParserConfigurationException, IOException {
		SAXParser parser = parserFactory.newSAXParser();
		ConfigParserHandler hndlr = new ConfigParserHandler();
		parser.parse(new InputSource(config), hndlr);
		streams = hndlr.getStreams();
		parsers = hndlr.getParsers();

		Utils.close(config);
	}

	/**
	 * Returns the stream with the specified name.
	 *
	 * @param streamName
	 *            name of stream, as specified in configuration file
	 *
	 * @return stream with specified name, or {@code null} if no such stream
	 */
	public TNTInputStream getStream(String streamName) {
		return streams == null ? null : streams.get(streamName);
	}

	/**
	 * Returns the set of streams found in the configuration.
	 *
	 * @return set of streams found
	 */
	public Map<String, TNTInputStream> getStreams() {
		return streams;
	}

	/**
	 * Returns the set of parsers found in the configuration.
	 *
	 * @return set of parsers found
	 */
	public Map<String, ActivityParser> getParsers() {
		return parsers;
	}

	/**
	 * Returns the parser with the specified name.
	 *
	 * @param parserName
	 *            name of parser, as specified in configuration file
	 *
	 * @return parser with specified name, or {@code null} if no such parser
	 */
	public ActivityParser getParser(String parserName) {
		return parsers == null ? null : parsers.get(parserName);
	}
}
