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

package com.jkool.tnt4j.streams.parsers;

import java.io.*;
import java.text.ParseException;
import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.jkool.tnt4j.streams.configure.ConfigParserHandler;
import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.fields.*;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements an activity data parser that assumes each activity data item is an
 * XML string, with the value for each field being retrieved from a particular
 * XML element or attribute.
 * </p>
 * <p>
 * This parser supports reading the activity data from several types of input
 * sources, and supports input streams containing multiple XML documents. If
 * there are multiple XML documents, each document must start with
 * {@code "<?xml ...>"}, and be separated by a new line.
 * </p>
 * <p>
 * This parser supports the following properties:
 * <ul>
 * <li>Namespace</li>
 * </ul>
 *
 * @version $Revision: 7 $
 */
public class ActivityXmlParser extends ActivityParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityXmlParser.class);

	/**
	 * Contains the XML namespace mappings.
	 */
	protected NamespaceMap namespaces = null;

	private final XPath xPath;
	private final DocumentBuilder builder;
	private final StringBuilder xmlBuffer;

	/**
	 * Property indicating that all attributes are required by default .
	 */
	protected boolean requireAll = false;

	/**
	 * Creates a new activity XML string parser.
	 *
	 * @throws ParserConfigurationException
	 *             if any errors configuring the parser
	 */
	public ActivityXmlParser() throws ParserConfigurationException {
		super(LOGGER);

		DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
		domFactory.setNamespaceAware(true);
		builder = domFactory.newDocumentBuilder();
		XPathFactory xPathFactory = XPathFactory.newInstance();
		xPath = xPathFactory.newXPath();
		xmlBuffer = new StringBuilder(1024);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Throwable {
		if (props == null) {
			return;
		}
		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamsConfig.PROP_NAMESPACE.equalsIgnoreCase(name)) {
				if (!StringUtils.isEmpty(value)) {
					if (namespaces == null) {
						namespaces = new NamespaceMap();
						namespaces.addPrefixUriMapping(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
					}
					String[] nsFields = value.split("="); // NON-NLS
					namespaces.addPrefixUriMapping(nsFields[0], nsFields[1]);
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("ActivityXmlParser.adding.mapping"),
							name, value);
				}
			} else if (StreamsConfig.PROP_REQUIRE_ALL.equalsIgnoreCase(name)) {
				if (!StringUtils.isEmpty(value)) {
					requireAll = Boolean.parseBoolean(value);
					LOGGER.log(OpLevel.DEBUG,
							StreamsResources.getStringFormatted("ActivityParser.setting", name, value));
				}
			}
			LOGGER.log(OpLevel.TRACE, StreamsResources.getStringFormatted("ActivityParser.ignoring", name));
		}
		if (namespaces != null) {
			xPath.setNamespaceContext(namespaces);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This parser supports the following class types (and all classes
	 * extending/implementing any of these):
	 * </p>
	 * <ul>
	 * <li>{@code java.lang.String}</li>
	 * <li>{@code byte[]}</li>
	 * <li>{@code java.io.Reader}</li>
	 * <li>{@code java.io.InputStream}</li>
	 * <li>{@code org.w3c.dom.Document}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return String.class.isInstance(data) || byte[].class.isInstance(data) || Reader.class.isInstance(data)
				|| InputStream.class.isInstance(data) || Document.class.isInstance(data);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActivityInfo parse(TNTInputStream stream, Object data) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("ActivityParser.parsing", data));
		ActivityInfo ai = new ActivityInfo();
		ActivityField field = null;
		try {
			Document xmlDoc;
			if (data instanceof Document) {
				xmlDoc = (Document) data;
			} else {
				String xmlString = getNextXmlString(data);
				if (StringUtils.isEmpty(xmlString)) {
					return null;
				}
				xmlDoc = builder.parse(IOUtils.toInputStream(xmlString));
			}
			String[] savedFormats = null;
			String[] savedUnits = null;
			String[] savedLocales = null;
			// apply fields for parser
			Object value;
			for (Map.Entry<ActivityField, List<ActivityFieldLocator>> fieldEntry : fieldMap.entrySet()) {
				value = null;
				field = fieldEntry.getKey();
				List<ActivityFieldLocator> locations = fieldEntry.getValue();
				if (locations != null) {
					// need to save format and units specification from config
					// in case individual entry in activity data overrides it
					if (savedFormats == null || savedFormats.length < locations.size()) {
						savedFormats = new String[locations.size()];
						savedUnits = new String[locations.size()];
						savedLocales = new String[locations.size()];
					}
					if (locations.size() == 1) {
						ActivityFieldLocator loc = locations.get(0);
						savedFormats[0] = loc.getFormat();
						savedUnits[0] = loc.getUnits();
						savedLocales[0] = loc.getLocale();
						value = getLocatorValue(stream, loc, xmlDoc);
						if (value == null && requireAll && !"false".equalsIgnoreCase(loc.getRequired())) { // NON-NLS
							LOGGER.log(OpLevel.TRACE, StreamsResources
									.getStringFormatted("ActivityXmlParser.required.locator.not.found", field));
							return null;
						}
					} else {
						Object[] values = new Object[locations.size()];
						for (int li = 0; li < locations.size(); li++) {
							ActivityFieldLocator loc = locations.get(li);
							savedFormats[li] = loc.getFormat();
							savedUnits[li] = loc.getUnits();
							savedLocales[li] = loc.getLocale();
							values[li] = getLocatorValue(stream, loc, xmlDoc);
							if (values[li] == null && requireAll && !"false".equalsIgnoreCase(loc.getRequired())) { // NON-NLS
								LOGGER.log(OpLevel.TRACE, StreamsResources
										.getStringFormatted("ActivityXmlParser.required.locator.not.found", field));
								return null;
							}
						}
						value = values;
					}
				}
				applyFieldValue(stream, ai, field, value);
				if (locations != null && savedFormats != null) {
					for (int li = 0; li < locations.size(); li++) {
						ActivityFieldLocator loc = locations.get(li);
						loc.setFormat(savedFormats[li], savedLocales[li]);
						loc.setUnits(savedUnits[li]);
					}
				}
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(
					StreamsResources.getStringFormatted("ActivityParser.parsing.failed", field), 0);
			pe.initCause(e);
			throw pe;
		}
		return ai;
	}

	private Object getLocatorValue(TNTInputStream stream, ActivityFieldLocator locator, Document xmlDoc)
			throws XPathExpressionException, ParseException {
		Object val = null;
		if (locator != null) {
			String locStr = locator.getLocator();
			if (!StringUtils.isEmpty(locStr)) {
				if (locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp) {
					val = stream.getProperty(locStr);
				} else {
					// get value for locator (element)
					XPathExpression expr = xPath.compile(locStr);
					String strVal = (String) expr.evaluate(xmlDoc, XPathConstants.STRING);
					if (!StringUtils.isEmpty(strVal)) {
						// Get list of attributes and their values for current
						// element
						NodeList attrs = (NodeList) xPath.evaluate(locStr + "/@*", xmlDoc, XPathConstants.NODESET); // NON-NLS
						int length = attrs == null ? 0 : attrs.getLength();
						if (length > 0) {
							String format = null;
							String locale = null;
							boolean formatAttrSet = false;
							for (int i = 0; i < length; i++) {
								Attr attr = (Attr) attrs.item(i);
								String attrName = attr.getName();
								String attrValue = attr.getValue();
								if (ConfigParserHandler.DATA_TYPE_ATTR.equals(attrName)) {
									locator.setDataType(ActivityFieldDataType.valueOf(attrValue));
								} else if (ConfigParserHandler.FORMAT_ATTR.equals(attrName)) {
									format = attrValue;
									formatAttrSet = true;
								} else if (ConfigParserHandler.LOCALE_ATTR.equals(attrName)) {
									locale = attrValue;
								} else if (ConfigParserHandler.UNITS_ATTR.equals(attrName)) {
									locator.setUnits(attrValue);
								}
							}
							if (formatAttrSet) {
								locator.setFormat(format, locale);
							}
						}
						val = strVal.trim();
					}
				}
			}
			val = locator.formatValue(val);
		}
		return val;
	}

	/**
	 * Reads the next complete XML document string from the specified data input
	 * source and returns it as a string. If the data input source contains
	 * multiple XML documents, then each document must start with "&lt;?xml",
	 * and be separated by a new line.
	 *
	 * @param data
	 *            input source for activity data
	 *
	 * @return XML document string, or {@code null} if end of input source has
	 *         been reached
	 *
	 * @throws IllegalArgumentException
	 *             if the class of input source supplied is not supported.
	 */
	protected String getNextXmlString(Object data) {
		if (data == null) {
			return null;
		}
		if (data instanceof String) {
			return (String) data;
		} else if (data instanceof byte[]) {
			return Utils.getString((byte[]) data);
		}
		BufferedReader rdr;
		if (data instanceof BufferedReader) {
			rdr = (BufferedReader) data;
		} else if (data instanceof Reader) {
			rdr = new BufferedReader((Reader) data);
		} else if (data instanceof InputStream) {
			rdr = new BufferedReader(new InputStreamReader((InputStream) data));
		} else {
			throw new IllegalArgumentException(
					StreamsResources.getStringFormatted("ActivityParser.data.unsupported", data.getClass().getName()));
		}
		String xmlString = null;
		try {
			for (String line; xmlString == null && (line = rdr.readLine()) != null;) {
				if (line.startsWith("<?xml")) { // NON-NLS
					if (xmlBuffer.length() > 0) {
						xmlString = xmlBuffer.toString();
						xmlBuffer.setLength(0);
					}
				}
				xmlBuffer.append(line);
			}
		} catch (EOFException eof) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("ActivityXmlParser.data.end"), eof);
		} catch (IOException ioe) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getString("ActivityXmlParser.error.reading"), ioe);
		}
		if (xmlString == null && xmlBuffer.length() > 0) {
			xmlString = xmlBuffer.toString();
			xmlBuffer.setLength(0);
		}
		return xmlString;
	}

	private static class NamespaceMap implements NamespaceContext {
		private final Map<String, String> map = new HashMap<String, String>();

		private NamespaceMap() {
		}

		/**
		 * Adds mapping of prefix to namespace URI.
		 *
		 * @param prefix
		 *            prefix to put into mapping
		 * @param uri
		 *            uri to put into mapping
		 */
		public void addPrefixUriMapping(String prefix, String uri) {
			map.put(prefix, uri);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getNamespaceURI(String prefix) {
			String uri = map.get(prefix);
			if (uri == null) {
				uri = XMLConstants.XML_NS_URI;
			}
			return uri;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getPrefix(String namespaceURI) {
			for (Map.Entry<String, String> entry : map.entrySet()) {
				if (Utils.equal(entry.getValue(), namespaceURI)) {
					return entry.getKey();
				}
			}
			return XMLConstants.DEFAULT_NS_PREFIX;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Iterator<String> getPrefixes(String namespaceURI) {
			return map.keySet().iterator();
		}
	}
}
