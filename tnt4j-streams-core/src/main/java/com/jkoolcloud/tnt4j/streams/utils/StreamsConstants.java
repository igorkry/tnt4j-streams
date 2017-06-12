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

package com.jkoolcloud.tnt4j.streams.utils;

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
	 * The constant to indicate default map key for activity transport definition.
	 */
	public static final String TRANSPORT_KEY = "ActivityTransport"; // NON-NLS

	/**
	 * The constant to indicate activity transport is HTTP.
	 */
	public static final String TRANSPORT_HTTP = "Http"; // NON-NLS

	/**
	 * Default object identification path delimiter used by streams parsers.
	 */
	public static final String DEFAULT_PATH_DELIM = "."; // NON-NLS

	/**
	 * Constant for locator path node token meaning complete map.
	 */
	public static final String MAP_NODE_TOKEN = "*"; // NON-NLS

	private StreamsConstants() {
	}
}
