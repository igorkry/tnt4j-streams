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

/**
 * Represents a specific activity field, containing the necessary information on
 * how to extract its value from the raw activity data.
 *
 * @version $Revision: 2 $
 */
public class ActivityField
{
//	privateClasses static final Logger logger = Logger.getLogger(ActivityField.class);

  private ActivityFieldType fieldType;
  private ArrayList<ActivityFieldLocator> locators;
  private String format;
  private String locale;
  private String separator = "";
  private String reqvalue = ""; /* string to allow no value */

  /**
   * Creates a new activity field entry.
   *
   * @param fieldType type of activity field
   *
   * @throws NullPointerException if field type is {@code null}
   */
  public ActivityField (ActivityFieldType fieldType)
  {
    if (fieldType == null)
    { throw new NullPointerException ("Activity field type cannot be null"); }
    this.fieldType = fieldType;
  }

  /**
   * Creates a new activity field entry.
   *
   * @param fieldType type of activity field
   */
  public ActivityField (ActivityFieldType fieldType, ActivityFieldDataType dataType)
  {
    this (fieldType);
    ActivityFieldLocator loc = new ActivityFieldLocator (ActivityFieldLocatorType.Index, "0");
    locators = new ArrayList<ActivityFieldLocator> (1);
    locators.add (loc);
  }

  /**
   * Indicates if the raw data value for this activity field must be converted to
   * a member or some enumeration type.
   *
   * @return {@code true} if value must be converted to an enumeration member,
   * {@code false} otherwise
   */
  public boolean isEnumeration ()
  {
    return fieldType.isEnumBasedValue ();
  }

  /**
   * Gets the type of this activity field.
   *
   * @return the activity field type
   */
  public ActivityFieldType getFieldType ()
  {
    return fieldType;
  }

  /**
   * @return the locators
   */
  public ArrayList<ActivityFieldLocator> getLocators ()
  {
    return locators;
  }

  /**
   * @param locator the locator to add
   */
  public void addLocator (ActivityFieldLocator locator)
  {
    if (locators == null)
    { locators = new ArrayList<ActivityFieldLocator> (); }
    locators.add (locator);
  }

  /**
   * Gets the string to insert between values when concatenating multiple
   * raw activity values into the converted value for this field.
   *
   * @return the string being used to separate raw values
   */
  public String getSeparator ()
  {
    return separator;
  }

  /**
   * Sets the string to insert between values when concatenating multiple
   * raw activity values into the converted value for this field.
   *
   * @param locatorSep the string to use to separate raw values
   */
  public void setSeparator (String locatorSep)
  {
    this.separator = locatorSep;
  }

  /**
   * <p>Gets the format string defining how to interpret the raw data field value.</p>
   * <p>Note: This is not applicable for all fields and will be ignored by those fields
   * to which it does not apply.</p>
   *
   * @return the format string for interpreting raw data value
   */
  public String getFormat ()
  {
    return format;
  }

  /**
   * <p>Sets the format string defining how to interpret the raw data field value.</p>
   * <p>Note: This is not applicable for all fields and will be ignored by those fields
   * to which it does not apply.</p>
   *
   * @param format the format string for interpreting raw data value
   */
  public void setFormat (String format)
  {
    this.format = format;
  }

  /**
   * <p>Gets the locale representation string used by formatter.</p>
   * <p>Note: This is not applicable for all fields and will be ignored by those fields
   * to which it does not apply.</p>
   *
   * @return the locale representation string used by formatter
   */
  public String getLocale ()
  {
    return locale;
  }

  /**
   * <p>Sets the locale representation string used by formatter.</p>
   * <p>Note: This is not applicable for all fields and will be ignored by those fields
   * to which it does not apply.</p>
   *
   * @param locale the locale representation string used by formatter
   */
  public void setLocale (String locale)
  {
    this.locale = locale;
  }

  /**
   * Gets the required flag  indicates where field is required or optional.
   */

  public String getRequired ()
  {
    return reqvalue;
  }

  /**
   * Sets the required flag  indicates where field is required or optional.
   *
   * @param reqvalue true/false string to use to separate raw values
   */
  public void setRequired (String reqvalue)
  {
    this.reqvalue = reqvalue;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals (Object obj)
  {
    if (obj == null)
    { return false; }
    if (!(obj instanceof ActivityField))
    { return false; }
    ActivityField other = (ActivityField) obj;
    return this.fieldType == other.fieldType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode ()
  {
    return fieldType.ordinal ();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString ()
  {
    return fieldType.toString ();
  }

  /**
   * Gets a string representation of this object for use in debugging, which
   * includes the value of each data member.
   *
   * @return debugging string representation
   */
  public String toDebugString ()
  {
    StringBuilder sb = new StringBuilder ();
    sb.append ("{fieldType='").append (getFieldType ()).append ("' ").append ("format='").append (getFormat ()).append ("' ").append (
        "locale='").append (getLocale ()).append ("' ").append ("separator='").append (getSeparator ()).append ("' ").append (
        "required='").append (getRequired ()).append ("'}");
    return sb.toString ();
  }
}
