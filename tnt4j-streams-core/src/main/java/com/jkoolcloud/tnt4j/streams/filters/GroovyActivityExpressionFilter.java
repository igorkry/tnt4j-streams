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

import org.apache.commons.collections4.CollectionUtils;

import com.jkoolcloud.tnt4j.core.Property;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

/**
 * Activity information data filtering based on Groovy expressions.
 *
 * @version $Revision: 1 $
 *
 * @see Binding
 * @see GroovyShell#evaluate(String, String)
 */
public class GroovyActivityExpressionFilter extends AbstractActivityFilter {

	/**
	 * Constructs a new GroovyActivityExpressionFilter. Handle type is set to
	 * {@link com.jkoolcloud.tnt4j.streams.filters.HandleType#INCLUDE}.
	 *
	 * @param filterExpression
	 *            filter expression string
	 */
	public GroovyActivityExpressionFilter(String filterExpression) {
		super(filterExpression);
	}

	/**
	 * Constructs a new GroovyActivityExpressionFilter.
	 *
	 * @param handleType
	 *            filter {@link com.jkoolcloud.tnt4j.streams.filters.HandleType} name
	 * @param filterExpression
	 *            filter expression string
	 */
	public GroovyActivityExpressionFilter(String handleType, String filterExpression) {
		super(handleType, filterExpression);
	}

	@Override
	public boolean doFilter(ActivityInfo activityInfo) throws FilterException {
		Binding binding = new Binding();

		if (CollectionUtils.isNotEmpty(exprVars)) {
			for (String eVar : exprVars) {
				Property eKV = resolveFieldKeyAndValue(eVar, activityInfo);

				binding.setVariable(eKV.getKey(), eKV.getValue());
			}
		}
		GroovyShell shell = new GroovyShell(binding, StreamsScriptingUtils.getDefaultGroovyCompilerConfig());

		try {
			boolean match = (boolean) shell.evaluate(getExpression());

			return isFilteredOut(getHandleType(), match);
		} catch (Exception exc) {
			throw new FilterException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ExpressionFilter.filtering.failed", filterExpression), exc);
		}
	}
}
