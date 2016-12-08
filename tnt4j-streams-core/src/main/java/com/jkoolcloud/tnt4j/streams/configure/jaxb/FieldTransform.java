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
import javax.xml.bind.annotation.*;

/**
 * Transforms locator resolved field value by applying transformation bean defined rules.
 * 
 * 
 * <p>
 * Java class for FieldTransform complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FieldTransform">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="beanRef" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="lang" type="{}ScriptLang" default="javascript" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FieldTransform")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T11:54:46+02:00", comments = "JAXB RI v2.2.4-2")
public class FieldTransform {

	@XmlAttribute(name = "name")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T11:54:46+02:00", comments = "JAXB RI v2.2.4-2")
	protected String name;
	@XmlAttribute(name = "beanRef")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T11:54:46+02:00", comments = "JAXB RI v2.2.4-2")
	protected String beanRef;
	@XmlAttribute(name = "lang")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T11:54:46+02:00", comments = "JAXB RI v2.2.4-2")
	protected ScriptLang lang;
	@XmlValue
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T11:54:46+02:00", comments = "JAXB RI v2.2.4-2")
	protected String tScript;

	public FieldTransform() {

	}

	public FieldTransform(String name, String beanRef) {
		this.name = name;
		this.beanRef = beanRef;
	}

	public FieldTransform(String name, String lang, String tScript) {
		this.name = name;
		this.lang = ScriptLang.fromValue(lang);
		this.tScript = tScript;
	}

	/**
	 * Gets the value of the name property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T11:54:46+02:00", comments = "JAXB RI v2.2.4-2")
	public String getName() {
		return name;
	}

	/**
	 * Sets the value of the name property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T11:54:46+02:00", comments = "JAXB RI v2.2.4-2")
	public void setName(String value) {
		this.name = value;
	}

	/**
	 * Gets the value of the beanRef property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T11:54:46+02:00", comments = "JAXB RI v2.2.4-2")
	public String getBeanRef() {
		return beanRef;
	}

	/**
	 * Sets the value of the beanRef property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T11:54:46+02:00", comments = "JAXB RI v2.2.4-2")
	public void setBeanRef(String value) {
		this.beanRef = value;
	}

	/**
	 * Gets the value of the lang property.
	 * 
	 * @return possible object is {@link ScriptLang }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T11:54:46+02:00", comments = "JAXB RI v2.2.4-2")
	public ScriptLang getLang() {
		if (lang == null) {
			return ScriptLang.JAVASCRIPT;
		} else {
			return lang;
		}
	}

	/**
	 * Sets the value of the lang property.
	 * 
	 * @param value
	 *            allowed object is {@link ScriptLang }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T11:54:46+02:00", comments = "JAXB RI v2.2.4-2")
	public void setLang(ScriptLang value) {
		this.lang = value;
	}

	/**
	 * Gets the value of the tScript property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T11:54:46+02:00", comments = "JAXB RI v2.2.4-2")
	public String getTSctipt() {
		return tScript;
	}

	/**
	 * Sets the value of the tScript property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T11:54:46+02:00", comments = "JAXB RI v2.2.4-2")
	public void setTSctipt(String tScript) {
		this.tScript = tScript;
	}

}
