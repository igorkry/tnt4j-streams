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

package com.jkool.tnt4j.streams.fields;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.jkool.tnt4j.streams.parsers.ActivityParser;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Represents a specific activity field, containing the necessary information on
 * how to extract its value from the raw activity data.
 *
 * @version $Revision: 2 $
 */
public class ActivityField {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityField.class);

	private final String fieldTypeName;
	private List<ActivityFieldLocator> locators = null;
	private String format = null;
	private String locale = null;
	private String separator = "";
	private String reqValue = ""; /* string to allow no value */
	private Collection<ActivityParser> stackedParsers;

	/**
	 * Creates a new activity field entry.
	 *
	 * @param fieldTypeName
	 *            name of activity field type
	 *
	 * @throws IllegalArgumentException
	 *             if field name is {@code null} or empty
	 */
	public ActivityField(String fieldTypeName) {
		if (StringUtils.isEmpty(fieldTypeName)) {
			throw new IllegalArgumentException(StreamsResources.getString("ActivityField.field.type.name.empty"));
		}
		this.fieldTypeName = fieldTypeName;
	}

	/**
	 * Creates a new activity field entry.
	 *
	 * @param fieldTypeName
	 *            name of activity field type
	 * @param dataType
	 *            type of field data type
	 *
	 * @throws NullPointerException
	 *             if field type is {@code null}
	 */
	public ActivityField(String fieldTypeName, ActivityFieldDataType dataType) {
		this(fieldTypeName);
		ActivityFieldLocator loc = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "0");
		locators = new ArrayList<ActivityFieldLocator>(1);
		locators.add(loc);
	}

	/**
	 * Indicates if the raw data value for this activity field must be converted
	 * to a member or some enumeration type.
	 *
	 * @return {@code true} if value must be converted to an enumeration member,
	 *         {@code false} otherwise
	 */
	public boolean isEnumeration() {
		StreamFieldType sft = getFieldType();

		return sft == null ? false : sft.getEnumerationClass() != null;
	}

	/**
	 * Gets the type of this activity field.
	 *
	 * @return the activity field type
	 */
	public StreamFieldType getFieldType() {
		try {
			StreamFieldType sft = StreamFieldType.valueOfIgnoreCase(fieldTypeName);
			return sft;
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
	 */
	public void addLocator(ActivityFieldLocator locator) {
		if (locators == null) {
			locators = new ArrayList<ActivityFieldLocator>();
		}
		locators.add(locator);
	}

	/**
	 * Gets the string to insert between values when concatenating multiple raw
	 * activity values into the converted value for this field.
	 *
	 * @return the string being used to separate raw values
	 */
	public String getSeparator() {
		return separator;
	}

	/**
	 * Sets the string to insert between values when concatenating multiple raw
	 * activity values into the converted value for this field.
	 *
	 * @param locatorSep
	 *            the string to use to separate raw values
	 */
	public void setSeparator(String locatorSep) {
		this.separator = locatorSep;
	}

	/**
	 * <p>
	 * Gets the format string defining how to interpret the raw data field
	 * value.
	 * </p>
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
	 * </p>
	 *
	 * @return the format string for interpreting raw data value
	 */
	public String getFormat() {
		return format;
	}

	/**
	 * <p>
	 * Sets the format string defining how to interpret the raw data field
	 * value.
	 * </p>
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
	 * </p>
	 *
	 * @param format
	 *            the format string for interpreting raw data value
	 */
	public void setFormat(String format) {
		this.format = format;
	}

	/**
	 * <p>
	 * Gets the locale representation string used by formatter.
	 * </p>
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
	 * </p>
	 *
	 * @return the locale representation string used by formatter
	 */
	public String getLocale() {
		return locale;
	}

	/**
	 * <p>
	 * Sets the locale representation string used by formatter.
	 * </p>
	 * <p>
	 * Note: This is not applicable for all fields and will be ignored by those
	 * fields to which it does not apply.
	 * </p>
	 *
	 * @param locale
	 *            the locale representation string used by formatter
	 */
	public void setLocale(String locale) {
		this.locale = locale;
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
	 *            {@code true}/{@code false} string to use to separate raw
	 *            values
	 */
	public void setRequired(String reqValue) {
		this.reqValue = reqValue;
	}

	/**
	 * Indicates whether some other object is "equal to" this field.
	 *
	 * @param obj
	 *            the reference object with which to compare.
	 *
	 * @return {@code true} if this field is the same as the obj argument;
	 *         {@code false} otherwise.
	 *
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ActivityField that = (ActivityField) o;

		return fieldTypeName.equals(that.fieldTypeName);
	}

	/**
	 * Returns hash code for this filed object.
	 *
	 * @return a hash code value for this field.
	 *
	 * @see Object#hashCode()
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
	 */
	public void addStackedParser(ActivityParser parser) {
		if (stackedParsers == null) {
			stackedParsers = new ArrayList<ActivityParser>();
		}

		stackedParsers.add(parser);
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
	 * Gets a string representation of this object for use in debugging, which
	 * includes the value of each data member.
	 *
	 * @return debugging string representation
	 */
	public String toDebugString() {
		final StringBuilder sb = new StringBuilder("ActivityField{"); // NON-NLS
		sb.append("fieldTypeName=").append(fieldTypeName); // NON-NLS
		sb.append(", format='").append(format).append('\''); // NON-NLS
		sb.append(", locale='").append(locale).append('\''); // NON-NLS
		sb.append(", separator='").append(separator).append('\''); // NON-NLS
		sb.append(", reqValue='").append(reqValue).append('\''); // NON-NLS
		sb.append(", stackedParsers='").append(stackedParsers).append('\''); // NON-NLS
		sb.append('}');
		return sb.toString();
	}
}
