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

package com.jkool.tnt4j.streams.filters;

/**
 * Lists the built-in comparison types for activity field values.
 *
 * @version $Revision: 1 $
 */
public enum FilterComparatorType {
	/**
	 * Indicates 'less or equal' comparison of activity field values.
	 */
	LESS_OR_EQUAL("<="), // NON-NLS

	/**
	 * Indicates 'greater or equal' comparison of activity field values.
	 */
	GREATER_OR_EQUAL(">="), // NON-NLS

	/**
	 * Indicates 'greater' comparison of activity field values.
	 */
	GREATER(">"), // NON-NLS

	/**
	 * Indicates 'less' comparison of activity field values.
	 */
	LESS("<"), // NON-NLS

	/**
	 * Indicates 'equal' comparison of activity field values.
	 */
	EQUAL("="), // NON-NLS

	/**
	 * Indicates 'not equal' comparison of activity field values.
	 */
	NOT_EQUAl("!="); // NON-NLS

	private String sign;

	private FilterComparatorType(String sign) {
		this.sign = sign;
	}

	/**
	 * TODO
	 *
	 * @return sign
	 */
	public String getSign() {
		return sign;
	}

	/**
	 * TODO
	 *
	 * @param sign
	 *            the sign
	 * @return comparator
	 */
	public FilterComparatorType getComparator(String sign) {
		for (FilterComparatorType fct : values()) {
			if (fct.getSign().equalsIgnoreCase(sign)) {
				return fct;
			}
		}

		return null;
	}

}
