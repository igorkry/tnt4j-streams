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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;

/**
 * Utility class to support TNT4J-Streams streamed data values caching.
 * <p>
 * Cache entries are defined using static or dynamic (i.e. patterns having field name variable to fill in data from
 * activity entity) values.
 *
 * @version $Revision: 2 $
 */
public class StreamsCache {
	private static final long DEFAULT_CACHE_MAX_SIZE = 100;
	private static final long DEFAULT_CACHE_EXPIRE_IN_MINUTES = 10;

	private static Cache<String, Object> valuesCache;
	private static Map<String, CacheEntry> cacheEntries = new HashMap<>();

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
	 * Fills in cache entries patterns with activity entity fields values and puts filled in entries to cache.
	 *
	 * @param ai
	 *            activity entity to be used to fill in patterns data
	 * @param parserName
	 *            parser name
	 */
	public static void cacheValues(ActivityInfo ai, String parserName) {
		if (valuesCache == null) {
			valuesCache = buildCache(DEFAULT_CACHE_MAX_SIZE, DEFAULT_CACHE_EXPIRE_IN_MINUTES);
		}

		for (CacheEntry cacheEntry : cacheEntries.values()) {
			String resolvedFieldKey = fillInKeyPattern(cacheEntry.getKey(), ai, parserName);
			Object resolvedFieldValue = fillInValuePattern(cacheEntry.getValue(), ai, parserName);

			if (resolvedFieldKey != null && resolvedFieldValue != null) {
				valuesCache.put(resolvedFieldKey, resolvedFieldValue);
			}
		}
	}

	private static String fillInKeyPattern(String pattern, ActivityInfo ai, String parserName) {
		List<String> vars = new ArrayList<>();
		Utils.resolveCfgVariables(vars, pattern);

		return Utils.fillInPattern(pattern, vars, ai, parserName);
	}

	private static Object fillInValuePattern(String pattern, ActivityInfo ai, String parserName) {
		List<String> vars = new ArrayList<>();
		Utils.resolveCfgVariables(vars, pattern);

		return vars.size() == 1 ? getActivityValue(vars.get(0), ai)
				: Utils.fillInPattern(pattern, vars, ai, parserName);
	}

	static Object getActivityValue(String var, ActivityInfo ai) {
		String varKey = var.substring(2, var.length() - 1);
		return ai.getFieldValue(varKey);
	}

	/**
	 * Resolves cache stored value identified by cache entry id.
	 *
	 * @param ai
	 *            activity entity to be used to fill in patterns data
	 * @param entryIdStr
	 *            cache entity pattern identifier string
	 * @param parserName
	 *            parser name
	 * @return resolved cached value or {@code null} if there is no such entry or data in cache
	 */
	public static Object getValue(ActivityInfo ai, String entryIdStr, String parserName) {
		CacheEntry cacheEntry = cacheEntries.get(entryIdStr);
		if (cacheEntry != null) {
			String cacheKey = fillInKeyPattern(cacheEntry.getKey(), ai, parserName);
			if (cacheKey != null) {
				return valuesCache == null ? null : valuesCache.getIfPresent(cacheKey);
			}
		}

		return null;
	}

	/**
	 * Cleans cache contents.
	 */
	public static void cleanup() {
		if (valuesCache != null) {
			valuesCache.cleanUp();
		}
		if (cacheEntries != null) {
			cacheEntries.clear();
		}
	}

	/**
	 * Adds cache entry pattern definition to cache entry patterns map.
	 *
	 * @param entryId
	 *            entry identifier
	 * @param key
	 *            entry key pattern
	 * @param value
	 *            entry value pattern
	 * @return previous cache entry instance stored
	 */
	public static CacheEntry addEntry(String entryId, String key, String value) {
		return cacheEntries.put(entryId, new CacheEntry(entryId, key, value));
	}

	/**
	 * Defines cache entry pattern.
	 */
	public static class CacheEntry {
		private String id;
		private String key;
		private String value;

		/**
		 * Constructs new CacheEntry.
		 *
		 * @param id
		 *            cache entry identifier
		 * @param key
		 *            cache entry key pattern
		 * @param value
		 *            cache entry value pattern
		 */
		private CacheEntry(String id, String key, String value) {
			this.id = id;
			this.key = key;
			this.value = value;
		}

		/**
		 * Returns cache entry identifier.
		 *
		 * @return entry identifier
		 */
		public String getId() {
			return id;
		}

		/**
		 * Returns cache entry key pattern.
		 *
		 * @return cache entry key pattern
		 */
		public String getKey() {
			return key;
		}

		/**
		 * Returns cache entry value pattern.
		 *
		 * @return cache entry value pattern
		 */
		public String getValue() {
			return value;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("CacheEntry{"); // NON-NLS
			sb.append("id=").append(Utils.quote(id)); // NON-NLS
			sb.append(", key=").append(Utils.quote(key)); // NON-NLS
			sb.append(", value=").append(Utils.quote(value)); // NON-NLS
			sb.append('}'); // NON-NLS
			return sb.toString();
		}
	}
}
