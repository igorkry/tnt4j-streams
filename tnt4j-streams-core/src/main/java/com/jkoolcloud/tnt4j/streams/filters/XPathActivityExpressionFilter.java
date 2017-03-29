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
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathVariableResolver;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsXMLUtils;

/**
 * Activity information data filtering based on XPath expressions.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.utils.StreamsXMLUtils#getStreamsXPath()
 * @see XPathVariableResolver
 * @see javax.xml.xpath.XPathFunctionResolver
 * @see javax.xml.namespace.NamespaceContext
 * @see javax.xml.xpath.XPathFunction
 * @see XPath#evaluate(String, Object)
 */
public class XPathActivityExpressionFilter extends AbstractActivityFilter {

	/**
	 * Constructs a new XPathActivityExpressionFilter. Handle type is set to
	 * {@link com.jkoolcloud.tnt4j.streams.filters.HandleType#INCLUDE}.
	 *
	 * @param filterExpression
	 *            filter expression string
	 */
	public XPathActivityExpressionFilter(String filterExpression) {
		super(filterExpression);
	}

	/**
	 * Constructs a new XPathActivityExpressionFilter.
	 * 
	 * @param handleType
	 *            filter {@link com.jkoolcloud.tnt4j.streams.filters.HandleType} name
	 * @param filterExpression
	 *            filter expression string
	 */
	public XPathActivityExpressionFilter(String handleType, String filterExpression) {
		super(handleType, filterExpression);
	}

	@Override
	public boolean doFilter(ActivityInfo activityInfo) throws FilterException {
		if (activityInfo == null) {
			return false;
		}

		Map<String, Object> valuesMap = new HashMap<>();

		if (CollectionUtils.isNotEmpty(exprVars)) {
			Object fValue;
			String fieldName;
			for (String eVar : exprVars) {
				fieldName = eVar.substring(2, eVar.length() - 1);
				fValue = activityInfo.getFieldValue(fieldName);
				fieldName = placeHoldersMap.get(eVar);
				valuesMap.put(StringUtils.isEmpty(fieldName) ? eVar : fieldName, fValue);
			}
		}

		XPath xPath = StreamsXMLUtils.getStreamsXPath();
		xPath.setXPathVariableResolver(new XPathActivityExpressionFilter.StreamsVariableResolver(valuesMap));

		try {
			boolean match = "true".equals(xPath.evaluate(getExpression(), (Object) null)); // NON-NLS

			boolean filteredOut = isFilteredOut(getHandleType(), match);
			activityInfo.setFiltered(filteredOut);

			return filteredOut;
		} catch (Exception exc) {
			throw new FilterException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ExpressionFilter.filtering.failed", filterExpression), exc);
		}
	}

	private static class StreamsVariableResolver implements XPathVariableResolver {
		private Map<String, Object> valuesMap;

		private StreamsVariableResolver(Map<String, Object> valuesMap) {
			this.valuesMap = valuesMap;
		}

		@Override
		public Object resolveVariable(QName variableName) {
			String varNameStr = "$" + variableName.toString(); // NON-NLS

			return valuesMap.get(varNameStr);
		}
	}
}
