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

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.CacheProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;

/**
 * Utility class to support TNT4J-Streams streamed data values caching.
 * <p>
 * Cache entries are defined using static or dynamic (e.g., patterns having field name variable to fill in data from
 * activity entity) values.
 * <p>
 * Streams cache supports the following configuration properties:
 * <ul>
 * <li>MaxSize - max. capacity of stream resolved values cache. Default value - {@code 100}. (Optional)</li>
 * <li>ExpireDuration - stream resolved values cache entries expiration duration in minutes. Default value - {@code 10}.
 * (Optional)</li>
 * <li>Persisted - flag indicating cache contents has to be persisted to file on close and loaded on initialization.
 * Default value - {@code false}. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 3 $
 */
public class StreamsCache {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(StreamsCache.class);

	private static final long DEFAULT_CACHE_MAX_SIZE = 100;
	private static final long DEFAULT_CACHE_EXPIRE_IN_MINUTES = 10;
	private static final String DEFAULT_FILE_NAME = "./persistedCache.xml"; // NON-NLS

	private static final String PARSER_NAME_VAR = "${ParserName}"; // NON-NLS

	private static Cache<String, Object> valuesCache;
	private static Map<String, CacheEntry> cacheEntries = new HashMap<>();
	private static AtomicInteger referencesCount = new AtomicInteger();

	private static long maxSize = DEFAULT_CACHE_MAX_SIZE;
	private static long expireDuration = DEFAULT_CACHE_EXPIRE_IN_MINUTES;
	private static boolean persistenceOn = false; // TODO: file naming, because there may be running multiple concurrent
													// streams configurations (JVMs)

	private static Cache<String, Object> buildCache(long cSize, long duration) {
		return CacheBuilder.newBuilder().maximumSize(cSize).expireAfterAccess(duration, TimeUnit.MINUTES).build();
	}

