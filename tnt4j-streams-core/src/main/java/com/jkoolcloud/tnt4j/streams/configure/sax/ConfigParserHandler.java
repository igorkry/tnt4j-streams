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

package com.jkoolcloud.tnt4j.streams.configure.sax;

import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import com.jkoolcloud.tnt4j.streams.configure.OutputProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigData;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldMappingType;
import com.jkoolcloud.tnt4j.streams.fields.DynamicNameActivityField;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements the SAX DefaultHandler for parsing TNT4J-Streams configuration.
 *
 * @version $Revision: 1 $
 *
 * @see StreamsConfigLoader
 * @see com.jkoolcloud.tnt4j.streams.configure.sax.StreamsConfigSAXParser
 */
public class ConfigParserHandler extends DefaultHandler {

	/**
	 * Constant for default location delimiter in configuration definition.
	 */
	public static final String LOC_DELIM = "|"; // NON-NLS

	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String CONFIG_ROOT_ELMT_OLD = "tw-direct-feed"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String CONFIG_ROOT_ELMT = "tnt-data-source"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String PARSER_ELMT = "parser"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	protected static final String STREAM_ELMT = "stream"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String PROPERTY_ELMT = "property"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String FIELD_ELMT = "field"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String FIELD_MAP_ELMT = "field-map"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String FIELD_LOC_ELMT = "field-locator"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String PARSER_REF_ELMT = "parser-ref"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String TNT4J_PROPERTIES_ELMT = "tnt4j-properties"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String FILTER_ELMT = "filter"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String RULE_ELMT = "rule"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String STEP_ELMT = "step"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String REF_ELMT = "reference"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String JAVA_OBJ_ELMT = "java-object"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String PARAM_ELMT = "param"; // NON-NLS

	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	protected static final String NAME_ATTR = "name"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	private static final String CLASS_ATTR = "class"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	private static final String VALUE_ATTR = "value"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	private static final String LOC_TYPE_ATTR = "locator-type"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	private static final String LOCATOR_ATTR = "locator"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	private static final String NAME_LOCATOR_ATTR = "nameLocator"; // NON-NLS
	
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	private static final String SEPARATOR_ATTR = "separator"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	private static final String DATA_TYPE_ATTR = "datatype"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	private static final String RADIX_ATTR = "radix"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	protected static final String UNITS_ATTR = "units"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	private static final String FORMAT_ATTR = "format"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	private static final String LOCALE_ATTR = "locale"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	private static final String TIMEZONE_ATTR = "timezone"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	private static final String SOURCE_ATTR = "source";
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	private static final String TARGET_ATTR = "target"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	private static final String TYPE_ATTR = "type"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute
	 * {@value}.
	 */
	private static final String TAGS_ATTR = "tags"; // NON-NLS

	private static final String REQUIRED_VALUE = "required"; // NON-NLS

	/**
	 * Currently configured TNT input stream.
	 */
	protected TNTInputStream<?, ?> currStream = null;
	private Collection<Map.Entry<String, String>> currProperties = null;
	private ActivityParser currParser = null;
	private ActivityField currField = null;
	private ActivityFieldLocator currLocator = null;

	private boolean currFieldHasLocValAttr = false;
	private boolean currFieldHasLocElmt = false;
	private boolean currFieldHasMapElmt = false;

	private StreamsConfigData streamsConfigData = null;
	private Map<String, Object> javaObjectsMap = null;

	/**
	 * Configuration parsing locator.
	 */
	protected Locator currParseLocation = null;

	private boolean processingTNT4JProperties = false;
	private JavaObjectData javaObjectData = null;
	private Property currProperty = null;
	private FieldLocator currFieldLocator = null;

