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

import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import com.jkool.tnt4j.streams.configure.StreamsConfigData;
import com.jkool.tnt4j.streams.fields.ActivityField;
import com.jkool.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkool.tnt4j.streams.fields.ActivityFieldMappingType;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Implements the SAX DefaultHandler for parsing TNT4J-Streams configuration.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkool.tnt4j.streams.configure.StreamsConfigLoader
 * @see com.jkool.tnt4j.streams.configure.sax.StreamsConfigSAXParser
 */
public class ConfigParserHandler extends DefaultHandler {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ConfigParserHandler.class);

	/**
	 * Constant for default location delimiter in configuration definition.
	 */
	public static final String LOC_DELIM = "|"; // NON-NLS

	private static final String CONFIG_ROOT_ELMT_OLD = "tw-direct-feed"; // NON-NLS
	private static final String CONFIG_ROOT_ELMT = "tnt-data-source"; // NON-NLS
	private static final String PARSER_ELMT = "parser"; // NON-NLS
	private static final String STREAM_ELMT = "stream"; // NON-NLS
	private static final String PROPERTY_ELMT = "property"; // NON-NLS
	private static final String FIELD_ELMT = "field"; // NON-NLS
	private static final String FIELD_MAP_ELMT = "field-map"; // NON-NLS
	private static final String FIELD_LOC_ELMT = "field-locator"; // NON-NLS
	private static final String PARSER_REF_ELMT = "parser-ref"; // NON-NLS
	private static final String TNT4J_PROPERTIES_ELMT = "tnt4j-properties"; // NON-NLS
	private static final String FILTER_ELMT = "filter"; // NON-NLS
	private static final String RULE_ELMT = "rule"; // NON-NLS
	private static final String STEP_ELMT = "step"; // NON-NLS

	private static final String NAME_ATTR = "name"; // NON-NLS
	private static final String CLASS_ATTR = "class"; // NON-NLS
	private static final String VALUE_ATTR = "value"; // NON-NLS
	private static final String LOC_TYPE_ATTR = "locator-type"; // NON-NLS
	private static final String LOCATOR_ATTR = "locator"; // NON-NLS
	private static final String SEPARATOR_ATTR = "separator"; // NON-NLS
	/**
	 * Constant for XML tag attribute name 'data type'.
	 */
	private static final String DATA_TYPE_ATTR = "datatype"; // NON-NLS
	private static final String RADIX_ATTR = "radix"; // NON-NLS
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
	private static final String TIMEZONE_ATTR = "timezone"; // NON-NLS
	private static final String SOURCE_ATTR = "source";
	private static final String TARGET_ATTR = "target"; // NON-NLS
	private static final String TYPE_ATTR = "type"; // NON-NLS
	private static final String FIELD_REF_ATTR = "field-ref"; // NON-NLS
	private static final String COMPARATOR_ATTR = "comparator"; // NON-NLS
	private static final String TAGS_ATTR = "tags"; // NON-NLS

	private static final String REQUIRED_VALUE = "required"; // NON-NLS

	private TNTInputStream currStream = null;
	private Collection<Map.Entry<String, String>> currProperties = null;
	private ActivityParser currParser = null;
	private ActivityField currField = null;
	private ActivityFieldLocator currLocator = null;

	private boolean currFieldHasLocValAttr = false;
	private boolean currFieldHasLocElmt = false;
	private boolean currFieldHasMapElmt = false;

	private StreamsConfigData streamsConfigData = null;

	private Locator currParseLocation = null;

	private boolean processingTNT4JProperties = false;

	/**
	 * Constructs a new ConfigurationParserHandler.
	 */
	public ConfigParserHandler() {
	}

	/**
	 * Returns streams and parsers data loaded from configuration.
	 * 
	 * @return un-marshaled streams and parsers data
	 */
	public StreamsConfigData getStreamsConfigData() {
		return streamsConfigData;
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		currParseLocation = locator;
	}

	@Override
	public void startDocument() throws SAXException {
		currStream = null;
		currProperties = null;
		currParser = null;
		currField = null;
		currLocator = null;
		currFieldHasLocValAttr = false;
		currFieldHasLocElmt = false;
		currFieldHasMapElmt = false;
		processingTNT4JProperties = false;
		streamsConfigData = new StreamsConfigData();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (CONFIG_ROOT_ELMT.equals(qName) || CONFIG_ROOT_ELMT_OLD.equals(qName)) {
			if (streamsConfigData.isStreamsAvailable()) {
				throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
						"ConfigParserHandler.multiple.elements", qName), currParseLocation);
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
		} else if (FILTER_ELMT.equals(qName)) {
			processFilter(attributes);
		} else if (RULE_ELMT.equals(qName)) {
			processRule(attributes);
		} else if (STEP_ELMT.equals(qName)) {
			processStep(attributes);
		} else if (TNT4J_PROPERTIES_ELMT.equals(qName)) {
			processTNT4JProperties(attributes);
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.malformed.configuration", PARSER_ELMT), currParseLocation);
		}
		String name = null;
		String className = null;
		String tags = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				name = attValue;
			} else if (CLASS_ATTR.equals(attName)) {
				className = attValue;
			} else if (TAGS_ATTR.equals(attName)) {
				tags = attValue;
			}
		}
		if (StringUtils.isEmpty(name)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.missing.attribute", PARSER_ELMT, NAME_ATTR), currParseLocation);
		}
		if (StringUtils.isEmpty(className)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.missing.attribute", PARSER_ELMT, CLASS_ATTR), currParseLocation);
		}
		if (streamsConfigData.getParser(name) != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.duplicate.parser.definition", name), currParseLocation);
		}
		try {
			ClassLoader cl = getClass().getClassLoader();
			Class<?> streamClass = cl.loadClass(className);
			Object newStream = streamClass.newInstance();
			if (!(newStream instanceof ActivityParser)) {
				throw new SAXNotSupportedException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_CORE, "ConfigParserHandler.not.implement.interface",
						PARSER_ELMT, CLASS_ATTR, className, ActivityParser.class.getName(), getLocationInfo()));
			}
			currParser = (ActivityParser) newStream;
		} catch (ClassNotFoundException cnfe) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.failed.to.load", PARSER_ELMT, CLASS_ATTR, className, getLocationInfo()), cnfe);
		} catch (InstantiationException ie) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.failed.to.load", PARSER_ELMT, CLASS_ATTR, className, getLocationInfo()), ie);
		} catch (IllegalAccessException iae) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.failed.to.load", PARSER_ELMT, CLASS_ATTR, className, getLocationInfo()), iae);
		}
		if (currParser != null) {
			currParser.setName(name);
			currParser.setTags(tags);
			streamsConfigData.addParser(currParser);
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.malformed.configuration", FIELD_ELMT), currParseLocation);
		}
		if (currParser == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ConfigParserHandler.malformed.configuration2", FIELD_ELMT, PARSER_ELMT),
					currParseLocation);
		}
		currFieldHasLocValAttr = false;
		currFieldHasLocElmt = false;
		currFieldHasMapElmt = false;
		String field = null;
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
				field = attValue;
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
		if (StringUtils.isEmpty(field)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.missing.attribute", FIELD_ELMT, NAME_ATTR), currParseLocation);
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
			String[] locators = currParser.canHaveDelimitedLocators() ? locator.split(Pattern.quote(LOC_DELIM))
					: new String[] { locator };
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.malformed.configuration", FIELD_LOC_ELMT), currParseLocation);
		}
		if (currField == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ConfigParserHandler.malformed.configuration2", FIELD_LOC_ELMT, FIELD_ELMT),
					currParseLocation);
		}
		if (currFieldHasLocValAttr) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.element.has.both", FIELD_ELMT, LOCATOR_ATTR, VALUE_ATTR, FIELD_LOC_ELMT,
					getLocationInfo()));
		}
		if (currFieldHasMapElmt) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.element.has.both2", FIELD_ELMT, FIELD_LOC_ELMT, FIELD_MAP_ELMT,
					getLocationInfo()));
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
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ConfigParserHandler.must.contain", FIELD_LOC_ELMT, LOCATOR_ATTR, VALUE_ATTR),
					currParseLocation);
		}
		if (locator != null && value != null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ConfigParserHandler.cannot.contain", FIELD_LOC_ELMT, LOCATOR_ATTR, VALUE_ATTR),
					currParseLocation);
		}
		// make sure any fields that are required based on other fields are
		// specified
		if (ActivityFieldDataType.DateTime == dataType) {
			if (format == null) {
				throw new SAXParseException(
						StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
								"ConfigParserHandler.missing.attribute2", FIELD_LOC_ELMT, FORMAT_ATTR, dataType),
						currParseLocation);
			}
			// if (locale == null)
			// {
			//
			// }
		} else if (ActivityFieldDataType.Timestamp == dataType) {
			if (units == null) {
				throw new SAXParseException(
						StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
								"ConfigParserHandler.missing.attribute2", FIELD_LOC_ELMT, UNITS_ATTR, dataType),
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
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ConfigParserHandler.malformed.configuration3", FIELD_MAP_ELMT, FIELD_ELMT, FIELD_LOC_ELMT),
					currParseLocation);
		}
		if (currFieldHasLocElmt && currLocator == null) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.element.has.both2", FIELD_ELMT, FIELD_LOC_ELMT, FIELD_MAP_ELMT,
					getLocationInfo()));
		}
		String source = null;
		String target = null;
		String type = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (SOURCE_ATTR.equals(attName)) {
				source = attValue;
			} else if (TARGET_ATTR.equals(attName)) {
				target = attValue;
			} else if (TYPE_ATTR.equals(attName)) {
				type = attValue;
			}
		}
		if (source == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.missing.attribute", FIELD_MAP_ELMT, SOURCE_ATTR), currParseLocation);
		}
		if (target == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.missing.attribute", FIELD_MAP_ELMT, TARGET_ATTR), currParseLocation);
		}
		if (currLocator != null) {
			currLocator.addValueMap(source, target,
					StringUtils.isEmpty(type) ? null : ActivityFieldMappingType.valueOf(type));
		} else {
			currFieldHasMapElmt = true;
			List<ActivityFieldLocator> locators = currField.getLocators();
			if (locators != null) {
				for (ActivityFieldLocator loc : locators) {
					loc.addValueMap(source, target,
							StringUtils.isEmpty(type) ? null : ActivityFieldMappingType.valueOf(type));
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.malformed.configuration", STREAM_ELMT), currParseLocation);
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.missing.attribute", STREAM_ELMT, NAME_ATTR), currParseLocation);
		}
		if (StringUtils.isEmpty(className)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.missing.attribute", STREAM_ELMT, CLASS_ATTR), currParseLocation);
		}
		if (streamsConfigData.getStream(name) != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.duplicate", STREAM_ELMT, name), currParseLocation);
		}
		try {
			ClassLoader cl = getClass().getClassLoader();
			Class<?> streamClass = cl.loadClass(className);
			Object newStream = streamClass.newInstance();
			if (!(newStream instanceof TNTInputStream)) {
				throw new SAXNotSupportedException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_CORE, "ConfigParserHandler.not.extend.class", STREAM_ELMT,
						CLASS_ATTR, className, TNTInputStream.class.getName(), getLocationInfo()));
			}
			currStream = (TNTInputStream) newStream;
		} catch (ClassNotFoundException cnfe) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.failed.to.load", STREAM_ELMT, CLASS_ATTR, className, getLocationInfo()), cnfe);
		} catch (InstantiationException ie) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.failed.to.load", STREAM_ELMT, CLASS_ATTR, className, getLocationInfo()), ie);
		} catch (IllegalAccessException iae) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.failed.to.load", STREAM_ELMT, CLASS_ATTR, className, getLocationInfo()), iae);
		}

		currStream.setName(name);
		streamsConfigData.addStream(currStream);
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
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ConfigParserHandler.malformed.configuration3", PROPERTY_ELMT, STREAM_ELMT, PARSER_ELMT),
					currParseLocation);
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.missing.attribute", PROPERTY_ELMT, NAME_ATTR), currParseLocation);
		}
		if (value == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.missing.attribute", PROPERTY_ELMT, VALUE_ATTR), currParseLocation);
		}

		if (processingTNT4JProperties) {
			currStream.addTNT4JProperty(name, value);
		} else {
			if (currProperties == null) {
				currProperties = new ArrayList<Map.Entry<String, String>>();
			}
			currProperties.add(new AbstractMap.SimpleEntry<String, String>(name, value));
		}
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
		if (currField == null && currStream == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ConfigParserHandler.malformed.configuration3", PARSER_REF_ELMT, FIELD_ELMT, STREAM_ELMT),
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.missing.attribute", PARSER_REF_ELMT, NAME_ATTR), currParseLocation);
		}
		ActivityParser parser = streamsConfigData.getParser(parserName);
		if (parser == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.undefined.reference", PARSER_REF_ELMT, parserName), currParseLocation);
		}

		if (currField != null) {
			currField.addStackedParser(parser);
		} else {
			try {
				currStream.addParser(parser);
			} catch (IllegalStateException exc) {
				throw new SAXParseException(
						StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
								"ConfigParserHandler.could.not.add.stream.parser", currStream.getName(), parserName),
						currParseLocation, exc);
			}
		}
	}

	/**
	 * TODO
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error parsing element
	 */
	private void processFilter(Attributes attrs) throws SAXException {

	}

	/**
	 * TODO
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error parsing element
	 */
	private void processRule(Attributes attrs) throws SAXException {

	}

	/**
	 * TODO
	 *
	 * @param attrs
	 *            List of element attributes
	 * @throws SAXException
	 *             if error parsing element
	 */
	private void processStep(Attributes attrs) throws SAXException {

	}

	private void processTNT4JProperties(Attributes attrs) {
		processingTNT4JProperties = true;
	}

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
				if (CollectionUtils.isEmpty(currField.getLocators())) {
					throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
							"ConfigParserHandler.element.must.have", FIELD_ELMT, LOCATOR_ATTR, VALUE_ATTR,
							FIELD_LOC_ELMT, getLocationInfo()));
				}
				currParser.addField(currField);
				currField = null;
				currFieldHasLocValAttr = false;
				currFieldHasLocElmt = false;
				currFieldHasMapElmt = false;
			} else if (FIELD_LOC_ELMT.equals(qName)) {
				currLocator = null;
			} else if (TNT4J_PROPERTIES_ELMT.equals(qName)) {
				processingTNT4JProperties = false;
			}
		} catch (SAXException exc) {
			throw exc;
		} catch (Exception e) {
			SAXException se = new SAXException(e.getLocalizedMessage() + getLocationInfo());
			se.initCause(e);
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
			locInfo = StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"ConfigParserHandler.at.line", currParseLocation.getLineNumber());
		}
		return locInfo;
	}
}
