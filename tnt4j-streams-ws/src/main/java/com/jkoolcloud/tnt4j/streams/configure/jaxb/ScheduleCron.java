/*
 * Copyright 2014-2018 JKOOL, LLC.
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
 * Defines Cron scheduler parameters.
 * 
 * 
 * <p>
 * Java class for ScheduleCron complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ScheduleCron">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="expression" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ScheduleCron")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T06:08:33+03:00", comments = "JAXB RI v2.2.4-2")
public class ScheduleCron {

	@XmlAttribute(name = "expression", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T06:08:33+03:00", comments = "JAXB RI v2.2.4-2")
	protected String expression;

	public ScheduleCron() {

	}

	public ScheduleCron(String expression) {
		this.expression = expression;
	}

	/**
	 * Gets the value of the expression property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T06:08:33+03:00", comments = "JAXB RI v2.2.4-2")
	public String getExpression() {
		return expression;
	}

	/**
	 * Sets the value of the expression property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T06:08:33+03:00", comments = "JAXB RI v2.2.4-2")
	public void setExpression(String value) {
		this.expression = value;
	}

}
