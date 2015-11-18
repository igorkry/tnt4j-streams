/*
 * Copyright (c) 2015 jKool, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * jKool, LLC. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with jKool, LLC.
 *
 * JKOOL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. JKOOL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 *
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
	private static final String RESOURCE_BUNDLE_BASE = "resources/tnt4j-streams"; // NON-NLS

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
