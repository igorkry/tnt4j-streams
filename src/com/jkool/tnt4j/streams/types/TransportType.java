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

package com.jkool.tnt4j.streams.types;

/**
 * Provides list of valid Transport Types for messages.
 *
 * @version $Revision: 9 $
 */
public enum TransportType
{
  UNKNOWN, JMS, TIBCO, TIBCO_RV, WMQ, SQL, HTTP, HTTPS, SSL,
  TCP, UDP, SMTP, RMI, SOAP, FILE, DNET;

  private static final TransportType[] ENUM_LIST = TransportType.values ();

  /**
   * Converts the specified value to a member of the enumeration.
   *
   * @param value enumeration value to convert
   *
   * @return enumeration member
   *
   * @throws IllegalArgumentException if there is no
   *                                  member of the enumeration with the specified value
   */
  public static TransportType valueOf (int value)
  {
    if (value < 0 || value >= ENUM_LIST.length)
    {
      throw new IllegalArgumentException ("value '" + value + "' is not valid for enumeration TransportType");
    }
    return ENUM_LIST[value];
  }

  /**
   * Converts the specified object to a member of the enumeration.
   *
   * @param value object to convert
   *
   * @return enumeration member
   *
   * @throws IllegalArgumentException if value is {@code null} or object cannot be matched to a
   *                                  member of the enumeration
   */
  public static TransportType valueOf (Object value)
  {
    if (value == null)
    {
      throw new IllegalArgumentException ("object must be non-null");
    }
    if (value instanceof Number)
    {
      return valueOf (((Number) value).intValue ());
    }
    if (value instanceof String)
    {
      return valueOf (value.toString ());
    }
    throw new IllegalArgumentException ("Cannot convert object of type '" + value.getClass ().getName () + "' enum TransportType");
  }
}
