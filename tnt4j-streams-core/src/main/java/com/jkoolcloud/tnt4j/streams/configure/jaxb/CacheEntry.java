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
import javax.xml.bind.annotation.*;

/**
 * 
 * Defines a stream cache entry key and values patterns.
 * 
 * 
 * <p>
 * Java class for CacheEntry complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CacheEntry">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="key">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *         &lt;element name="value">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CacheEntry", propOrder = { "key", "value" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T05:46:13+03:00", comments = "JAXB RI v2.2.4-2")
public class CacheEntry {

	@XmlElement(required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T05:46:13+03:00", comments = "JAXB RI v2.2.4-2")
	protected String key;
	@XmlElement(required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T05:46:13+03:00", comments = "JAXB RI v2.2.4-2")
	protected String value;
	@XmlAttribute(name = "id", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T05:46:13+03:00", comments = "JAXB RI v2.2.4-2")
	protected String id;

	public CacheEntry() {

	}

	public CacheEntry(String id, String key, String value) {
		this.id = id;
		this.key = key;
		this.value = value;
	}

	/**
	 * Gets the value of the key property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T05:46:13+03:00", comments = "JAXB RI v2.2.4-2")
	public String getKey() {
		return key;
	}

	/**
	 * Sets the value of the key property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T05:46:13+03:00", comments = "JAXB RI v2.2.4-2")
	public void setKey(String value) {
		this.key = value;
	}

	/**
	 * Gets the value of the value property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T05:46:13+03:00", comments = "JAXB RI v2.2.4-2")
	public String getValue() {
		return value;
	}

	/**
	 * Sets the value of the value property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T05:46:13+03:00", comments = "JAXB RI v2.2.4-2")
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Gets the value of the id property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T05:46:13+03:00", comments = "JAXB RI v2.2.4-2")
	public String getId() {
		return id;
	}

	/**
	 * Sets the value of the id property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-05-09T05:46:13+03:00", comments = "JAXB RI v2.2.4-2")
	public void setId(String value) {
		this.id = value;
	}

}
