/*
 * Copyright 2014-2018 JKOOL, LLC.
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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Data value transformation function resolving object name from provided fully qualified object name.
 * <p>
 * Syntax to be use in code: 'ts:getObjectName(objectFQN, options)' were:
 * <ul>
 * <li>'ts:' is function namespace</li>
 * <li>'getObjectName' - function name</li>
 * <li>'objectFQN' - function argument defining fully qualified object name</li>
 * <li>'options' - object name resolution options:
 * <ul>
 * <li>resolution options: DEFAULT, BEFORE, AFTER, REPLACE, SECTION, FULL. Optional.</li>
 * <li>search symbols. Optional.</li>
 * <li>replacement symbols. Optional</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class FuncGetObjectName extends AbstractFunction<String> {
	/**
	 * Constant for name of the function used in code: {@value}.
	 */
	public static final String FUNCTION_NAME = "getObjectName"; // NON-NLS

	private static final String OBJ_NAME_TOKEN_DELIMITERS = "@#$"; // NON-NLS

	/**
	 * Constructs a new getObjectName() function instance.
	 */
	public FuncGetObjectName() {
		setName(FUNCTION_NAME); // NON-NLS
	}

	/**
	 * Resolves desired object name from provided fully qualified object name. Fully qualified object name can be
	 * provided as {@link String}, {@link org.w3c.dom.Node} or {@link org.w3c.dom.NodeList} (first node item containing
	 * object name).
	 * <p>
	 * function arguments sequence:
	 * <ul>
	 * <li>1 - fully qualified object name. Required.</li>
	 * <li>2 - resolution options: DEFAULT, BEFORE, AFTER, REPLACE, SECTION, FULL. Optional.</li>
	 * <li>3 - search symbols. Optional.</li>
	 * <li>4 - replacement symbols. Optional</li>
	 * </ul>
	 *
	 * @param args
	 *            function arguments list
	 * @return object name resolved form provided fully qualified object name
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

		String objectFQN = null;
		if (param instanceof String) {
			objectFQN = (String) param;
		} else if (param instanceof Node) {
			objectFQN = ((Node) param).getTextContent();
		} else if (param instanceof NodeList) {
			NodeList nodes = (NodeList) param;

			if (nodes.getLength() > 0) {
				Node node = nodes.item(0);
				objectFQN = node.getTextContent();
			}
		}

		if (StringUtils.isEmpty(objectFQN)) {
			return objectFQN;
		}

		return resolveObjectName(objectFQN, args);
	}

	private static String resolveObjectName(String objectName, List<?> args) {
		String option = args.size() > 1 ? (String) args.get(1) : null;
		Options opt;

		try {
			opt = StringUtils.isEmpty(option) ? Options.DEFAULT : Options.valueOf(option.toUpperCase());
		} catch (IllegalArgumentException exc) {
			opt = Options.DEFAULT;
		}

		switch (opt) {
		case FULL:
			break;
		case BEFORE:
			String sSymbol = args.size() > 2 ? (String) args.get(2) : null;
			if (StringUtils.isNotEmpty(sSymbol)) {
				objectName = StringUtils.substringBefore(objectName, sSymbol);
			}
			break;
		case AFTER:
			sSymbol = args.size() > 2 ? (String) args.get(2) : null;
			if (StringUtils.isNotEmpty(sSymbol)) {
				objectName = StringUtils.substringAfter(objectName, sSymbol);
			}
			break;
		case REPLACE:
			sSymbol = args.size() > 2 ? (String) args.get(2) : null;
			if (StringUtils.isNotEmpty(sSymbol)) {
				String rSymbol = args.size() > 3 ? (String) args.get(3) : null;
				objectName = StringUtils.replaceChars(objectName, sSymbol, rSymbol == null ? "" : rSymbol);
			}
			break;
		case SECTION:
			String idxStr = args.size() > 2 ? (String) args.get(2) : null;
			int idx;
			try {
				idx = Integer.parseInt(idxStr);
			} catch (Exception exc) {
				idx = -1;
			}

			if (idx >= 0) {
				sSymbol = args.size() > 3 ? (String) args.get(3) : null;
				String[] onTokens = StringUtils.split(objectName,
						StringUtils.isEmpty(sSymbol) ? OBJ_NAME_TOKEN_DELIMITERS : sSymbol);
				objectName = idx < ArrayUtils.getLength(onTokens) ? onTokens[idx] : objectName;
			}
			break;
		case DEFAULT:
		default:
			idx = StringUtils.indexOfAny(objectName, OBJ_NAME_TOKEN_DELIMITERS);
			if (idx > 0) {
				objectName = StringUtils.substring(objectName, 0, idx);
			}
			break;
		}

		return objectName;
	}

	enum Options {
		DEFAULT, BEFORE, AFTER, REPLACE, SECTION, FULL
	}
}
