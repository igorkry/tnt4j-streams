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
 * Java class for FieldLocatorTypes.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="FieldLocatorTypes">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="StreamProp"/>
 *     &lt;enumeration value="Index"/>
 *     &lt;enumeration value="Label"/>
 *     &lt;enumeration value="REGroupNum"/>
 *     &lt;enumeration value="REMatchNum"/>
 *     &lt;enumeration value="REGroupName"/>
 *     &lt;enumeration value="Cache"/>
 *     &lt;enumeration value="Activity"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "FieldLocatorTypes")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-06-02T02:29:59+03:00", comments = "JAXB RI v2.2.4-2")
public enum FieldLocatorTypes {

	/**
	 * 
	 * Locator value is the value for the specified stream property.
	 * 
	 * 
	 */
	@XmlEnumValue("StreamProp")
	STREAM_PROP("StreamProp"),

	/**
	 * 
	 * Locator value is the value at the specified index/offset/position.
	 * 
	 * 
	 */
	@XmlEnumValue("Index")
	INDEX("Index"),

	/**
	 * 
	 * Locator value is the value for the specified label/expression (e.g. name/value pairs like label=value, XPath
	 * expression, etc.).
	 * 
	 * 
	 */
	@XmlEnumValue("Label")
	LABEL("Label"),

	/**
	 * 
	 * Locator value is the value for the specified regular expression group number.
	 * 
	 * 
	 */
	@XmlEnumValue("REGroupNum")
	RE_GROUP_NUM("REGroupNum"),

	/**
	 * 
	 * Locator value is the value for the specified regular expression match sequence position.
	 * 
	 * 
	 */
	@XmlEnumValue("REMatchNum")
	RE_MATCH_NUM("REMatchNum"),

	/**
	 * 
	 * Locator value is the value for the specified regular expression group name.
	 * 
	 * 
	 */
	@XmlEnumValue("REGroupName")
	RE_GROUP_NAME("REGroupName"),

	/**
	 * 
	 * Locator value is the value for stream stored cache key.
	 * 
	 * 
	 */
	@XmlEnumValue("Cache")
	CACHE("Cache"),

	/**
	 * 
	 * Locator value is the value for streamed activity entity field name.
	 * 
	 * 
	 */
	@XmlEnumValue("Activity")
	ACTIVITY("Activity");
	private final String value;

	FieldLocatorTypes(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static FieldLocatorTypes fromValue(String v) {
		for (FieldLocatorTypes c : FieldLocatorTypes.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
