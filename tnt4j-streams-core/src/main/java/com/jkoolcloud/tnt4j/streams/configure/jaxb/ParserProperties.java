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

package com.jkoolcloud.tnt4j.streams.configure.jaxb;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for ParserProperties.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="ParserProperties">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Pattern"/>
 *     &lt;enumeration value="FieldDelim"/>
 *     &lt;enumeration value="ValueDelim"/>
 *     &lt;enumeration value="StripQuotes"/>
 *     &lt;enumeration value="SignatureDelim"/>
 *     &lt;enumeration value="RequireDefault"/>
 *     &lt;enumeration value="StripHeaders"/>
 *     &lt;enumeration value="Namespace"/>
 *     &lt;enumeration value="ReadLines"/>
 *     &lt;enumeration value="LocPathDelim"/>
 *     &lt;enumeration value="UseActivityDataAsMessageForUnset"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ParserProperties")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T05:46:13+03:00", comments = "JAXB RI v2.2.4-2")
public enum ParserProperties {

	/**
	 * 
	 * Property value is a regular expression pattern.
	 * 
	 * 
	 */
	@XmlEnumValue("Pattern")
	PATTERN("Pattern"),

	/**
	 * 
	 * Property value represents the delimiter between fields in raw activity data.
	 * 
	 * 
	 */
	@XmlEnumValue("FieldDelim")
	FIELD_DELIM("FieldDelim"),

	/**
	 * 
	 * Property value represents the delimiter between the label and value for a field in raw activity data.
	 * 
	 * 
	 */
	@XmlEnumValue("ValueDelim")
	VALUE_DELIM("ValueDelim"),

	/**
	 * 
	 * Property value is "true" to remove surrounding double quotes from data value, or "false" to keep them.
	 * 
	 * 
	 */
	@XmlEnumValue("StripQuotes")
	STRIP_QUOTES("StripQuotes"),

	/**
	 * 
	 * Property value is string identifying the delimiter between signature elements.
	 * 
	 * 
	 */
	@XmlEnumValue("SignatureDelim")
	SIGNATURE_DELIM("SignatureDelim"),

	/**
	 * 
	 * Property indicates that all attributes are required by default.
	 * 
	 * 
	 */
	@XmlEnumValue("RequireDefault")
	REQUIRE_DEFAULT("RequireDefault"),

	/**
	 * 
	 * Property identifies whether stream should strip RAW activity data (e.g., WMQ message) headers.
	 * 
	 * 
	 */
	@XmlEnumValue("StripHeaders")
	STRIP_HEADERS("StripHeaders"),

	/**
	 * 
	 * Property to define additional XML namespace mappings.
	 * 
	 * 
	 */
	@XmlEnumValue("Namespace")
	NAMESPACE("Namespace"),

	/**
	 * 
	 * Property indicates that complete activity RAW data (e.g., JSON) package is single line.
	 * 
	 * 
	 */
	@XmlEnumValue("ReadLines")
	READ_LINES("ReadLines"),

	/**
	 *
	 * Property to defile locator path delimiter for a nested structures (e.g., maps).
	 *
	 *
	 */
	@XmlEnumValue("LocPathDelim")
	LOC_PATH_DELIM("LocPathDelim"),

	/**
	 *
	 * Property indicates that activity RAW data shall be put into 'Message' field of activity entity if no custom
	 * mapping for that field is defined.
	 *
	 *
	 */
	@XmlEnumValue("UseActivityDataAsMessageForUnset")
	USE_ACTIVITY_DATA_AS_MESSAGE_FOR_UNSET("UseActivityDataAsMessageForUnset");
	private final String value;

	ParserProperties(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static ParserProperties fromValue(String v) {
		for (ParserProperties c : ParserProperties.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
