/*
 * Copyright 2014-2019 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.transform.beans;

import java.util.List;

/**
 * Transformations bean to perform strings manipulation.
 *
 * @version $Revision: 1 $
 */
public class Strings {

	/**
	 * Replaces provided {@code strings} elements content matching {@code target} sequence with {@code replacement}
	 * sequence.
	 * 
	 * @param strings
	 *            strings array to replace content
	 * @param target
	 *            the sequence of char values to be replaced
	 * @param replacement
	 *            the replacement sequence of char values
	 * @return strings array with replaced content
	 */
	public static String[] replace(String[] strings, String target, String replacement) {
		if (strings != null) {
			for (int i = 0; i < strings.length; i++) {
				strings[i] = strings[i].replace(target, replacement);
			}
		}

		return strings;
	}

	/**
	 * Replaces provided {@code strings} elements content matching {@code target} sequence with {@code replacement}
	 * sequence.
	 * 
	 * @param strings
	 *            strings list to replace content
	 * @param target
	 *            the sequence of char values to be replaced
	 * @param replacement
	 *            the replacement sequence of char values
	 * @return strings list with replaced content
	 */
	public static List<String> replace(List<String> strings, String target, String replacement) {
		if (strings != null) {
			for (int i = 0; i < strings.size(); i++) {
				strings.set(i, strings.get(i).replace(target, replacement));
			}
		}

		return strings;
	}

	/**
	 * Replaces provided {@code string} content matching {@code target} sequence with {@code replacement} sequence.
	 *
	 * @param string
	 *            string to replace content
	 * @param target
	 *            the sequence of char values to be replaced
	 * @param replacement
	 *            the replacement sequence of char values
	 * @return string with replaced content
	 */
	public static String replace(String string, String target, String replacement) {
		return string == null ? string : string.replace(target, replacement);
	}
}
