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
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility class to support I18N of TNT4J-Streams.
 *
 * @version $Revision: 1 $
 */
public class StreamsResources {
	private static final String RESOURCE_BUNDLE_BASE = "tnt4j-streams"; // NON-NLS

	private static ResourceBundle resBundle;

	/**
	 * Initializes singleton instance of resource bundle for default locale and
	 * returns it.
	 *
	 * @return default locale bound resource bundle
	 *
	 * @see ResourceBundle#getBundle(String)
	 */
	public static ResourceBundle getBundle() {
		synchronized (StreamsResources.class) {
			if (resBundle == null) {
				resBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_BASE);
			}
		}
		return resBundle;
	}

	/**
	 * Returns localized string for the given key.
	 *
	 * @param key
	 *            localized string key
	 *
	 * @return localized string
	 */
	public static String getString(String key) {
		if (key == null) {
			return null;
		}

		try {
			return getBundle().getString(key);
		} catch (MissingResourceException mre) {
			return key;
		}
	}

	/**
	 * Returns formatted localized string for the given key.
	 *
	 * @param key
	 *            localized string key
	 * @param args
	 *            formatted string parameters
	 *
	 * @return localized string
	 *
	 * @see MessageFormat#format(String, Object...)
	 */
	public static String getStringFormatted(String key, Object... args) {
		if (key == null) {
			return null;
		}

		try {
			return MessageFormat.format(getBundle().getString(key), args);
		} catch (MissingResourceException mre) {
			return key;
		}
	}

	/**
	 * Returns localized string for the given enum constant key.
	 *
	 * @param key
	 *            localized string key
	 *
	 * @return localized string for enum constant
	 */
	public static String getString(Enum<?> key) {
		if (key == null) {
			return null;
		}

		try {
			return getBundle().getString(String.format("%s.%s", key.getClass().getName(), key.name())); // NON-NLS
		} catch (MissingResourceException mre) {
			return key.toString();
		}
	}

}
