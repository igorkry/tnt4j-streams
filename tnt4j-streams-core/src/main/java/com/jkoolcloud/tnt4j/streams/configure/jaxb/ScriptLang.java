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
 * Java class for ScriptLang.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="ScriptLang">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="javascript"/>
 *     &lt;enumeration value="groovy"/>
 *     &lt;enumeration value="xpath"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ScriptLang")
@XmlEnum
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T11:54:46+02:00", comments = "JAXB RI v2.2.4-2")
public enum ScriptLang {

	/**
	 * 
	 * JavaScript language used to script.
	 * 
	 * 
	 */
	@XmlEnumValue("javascript")
	JAVASCRIPT("javascript"),

	/**
	 * 
	 * Groovy language used to script.
	 * 
	 * 
	 */
	@XmlEnumValue("groovy")
	GROOVY("groovy"),

	/**
	 * 
	 * XPath syntax and functions used to script.
	 * 
	 * 
	 */
	@XmlEnumValue("xpath")
	XPATH("xpath");
	private final String value;

	ScriptLang(String v) {
		value = v;
	}

	public String value() {
		return value;
	}

	public static ScriptLang fromValue(String v) {
		for (ScriptLang c : ScriptLang.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}

}
