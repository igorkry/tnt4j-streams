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

/**
 * Contains the set of supported activity fields.
 *
 * @version $Revision: 3 $
 */
public enum ActivityFieldType
{
  /**
   * Host name of server to associate with activity.
   */
  ServerName (false),

  /**
   * IP Address of server to associate with activity.
   */
  ServerIp (false),

  /**
   * String identifying information (e.g. type, version) about operating system associated with activity.
   */
  ServerOs (false),

  /**
   * Name of application associated with the activity.
   */
  ApplName (false),

  /**
   * Name of user associated with the activity.
   */
  UserName (false),

  /**
   * Name of resource manager owning resource associated with the activity.
   */
  ResourceMgr (false),

  /**
   * Type of resource manager owning resource associated with the activity
   * - Value must match values in {@link com.jkool.tnt4j.streams.types.ResourceManagerType} enumeration.
   */
  ResMgrType (true),

  /**
   * Name of resource associated with the activity.
   */
  Resource (false),

  /**
   * Type of Resource associated with the activity
   * - Value must match values in {@link com.jkool.tnt4j.streams.types.ResourceType} enumeration.
   */
  ResType (true),

  /**
   * Name to assign to activity entry.  Examples are operation, method, API call, event, etc.
   */
  ActivityName (false),

  /**
   * Type of activity
   * - Value must match values in {@link com.nastel.jkool.tnt4j.core.OpType} enumeration.
   */
  ActivityType (true),

  /**
   * Time action associated with activity started.
   */
  StartTime (false),

  /**
   * Time action associated with activity ended.
   */
  EndTime (false),

  /**
   * Elapsed time of the activity.
   */
  ElapsedTime (false),

  /**
   * Indicates completion status of the activity
   * - Value must match values in {@link com.nastel.jkool.tnt4j.core.OpCompCode} enumeration.
   */
  StatusCode (true),

  /**
   * Numeric reason/error code associated with the activity.
   */
  ReasonCode (false),

  /**
   * Error/exception message associated with the activity.
   */
  ErrorMsg (false),

  /**
   * Identifier used to uniquely identify the data associated with this activity.
   */
  Signature (false),

  /**
   * User-defined label to associate with the activity, generally for locating activity.
   */
  Tag (false),

  /**
   * Identifier used to correlate/relate activity entries to group them into logical entities.
   */
  Correlator (false),

  /**
   * User data to associate with the activity.
   */
  ActivityData (false),

  /**
   * User-defined value associated with the activity (e.g. monetary value).
   */
  Value (false),

  /**
   * Transport through which activity data flowed.
   */
  Transport (true),

  /**
   * Indicates completion status of the activity
   * - Value can either be label from {@link com.nastel.jkool.tnt4j.core.OpLevel} enumeration or a numeric value.
   */
  Severity (true),

  /**
   * Location that activity occurred at.
   */
  Location (false);

  private boolean enumValue;

  /**
   * Initializes properties of each enumeration member.
   *
   * @param isEnum {@code true} if converted value must be an enumeration member,
   *               {@code false} otherwise
   */
  ActivityFieldType (boolean isEnum)
  {
    enumValue = isEnum;
  }

  /**
   * Indicates whether the raw value for this field must be converted to an
   * enumeration member.
   *
   * @return {@code true} if converted value must be an enumeration member,
   * {@code false} otherwise
   */
  public boolean isEnumBasedValue ()
  {
    return enumValue;
  }
}
