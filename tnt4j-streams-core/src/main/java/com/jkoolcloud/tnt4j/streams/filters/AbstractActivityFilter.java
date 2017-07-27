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
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class for abstract expression based activity information data values filter.
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractActivityFilter extends AbstractExpressionFilter<ActivityInfo> {

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
	 * Constructs a new AbstractActivityFilter.
	 *
	 * @param filterExpression
	 *            filter expression string
	 */
	protected AbstractActivityFilter(String filterExpression) {
		super(filterExpression);
	}

	/**
	 * Constructs a new AbstractActivityFilter.
	 *
	 * @param handleType
	 *            filter {@link com.jkoolcloud.tnt4j.streams.filters.HandleType} name
	 * @param filterExpression
	 *            filter expression string
	 */
	protected AbstractActivityFilter(String handleType, String filterExpression) {
		super(handleType, filterExpression);
	}

	/**
	 * Performs filter initialization: resolves expression defined variables.
	 */
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
	protected String getExpression() {
		return StringUtils.isEmpty(ppExpression) ? super.getExpression() : ppExpression;
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
}
