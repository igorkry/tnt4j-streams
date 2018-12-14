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

package com.jkoolcloud.tnt4j.streams.transform;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;

/**
 * Data value transformation using defined list of transformations. If more than one transformation defined,
 * transformations are applied sequentially where transformation input data is output of previous transformation.
 *
 * @param <V>
 *            the type of transformed data value
 * @param <T>
 *            the type of data after transformation
 *
 * @version $Revision: 1 $
 */
public class CascadedValueTransformation<V, T> extends AbstractValueTransformation<V, T> {

	private List<ValueTransformation<Object, Object>> transformations;

	/**
	 * Constructs a new CascadedValueTransformation.
	 *
	 * @param name
	 *            transformation name
	 */
	public CascadedValueTransformation(String name) {
		setName(name);
	}

	/**
	 * Adds data value transformation instance to transformations list.
	 *
	 * @param transformation
	 *            transformation to add
	 */
	public void addTransformation(ValueTransformation<Object, Object> transformation) {
		if (transformations == null) {
			transformations = new ArrayList<>();
		}

		transformations.add(transformation);
	}

	@Override
	@SuppressWarnings("unchecked")
	public T transform(V value, ActivityInfo ai) throws TransformationException {
		Object tValue = value;

		if (CollectionUtils.isNotEmpty(transformations)) {
			for (ValueTransformation<Object, Object> vt : transformations) {
				tValue = vt.transform(tValue, ai);
			}
		}

		return (T) tValue;
	}

	// @Override
	// public String toString() {
	//
	// }
}
