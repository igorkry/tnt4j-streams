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

import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import com.jkoolcloud.tnt4j.streams.configure.OutputProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigData;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldMappingType;
import com.jkoolcloud.tnt4j.streams.filters.AbstractExpressionFilter;
import com.jkoolcloud.tnt4j.streams.filters.DefaultValueFilter;
import com.jkoolcloud.tnt4j.streams.filters.StreamFiltersGroup;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser;
import com.jkoolcloud.tnt4j.streams.transform.AbstractScriptTransformation;
import com.jkoolcloud.tnt4j.streams.transform.ValueTransformation;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements the SAX DefaultHandler for parsing TNT4J-Streams configuration.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader
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
	private static final String FIELD_TRANSFORM_ELMT = "field-transform"; // NON-NLS
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
	// /**
	// * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	// */
	// private static final String RULE_ELMT = "rule"; // NON-NLS
	// /**
	// * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	// */
	// private static final String STEP_ELMT = "step"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String VALUE_ELMT = "value"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag {@value}.
	 */
	private static final String EXPRESSION_ELMT = "expression"; // NON-NLS
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
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	protected static final String NAME_ATTR = "name"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String CLASS_ATTR = "class"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String VALUE_ATTR = "value"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String LOC_TYPE_ATTR = "locator-type"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String LOCATOR_ATTR = "locator"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String VALUE_TYPE_ATTR = "value-type"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String SPLIT_ATTR = "split"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String SEPARATOR_ATTR = "separator"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String DATA_TYPE_ATTR = "datatype"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String RADIX_ATTR = "radix"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	protected static final String UNITS_ATTR = "units"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String FORMAT_ATTR = "format"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String LOCALE_ATTR = "locale"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String TIMEZONE_ATTR = "timezone"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String SOURCE_ATTR = "source";
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String TARGET_ATTR = "target"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String TYPE_ATTR = "type"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String TAGS_ATTR = "tags"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String REQUIRED_ATTR = "required"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String TRANSPARENT_ATTR = "transparent"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String ID_ATTR = "id"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String BEAN_REF_ATTR = "beanRef"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String LANG_ATTR = "lang"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String HANDLE_ATTR = "handle"; // NON-NLS
	/**
	 * Constant for name of TNT4J-Streams XML configuration tag attribute {@value}.
	 */
	private static final String EVALUATION_ATTR = "evaluation"; // NON-NLS

	private static final String CDATA = "<![CDATA[]]>"; // NON-NLS

	/**
	 * Currently configured TNT input stream.
	 */
	protected TNTInputStream<?, ?> currStream = null;
	private Map<String, String> currProperties = null;
	private ActivityParser currParser = null;
	private ActivityField currField = null;
	@SuppressWarnings("rawtypes")
	private StreamFiltersGroup currFilter = null;

	private boolean currFieldHasLocValAttr = false;
	// private boolean currFieldHasLocElmt = false;
	// private boolean currFieldHasMapElmt = false;

	private StreamsConfigData streamsConfigData = null;
	private Map<String, Object> javaObjectsMap = null;

	/**
	 * Configuration parsing locator.
	 */
	protected Locator currParseLocation = null;

	private boolean processingTNT4JProperties = false;
	private JavaObjectData javaObjectData = null;
	private Property currProperty = null;
	private FieldLocatorData currLocatorData = null;
	private FieldTransformData currTransform = null;
	private FilterValueData currFilterValue = null;
	private FilterExpressionData currFilterExpression = null;

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
		currFilter = null;
		currLocatorData = null;
		currFieldHasLocValAttr = false;
		// currFieldHasLocElmt = false;
		// currFieldHasMapElmt = false;
		processingTNT4JProperties = false;
		streamsConfigData = new StreamsConfigData();
		javaObjectsMap = new HashMap<>();
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
		} else if (VALUE_ELMT.equals(qName)) {
			processFilterValue(attributes);
		} else if (EXPRESSION_ELMT.equals(qName)) {
			processFilterExpression(attributes);
		} else if (TNT4J_PROPERTIES_ELMT.equals(qName)) {
			processTNT4JProperties(attributes);
		} else if (REF_ELMT.equals(qName)) {
			processReference(attributes);
		} else if (JAVA_OBJ_ELMT.equals(qName)) {
			processJavaObject(attributes);
		} else if (PARAM_ELMT.equals(qName)) {
			processParam(attributes);
		} else if (FIELD_TRANSFORM_ELMT.equals(qName)) {
			processFieldTransform(attributes);
		}
	}

	/**
	 * Processes a {@code <parser>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
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
			Object newParser = Utils.createInstance(className);
			if (!(newParser instanceof ActivityParser)) {
				throw new SAXNotSupportedException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "ConfigParserHandler.not.implement.interface",
						PARSER_ELMT, CLASS_ATTR, className, ActivityParser.class.getName(), getLocationInfo()));
			}
			currParser = (ActivityParser) newParser;
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
	 * Processes a {@code <field>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
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
		// currFieldHasLocElmt = false;
		// currFieldHasMapElmt = false;
		String field = null;
		ActivityFieldDataType dataType = null;
		String locatorType = null;
		String locator = null;
		String valueType = null;
		String separator = null;
		String units = null;
		String format = null;
		String locale = null;
		String timeZone = null;
		String value = null;
		int radix = 10;
		String reqVal = "";
		boolean transparent = false;
		boolean split = false;
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
			} else if (VALUE_TYPE_ATTR.equals(attName)) {
				valueType = attValue;
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
			} else if (REQUIRED_ATTR.equals(attName)) {
				reqVal = attValue;
			} else if (TRANSPARENT_ATTR.equals(attName)) {
				transparent = Boolean.parseBoolean(attValue);
			} else if (SPLIT_ATTR.equals(attName)) {
				split = Boolean.parseBoolean(attValue);
			}
		}
		// make sure required fields are present
		if (StringUtils.isEmpty(field)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", FIELD_ELMT, NAME_ATTR), currParseLocation);
		}

		ActivityFieldLocator afl;
		ActivityField af = new ActivityField(field);

		if (value != null) {
			currFieldHasLocValAttr = true;
			afl = new ActivityFieldLocator(value);
			afl.setRadix(radix);
			afl.setRequired(reqVal);
			if (dataType != null) {
				afl.setDataType(dataType);
			}
			if (StringUtils.isNotEmpty(units)) {
				afl.setUnits(units);
			}
			if (StringUtils.isNotEmpty(format)) {
				afl.setFormat(format, locale);
			}
			if (StringUtils.isNotEmpty(timeZone)) {
				afl.setTimeZone(timeZone);
			}
			af.addLocator(afl);
		} else if (StringUtils.isNotEmpty(locator)) {
			currFieldHasLocValAttr = true;
			String[] locators = currParser.canHaveDelimitedLocators() ? locator.split(Pattern.quote(LOC_DELIM))
					: new String[] { locator };
			for (String loc : locators) {
				if (StringUtils.isNotEmpty(loc)) {
					afl = new ActivityFieldLocator(locatorType, loc);
					afl.setRadix(radix);
					afl.setRequired(reqVal);
					if (dataType != null) {
						afl.setDataType(dataType);
					}
					if (StringUtils.isNotEmpty(units)) {
						afl.setUnits(units);
					}
					if (StringUtils.isNotEmpty(format)) {
						afl.setFormat(format, locale);
					}
					if (StringUtils.isNotEmpty(timeZone)) {
						afl.setTimeZone(timeZone);
					}
					af.addLocator(afl);
				}
			}
		} else if (StringUtils.isEmpty(locator)) {
			af.setGroupLocator(radix, reqVal, dataType, units, format, locale, timeZone);
		}

		if (separator != null) {
			af.setSeparator(separator);
		}
		if (StringUtils.isNotEmpty(valueType)) {
			af.setValueType(valueType);
		}
		af.setRequired(reqVal);
		af.setTransparent(transparent);
		af.setSplitCollection(split);
		currField = af;
	}

	/**
	 * Processes a {@code <field-locator>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processFieldLocator(Attributes attrs) throws SAXException {
		if (currLocatorData != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", FIELD_LOC_ELMT), currParseLocation);
		}
		if (currField == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", FIELD_LOC_ELMT, FIELD_ELMT),
					currParseLocation);
		}
		if (!currField.hasDynamicAttrs() && currFieldHasLocValAttr) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.element.has.both", FIELD_ELMT, LOCATOR_ATTR, VALUE_ATTR, FIELD_LOC_ELMT,
					getLocationInfo()));
		}
		// if (currFieldHasMapElmt) {
		// throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
		// "ConfigParserHandler.element.has.both2", FIELD_ELMT, FIELD_LOC_ELMT, FIELD_MAP_ELMT,
		// getLocationInfo()));
		// }

		currLocatorData = new FieldLocatorData();

		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (DATA_TYPE_ATTR.equals(attName)) {
				currLocatorData.dataType = ActivityFieldDataType.valueOf(attValue);
			} else if (LOC_TYPE_ATTR.equals(attName)) {
				currLocatorData.locatorType = attValue;
			} else if (LOCATOR_ATTR.equals(attName)) {
				currLocatorData.locator = attValue;
			} else if (RADIX_ATTR.equals(attName)) {
				currLocatorData.radix = Integer.parseInt(attValue);
			} else if (UNITS_ATTR.equals(attName)) {
				currLocatorData.units = attValue;
			} else if (FORMAT_ATTR.equals(attName)) {
				currLocatorData.format = attValue;
			} else if (LOCALE_ATTR.equals(attName)) {
				currLocatorData.locale = attValue;
			} else if (TIMEZONE_ATTR.equals(attName)) {
				currLocatorData.timeZone = attValue;
			} else if (VALUE_ATTR.equals(attName)) {
				currLocatorData.value = attValue;
			} else if (REQUIRED_ATTR.equals(attName)) {
				currLocatorData.reqVal = attValue;
			} else if (ID_ATTR.equals(attName)) {
				currLocatorData.id = attValue;
			}
		}

		if (currLocatorData.value != null && currLocatorData.value.isEmpty()) {
			currLocatorData.value = null;
		}

		// make sure any fields that are required based on other fields are
		// specified
		if (ActivityFieldDataType.DateTime == currLocatorData.dataType) {
			if (currLocatorData.format == null) {
				throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.missing.attribute2", FIELD_LOC_ELMT, FORMAT_ATTR,
						currLocatorData.dataType), currParseLocation);
			}
			// if (locale == null)
			// {
			//
			// }
		} else if (ActivityFieldDataType.Timestamp == currLocatorData.dataType) {
			if (currLocatorData.units == null) {
				throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.missing.attribute2", FIELD_LOC_ELMT, UNITS_ATTR, currLocatorData.dataType),
						currParseLocation);
			}
		}
		// currFieldHasLocElmt = true;

		elementData = new StringBuilder();
	}

	/**
	 * Processes a {@code <field-map>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processFieldMap(Attributes attrs) throws SAXException {
		if (currField == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration3", FIELD_MAP_ELMT, FIELD_ELMT, FIELD_LOC_ELMT),
					currParseLocation);
		}
		// if (currFieldHasLocElmt && currLocatorData == null) {
		// throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
		// "ConfigParserHandler.element.has.both2", FIELD_ELMT, FIELD_LOC_ELMT, FIELD_MAP_ELMT,
		// getLocationInfo()));
		// }
		if (CollectionUtils.isEmpty(currField.getLocators()) && currLocatorData == null) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.element.no.binding", FIELD_MAP_ELMT, FIELD_LOC_ELMT, getLocationInfo()));
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
		if (currLocatorData != null) {
			currLocatorData.valueMapItems.add(new FieldLocatorData.ValueMapData(source, target,
					StringUtils.isEmpty(type) ? null : ActivityFieldMappingType.valueOf(type)));
		} else {
			// currFieldHasMapElmt = true;
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
	 * Processes a {@code <field-transform>} element.
	 * 
	 * @param attrs
	 *            List of element attributes
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processFieldTransform(Attributes attrs) throws SAXException {
		if (currTransform != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", FIELD_TRANSFORM_ELMT), currParseLocation);
		}

		if (currField == null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration3", FIELD_TRANSFORM_ELMT, FIELD_ELMT, FIELD_LOC_ELMT),
					currParseLocation);
		}

		if (currTransform == null) {
			currTransform = new FieldTransformData();
		}

		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				currTransform.name = attValue;
			} else if (BEAN_REF_ATTR.equals(attName)) {
				currTransform.beanRef = attValue;
			} else if (LANG_ATTR.equals(attName)) {
				currTransform.scriptLang = attValue;
			}
		}

		if (StringUtils.isNoneEmpty(currTransform.beanRef, currTransform.scriptLang)) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.cannot.contain", FIELD_TRANSFORM_ELMT, BEAN_REF_ATTR, LANG_ATTR),
					currParseLocation);
		}

		handleFieldLocatorCDATA();

		elementData = new StringBuilder();
	}

	/**
	 * Processes a {@code <stream>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
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

		currStream.ensureOutputSet();
	}

	/**
	 * Processes a {@code <property>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
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
	 * Processes a {@code <parser-ref>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs element
	 */
	private void processParserRef(Attributes attrs) throws SAXException {
		if (currField == null && currStream == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration3", PARSER_REF_ELMT, FIELD_ELMT, STREAM_ELMT),
					currParseLocation);
		}
		String parserName = getRefObjectNameFromAttr(attrs);
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
	 * Processes a {@code <reference>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processReference(Attributes attrs) throws SAXException {
		if (currStream == null && currParser == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration3", REF_ELMT, STREAM_ELMT, PARSER_ELMT),
					currParseLocation);
		}

		if (currStream != null) {
			processStreamReference(attrs);
		}

		if (currParser != null) {
			processParserReference(attrs);
		}
	}

	/**
	 * Process parser {@code <reference>} element, i.e. pre-parser.
	 * 
	 * @param attrs
	 *            List of element attributes
	 * 
	 * @throws SAXParseException
	 *             if error occurs parsing element
	 */
	private void processParserReference(Attributes attrs) throws SAXParseException {
		String refObjName = getRefObjectNameFromAttr(attrs);
		if (StringUtils.isEmpty(refObjName)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", REF_ELMT, NAME_ATTR), currParseLocation);
		}
		Object refObject = findReference(refObjName);
		try {
			currParser.addReference(refObject);
		} catch (IllegalStateException exc) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.could.not.add.stream.reference", currStream.getName(), refObjName),
					currParseLocation, exc);
		}

	}

	/**
	 * Process stream {@code <reference>} element, i.e. parser, output, etc.
	 * 
	 * @param attrs
	 *            List of element attributes
	 * 
	 * @throws SAXParseException
	 *             if error occurs parsing element
	 */
	private void processStreamReference(Attributes attrs) throws SAXParseException {
		String refObjName = getRefObjectNameFromAttr(attrs);
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

	private String getRefObjectNameFromAttr(Attributes attrs) {
		String refObjName = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				refObjName = attValue;
			}
		}
		return refObjName;
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
	 * Processes a {@code <java-object>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
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
	 * Processes a {@code <param>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
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
	 * Processes a {@code <filter>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processFilter(Attributes attrs) throws SAXException {
		if (currFilter != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", FILTER_ELMT), currParseLocation);
		}

		if (currLocatorData == null && currField == null && currParser == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration3", FILTER_ELMT, FIELD_LOC_ELMT, FIELD_ELMT),
					currParseLocation);
		}

		String name = null;
		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (NAME_ATTR.equals(attName)) {
				name = attValue;
			}
		}

		// make sure required fields are present
		if (StringUtils.isEmpty(name)) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.missing.attribute", FILTER_ELMT, NAME_ATTR), currParseLocation);
		}

		if (currLocatorData != null || currField != null) {
			currFilter = new StreamFiltersGroup<>(name);
		} else if (currParser != null) {
			currFilter = new StreamFiltersGroup<>(name);
		}

		handleFieldLocatorCDATA();
	}

	/**
	 * Processes a {@code <value>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processFilterValue(Attributes attrs) throws SAXException {
		if (currFilterValue != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", VALUE_ELMT), currParseLocation);
		}

		if (currFilter == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", VALUE_ELMT, FILTER_ELMT),
					currParseLocation);
		}

		if (currFilterValue == null) {
			currFilterValue = new FilterValueData();
		}

		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (HANDLE_ATTR.equals(attName)) {
				currFilterValue.handle = attValue;
			} else if (EVALUATION_ATTR.equals(attName)) {
				currFilterValue.evaluation = attValue;
			} else if (FORMAT_ATTR.equals(attName)) {
				currFilterValue.format = attValue;
			}
		}

		elementData = new StringBuilder();
	}

	/**
	 * Processes a {@code <expression>} element.
	 *
	 * @param attrs
	 *            List of element attributes
	 *
	 * @throws SAXException
	 *             if error occurs parsing element
	 */
	private void processFilterExpression(Attributes attrs) throws SAXException {
		if (currFilterExpression != null) {
			throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.malformed.configuration", EXPRESSION_ELMT), currParseLocation);
		}

		if (currFilter == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", EXPRESSION_ELMT, FILTER_ELMT),
					currParseLocation);
		}

		if (currFilterExpression == null) {
			currFilterExpression = new FilterExpressionData();
		}

		for (int i = 0; i < attrs.getLength(); i++) {
			String attName = attrs.getQName(i);
			String attValue = attrs.getValue(i);
			if (HANDLE_ATTR.equals(attName)) {
				currFilterExpression.handle = attValue;
			} else if (LANG_ATTR.equals(attName)) {
				currFilterExpression.lang = attValue;
			}
		}

		elementData = new StringBuilder();
	}

	private void processTNT4JProperties(Attributes attrs) throws SAXException {
		if (currStream == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.malformed.configuration2", TNT4J_PROPERTIES_ELMT, STREAM_ELMT),
					currParseLocation);
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
	 * Returns string buffer contained string for current configuration element (token).
	 *
	 * @return configuration element (token) data string value, or {@code null} if no element data
	 */
	protected String getElementData() {
		return elementData == null ? null : elementData.toString().trim();
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		try {
			if (STREAM_ELMT.equals(qName)) {
				if (currProperties != null) {
					currStream.setProperties(currProperties.entrySet());
					currProperties.clear();
				}
				currStream = null;
			} else if (PARSER_ELMT.equals(qName)) {
				if (currProperties != null) {
					currParser.setProperties(currProperties.entrySet());
					currProperties.clear();
				}
				currParser = null;
			} else if (FIELD_ELMT.equals(qName)) {
				validateActivityField(currField);
				currParser.addField(currField);
				currField = null;
				currFieldHasLocValAttr = false;
				// currFieldHasLocElmt = false;
				// currFieldHasMapElmt = false;
			} else if (FIELD_LOC_ELMT.equals(qName)) {
				if (currLocatorData != null) {
					handleFieldLocator(currLocatorData);

					currLocatorData = null;
					elementData = null;
				}
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
			} else if (FIELD_TRANSFORM_ELMT.equals(qName)) {
				if (currTransform != null) {
					handleFieldTransform(currTransform);

					currTransform = null;
					elementData = null;
				}
			} else if (FILTER_ELMT.equals(qName)) {
				if (currFilter != null) {
					handleFilter(currFilter);

					currFilter = null;
				}
			} else if (VALUE_ELMT.equals(qName)) {
				if (currFilterValue != null) {
					handleFilterValue(currFilterValue);

					currFilterValue = null;
					elementData = null;
				}
			} else if (EXPRESSION_ELMT.equals(qName)) {
				if (currFilterExpression != null) {
					handleFilterExpression(currFilterExpression);

					currFilterExpression = null;
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

		if (processingTNT4JProperties) {
			Map.Entry<String, String> p = new AbstractMap.SimpleEntry<>(currProperty.name, currProperty.value);
			currStream.output().setProperty(OutputProperties.PROP_TNT4J_PROPERTY, p);
		} else {
			if (currProperties == null) {
				currProperties = new HashMap<>();
			}
			currProperties.put(currProperty.name, currProperty.value);
		}
	}

	private void handleFieldLocator(FieldLocatorData currLocatorData) throws SAXException {
		handleFieldLocatorCDATA();

		if (currLocatorData.locator != null && currLocatorData.locator.isEmpty()) {
			currLocatorData.locator = null;
		}

		// make sure common required fields are present
		if (currLocatorData.locator == null && currLocatorData.value == null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.must.contain", FIELD_LOC_ELMT, LOCATOR_ATTR, VALUE_ATTR),
					currParseLocation);
		}
		if (currLocatorData.locator != null && currLocatorData.value != null) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.cannot.contain", FIELD_LOC_ELMT, LOCATOR_ATTR, VALUE_ATTR),
					currParseLocation);
		}

		ActivityFieldLocator afl = currLocatorData.value != null ? new ActivityFieldLocator(currLocatorData.value)
				: new ActivityFieldLocator(currLocatorData.locatorType, currLocatorData.locator);
		afl.setRadix(currLocatorData.radix);
		afl.setRequired(currLocatorData.reqVal);
		afl.setId(currLocatorData.id);
		if (currLocatorData.format != null) {
			afl.setFormat(currLocatorData.format, currLocatorData.locale);
		}
		if (currLocatorData.dataType != null) {
			afl.setDataType(currLocatorData.dataType);
		}
		if (currLocatorData.units != null) {
			afl.setUnits(currLocatorData.units);
		}
		if (currLocatorData.timeZone != null) {
			afl.setTimeZone(currLocatorData.timeZone);
		}

		if (CollectionUtils.isNotEmpty(currLocatorData.valueMapItems)) {
			for (FieldLocatorData.ValueMapData vmd : currLocatorData.valueMapItems) {
				afl.addValueMap(vmd.source, vmd.target, vmd.mapTyp);
			}
		}

		if (CollectionUtils.isNotEmpty(currLocatorData.valueTransforms)) {
			for (ValueTransformation<Object, Object> vt : currLocatorData.valueTransforms) {
				afl.addTransformation(vt);
			}
		}

		if (currLocatorData.filter != null) {
			afl.setFilter(currLocatorData.filter);
		}

		currField.addLocator(afl);
	}

	private void handleFieldLocatorCDATA() throws SAXException {
		if (currLocatorData == null) {
			return;
		}

		String eDataVal = getElementData();

		if (StringUtils.isNotEmpty(eDataVal)) {
			if (StringUtils.isNotEmpty(currLocatorData.locator) && eDataVal.length() > 0) {
				throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.element.has.both3", FIELD_LOC_ELMT, LOCATOR_ATTR, getLocationInfo()));
			}

			currLocatorData.locator = eDataVal;
		}
	}

	@SuppressWarnings("unchecked")
	private void handleFieldTransform(FieldTransformData currTransformData) throws SAXException {
		String eDataVal = getElementData();

		if (eDataVal != null) {
			if (StringUtils.isNotEmpty(currTransformData.beanRef) && eDataVal.length() > 0) {
				throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.element.has.both3", FIELD_TRANSFORM_ELMT, BEAN_REF_ATTR,
						getLocationInfo()));
			} else if (currTransformData.scriptCode == null) {
				currTransformData.scriptCode = eDataVal;
			}
		}

		if (StringUtils.isEmpty(currTransformData.beanRef) && StringUtils.isEmpty(currTransformData.scriptCode)) {
			throw new SAXParseException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ConfigParserHandler.must.contain", FIELD_TRANSFORM_ELMT, BEAN_REF_ATTR, CDATA),
					currParseLocation);
		}

		ValueTransformation<Object, Object> transform;

		if (StringUtils.isNotEmpty(currTransformData.beanRef)) {
			Object tObj = javaObjectsMap.get(currTransformData.beanRef);

			if (tObj == null) {
				throw new SAXParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.undefined.reference", FIELD_TRANSFORM_ELMT, currTransformData.beanRef),
						currParseLocation);
			}

			if (tObj instanceof ValueTransformation) {
				transform = (ValueTransformation<Object, Object>) tObj;
			} else {
				throw new SAXNotSupportedException(
						StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
								"ConfigParserHandler.not.extend.class", FIELD_TRANSFORM_ELMT, BEAN_REF_ATTR,
								tObj.getClass().getName(), ValueTransformation.class.getName(), getLocationInfo()));
			}
		} else {
			transform = AbstractScriptTransformation.createScriptTransformation(currTransformData.name,
					currTransformData.scriptLang, currTransformData.scriptCode);
		}

		if (currLocatorData != null) {
			currLocatorData.valueTransforms.add(transform);
		} else {
			currField.addTransformation(transform);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleFilter(StreamFiltersGroup currFilter) throws SAXException {
		if (currLocatorData != null) {
			currLocatorData.filter = currFilter;
		} else if (currField != null) {
			currField.setFilter(currFilter);
		} else if (currParser != null) {
			((GenericActivityParser<?>) currParser).setActivityFilter(currFilter);
		}
	}

	@SuppressWarnings("unchecked")
	private void handleFilterValue(FilterValueData fvData) throws SAXException {
		String eDataVal = getElementData();

		if (eDataVal != null) {
			fvData.value = eDataVal;
		}

		currFilter.addFilter(new DefaultValueFilter(fvData.handle, fvData.evaluation, fvData.format, fvData.value));
	}

	@SuppressWarnings("unchecked")
	private void handleFilterExpression(FilterExpressionData feData) throws SAXException {
		String eDataVal = getElementData();

		if (eDataVal != null) {
			feData.expression = eDataVal;
		}

		if (currLocatorData != null || currField != null) {
			currFilter.addFilter(
					AbstractExpressionFilter.createExpressionFilter(feData.handle, feData.lang, feData.expression));
		} else if (currParser != null) {
			currFilter.addFilter(AbstractExpressionFilter.createActivityExpressionFilter(feData.handle, feData.lang,
					feData.expression));
		}
	}

	/**
	 * Gets a string representing the current line in the file being parsed. Used for error messages.
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

	private void validateActivityField(ActivityField aField) throws SAXException {
		if (CollectionUtils.isEmpty(aField.getLocators())) {
			throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ConfigParserHandler.element.must.have", FIELD_ELMT, LOCATOR_ATTR, VALUE_ATTR, FIELD_LOC_ELMT,
					getLocationInfo()));
		}

		List<String> dynamicLocators = new ArrayList<>();
		Utils.resolveCfgVariables(dynamicLocators, aField.getFieldTypeName(), aField.getValueType());

		for (String dLoc : dynamicLocators) {
			if (!aField.hasDynamicLocator(dLoc)) {
				throw new SAXException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ConfigParserHandler.element.ref.missing", FIELD_ELMT, FIELD_LOC_ELMT, dLoc,
						getLocationInfo()));
			}
		}
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

	private static class FieldLocatorData {
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
		String id;

		List<ValueMapData> valueMapItems;
		List<ValueTransformation<Object, Object>> valueTransforms;
		StreamFiltersGroup<Object> filter;

		private FieldLocatorData() {
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
			id = null;

			if (valueMapItems == null) {
				valueMapItems = new ArrayList<>();
			} else {
				valueMapItems.clear();
			}

			if (valueTransforms == null) {
				valueTransforms = new ArrayList<>();
			} else {
				valueTransforms.clear();
			}

			filter = null;
		}

		private static class ValueMapData {
			String source;
			String target;
			ActivityFieldMappingType mapTyp;

			private ValueMapData(String source, String target, ActivityFieldMappingType mapTyp) {
				this.source = source;
				this.target = target;
				this.mapTyp = mapTyp;
			}
		}
	}

	private static class JavaObjectData {
		private String name;
		private String className;
		private List<Object> args;
		private List<Class<?>> types;

		private void addArg(Object arg) {
			if (args == null) {
				args = new ArrayList<>();
			}

			args.add(arg);
		}

		private void addType(String typeClass) throws ClassNotFoundException {
			addType(Class.forName(typeClass));
		}

		private void addType(Class<?> typeClass) {
			if (types == null) {
				types = new ArrayList<>();
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

	private static class FieldTransformData {
		String name;
		String beanRef;
		String scriptLang;
		String scriptCode;

		private void reset() {
			name = "";
			beanRef = "";
			scriptLang = "";
			scriptCode = "";
		}
	}

	private static class FilterValueData {
		String handle;
		String evaluation;
		String format;
		String value;

		private void reset() {
			handle = "";
			evaluation = "";
			format = "";
			value = "";
		}
	}

	private static class FilterExpressionData {
		String handle;
		String lang;
		String expression;

		private void reset() {
			handle = "";
			lang = "";
			expression = "";
		}
	}
}
