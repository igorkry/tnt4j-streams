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

package com.jkoolcloud.tnt4j.streams.transform;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Data value transformation function resolving object name from provided object.
 * <p>
 * Syntax to be use in code: 'ts:getObjectName(object, options)' were:
 * <ul>
 * <li>'ts:' is function namespace</li>
 * <li>'getObjectName' - function name</li>
 * <li>'object' - function argument defining object name</li>
 * <li>'options' - object name resolution options</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class FuncGetObjectName extends AbstractFunction<String> {
	/**
	 * Constant for name of the function used in code: {@value}.
	 */
	public static final String FUNCTION_NAME = "getObjectName"; // NON-NLS

	/**
	 * Constructs a new getObjectName() function instance.
	 */
	public FuncGetObjectName() {
		setName(FUNCTION_NAME); // NON-NLS
	}

	/**
	 * Resolves object name from provided object. Object name can be provided as {@link String},
	 * {@link org.w3c.dom.Node} or {@link org.w3c.dom.NodeList} (first node item containing object name).
	 * <p>
	 * function arguments sequence:
	 * <ul>
	 * <li>1 - object name to resolve. Required.</li>
	 * <li>2 - resolution options: DIRECT, BEFORE, AFTER, REPLACE. Optional.</li>
	 * <li>3 - search symbols. Optional.</li>
	 * <li>4 - replacement symbols. Optional</li>
	 * </ul>
	 *
	 * @param args
	 *            function arguments list
	 * @return object name resolved form provided object
	 *
	 * @see org.w3c.dom.Node
	 * @see org.w3c.dom.NodeList
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public Object evaluate(List args) {
		Object param = CollectionUtils.isEmpty(args) ? null : args.get(0);

		if (param == null) {
			return param;
		}

		String objectName = null;
		if (param instanceof String) {
			objectName = (String) param;
		} else if (param instanceof Node) {
			objectName = ((Node) param).getTextContent();
		} else if (param instanceof NodeList) {
			NodeList nodes = (NodeList) param;

			if (nodes.getLength() > 0) {
				Node node = nodes.item(0);
				objectName = node.getTextContent();
			}
		}

		if (StringUtils.isEmpty(objectName)) {
			return objectName;
		}

		return resolveObjectName(objectName, args);
	}

	private static String resolveObjectName(String objectName, List<?> args) {
		String option = args.size() > 1 ? (String) args.get(1) : null;

		if (StringUtils.isNotEmpty(option)) {
			try {
				Options opt = Options.valueOf(option.toUpperCase());
				String sSymbol = args.size() > 2 ? (String) args.get(2) : null;
				String rSymbol = args.size() > 3 ? (String) args.get(3) : null;

				switch (opt) {
				case BEFORE:
					if (StringUtils.isNotEmpty(sSymbol)) {
						objectName = StringUtils.substringBefore(objectName, sSymbol);
					}
					break;
				case AFTER:
					if (StringUtils.isNotEmpty(sSymbol)) {
						objectName = StringUtils.substringAfter(objectName, sSymbol);
					}
					break;
				case REPLACE:
					if (StringUtils.isNotEmpty(sSymbol)) {
						objectName = StringUtils.replaceChars(objectName, sSymbol, rSymbol == null ? "" : rSymbol);
					}
					break;
				default:
					break;
				}
			} catch (IllegalArgumentException exc) {
			}
		}

		return objectName;
	}

	enum Options {
		DIRECT, BEFORE, AFTER, REPLACE
	}
}
