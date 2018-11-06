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

import java.util.ArrayList;
import java.util.List;

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
	private String method;
	private String username;
	private String password;
	private SchedulerData schedulerData;

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
	 * Sets request/command data. Request tag is set to {@code null}.
	 *
	 * @param request
	 *            request data
	 *
	 * @see #addRequest(String, String)
	 */
	public void addRequest(String request) {
		addRequest(request, null);
	}

	/**
	 * Sets request/command data and tag.
	 *
	 * @param request
	 *            request data
	 * @param tag
	 *            request tag
	 */
	public void addRequest(String request, String tag) {
		if (requests == null) {
			requests = new ArrayList<>();
		}

		requests.add(new WsRequest<>(request, tag));
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
}
