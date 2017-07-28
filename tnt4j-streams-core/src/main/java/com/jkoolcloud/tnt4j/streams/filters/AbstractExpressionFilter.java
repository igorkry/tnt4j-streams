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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.Property;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class for abstract expression based data value filter.
 *
 * @param <T>
 *            the type of filtered data value
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.filters.JavaScriptExpressionFilter
 * @see com.jkoolcloud.tnt4j.streams.filters.GroovyExpressionFilter
 * @see com.jkoolcloud.tnt4j.streams.filters.XPathExpressionFilter
 */
public abstract class AbstractExpressionFilter<T> extends AbstractEntityFilter<T> {
	/**
	 * Constant for field value variable name used in script/expression code.
	 */
	protected static final String FIELD_VALUE_VARIABLE_NAME = "fieldValue"; // NON-NLS
	/**
	 * Constant for field value variable expression used in script/expression code.
	 */
	protected static final String FIELD_VALUE_VARIABLE_EXPR = '$' + FIELD_VALUE_VARIABLE_NAME;

	/**
	 * Constant for name of scripting/expression language {@value}.
	 */
	protected static final String GROOVY_LANG = "groovy"; // NON-NLS
	/**
	 * Constant for name of scripting/expression language {@value}.
	 */
	protected static final String JAVA_SCRIPT_LANG = "javascript"; // NON-NLS
	/**
	 * Constant for name of scripting/expression language {@value}.
	 */
	protected static final String XPATH_SCRIPT_LANG = "xpath"; // NON-NLS

	/**
	 * Filter expression string.
	 */
	protected String filterExpression;
	private HandleType handleType;

	/**
	 * Set for variables of filter expression contained activity fields.
	 */
	protected Set<String> exprVars;
	/**
	 * Pre-processed filter expression.
	 */
	protected String ppExpression;
	/**
	 * Map for variable placeholders of filter expression contained activity fields.
	 */
	protected Map<String, String> placeHoldersMap;

	/**
	 * Constructs a new AbstractExpressionFilter. Handle type is set to
	 * {@link com.jkoolcloud.tnt4j.streams.filters.HandleType#INCLUDE}.
	 *
	 * @param filterExpression
	 *            filter expression string
	 */
	protected AbstractExpressionFilter(String filterExpression) {
		this(null, filterExpression);
	}

	/**
	 * Constructs a new AbstractExpressionFilter.
	 * 
	 * @param handleType
	 *            filter {@link com.jkoolcloud.tnt4j.streams.filters.HandleType} name
	 * @param filterExpression
	 *            filter expression string
	 */
	protected AbstractExpressionFilter(String handleType, String filterExpression) {
		this.handleType = StringUtils.isEmpty(handleType) ? HandleType.INCLUDE
				: HandleType.valueOf(handleType.toUpperCase());

		if (StringUtils.isEmpty(filterExpression)) {
			throw new IllegalArgumentException(StreamsResources
					.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME, "ExpressionFilter.empty.expression"));
		}

		this.filterExpression = filterExpression;

		initFilter();
	}

	/**
	 * Returns filter expression string.
	 * 
	 * @return filter expressions string
	 */
	protected String getExpression() {
		return StringUtils.isEmpty(ppExpression) ? filterExpression : ppExpression;
	}

	/**
	 * Returns filter handle type.
	 *
	 * @return filter handle type
	 */
	protected HandleType getHandleType() {
		return handleType;
	}

	@Override
	protected void initFilter() {
		exprVars = new HashSet<>();
		placeHoldersMap = new HashMap<>();
		Utils.resolveExpressionVariables(exprVars, filterExpression);

		String expString = filterExpression;
		if (CollectionUtils.isNotEmpty(exprVars)) {
			String varPlh;
			int idx = 0;
			for (String eVar : exprVars) {
				varPlh = "$TNT4J_ST_FLTR_PLH" + (idx++); // NON-NLS
				expString = expString.replace(eVar, varPlh);
				placeHoldersMap.put(eVar, varPlh);
			}
		}

		ppExpression = expString;
	}

	@Override
	public StreamFilterType getFilterType() {
		return StreamFilterType.EXPRESSION;
	}

	/**
	 * Creates expression filter instance based on provided parameters.
	 *
	 * @param handleType
	 *            filter {@link com.jkoolcloud.tnt4j.streams.filters.HandleType} name
	 * @param lang
	 *            scripting/expression language: '{@value #GROOVY_LANG}', '{@value #JAVA_SCRIPT_LANG}' ('js', 'jscript')
	 *            or '{@value #XPATH_SCRIPT_LANG}'
	 * @param expression
	 *            filter expression string
	 * @return created expression filter instance
	 *
	 * @throws IllegalArgumentException
	 *             if filter can not be created for provided language
	 */
	public static AbstractExpressionFilter<?> createExpressionFilter(String handleType, String lang, String expression)
			throws IllegalArgumentException {
		if (StringUtils.isEmpty(lang)) {
			lang = JAVA_SCRIPT_LANG;
		}

		if (GROOVY_LANG.equalsIgnoreCase(lang)) {
			return new GroovyExpressionFilter(handleType, expression);
		} else if (JAVA_SCRIPT_LANG.equalsIgnoreCase(lang) || "js".equalsIgnoreCase(lang)
				|| "jscript".equalsIgnoreCase(lang)) // NON-NLS
		{
			return new JavaScriptExpressionFilter(handleType, expression);
		} else if (XPATH_SCRIPT_LANG.equalsIgnoreCase(lang)) {
			return new XPathExpressionFilter(handleType, expression);
		}

		throw new IllegalArgumentException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
				"ExpressionFilter.unknown.language", lang));
	}

	/**
	 * Resolved activity entity field value for a expression variable defined field name.
	 *
	 * @param eVar
	 *            expression variable containing field name
	 * @param activityInfo
	 *            activity entity instance to resolve field value
	 * @return resolved activity entity field value
	 */
	protected Property resolveFieldKeyAndValue(String eVar, ActivityInfo activityInfo) {
		Object fValue = activityInfo.getFieldValue(eVar);
		String fieldName = placeHoldersMap.get(eVar);

		return new Property(StringUtils.isEmpty(fieldName) ? eVar : fieldName, fValue);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("AbstractExpressionFilter{"); // NON-NLS
		sb.append("handleType=").append(handleType); // NON-NLS
		sb.append(", filterExpression=").append(Utils.sQuote(filterExpression)); // NON-NLS
		sb.append('}');
		return sb.toString();
	}
}
