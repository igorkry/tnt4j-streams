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
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for StreamType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StreamType">
 *   &lt;complexContent>
 *     &lt;extension base="{}StreamType">
 *       &lt;redefine>
 *         &lt;complexType name="StreamType">
 *           &lt;complexContent>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *               &lt;sequence>
 *                 &lt;element name="property" type="{}StreamProperty" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;element name="parser-ref" type="{}Reference" maxOccurs="unbounded"/>
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
 *         &lt;element name="scenario" type="{}ScenarioType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StreamType", propOrder = { "scenario" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
public class StreamType extends OriginalStreamType {

	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	protected ScenarioType scenario;

	/**
	 * Gets the value of the scenario property.
	 * 
	 * @return possible object is {@link ScenarioType }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	public ScenarioType getScenario() {
		return scenario;
	}

	/**
	 * Sets the value of the scenario property.
	 * 
	 * @param value
	 *            allowed object is {@link ScenarioType }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	public void setScenario(ScenarioType value) {
		this.scenario = value;
	}

}
