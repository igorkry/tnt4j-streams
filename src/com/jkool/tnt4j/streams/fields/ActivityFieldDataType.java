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
 * List the supported raw field value data types.
 *
 * @version $Revision: 1 $
 */
public enum ActivityFieldDataType
{
  /**
   * Raw data value is interpreted as a character string.
   */
  String,

  /**
   * Raw data value is interpreted as a numeric value,
   * optionally in a particular format.
   */
  Number,

  /**
   * Raw data value is interpreted as a sequence of bytes.
   */
  Binary,

  /**
   * Raw data value is interpreted as a date, time, or date/time expression,
   * optionally in a particular format.
   */
  DateTime,

  /**
   * Raw data value is interpreted as a numeric timestamp,
   * optionally in the specified units (assumed to be in milliseconds if not specified).
   */
  Timestamp
}
