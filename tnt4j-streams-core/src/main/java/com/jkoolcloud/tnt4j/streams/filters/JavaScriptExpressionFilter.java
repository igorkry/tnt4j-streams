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

package com.jkoolcloud.tnt4j.streams.filters;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils;

/**
 * Data value filtering based on JavaScript expressions.
 * 
 * @version $Revision: 1 $
 *
 * @see ScriptEngineManager
 * @see ScriptEngine#eval(String)
 */
public class JavaScriptExpressionFilter extends AbstractExpressionFilter<Object> {

	/**
	 * Constructs a new XPathExpressionFilter. Handle type is set to
	 * {@link com.jkoolcloud.tnt4j.streams.filters.HandleType#INCLUDE}.
	 *
	 * @param filterExpression
	 *            filter expression string
	 */
	public JavaScriptExpressionFilter(String filterExpression) {
		super(filterExpression);
	}

	/**
	 * Constructs a new JavaScriptExpressionFilter.
	 *
	 * @param handleType
	 *            filter {@link com.jkoolcloud.tnt4j.streams.filters.HandleType} name
	 * @param filterExpression
	 *            filter expression string
	 */
	public JavaScriptExpressionFilter(String handleType, String filterExpression) {
		super(handleType, filterExpression);
	}

	@Override
	public boolean doFilter(Object value) throws FilterException {
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName(JAVA_SCRIPT_LANG);
		factory.put(FIELD_VALUE_VARIABLE_EXPR, value);

		try {
			boolean match = (boolean) engine.eval(StreamsScriptingUtils.addDefaultJSScriptImports(getExpression()));

			return isFilteredOut(getHandleType(), match);
		} catch (Exception exc) {
			throw new FilterException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ExpressionFilter.filtering.failed", filterExpression), exc);
		}
	}
}
