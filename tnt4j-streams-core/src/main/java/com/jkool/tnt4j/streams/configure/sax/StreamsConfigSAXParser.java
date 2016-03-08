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
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.jkool.tnt4j.streams.configure.StreamsConfigData;

/**
 * Utility class dedicated to load TNT4J-Streams configuration using SAX-based
 * parser.
 *
 * @version $Revision: 1 $
 */
public final class StreamsConfigSAXParser {

	private StreamsConfigSAXParser() {
	}

	/**
	 * Reads the configuration and invokes the (SAX-based) parser to parse the
	 * configuration file contents.
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
	public static StreamsConfigData parse(Reader config)
			throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser = parserFactory.newSAXParser();
		ConfigParserHandler hndlr = new ConfigParserHandler();
		parser.parse(new InputSource(config), hndlr);

		return hndlr.getStreamsConfigData();
	}
}
