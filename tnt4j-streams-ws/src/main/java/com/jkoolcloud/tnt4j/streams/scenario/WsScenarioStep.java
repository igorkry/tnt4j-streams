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

package com.jkoolcloud.tnt4j.streams.scenario;

import java.util.*;

import org.apache.commons.collections4.CollectionUtils;

/**
 * This class defines TNT4J-Streams-WS configuration scenario step.
 *
 * @version $Revision: 1 $
 */
public class WsScenarioStep {
	private String name;
	private String urlStr;
	private List<WsRequest<String>> requests;
	private Map<String, String> properties;
	private String method;
	private String username;
	private String password;
	private SchedulerData schedulerData;

	private WsScenario scenario;

	/**
	 * Constructs a new WsScenarioStep. Defines scenario step name.
	 *
	 * @param name
	 *            scenario step name
	 */
	public WsScenarioStep(String name) {
		this.name = name;
	}

	/**
	 * Returns scenario step name.
	 *
	 * @return scenario step name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns service URL string.
	 * <p>
	 * Used by {@link com.jkoolcloud.tnt4j.streams.inputs.RestStream} and
	 * {@link com.jkoolcloud.tnt4j.streams.inputs.WsStream}.
	 *
	 * @return service URL string.
	 */
	public String getUrlStr() {
		return urlStr;
	}

	/**
	 * Sets service URL string.
	 * <p>
	 * Used by {@link com.jkoolcloud.tnt4j.streams.inputs.RestStream} and
	 * {@link com.jkoolcloud.tnt4j.streams.inputs.WsStream}.
	 *
	 * @param urlStr
	 *            service URL string.
	 */
	public void setUrlStr(String urlStr) {
		this.urlStr = urlStr;
	}

	/**
	 * Returns requests/commands data.
	 *
	 * @return request data
	 */
	public List<WsRequest<String>> getRequests() {
		return requests;
	}

	/**
	 * Adds request/command data for this step. Request tag is set to {@code null}.
	 *
	 * @param request
	 *            request data
	 * @return constructed request instance
	 *
	 * @see #addRequest(String, String)
	 */
	public WsRequest<String> addRequest(String request) {
		return addRequest(request, null);
	}

	/**
	 * Adds request/command data and tag for this step.
	 *
	 * @param request
	 *            request data
	 * @param tag
	 *            request tag
	 * @return constructed request instance
	 */
	public WsRequest<String> addRequest(String request, String tag) {
		if (requests == null) {
			requests = new ArrayList<>();
		}

		WsRequest<String> req = new WsRequest<>(request, tag);
		requests.add(req);

		return req;
	}

	/**
	 * Checks if scenario step has no requests defined.
	 *
	 * @return flag indicating scenario has no requests defined
	 */
	public boolean isEmpty() {
		return CollectionUtils.isEmpty(requests);
	}

	/**
	 * Returns request invocation method name.
	 * <p>
	 * Used by {@link com.jkoolcloud.tnt4j.streams.inputs.RestStream}.
	 *
	 * @return request invocation method name
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Sets request invocation method name. It can be GET or POST.
	 * <p>
	 * Used by {@link com.jkoolcloud.tnt4j.streams.inputs.RestStream}.
	 *
	 * @param method
	 *            request invocation method name
	 */
	public void setMethod(String method) {
		this.method = method;
	}

	/**
	 * Returns request/command scheduler configuration data.
	 *
	 * @return scheduler configuration data
	 */
	public SchedulerData getSchedulerData() {
		return schedulerData;
	}

	/**
	 * Sets request/command scheduler configuration data.
	 *
	 * @param schedulerData
	 *            scheduler configuration data
	 */
	public void setSchedulerData(SchedulerData schedulerData) {
		this.schedulerData = schedulerData;
	}

	/**
	 * Sets user credentials (user name and password) used to perform request if service authentication is needed.
	 * <p>
	 * Used by {@link com.jkoolcloud.tnt4j.streams.inputs.RestStream}.
	 *
	 * @param username
	 *            user name used for authentication
	 * @param password
	 *            password used for authentication
	 */
	public void setCredentials(String username, String password) {
		this.username = username;
		this.password = password;
	}

	/**
	 * Returns user name used to perform request if service authentication is needed.
	 * <p>
	 * Used by {@link com.jkoolcloud.tnt4j.streams.inputs.RestStream}.
	 *
	 * @return user name used for authentication
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Returns password used to perform request if service authentication is needed.
	 * <p>
	 * Used by {@link com.jkoolcloud.tnt4j.streams.inputs.RestStream}.
	 *
	 * @return password used for authentication
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Sets scenario instance this step belongs to.
	 *
	 * @param scenario
	 *            scenario instance this step belongs to
	 */
	void setScenario(WsScenario scenario) {
		this.scenario = scenario;
	}

	/**
	 * Returns scenario instance this step belongs to.
	 * 
	 * @return scenario instance this step belongs to
	 */
	public WsScenario getScenario() {
		return scenario;
	}

	/**
	 * Searches step properties map for property having defined name and returns that property value. If step has no
	 * property with defined name - {@code null} is returned.
	 *
	 * @param propName
	 *            the property name
	 * @return the value of step property having defined name, or {@code null} is step has no property with defined name
	 */
	public String getProperty(String propName) {
		return properties == null ? null : properties.get(propName);
	}

	/**
	 * Sets property for this step.
	 * 
	 * @param name
	 *            property name
	 * @param value
	 *            property value
	 */
	public void setProperty(String name, String value) {
		if (properties == null) {
			properties = new HashMap<>();
		}

		properties.put(name, value);
	}

	/**
	 * Sets properties values map for this step.
	 *
	 * @param props
	 *            collection of properties to set for this step
	 */
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		if (properties == null) {
			properties = new HashMap<>();
		}

		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				properties.put(prop.getKey(), prop.getValue());
			}
		}
	}

}
