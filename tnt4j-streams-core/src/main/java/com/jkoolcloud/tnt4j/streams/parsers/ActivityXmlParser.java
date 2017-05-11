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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
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
import com.jkoolcloud.tnt4j.streams.utils.StreamsXMLUtils;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements an activity data parser that assumes each activity data item is an XML string, with the value for each
 * field being retrieved from a particular XML element or attribute.
 * <p>
 * This parser supports reading the activity data from several types of input sources, and supports input streams
 * containing multiple XML documents. If there are multiple XML documents, each document must start with
 * {@code "<?xml ...>"}, and be separated by a new line.
 * <p>
 * This parser supports the following properties (in addition to those supported by {@link GenericActivityParser}):
 * <ul>
 * <li>Namespace - additional XML namespace mappings. (Optional)</li>
 * <li>RequireDefault - indicates that all attributes are required by default. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityXmlParser extends GenericActivityParser<Node> {
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
		xPath = StreamsXMLUtils.getStreamsXPath();
		xmlBuffer = new StringBuilder(1024);

		if (namespaces == null) {
			if (xPath.getNamespaceContext() instanceof NamespaceMap) {
				namespaces = (NamespaceMap) xPath.getNamespaceContext();
			} else {
				namespaces = new NamespaceMap();
				xPath.setNamespaceContext(namespaces);
			}
		}

		namespaces.addPrefixUriMapping(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
		namespaces.addPrefixUriMapping("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI); // NON-NLS
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
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link org.w3c.dom.Node}</li>
	 * <li>{@link java.lang.String}</li>
	 * <li>{@code byte[]}</li>
	 * <li>{@link java.nio.ByteBuffer}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.InputStream}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return Node.class.isInstance(data) || super.isDataClassSupportedByParser(data);
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

		data = preParse(stream, data);

		Node xmlDoc = null;
		String xmlString = null;
		try {
			if (data instanceof Document) {
				xmlDoc = (Document) data;
			} else if (data instanceof Node) {
				xmlDoc = (Node) data;
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

		ActivityInfo ai = parsePreparedItem(stream, xmlString, xmlDoc);
		postParse(ai, stream, xmlDoc);

		return ai;
	}

	@Override
	protected ActivityInfo parsePreparedItem(TNTInputStream<?, ?> stream, String dataStr, Node xmlDoc)
			throws ParseException {
		if (xmlDoc == null) {
			return null;
		}

		ActivityInfo ai = new ActivityInfo();
		ActivityField field = null;
		ContextData cData = new ContextData(xmlDoc, stream, ai);
		try {
			String[] savedFormats = null;
			String[] savedUnits = null;
			String[] savedLocales = null;
			// apply fields for parser
			Object[] values;
			for (ActivityField aField : fieldList) {
				values = null;
				field = aField;
				List<ActivityFieldLocator> locators = field.getLocators();
				if (locators != null) {
					// need to save format and units specification from config
					// in case individual entry in activity data overrides it
					if (ArrayUtils.getLength(savedFormats) < locators.size()) {
						savedFormats = new String[locators.size()];
						savedUnits = new String[locators.size()];
						savedLocales = new String[locators.size()];
					}

					values = parseLocatorValues(locators, cData);
					for (int li = 0; li < locators.size(); li++) {
						ActivityFieldLocator loc = locators.get(li);
						savedFormats[li] = loc.getFormat();
						savedUnits[li] = loc.getUnits();
						savedLocales[li] = loc.getLocale();

						if (CollectionUtils.isEmpty(aField.getStackedParsers())) {
							if (values[li] instanceof Node) {
								values[li] = getTextContent(loc, (Node) values[li]);
							}
						}
						if (values[li] == null && requireAll && !loc.isOptional()) { // NON-NLS
							logger().log(OpLevel.WARNING,
									StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
											"ActivityXmlParser.required.locator.not.found"),
									loc, field);
							return null;
						}
					}
				}
				applyFieldValue(field, Utils.simplifyValue(values), cData);
				if (locators != null && savedFormats != null) {
					for (int li = 0; li < locators.size(); li++) {
						ActivityFieldLocator loc = locators.get(li);
						loc.setFormat(savedFormats[li], savedLocales[li]);
						loc.setUnits(savedUnits[li]);
					}
				}
			}

			if (useActivityAsMessage && ai.getMessage() == null && dataStr != null) {
				// save entire activity string as message data
				field = new ActivityField(StreamFieldType.Message.name());
				applyFieldValue(stream, ai, field, dataStr);
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
	 * @param cData
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
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ContextData cData,
			AtomicBoolean formattingNeeded) throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();
		Node xmlDoc = cData.getData();

		if (ActivityField.isDynamicAttr(locStr)) {
			ActivityInfo ai = cData.getActivity();
			List<String> vars = new ArrayList<>();
			Utils.resolveCfgVariables(vars, locStr);
			locStr = Utils.fillInPattern(locStr, vars, ai, this.getName());
		}

		if (StringUtils.isNotEmpty(locStr)) {
			Document nodeDocument = cropDocumentForNode(xmlDoc);
			try {
				XPathExpression expr = xPath.compile(locStr);

				if (nodeDocument != null) { // try expression relative to node
					val = resolveValueOverXPath(nodeDocument, expr, formattingNeeded);
				}
				if (val == null) { // otherwise try on complete document
					val = resolveValueOverXPath(xmlDoc, expr, formattingNeeded);
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

	private static Object resolveValueOverXPath(Node xmlDoc, XPathExpression expr, AtomicBoolean formattingNeeded)
			throws XPathExpressionException {
		Object val = null;
		NodeList nodes = null;
		try {
			nodes = (NodeList) expr.evaluate(xmlDoc, XPathConstants.NODESET);
		} catch (XPathException exc) {
			val = expr.evaluate(xmlDoc);
		}

		int length = nodes == null ? 0 : nodes.getLength();

		if (length > 0) {
			List<Object> valuesList = new ArrayList<>(length);
			for (int i = 0; i < length; i++) {
				Node node = nodes.item(i);
				valuesList.add(node);
			}

			val = Utils.simplifyValue(valuesList);
			formattingNeeded.set(false);
		}

		return val;
	}

	private Document cropDocumentForNode(Node xmlDoc) throws ParseException {
		if (xmlDoc.getParentNode() != null) { // if node is not document root node
			try {
				Document nodeXmlDoc = builder.newDocument();
				Node importedNode = nodeXmlDoc.importNode(xmlDoc, true);
				nodeXmlDoc.appendChild(importedNode);

				return nodeXmlDoc;
			} catch (Exception exc) {
				ParseException pe = new ParseException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityXmlParser.xmlDocument.parse.error"), 0);
				pe.initCause(exc);

				throw pe;
			}
		}

		return null;
	}

	/**
	 * Resolves DOM node contained textual data and formats it using provided locator.
	 *
	 * @param locator
	 *            locator instance to alter using XML attributes contained data type, format and units used to format
	 *            resolved value
	 * @param node
	 *            DOM node to collect textual data
	 * @return resolved textual value formatted based on the locator's formatting properties
	 * @throws ParseException
	 *             if exception occurs applying locator format properties to specified value
	 */
	protected static Object getTextContent(ActivityFieldLocator locator, Node node) throws ParseException {
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

				locCopy.setFormat(attrVal, StringUtils.isEmpty(attrLVal) ? locator.getLocale() : attrLVal);
			}

			attr = attrsMap.getNamedItem(UNITS_ATTR);
			attrVal = attr == null ? null : attr.getTextContent();
			if (StringUtils.isNotEmpty(attrVal)) {
				locCopy.setUnits(attrVal);
			}
		}

		return locCopy.formatValue(strValue.trim());
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
