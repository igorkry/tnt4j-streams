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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for Stream complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Stream">
 *   &lt;complexContent>
 *     &lt;extension base="{}Stream">
 *       &lt;redefine>
 *         &lt;complexType name="Stream">
 *           &lt;complexContent>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *               &lt;sequence>
 *                 &lt;element name="property" type="{}StreamProperty" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;element name="parser-ref" type="{}Reference" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;element name="reference" type="{}Reference" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;element name="tnt4j-properties" minOccurs="0">
 *                   &lt;complexType>
 *                     &lt;complexContent>
 *                       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                         &lt;sequence>
 *                           &lt;element name="property" type="{}Property" maxOccurs="unbounded"/>
 *                         &lt;/sequence>
 *                       &lt;/restriction>
 *                     &lt;/complexContent>
 *                   &lt;/complexType>
 *                 &lt;/element>
 *               &lt;/sequence>
 *               &lt;attGroup ref="{}EntityAttributeGroup"/>
 *             &lt;/restriction>
 *           &lt;/complexContent>
 *         &lt;/complexType>
 *       &lt;/redefine>
 *       &lt;sequence>
 *         &lt;element name="scenario" type="{}Scenario" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Stream", propOrder = { "scenario" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T06:08:33+03:00", comments = "JAXB RI v2.2.4-2")
public class Stream extends OriginalStream {

	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T06:08:33+03:00", comments = "JAXB RI v2.2.4-2")
	protected Scenario scenario;

	/**
	 * Gets the value of the scenario property.
	 * 
	 * @return possible object is {@link Scenario }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T06:08:33+03:00", comments = "JAXB RI v2.2.4-2")
	public Scenario getScenario() {
		return scenario;
	}

	/**
	 * Sets the value of the scenario property.
	 * 
	 * @param value
	 *            allowed object is {@link Scenario }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-25T06:08:33+03:00", comments = "JAXB RI v2.2.4-2")
	public void setScenario(Scenario value) {
		this.scenario = value;
	}

}
