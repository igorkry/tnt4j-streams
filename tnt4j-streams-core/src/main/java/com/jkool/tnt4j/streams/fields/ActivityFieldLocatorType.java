/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jkool.tnt4j.streams.fields;

/**
 * <p>
 * Lists the build-in raw activity field locator types.
 * <p>
 * Note: most parsers only support a single type of locator, so in many cases
 * the locator type is ignored, with the parser assuming that the locator
 * specification is a particular type. The types of locators supported is
 * parser-specific.
 *
 * @version $Revision: 1 $
 */
public enum ActivityFieldLocatorType {
	/**
	 * Indicates that raw data value is the value of a named property of the
	 * current stream.
	 */
	StreamProp,

	/**
	 * Indicates that raw data value is at a specified index location, offset,
	 * etc. This is a generic index/offset value whose interpretation is up to
	 * the specific parser applying the locator.
	 */
	Index,

	/**
	 * Indicates that raw data value is the value of a particular key or label.
	 * Examples of this are XPath expressions for XML elements, and where each
	 * element of a raw activity data string is a name/value pair.
	 */
	Label,

	/**
	 * Indicates that raw data value is the value of a specific regular
	 * expression group, for parsers that interpret the raw activity data using
	 * a regular expression pattern defined as a sequence of groups.
	 */
	REGroupNum,

	/**
	 * Indicates that raw data value is the value of a specific regular
	 * expression match, for parsers that interpret the raw activity data using
	 * a regular expression pattern defined as a sequence of repeating match
	 * patterns.
	 */
	REMatchNum
}
