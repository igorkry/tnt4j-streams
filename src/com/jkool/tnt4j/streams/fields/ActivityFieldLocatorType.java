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
 * <p>Lists the build-in raw activity field locator types.</p>
 * <p>Note: most parsers only support a single type of locator, so in many cases the
 * locator type is ignored, with the parser assuming that the locator specification
 * is a particular type.  The types of locators supported is parser-specific.</p>
 *
 * @version $Revision: 1 $
 */
public enum ActivityFieldLocatorType
{
  /**
   * Indicates that raw data value is the value of a named property of the current stream.
   */
  StreamProp,

  /**
   * Indicates that raw data value is at a specified index location, offset, etc.
   * This is a generic index/offset value whose interpretation is up to the specific
   * parser applying the locator.
   */
  Index,

  /**
   * Indicates that raw data value is the value of a particular key or label.  Examples
   * of this are XPath expressions for XML elements, and where each element of a raw
   * activity data string is a name/value pair.
   */
  Label,

  /**
   * Indicates that raw data value is the value of a specific regular expression group, for
   * parsers that interpret the raw activity data using a regular expression pattern defined
   * as a sequence of groups.
   */
  REGroupNum,

  /**
   * Indicates that raw data value is the value of a specific regular expression match, for
   * parsers that interpret the raw activity data using a regular expression pattern defined as
   * a sequence of repeating match patterns.
   */
  REMatchNum
}
