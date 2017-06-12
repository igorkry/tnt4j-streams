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

package com.jkoolcloud.tnt4j.streams.filters;

/**
 * This interface defines common operations for data value filters used by TNT4J-Streams.
 *
 * @param <T>
 *            the type of filtered data
 * 
 * @version $Revision: 1 $
 */
public interface StreamEntityFilter<T> {

	/**
	 * Applies filtering operation for a provided data value and returns flag indicating whether it should be excluded
	 * from streaming.
	 * 
	 * @param value
	 *            data value to apply filter
	 * @return {@code true} if filter matching value should be excluded from streaming, {@code false} - otherwise
	 * @throws com.jkoolcloud.tnt4j.streams.filters.FilterException
	 *             if evaluation of filter fails
	 */
	boolean doFilter(T value) throws FilterException;
}
