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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.*;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.NamespaceMap;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements an activity data parser that assumes each activity data item is an XML string, with the value for each
 * field being retrieved from a particular XML element or attribute.
 * <p>
 * This parser supports reading the activity data from several types of input sources, and supports input streams
 * containing multiple XML documents. If there are multiple XML documents, each document must start with
 * {@code "<?xml ...>"}, and be separated by a new line.
 * <p>
 * This parser supports the following properties:
 * <ul>
 * <li>Namespace - additional XML namespace mappings. (Optional)</li>
 * <li>RequireDefault - indicates that all attributes are required by default. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityXmlParser extends GenericActivityParser<Document> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityXmlParser.class);

	/**
	 * Constant for XML tag attribute name {@value}.
	 */
	private static final String DATA_TYPE_ATTR = "datatype"; // NON-NLS
	/**
	 * Constant for XML tag attribute name {@value}.
	 */
	private static final String UNITS_ATTR = "units"; // NON-NLS
	/**
	 * Constant for XML tag attribute name {@value}.
	 */
	private static final String FORMAT_ATTR = "format"; // NON-NLS
	/**
	 * Constant for XML tag attribute name {@value}.
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
	 * Constructs a new activity XML string parser.
	 *
	 * @throws ParserConfigurationException
	 *             if any errors configuring the parser
	 */
	public ActivityXmlParser() throws ParserConfigurationException {
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

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (ParserProperties.PROP_NAMESPACE.equalsIgnoreCase(name)) {
				if (StringUtils.isNotEmpty(value)) {
					String[] nsFields = value.split("="); // NON-NLS
					namespaces.addPrefixUriMapping(nsFields[0], nsFields[1]);
					logger().log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ActivityXmlParser.adding.mapping"), name, value);
				}
			} else if (ParserProperties.PROP_REQUIRE_ALL.equalsIgnoreCase(name)) {
				if (StringUtils.isNotEmpty(value)) {
					requireAll = Boolean.parseBoolean(value);
					logger().log(OpLevel.DEBUG,
							StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
							name, value);
				}
			}
		}
		if (namespaces != null) {
			xPath.setNamespaceContext(namespaces);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link org.w3c.dom.Document}</li>
	 * <li>{@link java.lang.String}</li>
	 * <li>{@code byte[]}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.InputStream}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return Document.class.isInstance(data) || super.isDataClassSupported(data);
	}

	@Override
	public boolean canHaveDelimitedLocators() {
		return false;
	}

	@Override
	public ActivityInfo parse(TNTInputStream<?, ?> stream, Object data) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}

		Document xmlDoc = null;
		String xmlString = null;
		try {
			if (data instanceof Document) {
				xmlDoc = (Document) data;
			} else {
				xmlString = getNextActivityString(data);
				if (StringUtils.isEmpty(xmlString)) {
					return null;
				}
				xmlDoc = builder.parse(IOUtils.toInputStream(xmlString, Utils.UTF8));
			}
		} catch (Exception e) {
			ParseException pe = new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityXmlParser.xmlDocument.parse.error"), 0);
			pe.initCause(e);

			throw pe;
		}

		if (xmlString == null) {
			try {
				xmlString = Utils.documentToString(xmlDoc);
			} catch (Exception exc) {
				logger().log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityXmlParser.xmlDocument.toString.error"), exc);
			}
		}

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.parsing"), xmlString);

		return parsePreparedItem(stream, xmlString, xmlDoc);
	}

	@Override
	protected ActivityInfo parsePreparedItem(TNTInputStream<?, ?> stream, String dataStr, Document xmlDoc)
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
			Object[] values;
			for (ActivityField aFieldList : fieldList) {
				values = null;
				field = aFieldList;
				List<ActivityFieldLocator> locations = field.getLocators();
				if (locations != null) {
					// need to save format and units specification from config
					// in case individual entry in activity data overrides it
					if (savedFormats == null || savedFormats.length < locations.size()) {
						savedFormats = new String[locations.size()];
						savedUnits = new String[locations.size()];
						savedLocales = new String[locations.size()];
					}

					values = new Object[locations.size()];
					for (int li = 0; li < locations.size(); li++) {
						ActivityFieldLocator loc = locations.get(li);
						savedFormats[li] = loc.getFormat();
						savedUnits[li] = loc.getUnits();
						savedLocales[li] = loc.getLocale();
						values[li] = getLocatorValue(stream, loc, xmlDoc);
						if (values[li] == null && requireAll && !loc.isOptional()) { // NON-NLS
							logger().log(OpLevel.WARNING,
									StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
											"ActivityXmlParser.required.locator.not.found"),
									loc, field);
							return null;
						}
					}
				}
				applyFieldValue(stream, ai, field, Utils.simplifyValue(values));
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
					StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.parsing.failed", field), 0);
			pe.initCause(e);
			throw pe;
		}
		return ai;
	}

	/**
	 * Gets field raw data value resolved by locator and formats it according locator definition.
	 *
	 * @param locator
	 *            activity field locator
	 * @param xmlDoc
	 *            activity object XML DOM document
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return value formatted based on locator definition or {@code null} if locator is not defined
	 *
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value or applying locator format properties to specified
	 *             value
	 *
	 * @see ActivityFieldLocator#formatValue(Object)
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, Document xmlDoc, AtomicBoolean formattingNeeded)
			throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();

		if (StringUtils.isNotEmpty(locStr)) {
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

					val = Utils.simplifyValue(valuesList);
					formattingNeeded.set(false);
				}
			} catch (XPathExpressionException exc) {
				ParseException pe = new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityXMLParser.xPath.exception"), 0);
				pe.initCause(exc);

				throw pe;
			}
		}

		return val;
	}

	/**
	 * Reads RAW activity data XML package string from {@link BufferedReader}. If the data input source contains
	 * multiple XML documents, then each document must start with "&lt;?xml", and be separated by a new line.
	 *
	 * @param rdr
	 *            reader to use for reading
	 * @return non empty RAW activity data XML package string, or {@code null} if the end of the stream has been reached
	 */
	@Override
	protected String readNextActivity(BufferedReader rdr) {
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
			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.data.end"),
					getActivityDataType(), eof);
		} catch (IOException ioe) {
			logger().log(OpLevel.WARNING,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.error.reading"),
					getActivityDataType(), ioe);
		}

		if (xmlString == null && xmlBuffer.length() > 0) {
			xmlString = xmlBuffer.toString();
			xmlBuffer.setLength(0);
		}

		return xmlString;
	}

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - XML
	 */
	@Override
	protected String getActivityDataType() {
		return "XML"; // NON-NLS
	}
}
