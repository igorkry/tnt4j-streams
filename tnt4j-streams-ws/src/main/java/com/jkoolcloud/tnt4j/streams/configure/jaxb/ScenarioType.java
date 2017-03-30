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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;
import javax.xml.bind.annotation.*;

/**
 * Java class for ScenarioType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ScenarioType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="step" type="{}ScenarioStepType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ScenarioType", propOrder = { "step" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
public class ScenarioType {

	@XmlElement(required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	protected List<ScenarioStepType> step;
	@XmlAttribute(name = "name", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	protected String name;

	/**
	 * Gets the value of the step property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the step property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getStep().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link ScenarioStepType }
	 * 
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	public List<ScenarioStepType> getStep() {
		if (step == null) {
			step = new ArrayList<ScenarioStepType>();
		}
		return this.step;
	}

	public void addStep(ScenarioStepType s) {
		getStep().add(s);
	}

	public void addStep(String expression, String request, String name, String url, String method) {
		getStep().add(new ScenarioStepType(expression, request, name, url, method));
	}

	public void addStep(int interval, UnitsTypes units, Integer repeatCount, String request, String name, String url,
			String method) {
		getStep().add(new ScenarioStepType(interval, units, repeatCount, request, name, url, method));
	}

	public void addStep(int interval, UnitsTypes units, Integer repeatCount, String request, String name, String url,
			String method, String username, String password) {
		getStep().add(
				new ScenarioStepType(interval, units, repeatCount, request, name, url, method, username, password));
	}

	/**
	 * Gets the value of the name property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	public void setName(String value) {
		this.name = value;
	}

}
