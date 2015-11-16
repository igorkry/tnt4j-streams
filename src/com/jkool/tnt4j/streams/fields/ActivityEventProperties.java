/*
 * Copyright (c) 2015 jKool, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * jKool, LLC. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with jKool, LLC.
 *
 * JKOOL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. JKOOL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 *
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
	EVENT_PROP_SERVER_NAME("sv_name"),

	/**
	 * Event property for activity server IP.
	 */
	EVENT_PROP_SERVER_IP("sv_ip"),

	/**
	 * Event property for activity application name.
	 */
	EVENT_PROP_APPL_NAME("ap_name"),

	/**
	 * Event property for activity custom user defined value.
	 */
	EVENT_PROP_MSG_VALUE("MsgValue");

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
