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

package com.jkool.tnt4j.streams.fields;

/**
 * Defines supported builtin activity event properties.
 *
 * @version $Revision: 1 $
 */
public enum ActivityEventProperties {

	/**
	 * Event property for activity server name.
	 */
	EVENT_PROP_SERVER_NAME("sv_name"), // NON-NLS

	/**
	 * Event property for activity server IP.
	 */
	EVENT_PROP_SERVER_IP("sv_ip"), // NON-NLS

	/**
	 * Event property for activity application name.
	 */
	EVENT_PROP_APPL_NAME("ap_name"); // NON-NLS

	private String key;

	ActivityEventProperties(String key) {
		this.key = key;
	}

	/**
	 * Returns property key value string.
	 *
	 * @return key string
	 */
	public String getKey() {
		return key;
	}
}
