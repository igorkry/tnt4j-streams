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
 * Defines scenario step providing request/command params and scheduler.
 * 
 * 
 * <p>
 * Java class for ScenarioStepType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ScenarioStepType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element name="schedule-cron" type="{}ScheduleCronType"/>
 *           &lt;element name="schedule-simple" type="{}ScheduleSimpleType"/>
 *         &lt;/choice>
 *         &lt;element name="request" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="url" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="method" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="username" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="password" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ScenarioStepType", propOrder = { "scheduleCron", "scheduleSimple", "request" })
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
public class ScenarioStepType {

	@XmlElement(name = "schedule-cron")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	protected ScheduleCronType scheduleCron;
	@XmlElement(name = "schedule-simple")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	protected ScheduleSimpleType scheduleSimple;
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	protected String request;
	@XmlAttribute(name = "name", required = true)
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	protected String name;
	@XmlAttribute(name = "url")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	protected String url;
	@XmlAttribute(name = "method")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	protected String method;
	@XmlAttribute(name = "username")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	protected String username;
	@XmlAttribute(name = "password")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	protected String password;

	public ScenarioStepType() {

	}

	public ScenarioStepType(String expression, String request, String name, String url, String method) {
		this.scheduleCron = new ScheduleCronType(expression);
		this.request = request;
		this.name = name;
		this.url = url;
		this.method = method;
	}

	public ScenarioStepType(int interval, UnitsTypes units, Integer repeatCount, String request, String name,
			String url, String method) {
		this.scheduleSimple = new ScheduleSimpleType(interval, units, repeatCount);
		this.request = request;
		this.name = name;
		this.url = url;
		this.method = method;
	}

	public ScenarioStepType(int interval, UnitsTypes units, Integer repeatCount, String request, String name,
			String url, String method, String username, String password) {
		this.scheduleSimple = new ScheduleSimpleType(interval, units, repeatCount);
		this.request = request;
		this.name = name;
		this.url = url;
		this.method = method;
		this.username = username;
		this.password = password;
	}

	/**
	 * Gets the value of the scheduleCron property.
	 * 
	 * @return possible object is {@link ScheduleCronType }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	public ScheduleCronType getScheduleCron() {
		return scheduleCron;
	}

	/**
	 * Sets the value of the scheduleCron property.
	 * 
	 * @param value
	 *            allowed object is {@link ScheduleCronType }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	public void setScheduleCron(ScheduleCronType value) {
		this.scheduleCron = value;
	}

	/**
	 * Gets the value of the scheduleSimple property.
	 * 
	 * @return possible object is {@link ScheduleSimpleType }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	public ScheduleSimpleType getScheduleSimple() {
		return scheduleSimple;
	}

	/**
	 * Sets the value of the scheduleSimple property.
	 * 
	 * @param value
	 *            allowed object is {@link ScheduleSimpleType }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	public void setScheduleSimple(ScheduleSimpleType value) {
		this.scheduleSimple = value;
	}

	/**
	 * Gets the value of the request property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	public void setRequest(String value) {
		this.request = value;
	}

	/**
	 * Gets the value of the name property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
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
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	public void setName(String value) {
		this.name = value;
	}

	/**
	 * Gets the value of the url property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	public String getUrl() {
		return url;
	}

	/**
	 * Sets the value of the url property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	public void setUrl(String value) {
		this.url = value;
	}

	/**
	 * Gets the value of the method property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	public String getMethod() {
		return method;
	}

	/**
	 * Sets the value of the method property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	public void setMethod(String value) {
		this.method = value;
	}

	/**
	 * Gets the value of the username property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the value of the username property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	public void setUsername(String value) {
		this.username = value;
	}

	/**
	 * Gets the value of the password property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	public String getPassword() {
		return password;
	}

	/**
	 * Sets the value of the password property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-12-08T03:22:22+02:00", comments = "JAXB RI v2.2.4-2")
	public void setPassword(String value) {
		this.password = value;
	}

	public void setUserCredentials(String username, String password) {
		this.username = username;
		this.password = password;
	}

}
