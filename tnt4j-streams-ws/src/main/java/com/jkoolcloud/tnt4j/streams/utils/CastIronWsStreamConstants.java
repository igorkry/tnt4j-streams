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
 * TNT4J-Streams "WS" module constants.
 *
 * @version $Revision: 1 $
 */
public class CastIronWsStreamConstants {
	/**
	 * Resource bundle name constant for TNT4J-Streams "ws" module.
	 */
	public static final String RESOURCE_BUNDLE_NAME = "tnt4j-streams-ws-ci"; // NON-NLS

	/**
	 * The constant to indicate activity transport is WS.
	 */
	public static final String PROP_SECURITY_RESPONSE_PARSER = "SecurityResponseParserTag"; // NON-NLS
	/**
	 * The constant to indicate activity transport is REST.
	 */

	public static final String PROP_TOKEN_KEY = "SecurityCachedTokenKey";

	public static final String PROP_LOGIN_URL = "LoginServiceURL";
	
	private CastIronWsStreamConstants() {
	}
}
