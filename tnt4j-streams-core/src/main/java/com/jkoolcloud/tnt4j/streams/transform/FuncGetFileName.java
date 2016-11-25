/*
 * Copyright 2014-2016 JKOOL, LLC.
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

/**
 * Data value transformation function resolving file name from provided file path.
 * <p>
 * Syntax to be use in code: 'ts:getFileName(filePath)' were:
 * <ul>
 * <li>'ts:' is function namespace</li>
 * <li>'getFileName' - function name</li>
 * <li>'filePath' - function argument defining file path</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class FuncGetFileName extends AbstractFunction<String> {

	/**
	 * Constant for name of the function used in code: {@value}.
	 */
	public static final String FUNCTION_NAME = "getFileName"; // NON-NLS

	private static final char UNIX_PATH_SEPARATOR = '/';
	private static final char WIN_PATH_SEPARATOR = '\\';

	/**
	 * Constructs a new getFileName() function instance.
	 */
	public FuncGetFileName() {
		setName(FUNCTION_NAME); // NON-NLS
	}

	/**
	 * Resolves file name from provided file path.
	 *
	 * @param args
	 *            arguments list containing file path as first item
	 * @return file name resolved from provided path
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public Object evaluate(List args) {
		String param = CollectionUtils.isEmpty(args) ? null : String.valueOf(args.get(0));

		if (StringUtils.isEmpty(param)) {
			return param;
		}

		return param.substring(param.lastIndexOf(UNIX_PATH_SEPARATOR) + 1)
				.substring(param.lastIndexOf(WIN_PATH_SEPARATOR) + 1);
	}
}
