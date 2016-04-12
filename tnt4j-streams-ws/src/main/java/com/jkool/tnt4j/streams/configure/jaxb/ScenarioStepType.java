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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * 
 * Defines scenario step providing request/command params and CRON schedule
 * expression.
 * 
 * 
 * <p>
 * Java class for ScenarioStepType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="ScenarioStepType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="request" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="schedule" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="iterations" type="{http://www.w3.org/2001/XMLSchema}long" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ScenarioStepType")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
public class ScenarioStepType {

	@XmlAttribute(name = "name", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	protected String name;
	@XmlAttribute(name = "request", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	protected String request;
	@XmlAttribute(name = "schedule")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	protected String schedule;
	@XmlAttribute(name = "iterations")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	protected Long iterations;

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

	/**
	 * Gets the value of the request property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	public String getRequest() {
		return request;
	}

	/**
	 * Sets the value of the request property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	public void setRequest(String value) {
		this.request = value;
	}

	/**
	 * Gets the value of the schedule property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	public String getSchedule() {
		return schedule;
	}

	/**
	 * Sets the value of the schedule property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	public void setSchedule(String value) {
		this.schedule = value;
	}

	/**
	 * Gets the value of the iterations property.
	 * 
	 * @return possible object is {@link Long }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	public Long getIterations() {
		return iterations;
	}

	/**
	 * Sets the value of the iterations property.
	 * 
	 * @param value
	 *            allowed object is {@link Long }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-03-18T04:17:48+02:00", comments = "JAXB RI v2.2.4-2")
	public void setIterations(Long value) {
		this.iterations = value;
	}

}
