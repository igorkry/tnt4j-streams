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

package com.jkoolcloud.tnt4j.streams.preparsers;

/**
 * Base class for abstract activity RAW data value pre-parser.
 *
 * @param <V>
 *            the type of activity data to convert
 * @param <O>
 *            type of converted activity data
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractPreParser<V, O> implements ActivityDataPreParser<V, O> {

	private String name;

	protected AbstractPreParser() {
	}

	/**
	 * Sets pre-parser name.
	 *
	 * @param name
	 *            pre-parser name
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns pre-parser name.
	 *
	 * @return pre-parser name
	 */
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String dataTypeReturned() {
		return "OBJECT"; // NON-NLS
	}

	@Override
	public boolean isUsingParserForInput() {
		return false;
	}
}
