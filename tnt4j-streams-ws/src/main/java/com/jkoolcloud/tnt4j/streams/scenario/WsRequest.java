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
 * This class defines TNT4J-Streams-WS request data container.
 *
 * @param <T>
 *            type of request data
 *
 * @version $Revision: 2 $
 */
public class WsRequest<T> {
	private String tag;
	private T data;

	/**
	 * Constructs a new WsRequest. Defines request data and tag as {@code null}.
	 *
	 * @param requestData
	 *            request data string
	 */
	public WsRequest(T requestData) {
		this(requestData, null);
	}

	/**
	 * Constructs a new WsRequest. Defines request data and tag.
	 *
	 * @param requestData
	 *            request data string
	 * @param tag
	 *            request tag
	 */
	public WsRequest(T requestData, String tag) {
		this.data = requestData;
		this.tag = tag;
	}

	/**
	 * Returns tag string.
	 *
	 * @return tag string
	 */
	public String getTag() {
		return tag;
	}

	/**
	 * Returns data string.
	 *
	 * @return data string
	 */
	public T getData() {
		return data;
	}

	@Override
	public String toString() {
		return String.valueOf(data);
	}
}
