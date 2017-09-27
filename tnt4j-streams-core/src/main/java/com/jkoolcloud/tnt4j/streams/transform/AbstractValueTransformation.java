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

package com.jkoolcloud.tnt4j.streams.transform;

/**
 * Base class for abstract data value transformation.
 *
 * @param <V>
 *            the type of transformed data value
 * @param <T>
 *            the type of data after transformation
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractValueTransformation<V, T> implements ValueTransformation<V, T> {
	private String name;
	private Phase phase;

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets transformation name.
	 *
	 * @param name
	 *            transformation name
	 */
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Phase getPhase() {
		return phase;
	}

	@Override
	public void setPhase(Phase phase) {
		this.phase = phase;
	}

	@Override
	public String toString() {
		return name + "@" + phase; // NON-NLS
	}
}
