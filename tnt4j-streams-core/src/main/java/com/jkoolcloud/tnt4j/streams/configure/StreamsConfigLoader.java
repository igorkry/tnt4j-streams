/*
 * Copyright 2014-2017 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jkoolcloud.tnt4j.streams.configure;

import java.io.*;
import java.util.Collection;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import com.jkoolcloud.tnt4j.streams.configure.sax.StreamsConfigSAXParser;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * This class will load the specified stream configuration.
 *
 * @version $Revision: 2 $
 */
public class StreamsConfigLoader {

	/**
	 * Name of default streams configuration file referring system property name ({@value})
	 */
	public static final String STREAMS_CONFIG_KEY = "tnt4j.streams.config"; // NON-NLS
	/**
	 * Name of default configuration file name ({@value})
	 */
	public static final String DFLT_CFG_FILE_NAME = "tnt-data-source.xml"; // NON-NLS

	private static final String DFLT_CONFIG_PATH = "./../config"; // NON-NLS
	private static final String DFLT_CONFIG_PATH2 = "./config"; // NON-NLS

	private static final String DFLT_CONFIG_FILE_PATH = DFLT_CONFIG_PATH + File.separator + DFLT_CFG_FILE_NAME;
	private static final String DFLT_CONFIG_FILE_PATH2 = DFLT_CONFIG_PATH2 + File.separator + DFLT_CFG_FILE_NAME;

	private StreamsConfigData streamsCfgData;

	/**
	 * Constructs a new TNT4J-Streams Configuration loader, using the default configuration file
	 * ({@value #DFLT_CFG_FILE_NAME}), which is assumed to be in the classpath.
	 *
	 * @throws SAXException
	 *             if there was an error parsing the file
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the file
	 */
	public StreamsConfigLoader() throws SAXException, ParserConfigurationException, IOException {
		InputStream config;
		String cfgPath = System.getProperty(STREAMS_CONFIG_KEY);
		if (StringUtils.isNotEmpty(cfgPath)) {
			config = openCfgFile(cfgPath);

			if (config == null) {
				throw new FileNotFoundException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsConfig.file.not.found", cfgPath));
			}
		} else {
			config = openCfgFile(DFLT_CONFIG_FILE_PATH);
			if (config == null) {
				config = openCfgFile(DFLT_CONFIG_FILE_PATH2);
			}

			if (config == null) {
				throw new FileNotFoundException(
						StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
								"StreamsConfig.files.not.found", DFLT_CONFIG_FILE_PATH, DFLT_CONFIG_FILE_PATH2));
			}
		}

		load(config);
	}

	private static InputStream openCfgFile(String path) {
		InputStream config = null;
		try {
			config = new FileInputStream(path);
		} catch (FileNotFoundException e) {
		}
		// if could not locate file on file system, try classpath
		if (config == null) {
			config = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		}

		return config;
	}

	/**
	 * Returns configuration file defined by one of default paths: {@code "./../config"} or {@code "./config"}.
	 *
	 * @return existing configuration file, or {@code null} if no configuration file found using default paths
	 */
	public static File getDefaultFile() {
		return getDefaultFile(DFLT_CONFIG_FILE_PATH, DFLT_CONFIG_FILE_PATH2);
	}

	private static File getDefaultFile(String... paths) {
		if (paths != null) {
			for (String path : paths) {
				File f = new File(path);
				if (f.exists()) {
					return f;
				}
			}
		}

		return null;
	}

	/**
	 * Constructs a new TNT4J-Streams Configuration loader for the file with the specified file name.
	 *
	 * @param configFileName
	 *            name of configuration file
	 * @throws SAXException
	 *             if there was an error parsing the file
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the file
	 */
	public StreamsConfigLoader(String configFileName) throws SAXException, ParserConfigurationException, IOException {
		load(new FileInputStream(configFileName));
	}

	/**
	 * Constructs a new TNT4J-Streams Configuration loader for the specified {@link java.io.File}.
	 *
	 * @param configFile
	 *            configuration file
	 * @throws SAXException
	 *             if there was an error parsing the file
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the file
	 */
	public StreamsConfigLoader(File configFile) throws SAXException, ParserConfigurationException, IOException {
		load(new FileInputStream(configFile));
	}

	/**
	 * Constructs a new TNT4J-Streams Configuration loader, using the specified {@link java.io.Reader} to obtain the
	 * configuration data.
	 *
	 * @param configReader
	 *            reader to get configuration data from
	 * @throws SAXException
	 *             if there was an error parsing the configuration
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the configuration data
	 */
	public StreamsConfigLoader(Reader configReader) throws SAXException, ParserConfigurationException, IOException {
		load(new ReaderInputStream(configReader, Utils.UTF8));
	}

	/**
	 * Constructs a new TNT4J-Streams Configuration loader, using the specified {@link java.io.InputStream} to obtain
	 * the configuration data.
	 * 
	 * @param is
	 *            input stream to get configuration data from
	 * @throws SAXException
	 *             if there was an error parsing the configuration
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the configuration data
	 */
	public StreamsConfigLoader(InputStream is) throws SAXException, ParserConfigurationException, IOException {
		load(is);
	}

	/**
	 * Loads the configuration XML using SAX parser.
	 * <p>
	 * Configuration XML validation against XSD schema is performed if system property
	 * {@code com.jkoolcloud.tnt4j.streams.validate.config} is set to {@code true}.
	 *
	 * @param config
	 *            input stream to get configuration data from
	 * @throws SAXException
	 *             if there was an error parsing the configuration
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws IOException
	 *             if there is an error reading the configuration data
	 * @see StreamsConfigSAXParser#parse(InputStream, boolean)
	 */
	protected void load(InputStream config) throws SAXException, ParserConfigurationException, IOException {
		boolean validate = Boolean.getBoolean("com.jkoolcloud.tnt4j.streams.validate.config");
		try {
			streamsCfgData = StreamsConfigSAXParser.parse(config, validate);
		} finally {
			Utils.close(config);
		}
	}

	/**
	 * Returns the stream with the specified name.
	 *
	 * @param streamName
	 *            name of stream, as specified in configuration file
	 * @return stream with specified name, or {@code null} if no such stream
	 */
	public TNTInputStream<?, ?> getStream(String streamName) {
		return streamsCfgData == null ? null : streamsCfgData.getStream(streamName);
	}

	/**
	 * Returns the set of streams found in the configuration.
	 *
	 * @return set of streams found
	 */
	public Collection<TNTInputStream<?, ?>> getStreams() {
		return streamsCfgData == null ? null : streamsCfgData.getStreams();
	}

	/**
	 * Returns the set of parsers found in the configuration.
	 *
	 * @return set of parsers found
	 */
	public Collection<ActivityParser> getParsers() {
		return streamsCfgData == null ? null : streamsCfgData.getParsers();
	}

	/**
	 * Returns the parser with the specified name.
	 *
	 * @param parserName
	 *            name of parser, as specified in configuration file
	 * @return parser with specified name, or {@code null} if no such parser
	 */
	public ActivityParser getParser(String parserName) {
		return streamsCfgData == null ? null : streamsCfgData.getParser(parserName);
	}
}
