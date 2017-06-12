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

package com.jkoolcloud.tnt4j.streams.configure.sax;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigData;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Utility class dedicated to load TNT4J-Streams configuration using SAX-based parser.
 *
 * @version $Revision: 2 $
 */
public final class StreamsConfigSAXParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(StreamsConfigSAXParser.class);

	private static final String HANDLER_PROP_KEY = "tnt4j.streams.config.sax.handler";

	private StreamsConfigSAXParser() {
	}

	/**
	 * Reads the configuration and invokes the (SAX-based) parser to parse the configuration file contents.
	 *
	 * @param config
	 *            input stream to get configuration data from
	 * @param validate
	 *            flag indicating whether to validate configuration XML against XSD schema
	 * @return streams configuration data
	 * @throws ParserConfigurationException
	 *             if there is an inconsistency in the configuration
	 * @throws SAXException
	 *             if there was an error parsing the configuration
	 * @throws IOException
	 *             if there is an error reading the configuration data
	 */
	public static StreamsConfigData parse(InputStream config, boolean validate)
			throws ParserConfigurationException, SAXException, IOException {
		if (validate) {
			config = config.markSupported() ? config : new ByteArrayInputStream(IOUtils.toByteArray(config));

			Map<OpLevel, List<SAXParseException>> validationErrors = validate(config);

			if (MapUtils.isNotEmpty(validationErrors)) {
				for (Map.Entry<OpLevel, List<SAXParseException>> vee : validationErrors.entrySet()) {
					for (SAXParseException ve : vee.getValue()) {
						LOGGER.log(OpLevel.WARNING,
								StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
										"StreamsConfigSAXParser.xml.validation.error"),
								ve.getLineNumber(), ve.getColumnNumber(), vee.getKey(), ve.getLocalizedMessage());
					}
				}
			}
		}

		Properties p = Utils.loadPropertiesResource("sax.properties"); // NON-NLS

		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		SAXParser parser = parserFactory.newSAXParser();
		ConfigParserHandler hndlr = null;
		try {
			String handlerClassName = p.getProperty(HANDLER_PROP_KEY, ConfigParserHandler.class.getName());
			if (StringUtils.isNotEmpty(handlerClassName)) {
				hndlr = (ConfigParserHandler) Utils.createInstance(handlerClassName);
			}
		} catch (Exception exc) {
		}

		if (hndlr == null) {
			hndlr = new ConfigParserHandler();
		}

		parser.parse(config, hndlr);

		return hndlr.getStreamsConfigData();
	}

	/**
	 * Validates configuration XML against XML defined XSD schema.
	 *
	 * @param config
	 *            {@link InputStream} to get configuration data from
	 * @return map of found validation errors
	 * @throws SAXException
	 *             if there was an error parsing the configuration
	 * @throws IOException
	 *             if there is an error reading the configuration data
	 */
	public static Map<OpLevel, List<SAXParseException>> validate(InputStream config) throws SAXException, IOException {
		final Map<OpLevel, List<SAXParseException>> validationErrors = new HashMap<>();
		try {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			Schema schema = factory.newSchema();
			Validator validator = schema.newValidator();
			validator.setErrorHandler(new ErrorHandler() {
				@Override
				public void warning(SAXParseException exception) throws SAXException {
					handleValidationError(OpLevel.WARNING, exception);
				}

				@Override
				public void error(SAXParseException exception) throws SAXException {
					handleValidationError(OpLevel.ERROR, exception);
				}

				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
					handleValidationError(OpLevel.FATAL, exception);
				}

				private void handleValidationError(OpLevel level, SAXParseException exception) {
					List<SAXParseException> lErrorsList = validationErrors.get(level);
					if (lErrorsList == null) {
						lErrorsList = new ArrayList<>();
						validationErrors.put(level, lErrorsList);
					}

					lErrorsList.add(exception);
				}
			});
			validator.validate(new StreamSource(config));
		} finally {
			if (config.markSupported()) {
				config.reset();
			}
		}

		return validationErrors;
	}
}
