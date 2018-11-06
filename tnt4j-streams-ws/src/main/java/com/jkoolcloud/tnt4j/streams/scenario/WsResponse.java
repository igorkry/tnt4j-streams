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

/**
 * This class defines TNT4J-Streams-WS response data container.
 *
 * @param <T>
 *            type of response data
 *
 * @version $Revision: 2 $
 */
public class WsResponse<T> extends WsRequest<T> {

	/**
	 * Constructs a new WsResponse. Defines response data and tag as {@code null}.
	 *
	 * @param responseData
	 *            response data string
	 */
	public WsResponse(T responseData) {
		super(responseData);
	}

	/**
	 * Constructs a new WsResponse. Defines response data and tag.
	 *
	 * @param responseData
	 *            response data string
	 * @param tag
	 *            response tag
	 */
	public WsResponse(T responseData, String tag) {
		super(responseData, tag);
	}
}
