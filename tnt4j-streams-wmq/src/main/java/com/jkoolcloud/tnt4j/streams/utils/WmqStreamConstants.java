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
 * TNT4J-Streams "Wmq" module constants.
 *
 * @version $Revision: 1 $
 */
public final class WmqStreamConstants {

	/**
	 * Resource bundle name constant for TNT4J-Streams "wmq" module.
	 */
	public static final String RESOURCE_BUNDLE_NAME = "tnt4j-streams-wmq"; // NON-NLS

	/**
	 * Custom PCF parameter identifier to store PCF message contained traces count.
	 */
	public static final int TRACES_COUNT = 919191919;
	/**
	 * Custom PCF parameter identifier to store processed PCF message trace entry index.
	 */
	public static final int TRACE_MARKER = 929292929;

	private WmqStreamConstants() {
	}
}
