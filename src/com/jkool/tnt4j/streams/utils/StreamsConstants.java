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

package com.jkool.tnt4j.streams.utils;

/**
 * TNT4J-Streams constants.
 *
 * @version $Revision: 1 $
 */
public final class StreamsConstants {

	/**
	 * The constant to indicate default map key for topic definition.
	 */
	public static final String TOPIC_KEY = "ActivityTopic"; // NON-NLS
	/**
	 * The constant to indicate default map key for activity data definition.
	 */
	public static final String ACTIVITY_DATA_KEY = "ActivityData"; // NON-NLS
	/**
	 * The constant to indicate default map key for activity transport
	 * definition.
	 */
	public static final String TRANSPORT_KEY = "ActivityTransport"; // NON-NLS

	/**
	 * The constant to indicate activity transport is Apache Kafka.
	 */
	public static final String TRANSPORT_KAFKA = "Kafka"; // NON-NLS
	/**
	 * The constant to indicate activity transport is MQTT.
	 */
	public static final String TRANSPORT_MQTT = "Mqtt"; // NON-NLS
	/**
	 * The constant to indicate activity transport is JMS.
	 */
	public static final String TRANSPORT_JMS = "JMS"; // NON-NLS
	/**
	 * The constant to indicate activity transport is HTTP.
	 */
	public static final String TRANSPORT_HTTP = "Http"; // NON-NLS

}
