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
package com.jkool.tnt4j.streams.configure.jaxb;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for FieldLocator.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="FieldLocator">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="StreamProp"/>
 *     &lt;enumeration value="Index"/>
 *     &lt;enumeration value="Label"/>
 *     &lt;enumeration value="REGroupNum"/>
 *     &lt;enumeration value="REMatchNum"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "FieldLocator")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-02-22T04:46:33+02:00", comments = "JAXB RI v2.2.4-2")
public enum FieldLocator {

	/**
	 * 
	 * Field value is the value for the specified stream property.
	 * 
	 * 
	 */
	@XmlEnumValue("StreamProp") STREAM_PROP("StreamProp"),

	/**
	 * 
	 * Field value is the value at the specified index/offset/position.
	 * 
	 * 
	 */
	@XmlEnumValue("Index") INDEX("Index"),

	/**
	 * 
	 * Field value is the value for the specified label (e.g. name/value pairs
	 * like label=value).
	 * 
	 * 
	 */
	@XmlEnumValue("Label") LABEL("Label"),

	/**
	 * 
	 * Field value is the value for the specified regular expression group.
	 * 
	 * 
	 */
	@XmlEnumValue("REGroupNum") RE_GROUP_NUM("REGroupNum"),

	/**
	 * 
	 * Field value is the value for the specified regular expression match
	 * sequence position.
	 * 
	 * 
	 */
	@XmlEnumValue("REMatchNum") RE_MATCH_NUM("REMatchNum");
	private final String value;

	FieldLocator(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static FieldLocator fromValue(String v) {
		for (FieldLocator c : FieldLocator.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
