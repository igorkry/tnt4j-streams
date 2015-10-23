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
 * Provides list of valid Resource Types.
 *
 * @version $Revision: 18 $
 */
public enum ResourceType
{
  UNKNOWN,
  WMQ_QMGR, WMQ_QUEUE, WMQ_CHANNEL,
  DATABASE, FILE,
  JMS_TOPIC, JMS_QUEUE,
  JAVA_RUNTIME, C_RUNTIME, DNET_RUNTIME, OS_RUNTIME,
  NETWORK, WEB_APP,
  WMQ_TOPIC,
  JOURNAL, PROGRAM, PARTITION, CORBA_OBJECT, ENQ_MODEL,
  MAP, PORTAL_APP, WEB_SERVICE, MSMQ_QUEUE, JMS_DESTINATION;

  private static final ResourceType[] ENUM_LIST = ResourceType.values ();

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
  public static ResourceType valueOf (int value)
  {
    if (value < 0 || value >= ENUM_LIST.length)
    {
      throw new IllegalArgumentException ("value '" + value + "' is not valid for enumeration ResourceType");
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
  public static ResourceType valueOf (Object value)
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
    throw new IllegalArgumentException ("Cannot convert object of type '" + value.getClass ().getName () + "' enum ResourceType");
  }
}
