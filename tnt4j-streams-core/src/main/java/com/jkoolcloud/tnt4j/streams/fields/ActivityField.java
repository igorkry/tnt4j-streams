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

package com.jkoolcloud.tnt4j.streams.fields;

import java.util.*;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Represents a specific activity field, containing the necessary information on how to extract its value from the raw
 * activity data.
 *
 * @version $Revision: 1 $
 */
public class ActivityField {

	/**
	 * Activity field attribute dynamic value variable definition start token.
	 */
	private static final String FIELD_DYNAMIC_ATTR_START_TOKEN = "${"; // NON-NLS
	private static final String FIELD_DYNAMIC_ATTR_END_TOKEN = "}"; // NON-NLS

	private String fieldTypeName;
	private List<ActivityFieldLocator> locators = null;
	private String format = null;
	private String locale = null;
	private String separator = "";
	private String reqValue = ""; /* string to allow no value */
	private Collection<ActivityParser> stackedParsers;
	private boolean transparent = false;
	private boolean splitCollection = false;
	private String valueType = null;
	private Map<String, ActivityFieldLocator> dynamicAttrLocators = null;

	/**
	 * Constructs a new activity field entry.
	 *
	 * @param fieldTypeName
	 *            name of activity field type
	 * @throws IllegalArgumentException
	 *             if field type name is {@code null} or empty
	 */
	public ActivityField(String fieldTypeName) {
		if (StringUtils.isEmpty(fieldTypeName)) {
			throw new IllegalArgumentException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ActivityField.field.type.name.empty"));
		}
		this.fieldTypeName = fieldTypeName;
	}

