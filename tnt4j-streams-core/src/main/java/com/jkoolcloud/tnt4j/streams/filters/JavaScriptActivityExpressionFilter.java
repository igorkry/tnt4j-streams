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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Activity information data filtering based on JavaScript expressions.
 * 
 * @version $Revision: 1 $
 *
 * @see ScriptEngineManager
 * @see ScriptEngine#eval(String)
 */
public class JavaScriptActivityExpressionFilter extends AbstractActivityFilter {

	/**
	 * Constructs a new JavaScriptActivityExpressionFilter. Handle type is set to
	 * {@link com.jkoolcloud.tnt4j.streams.filters.HandleType#INCLUDE}.
	 *
	 * @param filterExpression
	 *            filter expression string
	 */
	public JavaScriptActivityExpressionFilter(String filterExpression) {
		super(filterExpression);
	}

	/**
	 * Constructs a new JavaScriptActivityExpressionFilter.
	 *
	 * @param handleType
	 *            filter {@link com.jkoolcloud.tnt4j.streams.filters.HandleType} name
	 * @param filterExpression
	 *            filter expression string
	 */
	public JavaScriptActivityExpressionFilter(String handleType, String filterExpression) {
		super(handleType, filterExpression);
	}

	@Override
	public boolean doFilter(ActivityInfo activityInfo) throws FilterException {
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName(JAVA_SCRIPT_LANG);

		if (CollectionUtils.isNotEmpty(exprVars)) {
			Object fValue;
			String fieldName;
			for (String eVar : exprVars) {
				fieldName = eVar.substring(2, eVar.length() - 1);
				fValue = activityInfo.getFieldValue(fieldName);
				fieldName = placeHoldersMap.get(eVar);
				factory.put(StringUtils.isEmpty(fieldName) ? eVar : fieldName, fValue);
			}
		}

		try {
			boolean match = (boolean) engine.eval(getExpression());

			boolean filteredOut = isFilteredOut(getHandleType(), match);
			activityInfo.setFiltered(filteredOut);

			return filteredOut;
		} catch (Exception exc) {
			throw new FilterException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ExpressionFilter.filtering.failed", filterExpression), exc);
		}
	}
}
