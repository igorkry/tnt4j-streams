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

package com.jkoolcloud.tnt4j.streams.utils;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Utility class to support TNT4J-Streams streamed data values caching.
 * 
 * @version $Revision: 1 $
 */
public class StreamsCache {
	private static final long DEFAULT_CACHE_MAX_SIZE = 100;
	private static final long DEFAULT_CACHE_EXPIRE_IN_MINUTES = 10;

	private static Cache<String, Object> valuesCache;

	private static Cache<String, Object> buildCache(long cSize, long duration) {
		return CacheBuilder.newBuilder().maximumSize(cSize).expireAfterAccess(duration, TimeUnit.MINUTES).build();
	}

	/**
	 * Initializes cache setting maximum cache size and cache entries expiration duration.
	 *
	 * @param cSize
	 *            maximum cache size
	 * @param duration
	 *            cache entries expiration duration
	 */
	public static void initCache(Integer cSize, Integer duration) {
		valuesCache = buildCache(cSize == null ? DEFAULT_CACHE_MAX_SIZE : cSize,
				duration == null ? DEFAULT_CACHE_EXPIRE_IN_MINUTES : duration);
	}

	/**
	 * Puts field value to cache.
	 *
	 * @param locKeyStr
	 *            locator string referencing cache entry key
	 * @param fieldValue
	 *            field value to cache
	 */
	public static void cacheValue(String locKeyStr, Object fieldValue) {
		if (valuesCache == null) {
			valuesCache = buildCache(DEFAULT_CACHE_MAX_SIZE, DEFAULT_CACHE_EXPIRE_IN_MINUTES);
		}

		valuesCache.put(locKeyStr, fieldValue);
	}

	/**
	 * Returns cached value.
	 * 
	 * @param locKeyStr
	 *            locator string referencing cache entry key
	 * @return cached value if it is available in cache, or {@code null} - otherwise
	 */
	public static Object getValue(String locKeyStr) {
		return valuesCache == null ? null : valuesCache.getIfPresent(locKeyStr);
	}

	/**
	 * Cleans cache contents.
	 */
	public static void cleanup() {
		if (valuesCache != null) {
			valuesCache.cleanUp();
		}
	}
}
