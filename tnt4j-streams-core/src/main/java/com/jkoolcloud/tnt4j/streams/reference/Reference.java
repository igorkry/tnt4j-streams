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

package com.jkoolcloud.tnt4j.streams.reference;

/**
 * This interface defines reference used between streaming entities, e.g. stream-parser, field-stacked parser, and so
 * on.
 *
 * @param <T>
 *            the type of referred object type
 *
 * @version $Revision: 1 $
 */
public interface Reference<T> {
	/**
	 * Returns referred object instance.
	 *
	 * @return referred object instance
	 */
	T getReferent();
}
