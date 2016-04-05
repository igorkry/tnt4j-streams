/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jkool.tnt4j.streams.utils;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility class to support I18N of TNT4J-Streams.
 *
 * @version $Revision: 1 $
 */
public final class StreamsResources {
	/**
	 * Resource bundle name constant for TNT4J-Streams "core" module.
	 */
	public static final String RESOURCE_BUNDLE_CORE = "tnt4j-streams-core"; // NON-NLS

	private static final Map<String, ResourceBundle> resBundlesMap = new HashMap<String, ResourceBundle>(8);

	private StreamsResources() {

	}

	/**
	 * Initializes singleton instance of resource bundle for default locale if
	 * such is not available in bundles cache map. After initialization bundle
	 * object is placed into cache map and retrieved from it on subsequent
	 * calls. Cache map entry key is {@code bundleName}
	 *
	 * @param bundleName
	 *            the base name of the resource bundle
	 *
	 * @return default locale bound resource bundle
	 *
	 * @see ResourceBundle#getBundle(String)
	 */
	public static ResourceBundle getBundle(String bundleName) {
		synchronized (resBundlesMap) {
			ResourceBundle resBundle = resBundlesMap.get(bundleName);
			if (resBundle == null) {
				resBundle = ResourceBundle.getBundle(bundleName);
				resBundlesMap.put(bundleName, resBundle);
			}

			return resBundle;
		}
	}

	/**
	 * Returns localized string for the given key.
	 *
	 * @param bundleName
	 *            the base name of the resource bundle
	 * @param key
	 *            localized string key
	 *
	 * @return localized string
	 */
	public static String getString(String bundleName, String key) {
		if (key == null) {
			return null;
		}

		try {
			return getBundle(bundleName).getString(key);
		} catch (MissingResourceException mre) {
			return key;
		}
	}

	/**
	 * Returns formatted localized string for the given key.
	 *
	 * @param bundleName
	 *            the base name of the resource bundle
	 * @param key
	 *            localized string key
	 * @param args
	 *            formatted string parameters
	 *
	 * @return localized string
	 *
	 * @see MessageFormat#format(String, Object...)
	 */
	public static String getStringFormatted(String bundleName, String key, Object... args) {
		if (key == null) {
			return null;
		}

		try {
			return MessageFormat.format(getBundle(bundleName).getString(key), args);
		} catch (MissingResourceException mre) {
			return key;
		}
	}

	/**
	 * Returns localized string for the given enum constant key.
	 *
	 * @param bundleName
	 *            the base name of the resource bundle
	 * @param key
	 *            localized string key
	 *
	 * @return localized string for enum constant
	 */
	public static String getString(String bundleName, Enum<?> key) {
		if (key == null) {
			return null;
		}

		try {
			return getBundle(bundleName).getString(String.format("%s.%s", key.getClass().getName(), key.name())); // NON-NLS
		} catch (MissingResourceException mre) {
			return key.toString();
		}
	}

}
