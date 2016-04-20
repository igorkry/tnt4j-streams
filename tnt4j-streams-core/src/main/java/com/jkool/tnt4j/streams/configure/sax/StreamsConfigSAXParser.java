/*
 * Copyright 2014-2016 JKOOL, LLC.
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

package com.jkool.tnt4j.streams.configure.sax;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.jkool.tnt4j.streams.configure.StreamsConfigData;
import com.jkool.tnt4j.streams.utils.Utils;

/**
 * Utility class dedicated to load TNT4J-Streams configuration using SAX-based
 * parser.
 *
 * @version $Revision: 1 $
 */
public final class StreamsConfigSAXParser {

	private static final String HANDLER_PROP_KEY = "tnt4j.streams.config.sax.handler";

	private StreamsConfigSAXParser() {
	}

	/**
	 * Reads the configuration and invokes the (SAX-based) parser to parse the
	 * configuration file contents.
	 *
	 * @param config
	 *            Reader to get configuration data from
	 * @return streams configuration data
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws SAXException
	 *             if there was an error parsing the configuration
	 * @throws IOException
	 *             if there is an error reading the configuration data
	 */
	public static StreamsConfigData parse(Reader config)
			throws ParserConfigurationException, SAXException, IOException {
		Properties p = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		InputStream ins = loader.getResourceAsStream("sax.properties"); // NON-NLS
		p.load(ins);
		Utils.close(ins);

		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser = parserFactory.newSAXParser();
		ConfigParserHandler hndlr = null;
		try {
			String handlerClassName = p.getProperty(HANDLER_PROP_KEY, ConfigParserHandler.class.getName());
			hndlr = (ConfigParserHandler) Utils.createInstance(handlerClassName);
		} catch (Exception exc) {
		}

		if (hndlr == null) {
			hndlr = new ConfigParserHandler();
		}

		parser.parse(new InputSource(config), hndlr);

		return hndlr.getStreamsConfigData();
	}
}
