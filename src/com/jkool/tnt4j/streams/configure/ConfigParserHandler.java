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

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import com.jkool.tnt4j.streams.fields.ActivityField;
import com.jkool.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkool.tnt4j.streams.fields.StreamFieldType;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Implements the SAX DefaultHandler for parsing jKool LLC TNT4J-Streams
 * configuration.
 *
 * @version $Revision: 7 $
 * @see StreamsConfig
 */
public class ConfigParserHandler extends DefaultHandler {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ConfigParserHandler.class);

	/**
	 * Constant for default location delimiter in configuration definition.
	 */
	public static final String LOC_DELIM = ",";

	private static final String CONFIG_ROOT_ELMT_OLD = "tw-direct-feed";
	private static final String CONFIG_ROOT_ELMT = "tnt-data-source";
	private static final String PARSER_ELMT = "parser";
	private static final String STREAM_ELMT = "stream";
	private static final String PROPERTY_ELMT = "property";
	private static final String FIELD_ELMT = "field";
	private static final String FIELD_MAP_ELMT = "field-map";
	private static final String FIELD_LOC_ELMT = "field-locator";
	private static final String PARSER_REF_ELMT = "parser-ref";
	private static final String FILTER_ELMT = "filter";
	private static final String RULE_ELMT = "rule";
	private static final String STEP_ELMT = "step";

	private static final String NAME_ATTR = "name";
	private static final String CLASS_ATTR = "class";
	private static final String VALUE_ATTR = "value";
	private static final String LOC_TYPE_ATTR = "locator-type";
	private static final String LOCATOR_ATTR = "locator";
	private static final String SEPARATOR_ATTR = "separator";
	private static final String DATA_TYPE_ATTR = "datatype";
	private static final String RADIX_ATTR = "radix";
	private static final String UNITS_ATTR = "units";
	private static final String FORMAT_ATTR = "format";
	private static final String LOCALE_ATTR = "locale";
	private static final String TIMEZONE_ATTR = "timezone";
	private static final String SOURCE_ATTR = "source";
	private static final String TARGET_ATTR = "target";
	private static final String FIELD_REF_ATTR = "field-ref";
	private static final String COMPARATOR_ATTR = "comparator";

	private static final String REQUIRED_VALUE = "required";

	private TNTInputStream currStream = null;
	private Collection<Map.Entry<String, String>> currProperties = null;
	private ActivityParser currParser = null;
	private ActivityField currField = null;
	private ActivityFieldLocator currLocator = null;
	// private StreamFilter currFilter = null; //NEXT_FEATURE:

	private boolean currFieldHasLocValAttr = false;
	private boolean currFieldHasLocElmt = false;
	private boolean currFieldHasMapElmt = false;

	private Map<String, ActivityParser> parsers = null;
	private Map<String, TNTInputStream> streams = null;

	private Locator currParseLocation = null;

	/**
	 * Constructs a ConfigurationParserHandler.
	 */
	public ConfigParserHandler() {
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
	 * {@inheritDoc}
	 */
	@Override
	public void setDocumentLocator(Locator locator) {
		currParseLocation = locator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startDocument() throws SAXException {
		currStream = null;
		currProperties = null;
		currParser = null;
		currField = null;
		currLocator = null;
		// currFilter = null; //NEXT_FEATURE:
		currFieldHasLocValAttr = false;
		currFieldHasLocElmt = false;
		currFieldHasMapElmt = false;
		streams = new HashMap<String, TNTInputStream>();
		parsers = new HashMap<String, ActivityParser>();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (CONFIG_ROOT_ELMT.equals(qName) || CONFIG_ROOT_ELMT_OLD.equals(qName)) {
			if (streams != null && !streams.isEmpty()) {
				throw new SAXParseException("Cannot have multiple " + qName + " elements", currParseLocation);
			}
		} else if (PROPERTY_ELMT.equals(qName)) {
			processProperty(attributes);
		} else if (FIELD_ELMT.equals(qName)) {
			processField(attributes);
		} else if (FIELD_LOC_ELMT.equals(qName)) {
			processFieldLocator(attributes);
		} else if (FIELD_MAP_ELMT.equals(qName)) {
			processFieldMap(attributes);
		} else if (PARSER_REF_ELMT.equals(qName)) {
			processParserRef(attributes);
		} else if (PARSER_ELMT.equals(qName)) {
			processParser(attributes);
		} else if (STREAM_ELMT.equals(qName)) {
			processStream(attributes);
			// } else if (FILTER_ELMT.equals(qName)) { //NEXT_FEATURE:
			// processFilter(attributes);
			// } else if (RULE_ELMT.equals(qName)) {
			// processRule(attributes);
			// } else if (STEP_ELMT.equals(qName)) {
			// processStep(attributes);
		}
	}

	/**
	 * Processes a parser element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error parsing element
	 */
	private void processParser(Attributes attrs) throws SAXException {
		if (currParser != null) {
			throw new SAXParseException("Malformed configuration: Detected nested " + PARSER_ELMT + " definition",
					currParseLocation);
		}
		String name = null;
		String className = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				name = attValue;
			} else if (CLASS_ATTR.equals(attName)) {
				className = attValue;
			}
		}
		if (StringUtils.isEmpty(name)) {
			throw new SAXParseException("Missing " + PARSER_ELMT + " attribute '" + NAME_ATTR + "'", currParseLocation);
		}
		if (StringUtils.isEmpty(className)) {
			throw new SAXParseException("Missing " + PARSER_ELMT + " attribute '" + CLASS_ATTR + "'",
					currParseLocation);
		}
		if (parsers.containsKey(name)) {
			throw new SAXParseException("Duplicate parser definition '" + name + "'", currParseLocation);
		}
		try {
			ClassLoader cl = getClass().getClassLoader();
			Class<?> streamClass = cl.loadClass(className);
			Object newStream = streamClass.newInstance();
			if (!(newStream instanceof ActivityParser)) {
				throw new SAXNotSupportedException(
						PARSER_ELMT + " " + CLASS_ATTR + " '" + className + "' does not implement interface '"
								+ ActivityParser.class.getName() + "'" + getLocationInfo());
			}
			currParser = (ActivityParser) newStream;
		} catch (ClassNotFoundException cnfe) {
			throw new SAXException(
					"Failed to load " + PARSER_ELMT + " " + CLASS_ATTR + " '" + className + "'" + getLocationInfo(),
					cnfe);
		} catch (InstantiationException ie) {
			throw new SAXException(
					"Failed to load " + PARSER_ELMT + " " + CLASS_ATTR + " '" + className + "'" + getLocationInfo(),
					ie);
		} catch (IllegalAccessException iae) {
			throw new SAXException(
					"Failed to load " + PARSER_ELMT + " " + CLASS_ATTR + " '" + className + "'" + getLocationInfo(),
					iae);
		}
		if (currParser != null) {
			currParser.setName(name);
			parsers.put(name, currParser);
		}
	}

	/**
	 * Processes a field element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error parsing element
	 */
	private void processField(Attributes attrs) throws SAXException {
		if (currField != null) {
			throw new SAXParseException("Malformed configuration: Detected nested " + FIELD_ELMT + " definition",
					currParseLocation);
		}
		if (currParser == null) {
			throw new SAXParseException(
					"Malformed configuration: " + FIELD_ELMT + " expected to have " + PARSER_ELMT + " as parent",
					currParseLocation);
		}
		currFieldHasLocValAttr = false;
		currFieldHasLocElmt = false;
		currFieldHasMapElmt = false;
		StreamFieldType field = null;
		ActivityFieldDataType dataType = null;
		String locatorType = null;
		String locator = null;
		String separator = null;
		String units = null;
		String format = null;
		String locale = null;
		String timeZone = null;
		String value = null;
		int radix = 10;
		String reqVal = "";
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				field = StreamFieldType.valueOf(attValue);
			} else if (DATA_TYPE_ATTR.equals(attName)) {
				dataType = ActivityFieldDataType.valueOf(attValue);
			} else if (LOC_TYPE_ATTR.equals(attName)) {
				locatorType = attValue;
			} else if (LOCATOR_ATTR.equals(attName)) {
				locator = attValue;
			} else if (SEPARATOR_ATTR.equals(attName)) {
				separator = attValue;
			} else if (RADIX_ATTR.equals(attName)) {
				radix = Integer.parseInt(attValue);
			} else if (UNITS_ATTR.equals(attName)) {
				units = attValue;
			} else if (FORMAT_ATTR.equals(attName)) {
				format = attValue;
			} else if (LOCALE_ATTR.equals(attName)) {
				locale = attValue;
			} else if (TIMEZONE_ATTR.equals(attName)) {
				timeZone = attValue;
			} else if (VALUE_ATTR.equals(attName)) {
				value = attValue;
			} else if (REQUIRED_VALUE.equals(attName)) {
				reqVal = attValue;
			}
		}
		if (locator != null && locator.isEmpty()) {
			locator = null;
		}
		if (value != null && value.isEmpty()) {
			value = null;
		}
		// make sure required fields are present
		if (field == null) {
			throw new SAXParseException("Missing " + FIELD_ELMT + " attribute '" + NAME_ATTR + "'", currParseLocation);
		}
		ActivityField af = new ActivityField(field);
		ActivityFieldLocator afl;
		if (value != null) {
			currFieldHasLocValAttr = true;
			afl = new ActivityFieldLocator(value);
			afl.setRadix(radix);
			afl.setRequired(reqVal);
			if (dataType != null) {
				afl.setDataType(dataType);
			}
			if (units != null) {
				afl.setUnits(units);
			}
			if (format != null) {
				afl.setFormat(format, locale);
			}
			if (timeZone != null) {
				afl.setTimeZone(timeZone);
			}
			af.addLocator(afl);
		} else if (locator != null) {
			currFieldHasLocValAttr = true;
			String[] locators = locator.split(LOC_DELIM);
			for (String loc : locators) {
				if (StringUtils.isEmpty(loc)) {
					af.addLocator(null);
				} else {
					afl = new ActivityFieldLocator(locatorType, loc);
					afl.setRadix(radix);
					afl.setRequired(reqVal);
					if (dataType != null) {
						afl.setDataType(dataType);
					}
					if (units != null) {
						afl.setUnits(units);
					}
					if (format != null) {
						afl.setFormat(format, locale);
					}
					if (timeZone != null) {
						afl.setTimeZone(timeZone);
					}
					af.addLocator(afl);
				}
			}
		}
		if (format != null) {
			af.setFormat(format);
		}
		if (locale != null) {
			af.setLocale(locale);
		}
		if (separator != null) {
			af.setSeparator(separator);
		}
		af.setRequired(reqVal);
		currField = af;
	}

	/**
	 * Processes a field locator element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error parsing element
	 */
	private void processFieldLocator(Attributes attrs) throws SAXException {
		if (currLocator != null) {
			throw new SAXParseException("Malformed configuration: Detected nested " + FIELD_LOC_ELMT + " definition",
					currParseLocation);
		}
		if (currField == null) {
			throw new SAXParseException(
					"Malformed configuration: " + FIELD_LOC_ELMT + " expected to have " + FIELD_ELMT + " as parent",
					currParseLocation);
		}
		if (currFieldHasLocValAttr) {
			throw new SAXException("Element '" + FIELD_ELMT + "' must not have both '" + LOCATOR_ATTR + "' or '"
					+ VALUE_ATTR + "' attributes defined and one or more '" + FIELD_LOC_ELMT + "' child elements"
					+ getLocationInfo());
		}
		if (currFieldHasMapElmt) {
			throw new SAXException("Element '" + FIELD_ELMT + "' cannot have both '" + FIELD_LOC_ELMT + "' and '"
					+ FIELD_MAP_ELMT + "' child elements" + getLocationInfo());
		}
		ActivityFieldDataType dataType = null;
		String locatorType = null;
		String locator = null;
		String units = null;
		String format = null;
		String locale = null;
		String timeZone = null;
		String value = null;
		int radix = 10;
		String reqVal = ""; /* string to allow override */
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (DATA_TYPE_ATTR.equals(attName)) {
				dataType = ActivityFieldDataType.valueOf(attValue);
			} else if (LOC_TYPE_ATTR.equals(attName)) {
				locatorType = attValue;
			} else if (LOCATOR_ATTR.equals(attName)) {
				locator = attValue;
			} else if (RADIX_ATTR.equals(attName)) {
				radix = Integer.parseInt(attValue);
			} else if (UNITS_ATTR.equals(attName)) {
				units = attValue;
			} else if (FORMAT_ATTR.equals(attName)) {
				format = attValue;
			} else if (LOCALE_ATTR.equals(attName)) {
				locale = attValue;
			} else if (TIMEZONE_ATTR.equals(attName)) {
				timeZone = attValue;
			} else if (VALUE_ATTR.equals(attName)) {
				value = attValue;
			} else if (REQUIRED_VALUE.equals(attName)) {
				reqVal = attValue;
			}
		}
		if (locator != null && locator.isEmpty()) {
			locator = null;
		}
		if (value != null && value.isEmpty()) {
			value = null;
		}
		// make sure common required fields are present
		if (locator == null && value == null) {
			throw new SAXParseException(
					FIELD_LOC_ELMT + " must contain one of attributes '" + LOCATOR_ATTR + "' or '" + VALUE_ATTR + "'",
					currParseLocation);
		}
		if (locator != null && value != null) {
			throw new SAXParseException(
					FIELD_LOC_ELMT + " cannot contain both attributes '" + LOCATOR_ATTR + "' and '" + VALUE_ATTR + "'",
					currParseLocation);
		}
		// make sure any fields that are required based on other fields are
		// specified
		if (ActivityFieldDataType.DateTime == dataType) {
			if (format == null) {
				throw new SAXParseException(
						"Missing " + FIELD_LOC_ELMT + " attribute '" + FORMAT_ATTR + "' for " + dataType,
						currParseLocation);
			}
			// if (locale == null)
			// {
			//
			// }
		} else if (ActivityFieldDataType.Timestamp == dataType) {
			if (units == null) {
				throw new SAXParseException(
						"Missing " + FIELD_LOC_ELMT + " attribute '" + UNITS_ATTR + "' for " + dataType,
						currParseLocation);
			}
		}
		ActivityFieldLocator afl = value != null ? new ActivityFieldLocator(value)
				: new ActivityFieldLocator(locatorType, locator);
		afl.setRadix(radix);
		afl.setRequired(reqVal);
		if (format != null) {
			afl.setFormat(format, locale);
		}
		if (dataType != null) {
			afl.setDataType(dataType);
		}
		if (units != null) {
			afl.setUnits(units);
		}
		if (timeZone != null) {
			afl.setTimeZone(timeZone);
		}
		currLocator = afl;
		currField.addLocator(afl);
		currFieldHasLocElmt = true;
	}

	/**
	 * Processes a field-map element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error parsing element
	 */
	private void processFieldMap(Attributes attrs) throws SAXException {
		if (currField == null) {
			throw new SAXParseException("Malformed configuration: " + FIELD_MAP_ELMT + " expected to have " + FIELD_ELMT
					+ " or " + FIELD_LOC_ELMT + " as parent", currParseLocation);
		}
		if (currFieldHasLocElmt && currLocator == null) {
			throw new SAXException("Element '" + FIELD_ELMT + "' cannot have both '" + FIELD_LOC_ELMT + "' and '"
					+ FIELD_MAP_ELMT + "' child elements" + getLocationInfo());
		}
		String source = null;
		String target = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (SOURCE_ATTR.equals(attName)) {
				source = attValue;
			} else if (TARGET_ATTR.equals(attName)) {
				target = attValue;
			}
		}
		if (source == null) {
			throw new SAXParseException("Missing " + FIELD_MAP_ELMT + " attribute '" + SOURCE_ATTR + "'",
					currParseLocation);
		}
		if (target == null) {
			throw new SAXParseException("Missing " + FIELD_MAP_ELMT + " attribute '" + TARGET_ATTR + "'",
					currParseLocation);
		}
		if (currLocator != null) {
			currLocator.addValueMap(source, target);
		} else {
			currFieldHasMapElmt = true;
			List<ActivityFieldLocator> locators = currField.getLocators();
			if (locators != null) {
				for (ActivityFieldLocator loc : locators) {
					loc.addValueMap(source, target);
				}
			}
		}
	}

	/**
	 * Processes a stream element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error parsing element
	 */
	private void processStream(Attributes attrs) throws SAXException {
		if (currStream != null) {
			throw new SAXParseException("Malformed configuration: Detected nested " + STREAM_ELMT + " definitions",
					currParseLocation);
		}
		String name = null;
		String className = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				name = attValue;
			} else if (CLASS_ATTR.equals(attName)) {
				className = attValue;
			}
		}
		if (StringUtils.isEmpty(name)) {
			throw new SAXParseException("Missing " + STREAM_ELMT + " attribute '" + NAME_ATTR + "'", currParseLocation);
		}
		if (StringUtils.isEmpty(className)) {
			throw new SAXParseException("Missing " + STREAM_ELMT + " attribute '" + CLASS_ATTR + "'",
					currParseLocation);
		}
		if (streams.containsKey(name)) {
			throw new SAXParseException("Duplicate " + STREAM_ELMT + " '" + name + "'", currParseLocation);
		}
		try {
			ClassLoader cl = getClass().getClassLoader();
			Class<?> streamClass = cl.loadClass(className);
			Object newStream = streamClass.newInstance();
			if (!(newStream instanceof TNTInputStream)) {
				throw new SAXNotSupportedException(STREAM_ELMT + " " + CLASS_ATTR + " '" + className
						+ "' does not extend class '" + TNTInputStream.class.getName() + "'" + getLocationInfo());
			}
			currStream = (TNTInputStream) newStream;
		} catch (ClassNotFoundException cnfe) {
			throw new SAXException(
					"Failed to load " + STREAM_ELMT + " " + CLASS_ATTR + " '" + className + "'" + getLocationInfo(),
					cnfe);
		} catch (InstantiationException ie) {
			throw new SAXException(
					"Failed to load " + STREAM_ELMT + " " + CLASS_ATTR + " '" + className + "'" + getLocationInfo(),
					ie);
		} catch (IllegalAccessException iae) {
			throw new SAXException(
					"Failed to load " + STREAM_ELMT + " " + CLASS_ATTR + " '" + className + "'" + getLocationInfo(),
					iae);
		}
		streams.put(name, currStream);
	}

	/**
	 * Processes a property element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error parsing element
	 */
	private void processProperty(Attributes attrs) throws SAXException {
		if (currStream == null && currParser == null) {
			throw new SAXParseException("Malformed configuration: " + PROPERTY_ELMT + " expected to have " + STREAM_ELMT
					+ " or " + PARSER_ELMT + " as parent", currParseLocation);
		}
		String name = null;
		String value = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				name = attValue;
			} else if (VALUE_ATTR.equals(attName)) {
				value = attValue;
			}
		}
		if (StringUtils.isEmpty(name)) {
			throw new SAXParseException("Missing " + PROPERTY_ELMT + " attribute '" + NAME_ATTR + "'",
					currParseLocation);
		}
		if (value == null) {
			throw new SAXParseException("Missing " + PROPERTY_ELMT + " attribute '" + VALUE_ATTR + "'",
					currParseLocation);
		}
		if (currProperties == null) {
			currProperties = new ArrayList<Map.Entry<String, String>>();
		}
		currProperties.add(new AbstractMap.SimpleEntry<String, String>(name, value));
	}

	/**
	 * Processes a parser-ref element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error parsing element
	 */
	private void processParserRef(Attributes attrs) throws SAXException {
		if (currStream == null) {
			throw new SAXParseException(
					"Malformed configuration: " + PARSER_REF_ELMT + " expected to have " + STREAM_ELMT + " as parent",
					currParseLocation);
		}
		String parserName = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				parserName = attValue;
			}
		}
		if (StringUtils.isEmpty(parserName)) {
			throw new SAXParseException("Missing " + PARSER_REF_ELMT + " attribute '" + NAME_ATTR + "'",
					currParseLocation);
		}
		ActivityParser parser = parsers.get(parserName);
		if (parser == null) {
			throw new SAXParseException("Undefined " + PARSER_REF_ELMT + " reference '" + parserName + "'",
					currParseLocation);
		}
		currStream.addParser(parser);
	}

	// /** //NEXT_FEATURE:
	// * TODO
	// *
	// * @param attrs
	// * List of element attributes
	// *
	// * @throws SAXException
	// * if error parsing element
	// */
	// private void processFilter(Attributes attrs) throws SAXException {
	//
	// }
	//
	// /**
	// * TODO
	// *
	// * @param attrs
	// * List of element attributes
	// *
	// * @throws SAXException
	// * if error parsing element
	// */
	// private void processRule(Attributes attrs) throws SAXException {
	//
	// }
	//
	// /**
	// * TODO
	// *
	// * @param attrs
	// * List of element attributes
	// * @throws SAXException
	// * if error parsing element
	// */
	// private void processStep(Attributes attrs) throws SAXException {
	//
	// }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			if (STREAM_ELMT.equals(qName)) {
				if (currProperties != null) {
					currStream.setProperties(currProperties);
				}
				currStream = null;
				currProperties = null;
			} else if (PARSER_ELMT.equals(qName)) {
				if (currProperties != null) {
					currParser.setProperties(currProperties);
				}
				currParser = null;
				currProperties = null;
			} else if (FIELD_ELMT.equals(qName)) {
				List<ActivityFieldLocator> locators = currField.getLocators();
				if (locators == null || locators.isEmpty()) {
					throw new SAXException("Element '" + FIELD_ELMT + "' must have '" + LOCATOR_ATTR + "' or '"
							+ VALUE_ATTR + "' attributes defined or one or more '" + FIELD_LOC_ELMT + "' child elements"
							+ getLocationInfo());
				}
				currParser.addField(currField);
				currField = null;
				currFieldHasLocValAttr = false;
				currFieldHasLocElmt = false;
				currFieldHasMapElmt = false;
			} else if (FIELD_LOC_ELMT.equals(qName)) {
				currLocator = null;
				// } else if (FILTER_ELMT.equals(qName)) { //NEXT_FEATURE:
				// currParser.addFilter(currFilter);
				// currFilter = null;
			}
		} catch (SAXException exc) {
			throw exc;
		} catch (Throwable t) {
			SAXException se = new SAXException(t.getMessage() + getLocationInfo());
			se.initCause(t);
			throw se;
		}
	}

	/**
	 * Gets a string representing the current line in the file being parsed.
	 * Used for error messages.
	 *
	 * @return string representing current line number being parsed
	 */
	private String getLocationInfo() {
		String locInfo = "";
		if (currParseLocation != null) {
			locInfo = ", at line " + currParseLocation.getLineNumber();
		}
		return locInfo;
	}
}
