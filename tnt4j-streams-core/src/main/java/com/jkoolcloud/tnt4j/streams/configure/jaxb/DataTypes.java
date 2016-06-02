/*
 * Copyright 2014-2016 JKOOL, LLC.
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
 * <p>
 * Java class for DataTypes.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="DataTypes">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="String"/>
 *     &lt;enumeration value="Binary"/>
 *     &lt;enumeration value="Number"/>
 *     &lt;enumeration value="DateTime"/>
 *     &lt;enumeration value="Timestamp"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "DataTypes")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-02-22T04:46:33+02:00", comments = "JAXB RI v2.2.4-2")
public enum DataTypes {

	/**
	 * 
	 * Field value is a character string.
	 * 
	 * 
	 */
	@XmlEnumValue("String") STRING("String"),

	/**
	 * 
	 * Field value is a generic sequence of bytes.
	 * 
	 * 
	 */
	@XmlEnumValue("Binary") BINARY("Binary"),

	/**
	 * 
	 * Field value is a numeric value.
	 * 
	 * 
	 */
	@XmlEnumValue("Number") NUMBER("Number"),

	/**
	 * 
	 * Field value is a date, time, or date/time expression with a specific
	 * format.
	 * 
	 * 
	 */
	@XmlEnumValue("DateTime") DATE_TIME("DateTime"),

	/**
	 * 
	 * Field value is a numeric value representing a date/time in the specified
	 * resolution.
	 * 
	 * 
	 */
	@XmlEnumValue("Timestamp") TIMESTAMP("Timestamp");
	private final String value;

	DataTypes(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static DataTypes fromValue(String v) {
		for (DataTypes c : DataTypes.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
