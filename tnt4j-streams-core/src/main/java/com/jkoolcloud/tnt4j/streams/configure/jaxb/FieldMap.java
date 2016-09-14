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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Maps a raw value from the activity string to the required field value for jKool Cloud Service.
 * 
 * 
 * <p>
 * Java class for FieldMap complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FieldMap">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="source" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="target" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="type" type="{}FieldMapType" default="Value" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FieldMap")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-09T11:44:31+02:00", comments = "JAXB RI v2.2.4-2")
public class FieldMap {

	@XmlAttribute(name = "source", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-09T11:44:31+02:00", comments = "JAXB RI v2.2.4-2")
	protected String source;
	@XmlAttribute(name = "target", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-09T11:44:31+02:00", comments = "JAXB RI v2.2.4-2")
	protected String target;
	@XmlAttribute(name = "type")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-09T11:44:31+02:00", comments = "JAXB RI v2.2.4-2")
	protected FieldMapType type;

	public FieldMap() {
	}

	public FieldMap(String source, String target) {
		this(source, target, FieldMapType.VALUE);
	}

	public FieldMap(String source, String target, FieldMapType type) {
		this.source = source;
		this.target = target;
		this.type = type;
	}

	/**
	 * Gets the value of the source property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-09T11:44:31+02:00", comments = "JAXB RI v2.2.4-2")
	public String getSource() {
		return source;
	}

	/**
	 * Sets the value of the source property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-09T11:44:31+02:00", comments = "JAXB RI v2.2.4-2")
	public void setSource(String value) {
		this.source = value;
	}

	/**
	 * Gets the value of the target property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-09T11:44:31+02:00", comments = "JAXB RI v2.2.4-2")
	public String getTarget() {
		return target;
	}

	/**
	 * Sets the value of the target property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-09T11:44:31+02:00", comments = "JAXB RI v2.2.4-2")
	public void setTarget(String value) {
		this.target = value;
	}

	/**
	 * Gets the value of the type property.
	 * 
	 * @return possible object is {@link FieldMapType }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-09T11:44:31+02:00", comments = "JAXB RI v2.2.4-2")
	public FieldMapType getType() {
		if (type == null) {
			return FieldMapType.VALUE;
		} else {
			return type;
		}
	}

	/**
	 * Sets the value of the type property.
	 * 
	 * @param value
	 *            allowed object is {@link FieldMapType }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-09T11:44:31+02:00", comments = "JAXB RI v2.2.4-2")
	public void setType(FieldMapType value) {
		this.type = value;
	}

}
