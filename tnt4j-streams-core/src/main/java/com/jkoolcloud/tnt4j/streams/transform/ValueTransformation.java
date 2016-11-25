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

package com.jkoolcloud.tnt4j.streams.transform;

/**
 * This interface defines common operations for data value transformations used by TNT4J-Streams.
 *
 * @param <V>
 *            the type of transformed data value
 * @param <T>
 *            the type of data after transformation
 *
 * @version $Revision: 1 $
 */
public interface ValueTransformation<V, T> {
	/**
	 * Returns name of transformation.
	 *
	 * @return name of transformation
	 */
	String getName();

	/**
	 * Transforms provided data value applying some business rules.
	 *
	 * @param value
	 *            data value to transform
	 * @return transformed data value
	 *
	 * @throws TransformationException
	 *             if transformation operation fails
	 */
	T transform(V value) throws TransformationException;
}
