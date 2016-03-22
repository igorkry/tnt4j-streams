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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.*;

import com.jkool.tnt4j.streams.configure.ParserProperties;
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
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityXmlParser extends GenericActivityParser<Document> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityXmlParser.class);

	/**
	 * Constant for XML tag attribute name 'data type'.
	 */
	private static final String DATA_TYPE_ATTR = "datatype"; // NON-NLS
	/**
	 * Constant for XML tag attribute name 'units'.
	 */
	private static final String UNITS_ATTR = "units"; // NON-NLS
	/**
	 * Constant for XML tag attribute name 'format'.
	 */
	private static final String FORMAT_ATTR = "format"; // NON-NLS
	/**
	 * Constant for XML tag attribute name 'locale'.
	 */
	private static final String LOCALE_ATTR = "locale"; // NON-NLS

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

		if (namespaces == null) {
			namespaces = new NamespaceMap();
			namespaces.addPrefixUriMapping(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
			namespaces.addPrefixUriMapping("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI); // NON-NLS
		}
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
			if (ParserProperties.PROP_NAMESPACE.equalsIgnoreCase(name)) {
				if (!StringUtils.isEmpty(value)) {
					String[] nsFields = value.split("="); // NON-NLS
					namespaces.addPrefixUriMapping(nsFields[0], nsFields[1]);
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ActivityXmlParser.adding.mapping"), name, value);
				}
			} else if (ParserProperties.PROP_REQUIRE_ALL.equalsIgnoreCase(name)) {
				if (!StringUtils.isEmpty(value)) {
					requireAll = Boolean.parseBoolean(value);
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ActivityParser.setting", name, value));
				}
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
	public boolean canHaveDelimitedLocators() {
		return false;
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
		String xmlString = null;
		try {
			if (data instanceof Document) {
				xmlDoc = (Document) data;
			} else {
				xmlString = getNextXmlString(data);
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

		if (xmlString == null) {
			try {
				xmlString = Utils.documentToString(xmlDoc);
			} catch (Exception exc) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
						"ActivityXmlParser.xmlDocument.toString.error"), exc);
			}
		}

		return parsePreparedItem(stream, xmlString, xmlDoc);
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
			for (int i = 0; i < fieldList.size(); i++) {
				value = null;
				field = fieldList.get(i);
				List<ActivityFieldLocator> locations = field.getLocators();
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
			boolean format = true;
			String locStr = locator.getLocator();
			if (!StringUtils.isEmpty(locStr)) {
				if (locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp) {
					val = stream.getProperty(locStr);
				} else {
					// get value for locator (element)
					try {
						XPathExpression expr = xPath.compile(locStr);
						NodeList nodes = null;
						try {
							nodes = (NodeList) expr.evaluate(xmlDoc, XPathConstants.NODESET);
						} catch (XPathException exc) {
							val = expr.evaluate(xmlDoc);
						}
						int length = nodes == null ? 0 : nodes.getLength();

						if (length > 0) {
							List<Object> valuesList = new ArrayList<Object>(length);
							for (int i = 0; i < length; i++) {
								Node node = nodes.item(i);

								String strValue = node.getTextContent();
								Node attrsNode = node;

								if (node instanceof Attr) {
									Attr attr = (Attr) node;

									attrsNode = attr.getOwnerElement();
								}

								// Get list of attributes and their values for
								// current element
								NamedNodeMap attrsMap = attrsNode == null ? null : attrsNode.getAttributes();

								Node attr;
								String attrVal;
								ActivityFieldLocator locCopy = locator.clone();
								if (attrsMap != null && attrsMap.getLength() > 0) {
									attr = attrsMap.getNamedItem(DATA_TYPE_ATTR);
									attrVal = attr == null ? null : attr.getTextContent();
									if (StringUtils.isNotEmpty(attrVal)) {
										locCopy.setDataType(ActivityFieldDataType.valueOf(attrVal));
									}

									attr = attrsMap.getNamedItem(FORMAT_ATTR);
									attrVal = attr == null ? null : attr.getTextContent();
									if (StringUtils.isNotEmpty(attrVal)) {
										attr = attrsMap.getNamedItem(LOCALE_ATTR);
										String attrLVal = attr == null ? null : attr.getTextContent();

										locCopy.setFormat(attrVal,
												StringUtils.isEmpty(attrLVal) ? locCopy.getLocale() : attrLVal);
									}

									attr = attrsMap.getNamedItem(UNITS_ATTR);
									attrVal = attr == null ? null : attr.getTextContent();
									if (StringUtils.isNotEmpty(attrVal)) {
										locCopy.setUnits(attrVal);
									}
								}

								valuesList.add(locCopy.formatValue(strValue.trim()));
							}

							val = wrapValue(valuesList);
							format = false;
						}
					} catch (XPathExpressionException exc) {
						ParseException pe = new ParseException(StreamsResources.getString(
								StreamsResources.RESOURCE_BUNDLE_CORE, "ActivityXMLParser.xPath.exception"), 0);
						pe.initCause(exc);

						throw pe;
					}
				}
			}

			if (format) {
				val = locator.formatValue(val);
			}
		}

		return val;
	}

	private Object wrapValue(List<Object> valuesList) {
		if (CollectionUtils.isEmpty(valuesList)) {
			return null;
		}

		return valuesList.size() == 1 ? valuesList.get(0) : valuesList.toArray();
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