	/**
	 * Constructs a new activity field entry.
	 *
	 * @param fieldTypeName
	 *            name of activity field type
	 * @param dataType
	 *            type of field data type
	 * @throws NullPointerException
	 *             if field type is {@code null}
	 */
	public ActivityField(String fieldTypeName, ActivityFieldDataType dataType) {
		this(fieldTypeName);
		ActivityFieldLocator loc = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "0");
		loc.setDataType(dataType);
		locators = new ArrayList<ActivityFieldLocator>(1);
		locators.add(loc);
	}

	/**
	 * Indicates if the raw data value for this activity field must be converted to a member or some enumeration type.
	 *
	 * @return {@code true} if value must be converted to an enumeration member, {@code false} otherwise
	 */
	public boolean isEnumeration() {
		StreamFieldType sft = getFieldType();

		return sft != null && sft.isEnumField();
	}

	/**
	 * Gets the type of this activity field.
	 *
	 * @return the activity field type
	 */
	public StreamFieldType getFieldType() {
		try {
			return StreamFieldType._valueOfIgnoreCase(fieldTypeName);
		} catch (IllegalArgumentException exc) {
		}

		return null;
	}

	/**
	 * Gets the type name of this activity field.
	 *
	 * @return the activity field type name
	 */
	public String getFieldTypeName() {
		return fieldTypeName;
	}

	/**
	 * Sets the type name of this activity field.
	 *
	 * @param fieldTypeName
	 *            the activity field type name
	 */
	public void setFieldTypeName(String fieldTypeName) {
		this.fieldTypeName = fieldTypeName;
	}

	/**
	 * Gets activity field locators list.
	 *
	 * @return the locators list
	 */
	public List<ActivityFieldLocator> getLocators() {
		return locators;
	}

	/**
	 * Adds activity field locator.
	 *
	 * @param locator
	 *            the locator to add
	 * @return instance of this activity field
	 */
	public ActivityField addLocator(ActivityFieldLocator locator) {
		if (locator != null) {
			boolean dynamic = false;
			if (StringUtils.isNotEmpty(locator.getId())) {
				String did = FIELD_DYNAMIC_ATTR_START_TOKEN + locator.getId() + FIELD_DYNAMIC_ATTR_END_TOKEN;

				if (fieldTypeName.contains(did) || (valueType != null && valueType.contains(did))) {
					addDynamicLocator(did, locator);
					dynamic = true;
				}
			}

			if (!dynamic) {
				addStaticLocator(locator);
			}
		}

		return this;
	}

	private void addDynamicLocator(String id, ActivityFieldLocator locator) {
		if (dynamicAttrLocators == null) {
			dynamicAttrLocators = new HashMap<String, ActivityFieldLocator>();
		}

		dynamicAttrLocators.put(id, locator);
	}

	private void addStaticLocator(ActivityFieldLocator locator) {
		if (locators == null) {
			locators = new ArrayList<ActivityFieldLocator>();
		}

		locators.add(locator);
	}

	/**
	 * Gets the string to insert between values when concatenating multiple raw activity values into the converted value
	 * for this field.
	 *
	 * @return the string being used to separate raw values
	 */
	public String getSeparator() {
		return separator;
	}

	/**
	 * Sets the string to insert between values when concatenating multiple raw activity values into the converted value
	 * for this field.
	 *
	 * @param locatorSep
	 *            the string to use to separate raw values
	 * @return instance of this activity field
	 */
	public ActivityField setSeparator(String locatorSep) {
		this.separator = locatorSep;

		return this;
	}

	/**
	 * <p>
	 * Gets the format string defining how to interpret the raw data field value.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 *
	 * @return the format string for interpreting raw data value
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * <p>
	 * Sets the format string defining how to interpret the raw data field value.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 *
	 * @param format
	 *            the format string for interpreting raw data value
	 * @return instance of this activity field
	 */
	public ActivityField setFormat(String format) {
		this.format = format;

		return this;
	}

	/**
	 * <p>
	 * Gets the locale representation string used by formatter.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 *
	 * @return the locale representation string used by formatter
	 */
	public String getLocale() {
		return locale;
	}

	/**
	 * <p>
	 * Sets the locale representation string used by formatter.
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those fields to which it does not apply.
	 *
	 * @param locale
	 *            the locale representation string used by formatter
	 * @return instance of this activity field
	 */
	public ActivityField setLocale(String locale) {
		this.locale = locale;

		return this;
	}

	/**
	 * Gets the required flag indicating whether field is required or optional.
	 *
	 * @return flag indicating whether field is required or optional
	 */
	public String getRequired() {
		return reqValue;
	}

	/**
	 * Sets the required flag indicates where field is required or optional.
	 *
	 * @param reqValue
	 *            string representing flag value
	 * @return instance of this activity field
	 */
	public ActivityField setRequired(String reqValue) {
		this.reqValue = reqValue;

		return this;
	}

	/**
	 * Gets field value type.
	 * 
	 * @return string representing field value type
	 */
	public String getValueType() {
		return valueType;
	}

	/**
	 * Sets field value type.
	 *
	 * @param valueType
	 *            string representing field value type
	 * @return instance of this activity field
	 */
	public ActivityField setValueType(String valueType) {
		this.valueType = valueType;

		return this;
	}

	/**
	 * Indicates whether some other object is "equal to" this field.
	 *
	 * @param obj
	 *            the reference object with which to compare.
	 *
	 * @return {@code true} if this field is the same as the obj argument, {@code false} otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;

		ActivityField that = (ActivityField) obj;

		return fieldTypeName.equals(that.fieldTypeName);
	}

	/**
	 * Returns hash code for this filed object.
	 *
	 * @return a hash code value for this field.
	 */
	@Override
	public int hashCode() {
		return fieldTypeName.hashCode();
	}

	/**
	 * Returns string representing activity field by field type.
	 *
	 * @return a string representing field.
	 */
	@Override
	public String toString() {
		return fieldTypeName;
	}

	/**
	 * Adds activity field stacked parser.
	 *
	 * @param parser
	 *            the stacked parser to add
	 * @return instance of this activity field
	 */
	public ActivityField addStackedParser(ActivityParser parser) {
		if (parser != null) {
			if (stackedParsers == null) {
				stackedParsers = new ArrayList<ActivityParser>();
			}

			stackedParsers.add(parser);
		}

		return this;
	}

	/**
	 * Gets activity field stacked parsers collection.
	 *
	 * @return stacked parsers collection
	 */
	public Collection<ActivityParser> getStackedParsers() {
		return stackedParsers;
	}

	/**
	 * Gets the transparent flag indicating whether field value has to be added to activity info data package.
	 *
	 * @return flag indicating whether field value has to be added to activity info data package
	 */
	public boolean isTransparent() {
		return transparent;
	}

	/**
	 * Sets the transparent flag indicating whether field value has to be added to activity info data package.
	 *
	 * @param transparent
	 *            flag indicating whether field value has to be added to activity info data package
	 * @return instance of this activity field
	 */
	public ActivityField setTransparent(boolean transparent) {
		this.transparent = transparent;

		return this;
	}

	/**
	 * Gets the splitCollection flag indicating whether resolved field value collection (list/array) has to be split
	 * into separate fields of activity info data package.
	 * 
	 * @return flag indicating whether resolved field value collection (list/array) has to be split into separate fields
	 *         of activity info data package
	 */
	public boolean isSplitCollection() {
		return splitCollection;
	}

	/**
	 * Sets the splitCollection flag indicating whether resolved field value collection (list/array) has to be split
	 * into separate fields of activity info data package.
	 *
	 * @param splitCollection
	 *            flag indicating whether resolved field value collection (list/array) has to be split into separate
	 *            fields of activity info data package
	 * @return instance of this activity field
	 */
	public ActivityField setSplitCollection(boolean splitCollection) {
		this.splitCollection = splitCollection;

		return this;
	}

	/**
	 * Gets activity field dynamic attribute values locators map.
	 *
	 * @return the dynamic attribute values locators map
	 */
	public Map<String, ActivityFieldLocator> getDynamicLocators() {
		return dynamicAttrLocators;
	}

	/**
	 * Checks if field has any dynamic locators defined.
	 *
	 * @return {@code true} if field has any dynamic locators defined, {@code false} - otherwise
	 */
	public boolean isDynamic() {
		return MapUtils.isNotEmpty(dynamicAttrLocators);
	}

	/**
	 * Checks if field attributes {@link #fieldTypeName} and {@link #valueType} values contains variable expressions.
	 *
	 * @return {@code true} if field attribute values contains variable expressions, {@code false} - otherwise
	 */
	public boolean hasDynamicAttrs() {
		return hasDynamicAttrs(fieldTypeName, valueType);
	}

	/**
	 * Checks if any of attribute values from set contains variable expression.
	 * 
	 * @param attrValues
	 *            set of attribute values to check
	 * @return {@code true} if value of any attribute from set contains variable expression, {@code false} - otherwise
	 */
	public static boolean hasDynamicAttrs(String... attrValues) {
		if (attrValues != null) {
			for (String attrValue : attrValues) {
				if (isDynamicAttr(attrValue)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Checks if attribute value string contains variable expression.
	 * 
	 * @param attrValue
	 *            attribute value
	 * @return {@code true} if attribute value string contains variable expression, {@code false} - otherwise
	 */
	public static boolean isDynamicAttr(String attrValue) {
		return attrValue != null && attrValue.contains(FIELD_DYNAMIC_ATTR_START_TOKEN);
	}

	/**
	 * Checks if dynamic locators map contains entry keyed by identifier variable.
	 * 
	 * @param dLocIdVar
	 *            dynamic locator identifier variable
	 * @return {@code true} if dynamic locators map contains entry keyed by identifier variable, {@code false} -
	 *         otherwise
	 */
	public boolean hasDynamicLocator(String dLocIdVar) {
		return dynamicAttrLocators != null && dynamicAttrLocators.containsKey(dLocIdVar);
	}

	/**
	 * Creates temporary field taking this field as template and filling dynamic attributes values with ones from
	 * dynamic locators resolved values map.
	 *
	 * @param dValues
	 *            dynamic locators resolved values map
	 * @param valueIndex
	 *            index of value in collection
	 * @return temporary field instance
	 */
	public ActivityField createTempField(Map<String, Object> dValues, int valueIndex) {
		ActivityField tField = new ActivityField(fillDynamicAttr(fieldTypeName, dValues, valueIndex));
		tField.locators = locators;
		tField.format = format;
		tField.locale = locale;
		tField.separator = separator;
		tField.reqValue = reqValue;
		tField.stackedParsers = stackedParsers;
		tField.valueType = fillDynamicAttr(valueType, dValues, valueIndex);

		return tField;
	}

	private static String fillDynamicAttr(String dAttr, Map<String, Object> dValMap, int valueIndex) {
		String tAttr = dAttr;

		if (isDynamicAttr(dAttr) && MapUtils.isNotEmpty(dValMap)) {
			List<String> vars = new ArrayList<String>();
			Utils.resolveVariables(vars, dAttr);

			for (String var : vars) {
				tAttr = tAttr.replace(var, String.valueOf(Utils.getItem(dValMap.get(var), valueIndex)));
			}
		}

		return tAttr;
	}
}
