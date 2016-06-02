/*
 * Copyright 2014-2016 JKOOL, LLC.
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

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.format.JSONFormatter;
import com.jkoolcloud.tnt4j.source.Source;

import net.minidev.json.JSONValue;

/**
 * @author akausinis
 * @version 1.0 TODO
 */
public class StreamRouteFormatter extends JSONFormatter {

	/**
	 * Creates a new StreamRouteFormatter without newlines during formatting
	 */
	public StreamRouteFormatter() {
		super();
	}

	/**
	 * Creates a new StreamRouteFormatter and conditionally format with newline
	 * 
	 * @param newLine
	 *            apply newline formatting to JSON
	 */
	public StreamRouteFormatter(boolean newLine) {
		super(newLine);
	}

	/**
	 * {@inheritDoc}
	 *
	 * If {@code o} is valid JSON then no additional formatting is performed.
	 */
	@Override
	public String format(Object o, Object... objects) {
		if (o instanceof String && isJsonValid((String) o)) {
			return String.valueOf(o);
		}

		return super.format(o, objects);
	}

	/**
	 * {@inheritDoc}
	 *
	 * If {@code msg} is valid JSON then no additional formatting is performed.
	 */
	@Override
	public String format(long ttl, Source source, OpLevel level, String msg, Object... args) {
		if (isJsonValid(msg)) {
			return msg;
		}

		return super.format(ttl, source, level, msg, args);
	}

	private boolean isJsonValid(String json) {
		boolean valid = JSONValue.isValidJson(json);

		return valid;
	}
}