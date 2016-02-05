/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.*;

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
 * <p>
 * This parser supports reading the activity data from several types of input
 * sources, and supports input streams containing multiple XML documents. If
 * there are multiple XML documents, each document must start with
 * {@code "<?xml ...>"}, and be separated by a new line.
 * <p>
 * This parser supports the following properties:
 * <ul>
 * <li>Namespace - additional XML namespace mappings. (Optional)</li>
 * <li>RequireDefault - indicates that all attributes are required by default.
 * (Optional)</li>
 * <li>ValueDelim - delimiter to use if XPath expression evaluates multiple
 * values. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityXmlParser extends GenericActivityParser<Document> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityXmlParser.class);

	/**
	 * Contains the XML namespace mappings.
	 */
	protected NamespaceMap namespaces = null;

	private final XPath xPath;
	private final DocumentBuilder builder;
	private final StringBuilder xmlBuffer;

	/**
	 * Property indicating that all attributes are required by default.
	 */
	protected boolean requireAll = false;

	/**
	 * Property indicating what delimiter to use if XPath expression evaluates
	 * multiple values.
	 */
	protected String valuesDelim = DEFAULT_DELIM;

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
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
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
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ActivityXmlParser.adding.mapping"), name, value);
				}
			} else if (StreamsConfig.PROP_REQUIRE_ALL.equalsIgnoreCase(name)) {
				if (!StringUtils.isEmpty(value)) {
					requireAll = Boolean.parseBoolean(value);
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ActivityParser.setting", name, value));
				}
			} else if (StreamsConfig.PROP_VAL_DELIM.equalsIgnoreCase(name)) {
				valuesDelim = value;
			}

			LOGGER.log(OpLevel.TRACE, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ActivityParser.ignoring", name));
		}
		if (namespaces != null) {
			xPath.setNamespaceContext(namespaces);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActivityInfo parse(TNTInputStream stream, Object data) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
				"ActivityParser.parsing", data));

		Document xmlDoc = null;
		try {
			if (data instanceof Document) {
				xmlDoc = (Document) data;
			} else {
				String xmlString = getNextXmlString(data);
				if (StringUtils.isEmpty(xmlString)) {
					return null;
				}
				xmlDoc = builder.parse(IOUtils.toInputStream(xmlString));
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ActivityXmlParser.xmlDocument.parse.error"), 0);
			pe.initCause(e);

			throw pe;
		}

		return parsePreparedItem(stream, null, xmlDoc);
	}

	@Override
	protected ActivityInfo parsePreparedItem(TNTInputStream stream, String dataStr, Document xmlDoc)
			throws ParseException {
		if (xmlDoc == null) {
			return null;
		}

		ActivityInfo ai = new ActivityInfo();
		ActivityField field = null;
		try {
			if (dataStr != null) {
				// save entire activity string as message data
				field = new ActivityField(StreamFieldType.Message.name());
				applyFieldValue(stream, ai, field, dataStr);
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
							LOGGER.log(OpLevel.TRACE,
									StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
											"ActivityXmlParser.required.locator.not.found", field));
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
								LOGGER.log(OpLevel.TRACE,
										StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
												"ActivityXmlParser.required.locator.not.found", field));
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
			ParseException pe = new ParseException(StreamsResources.getStringFormatted(
					StreamsResources.RESOURCE_BUNDLE_CORE, "ActivityParser.parsing.failed", field), 0);
			pe.initCause(e);
			throw pe;
		}
		return ai;
	}

	/**
	 * Gets field value from raw data location and formats it according locator
	 * definition.
	 *
	 * @param stream
	 *            parent stream
	 * @param locator
	 *            activity field locator
	 * @param xmlDoc
	 *            activity object XML DOM document
	 *
	 * @return value formatted based on locator definition or {@code null} if
	 *         locator is not defined
	 *
	 * @throws ParseException
	 *             if error applying locator format properties to specified
	 *             value
	 *
	 * @see ActivityFieldLocator#formatValue(Object)
	 */
	@Override
	protected Object getLocatorValue(TNTInputStream stream, ActivityFieldLocator locator, Document xmlDoc)
			throws ParseException {
		Object val = null;
		if (locator != null) {
			String locStr = locator.getLocator();
			if (!StringUtils.isEmpty(locStr)) {
				if (locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp) {
					val = stream.getProperty(locStr);
					val = locator.formatValue(val);
				} else {
					// get value for locator (element)
					try {
						XPathExpression expr = xPath.compile(locStr);
						NodeList nodes = (NodeList) expr.evaluate(xmlDoc, XPathConstants.NODESET);
						int length = nodes == null ? 0 : nodes.getLength();

						if (length > 0) {
							List<Object> valuesList = new ArrayList<Object>(length);
							for (int i = 0; i < length; i++) {
								Attr node = (Attr) nodes.item(i);
								String strValue = node.getValue();
								Node parentNode = node.getOwnerElement();

								// Get list of attributes and their values for
								// current element
								NamedNodeMap attrsMap = parentNode == null ? null : parentNode.getAttributes();

								Attr attr;
								Attr attr2;
								ActivityFieldLocator locCopy = locator.clone();
								if (attrsMap != null && attrsMap.getLength() > 0) {
									attr = (Attr) attrsMap.getNamedItem(ConfigParserHandler.DATA_TYPE_ATTR);
									if (attr != null && StringUtils.isEmpty(attr.getValue())) {
										locCopy.setDataType(ActivityFieldDataType.valueOf(attr.getValue()));
									}

									attr = (Attr) attrsMap.getNamedItem(ConfigParserHandler.FORMAT_ATTR);
									attr2 = (Attr) attrsMap.getNamedItem(ConfigParserHandler.LOCALE_ATTR);
									if (attr != null && StringUtils.isEmpty(attr.getValue())) {
										locCopy.setFormat(attr.getValue(),
												attr2 == null || StringUtils.isEmpty(attr2.getValue())
														? locator.getLocale() : attr2.getValue());
									}

									attr = (Attr) attrsMap.getNamedItem(ConfigParserHandler.UNITS_ATTR);
									if (attr != null && StringUtils.isEmpty(attr.getValue())) {
										locCopy.setUnits(attr.getValue());
									}
								}

								valuesList.add(locCopy.formatValue(strValue.trim()));
							}

							val = wrapValue(valuesList);
						}
					} catch (XPathExpressionException exc) {
						ParseException pe = new ParseException(StreamsResources.getString(
								StreamsResources.RESOURCE_BUNDLE_CORE, "ActivityXMLParser.xPath.exception"), 0);
						pe.initCause(exc);

						throw pe;
					}
				}
			}
		}

		return val;
	}

	private Object wrapValue(List<Object> valuesList) {
		if (CollectionUtils.isEmpty(valuesList)) {
			return null;
		}

		if (valuesList.size() == 1) {
			return valuesList.get(0);
		}

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < valuesList.size(); i++) {
			sb.append(valuesList.get(i));

			if (i < valuesList.size() - 1) {
				sb.append(valuesDelim);
			}
		}

		return sb.toString();
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
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ActivityParser.data.unsupported", data.getClass().getName()));
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
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "ActivityXmlParser.data.end"),
					eof);
		} catch (IOException ioe) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ActivityXmlParser.error.reading"), ioe);
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
