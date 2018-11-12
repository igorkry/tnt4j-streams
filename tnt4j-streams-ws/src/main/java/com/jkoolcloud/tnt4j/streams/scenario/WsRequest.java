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

import java.util.HashMap;
import java.util.Map;

import com.jkoolcloud.tnt4j.streams.utils.Utils;

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
	private Map<String, Parameter> parameters = new HashMap<>();

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

	/**
	 * Returns request (command/query/etc.) parameters map.
	 *
	 * @return request parameters map
	 */
	public Map<String, Parameter> getParameters() {
		return parameters;
	}

	/**
	 * Adds request (command/query/etc.) parameter.
	 *
	 * @param parameter
	 *            request parameter
	 */
	public void addParameter(Parameter parameter) {
		parameters.put(parameter.id, parameter);
	}

	/**
	 * Adds request (command/query/etc.) parameter.
	 *
	 * @param id
	 *            parameter identifier
	 * @param value
	 *            parameter value
	 */
	public void addParameter(String id, String value) {
		parameters.put(id, new Parameter(id, value));
	}

	/**
	 * Adds request (command/query/etc.) parameter.
	 *
	 * @param id
	 *            parameter identifier
	 * @param value
	 *            parameter value
	 * @param type
	 *            parameter type
	 */
	public void addParameter(String id, String value, String type) {
		parameters.put(id, new Parameter(id, value, type));
	}

	/**
	 * Class defining request parameter properties.
	 */
	public static class Parameter {
		private String id;
		private String value;
		private String type;

		/**
		 * Constructs a new Parameter. Defines parameter identifier and value.
		 *
		 * @param id
		 *            parameter identifier
		 * @param value
		 *            parameter value
		 */
		public Parameter(String id, String value) {
			this.id = id;
			this.value = value;
		}

		/**
		 * Constructs a new Parameter. Defines parameter identifier, value and type.
		 *
		 * @param id
		 *            parameter identifier
		 * @param value
		 *            parameter value
		 * @param type
		 *            parameter type
		 */
		public Parameter(String id, String value, String type) {
			this.id = id;
			this.value = value;
			this.type = type;
		}

		/**
		 * Returns parameter identifier.
		 *
		 * @return parameter identifier
		 */
		public String getId() {
			return id;
		}

		/**
		 * Returns parameter value.
		 *
		 * @return parameter value
		 */
		public String getValue() {
			return value;
		}

		/**
		 * Returns parameter type.
		 *
		 * @return parameter type
		 */
		public String getType() {
			return type;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("Parameter{"); // NON-NLS
			sb.append("id=").append(Utils.sQuote(id)); // NON-NLS
			sb.append(", value=").append(Utils.sQuote(value)); // NON-NLS
			sb.append(", type=").append(Utils.sQuote(type)); // NON-NLS
			sb.append('}'); // NON-NLS
			return sb.toString();
		}
	}
}