	/**
	 * Sets cache configuration properties collection.
	 *
	 * @param props
	 *            configuration properties to set
	 *
	 * @see #initialize()
	 */
	public static void setProperties(Collection<Map.Entry<String, String>> props) {
		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				String name = prop.getKey();
				String value = prop.getValue();
				if (CacheProperties.PROP_MAX_SIZE.equalsIgnoreCase(name)) {
					maxSize = Long.parseLong(value);
				} else if (CacheProperties.PROP_EXPIRE_DURATION.equalsIgnoreCase(name)) {
					expireDuration = Long.parseLong(value);
				} else if (CacheProperties.PROP_PERSISTED.equalsIgnoreCase(name)) {
					persistenceOn = Boolean.parseBoolean(value);
				}
			}
		}

		initialize();
	}

	/**
	 * Initializes cache setting maximum cache size and cache entries expiration duration.
	 */
	public static void initialize() {
		valuesCache = buildCache(maxSize, expireDuration);

		if (persistenceOn) {
			loadPersisted();
		}
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
			// valuesCache = buildCache(maxSize, expireDuration);
			return;
		}

		for (CacheEntry cacheEntry : cacheEntries.values()) {
			String resolvedFieldKey = fillInKeyPattern(cacheEntry.getKey(), ai, parserName);
			Object resolvedFieldValue = fillInValuePattern(cacheEntry.getValue(), ai, parserName);

			if (resolvedFieldKey != null && resolvedFieldValue != null) {
				valuesCache.put(resolvedFieldKey, resolvedFieldValue);
			}
		}
	}

	/**
	 * Fills in some key pattern string with activity entity fields values.
	 * 
	 * @param pattern
	 *            pattern string to fill
	 * @param ai
	 *            activity entity data
	 * @param parserName
	 *            parser name
	 * @return pattern string filled in with data values
	 */
	public static String fillInKeyPattern(String pattern, ActivityInfo ai, String parserName) {
		List<String> vars = new ArrayList<>();
		Utils.resolveCfgVariables(vars, pattern);

		return fillInPattern(pattern, vars, ai, parserName);
	}

	private static Object fillInValuePattern(String pattern, ActivityInfo ai, String parserName) {
		List<String> vars = new ArrayList<>();
		Utils.resolveCfgVariables(vars, pattern);

		return vars.size() == 1 ? ai.getFieldValue(vars.get(0)) : fillInPattern(pattern, vars, ai, parserName);
	}

	private static String fillInPattern(String pattern, List<String> vars, ActivityInfo ai, String parserName) {
		String filledInValue = pattern;

		for (String var : vars) {
			Object fieldValue;
			if (var.equals(PARSER_NAME_VAR)) {
				fieldValue = parserName;
			} else {
				fieldValue = ai.getFieldValue(var);
			}

			if (fieldValue != null) {
				filledInValue = filledInValue.replace(var, Utils.toString(fieldValue));
			}
		}

		return filledInValue;
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
			} else {
				return cacheEntry.getDefaultValue();
			}
		}
		return null;
	}

	/**
	 * Resolves cache stored value identified by cache entry key.
	 *
	 * @param cacheKey
	 *            cache entry key
	 * @return resolved cached value or {@code null} if there is no such entry or data in cache
	 */
	public static Object getValue(String cacheKey) {
		Object value = valuesCache == null ? null : valuesCache.getIfPresent(cacheKey);
		if (value == null) {
			CacheEntry cacheEntry = cacheEntries.get(cacheKey);

			return cacheEntry == null ? null : cacheEntry.getDefaultValue();
		}
		return value;
	}

	/**
	 * Cleans cache contents.
	 *
	 * @see #unreferStream()
	 */
	public static void cleanup() {
		if (valuesCache != null) {
			if (persistenceOn) {
				persist(valuesCache.asMap());
			}
			valuesCache.cleanUp();
		}
		cacheEntries.clear();
	}

	/**
	 * Adds stream-cache reference.
	 */
	public static void referStream() {
		if (valuesCache == null) {
			return;
		}

		referencesCount.getAndIncrement();
	}

	/**
	 * Removes stream-cache reference. When last stream is unreferenced, cache gets cleaned.
	 *
	 * @see #cleanup()
	 */
	public static void unreferStream() {
		if (valuesCache == null) {
			return;
		}

		int crc = referencesCount.decrementAndGet();

		if (crc <= 0) {
			cleanup();
			referencesCount.set(0);
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
	 * @param defaultValue
	 *            default entry value
	 * @return previous cache entry instance stored
	 */
	public static CacheEntry addEntry(String entryId, String key, String value, String defaultValue) {
		return cacheEntries.put(entryId, new CacheEntry(entryId, key, value, defaultValue));
	}

	public static void loadPersisted() {
		try {
			JAXBContext jc = JAXBContext.newInstance(CacheRoot.class);
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			File persistedFile = new File(DEFAULT_FILE_NAME);
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsCache.loading.file"),
					persistedFile.getAbsolutePath());
			if (!persistedFile.exists()) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"StreamsCache.loading.file.not.found"));
				return;
			}

			CacheRoot root = (CacheRoot) unmarshaller.unmarshal(persistedFile);

			Map<String, Object> mapProperty = root.getEntriesMap();
			if (MapUtils.isNotEmpty(mapProperty)) {
				for (Map.Entry<String, Object> entry : mapProperty.entrySet()) {
					valuesCache.put(entry.getKey(), entry.getValue());
				}
			}
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsCache.loading.done"),
					mapProperty == null ? 0 : mapProperty.size(), persistedFile.getAbsolutePath());
		} catch (JAXBException exc) {
			LOGGER.log(OpLevel.ERROR,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsCache.loading.failed"),
					exc);
		}
	}

	protected static void persist(Map<String, Object> cacheEntries) {
		try {
			JAXBContext jc = JAXBContext.newInstance(CacheRoot.class);
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			CacheRoot root = new CacheRoot();
			root.setEntriesMap(cacheEntries);
			File persistedFile = new File(DEFAULT_FILE_NAME);
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsCache.persisting.file"),
					persistedFile.getAbsolutePath());
			marshaller.marshal(root, persistedFile);
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsCache.persisting.done"),
					cacheEntries.size(), persistedFile.getAbsolutePath());
		} catch (JAXBException exc) {
			LOGGER.log(OpLevel.ERROR,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsCache.persisting.failed"),
					exc);
		}
	}

	/**
	 * Defines cache entry pattern.
	 */
	public static class CacheEntry {
		private String id;
		private String key;
		private String value;
		private String defaultValue;

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
		private CacheEntry(String id, String key, String value, String defaultValue) {
			this.id = id;
			this.key = key;
			this.value = value;
			this.defaultValue = defaultValue;
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

		/**
		 * Returns default cache entry value.
		 *
		 * @return default cache entry value
		 */
		public String getDefaultValue() {
			return defaultValue;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder("CacheEntry{"); // NON-NLS
			sb.append("id="); // NON-NLS
			Utils.quote(id, sb);
			sb.append(", key="); // NON-NLS
			Utils.quote(key, sb);
			sb.append(", value="); // NON-NLS
			Utils.quote(value, sb);
			sb.append(", defaultValue="); // NON-NLS
			Utils.quote(defaultValue, sb);
			sb.append('}'); // NON-NLS
			return sb.toString();
		}
	}

	public static class MapAdapter extends XmlAdapter<MapEntry[], Map<String, Object>> {
		@Override
		public MapEntry[] marshal(Map<String, Object> cache) throws Exception {
			MapEntry[] mapElements = new MapEntry[cache.size()];
			int i = 0;
			for (Map.Entry<String, Object> entry : cache.entrySet()) {
				mapElements[i++] = new MapEntry(entry.getKey(), entry.getValue());
				LOGGER.log(OpLevel.TRACE,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "StreamsCache.entry.marshal"),
						entry.getKey(), entry.getValue());
			}

			return mapElements;
		}

		@Override
		public Map<String, Object> unmarshal(MapEntry[] mapElements) throws Exception {
			Map<String, Object> r = new ConcurrentHashMap<>(mapElements.length);
			for (MapEntry mapElement : mapElements) {
				r.put(mapElement.key, mapElement.getValue());
				LOGGER.log(OpLevel.TRACE, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"StreamsCache.entry.unmarshal"), mapElement.key, mapElement.getValue());
			}
			return r;
		}
	}

	public static class MapEntry {
		@XmlElement
		public String key;
		private Object value;

		@XmlElement
		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			if (value instanceof XMLGregorianCalendar) {
				this.value = ((XMLGregorianCalendar) value).toGregorianCalendar().getTime();
			} else {
				this.value = value;
			}

		}

		private MapEntry() {
		} // Required by JAXB

		public MapEntry(String key, Object value) {
			this.key = key;
			setValue(value);
		}
	}

	@XmlRootElement
	public static class CacheRoot {

		private Map<String, Object> entriesMap;

		@XmlJavaTypeAdapter(MapAdapter.class)
		public Map<String, Object> getEntriesMap() {
			return entriesMap;
		}

		public void setEntriesMap(Map<String, Object> map) {
			this.entriesMap = map;
		}

	}
	/*
	 * private static class ByteArrayAdapter extends XmlAdapter<String, Byte[]> {
	 * 
	 * @Override public Byte[] unmarshal(String v) throws Exception { return ArrayUtils.toObject(v.getBytes()); }
	 * 
	 * @Override public String marshal(Byte[] v) throws Exception { return String.valueOf(v); } }
	 */
}