	/**
	 * Buffer to put current configuration element (token) data value
	 */
	protected StringBuilder elementData;

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
		javaObjectsMap = new HashMap<String, Object>();
	}

	@Override
	public void endDocument() throws SAXException {
		javaObjectsMap.clear();
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (CONFIG_ROOT_ELMT.equals(qName) || CONFIG_ROOT_ELMT_OLD.equals(qName)) {
			if (streamsConfigData.isStreamsAvailable()) {
				throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
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
		} else if (REF_ELMT.equals(qName)) {
			processReference(attributes);
		} else if (JAVA_OBJ_ELMT.equals(qName)) {
			processJavaObject(attributes);
		} else if (PARAM_ELMT.equals(qName)) {
			processParam(attributes);
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", PARSER_ELMT, NAME_ATTR), currParseLocation);
		}
		if (StringUtils.isEmpty(className)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", PARSER_ELMT, CLASS_ATTR), currParseLocation);
		}
		if (streamsConfigData.getParser(name) != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.duplicate.parser.definition", name), currParseLocation);
		}
		try {
			Object newStream = Utils.createInstance(className);
			if (!(newStream instanceof ActivityParser)) {
				throw new SAXNotSupportedException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "ConfigParserHandler.not.implement.interface",
						PARSER_ELMT, CLASS_ATTR, className, ActivityParser.class.getName(), getLocationInfo()));
			}
			currParser = (ActivityParser) newStream;
		} catch (Exception exc) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.failed.to.load", PARSER_ELMT, CLASS_ATTR, className, getLocationInfo()), exc);
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", FIELD_ELMT), currParseLocation);
		}
		if (currParser == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
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
		String nameLocator = null;
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
			} else if (NAME_LOCATOR_ATTR.equals(attName)) {
				nameLocator = attValue;
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
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
		if (nameLocator != null) {
			final String[] nameLocators = nameLocator.split(",");
			List<ActivityFieldLocator> locatorsCopy = new ArrayList<ActivityFieldLocator>(af.getLocators());
			af = new DynamicNameActivityField(field);
			((DynamicNameActivityField) af).addLocators(locatorsCopy);
			for (String nameLocatore : nameLocators) {
				afl = new ActivityFieldLocator(ActivityFieldLocatorType.Label, nameLocatore != null ? nameLocatore : "default");
				afl.setNameLocator(true);
				((DynamicNameActivityField) af).addNameLocator(afl);
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", FIELD_LOC_ELMT), currParseLocation);
		}
		if (currField == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", FIELD_LOC_ELMT, FIELD_ELMT),
					currParseLocation);
		}
		if (currFieldHasLocValAttr) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.element.has.both", FIELD_ELMT, LOCATOR_ATTR, VALUE_ATTR, FIELD_LOC_ELMT,
					getLocationInfo()));
		}
		if (currFieldHasMapElmt) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.element.has.both2", FIELD_ELMT, FIELD_LOC_ELMT, FIELD_MAP_ELMT,
					getLocationInfo()));
		}

		if (currFieldLocator == null) {
			currFieldLocator = new FieldLocator();
		} else {
			currFieldLocator.reset();
		}
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (DATA_TYPE_ATTR.equals(attName)) {
				currFieldLocator.dataType = ActivityFieldDataType.valueOf(attValue);
			} else if (LOC_TYPE_ATTR.equals(attName)) {
				currFieldLocator.locatorType = attValue;
			} else if (LOCATOR_ATTR.equals(attName)) {
				currFieldLocator.locator = attValue;
			} else if (RADIX_ATTR.equals(attName)) {
				currFieldLocator.radix = Integer.parseInt(attValue);
			} else if (UNITS_ATTR.equals(attName)) {
				currFieldLocator.units = attValue;
			} else if (FORMAT_ATTR.equals(attName)) {
				currFieldLocator.format = attValue;
			} else if (LOCALE_ATTR.equals(attName)) {
				currFieldLocator.locale = attValue;
			} else if (TIMEZONE_ATTR.equals(attName)) {
				currFieldLocator.timeZone = attValue;
			} else if (VALUE_ATTR.equals(attName)) {
				currFieldLocator.value = attValue;
			} else if (REQUIRED_VALUE.equals(attName)) {
				currFieldLocator.reqVal = attValue;
			}
		}

		if (currFieldLocator.value != null && currFieldLocator.value.isEmpty()) {
			currFieldLocator.value = null;
		}

		// make sure any fields that are required based on other fields are
		// specified
		if (ActivityFieldDataType.DateTime == currFieldLocator.dataType) {
			if (currFieldLocator.format == null) {
				throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.missing.attribute2", FIELD_LOC_ELMT, FORMAT_ATTR,
						currFieldLocator.dataType), currParseLocation);
			}
			// if (locale == null)
			// {
			//
			// }
		} else if (ActivityFieldDataType.Timestamp == currFieldLocator.dataType) {
			if (currFieldLocator.units == null) {
				throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.missing.attribute2", FIELD_LOC_ELMT, UNITS_ATTR,
						currFieldLocator.dataType), currParseLocation);
			}
		}
		currFieldHasLocElmt = true;

		elementData = new StringBuilder();
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
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration3", FIELD_MAP_ELMT, FIELD_ELMT, FIELD_LOC_ELMT),
					currParseLocation);
		}
		if (currFieldHasLocElmt && currLocator == null) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", FIELD_MAP_ELMT, SOURCE_ATTR), currParseLocation);
		}
		if (target == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", STREAM_ELMT, NAME_ATTR), currParseLocation);
		}
		if (StringUtils.isEmpty(className)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", STREAM_ELMT, CLASS_ATTR), currParseLocation);
		}
		if (streamsConfigData.getStream(name) != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.duplicate", STREAM_ELMT, name), currParseLocation);
		}
		try {
			Object newStream = Utils.createInstance(className);
			if (!(newStream instanceof TNTInputStream)) {
				throw new SAXNotSupportedException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "ConfigParserHandler.not.extend.class", STREAM_ELMT,
						CLASS_ATTR, className, TNTInputStream.class.getName(), getLocationInfo()));
			}
			currStream = (TNTInputStream<?, ?>) newStream;
		} catch (Exception exc) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.failed.to.load", STREAM_ELMT, CLASS_ATTR, className, getLocationInfo()), exc);
		}

		currStream.setName(name);
		streamsConfigData.addStream(currStream);

		if (currStream.getOutput() == null) {
			currStream.setDefaultStreamOutput();
		}
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
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration3", PROPERTY_ELMT, STREAM_ELMT, PARSER_ELMT),
					currParseLocation);
		}

		if (currProperty == null) {
			currProperty = new Property();
		} else {
			currProperty.reset();
		}
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				currProperty.name = attValue;
			} else if (VALUE_ATTR.equals(attName)) {
				currProperty.value = attValue;
			}
		}
		if (StringUtils.isEmpty(currProperty.name)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", PROPERTY_ELMT, NAME_ATTR), currParseLocation);
		}

		elementData = new StringBuilder();
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
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", PARSER_REF_ELMT, NAME_ATTR), currParseLocation);
		}
		ActivityParser parser = streamsConfigData.getParser(parserName);
		if (parser == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.undefined.reference", PARSER_REF_ELMT, parserName), currParseLocation);
		}

		if (currField != null) {
			currField.addStackedParser(parser);
		} else {
			try {
				currStream.addReference(parser);
			} catch (IllegalStateException exc) {
				throw new SAXParseException(
						StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
								"ConfigParserHandler.could.not.add.stream.parser", currStream.getName(), parserName),
						currParseLocation, exc);
			}
		}
	}

	/**
	 * Processes a reference element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error parsing element
	 */
	private void processReference(Attributes attrs) throws SAXException {
		if (currStream == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration2", REF_ELMT, STREAM_ELMT), currParseLocation);
		}
		String refObjName = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				refObjName = attValue;
			}
		}
		if (StringUtils.isEmpty(refObjName)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", REF_ELMT, NAME_ATTR), currParseLocation);
		}
		Object refObject = findReference(refObjName);
		if (refObject == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.undefined.reference", REF_ELMT, refObjName), currParseLocation);
		}

		try {
			currStream.addReference(refObject);
		} catch (IllegalStateException exc) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.could.not.add.stream.reference", currStream.getName(), refObjName),
					currParseLocation, exc);
		}
	}

	private Object findReference(String refName) {
		Object refObject = streamsConfigData == null ? null : streamsConfigData.getParser(refName);
		if (refObject == null) {
			refObject = streamsConfigData == null ? null : streamsConfigData.getStream(refName);
		}
		if (refObject == null) {
			refObject = javaObjectsMap == null ? null : javaObjectsMap.get(refName);
		}

		return refObject;
	}

	/**
	 * Processes a java-object element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error parsing element
	 */
	private void processJavaObject(Attributes attrs) throws SAXException {
		if (javaObjectData == null) {
			javaObjectData = new JavaObjectData();
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
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", JAVA_OBJ_ELMT, NAME_ATTR), currParseLocation);
		}
		if (StringUtils.isEmpty(className)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", JAVA_OBJ_ELMT, CLASS_ATTR), currParseLocation);
		}

		javaObjectData.name = name;
		javaObjectData.className = className;
	}

	/**
	 * Processes a param element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error parsing element
	 */
	private void processParam(Attributes attrs) throws SAXException {
		if (javaObjectData == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", PARAM_ELMT, JAVA_OBJ_ELMT),
					currParseLocation);
		}

		String name = null;
		String value = null;
		String type = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				name = attValue;
			} else if (VALUE_ATTR.equals(attName)) {
				value = attValue;
			} else if (TYPE_ATTR.equals(attName)) {
				type = attValue;
			}
		}

		if (StringUtils.isEmpty(name)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", PARAM_ELMT, NAME_ATTR), currParseLocation);
		}
		if (StringUtils.isEmpty(type)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", PARAM_ELMT, TYPE_ATTR), currParseLocation);
		}

		try {
			Object obj;
			if (StringUtils.isEmpty(value)) {
				obj = Utils.createInstance(type);
			} else {
				obj = javaObjectsMap.get(value);
				if (obj == null) {
					obj = Utils.createInstance(type, new Object[] { value }, String.class);
				}
			}
			javaObjectData.addArg(obj);
			javaObjectData.addType(type);
		} catch (Exception exc) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.could.not.init.obj.param", javaObjectData.name, name),
					currParseLocation, exc);
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

	private void processTNT4JProperties(Attributes attrs) throws SAXException {
		if (currStream == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", TNT4J_PROPERTIES_ELMT, STREAM_ELMT),
					currParseLocation);
		}

		if (currStream.getOutput() == null) {
			currStream.setDefaultStreamOutput();
		}

		processingTNT4JProperties = true;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {

		String cdata = new String(ch, start, length);

		if (this.elementData != null) {
			this.elementData.append(cdata);
		}
	}

	/**
	 * Returns string buffer contained string for current configuration element
	 * (token).
	 *
	 * @return configuration element (token) data string value, or {@code null}
	 *         if no element data
	 */
	protected String getElementData() {
		return elementData == null ? null : elementData.toString().trim();
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
					throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.element.must.have", FIELD_ELMT, LOCATOR_ATTR, VALUE_ATTR,
							FIELD_LOC_ELMT, getLocationInfo()));
				}
				currParser.addField(currField);
				currField = null;
				currFieldHasLocValAttr = false;
				currFieldHasLocElmt = false;
				currFieldHasMapElmt = false;
			} else if (FIELD_LOC_ELMT.equals(qName)) {
				if (currFieldLocator != null) {
					handleFieldLocator(currFieldLocator);

					currFieldLocator.reset();
					elementData = null;
				}

				currLocator = null;
			} else if (TNT4J_PROPERTIES_ELMT.equals(qName)) {
				processingTNT4JProperties = false;
			} else if (JAVA_OBJ_ELMT.equals(qName)) {
				if (javaObjectData != null) {
					handleJavaObject(javaObjectData);

					javaObjectData.reset();
				}
			} else if (PROPERTY_ELMT.equals(qName)) {
				if (currProperty != null) {
					handleProperty(currProperty);

					currProperty.reset();
					elementData = null;
				}
			}
		} catch (SAXException exc) {
			throw exc;
		} catch (Exception e) {
			throw new SAXException(e.getLocalizedMessage() + getLocationInfo(), e);
		}
	}

	private void handleJavaObject(JavaObjectData javaObjectData) throws Exception {
		if (javaObjectData == null) {
			return;
		}

		Object obj = Utils.createInstance(javaObjectData.className, javaObjectData.getArgs(),
				javaObjectData.getTypes());

		javaObjectsMap.put(javaObjectData.name, obj);
	}

	private void handleProperty(Property currProperty) throws SAXException {
		String eDataVal = getElementData();
		if (eDataVal != null) {
			if (currProperty.value != null && eDataVal.length() > 0) {
				throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.element.has.both3", PROPERTY_ELMT, VALUE_ATTR, getLocationInfo()));
			} else if (currProperty.value == null) {
				currProperty.value = eDataVal;
			}
		}

		if (currProperty.value == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", PROPERTY_ELMT, VALUE_ATTR), currParseLocation);
		}

		Map.Entry<String, String> p = new AbstractMap.SimpleEntry<String, String>(currProperty.name,
				currProperty.value);
		if (processingTNT4JProperties) {
			currStream.getOutput().setProperty(OutputProperties.PROP_TNT4J_PROPERTY, p);
		} else {
			if (currProperties == null) {
				currProperties = new ArrayList<Map.Entry<String, String>>();
			}
			currProperties.add(p);
		}
	}

	private void handleFieldLocator(FieldLocator currFieldLocator) throws SAXException {
		String eDataVal = getElementData();

		if (eDataVal != null) {
			if (currFieldLocator.locator != null && eDataVal.length() > 0) {
				throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.element.has.both3", FIELD_LOC_ELMT, LOCATOR_ATTR, getLocationInfo()));
			} else if (currFieldLocator.locator == null) {
				currFieldLocator.locator = eDataVal;
			}
		}

		if (currFieldLocator.locator != null && currFieldLocator.locator.isEmpty()) {
			currFieldLocator.locator = null;
		}

		// make sure common required fields are present
		if (currFieldLocator.locator == null && currFieldLocator.value == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.must.contain", FIELD_LOC_ELMT, LOCATOR_ATTR, VALUE_ATTR),
					currParseLocation);
		}
		if (currFieldLocator.locator != null && currFieldLocator.value != null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.cannot.contain", FIELD_LOC_ELMT, LOCATOR_ATTR, VALUE_ATTR),
					currParseLocation);
		}

		ActivityFieldLocator afl = currFieldLocator.value != null ? new ActivityFieldLocator(currFieldLocator.value)
				: new ActivityFieldLocator(currFieldLocator.locatorType, currFieldLocator.locator);
		afl.setRadix(currFieldLocator.radix);
		afl.setRequired(currFieldLocator.reqVal);
		if (currFieldLocator.format != null) {
			afl.setFormat(currFieldLocator.format, currFieldLocator.locale);
		}
		if (currFieldLocator.dataType != null) {
			afl.setDataType(currFieldLocator.dataType);
		}
		if (currFieldLocator.units != null) {
			afl.setUnits(currFieldLocator.units);
		}
		if (currFieldLocator.timeZone != null) {
			afl.setTimeZone(currFieldLocator.timeZone);
		}

		currLocator = afl;
		currField.addLocator(afl);
	}

	/**
	 * Gets a string representing the current line in the file being parsed.
	 * Used for error messages.
	 *
	 * @return string representing current line number being parsed
	 */
	protected String getLocationInfo() {
		String locInfo = "";
		if (currParseLocation != null) {
			locInfo = StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.at.line", currParseLocation.getLineNumber());
		}
		return locInfo;
	}

	private static class Property {
		private String name;
		private String value;

		private Property() {
			reset();
		}

		private void reset() {
			name = null;
			value = null;
		}
	}

	private static class FieldLocator {
		ActivityFieldDataType dataType;
		String locatorType;
		String locator;
		String units;
		String format;
		String locale;
		String timeZone;
		String value;
		int radix;
		String reqVal;

		private FieldLocator() {
			reset();
		}

		private void reset() {
			dataType = null;
			locatorType = null;
			locator = null;
			units = null;
			format = null;
			locale = null;
			timeZone = null;
			value = null;
			radix = 10;
			reqVal = ""; /* string to allow override */
		}
	}

	private static class JavaObjectData {
		private String name;
		private String className;
		private List<Object> args;
		private List<Class<?>> types;

		private void addArg(Object arg) {
			if (args == null) {
				args = new ArrayList<Object>();
			}

			args.add(arg);
		}

		private void addType(String typeClass) throws ClassNotFoundException {
			addType(Class.forName(typeClass));
		}

		private void addType(Class<?> typeClass) {
			if (types == null) {
				types = new ArrayList<Class<?>>();
			}

			types.add(typeClass);
		}

		private Object[] getArgs() {
			return args == null ? new Object[0] : args.toArray();
		}

		private Class<?>[] getTypes() {
			Class<?>[] typesArray = new Class[types == null ? 0 : types.size()];
			if (types != null) {
				typesArray = types.toArray(typesArray);
			}

			return typesArray;
		}

		private void reset() {
			name = "";
			className = "";
			if (args != null) {
				args.clear();
			}
			if (types != null) {
				types.clear();
			}
		}
	}
}
