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

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathVariableResolver;

import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsXMLUtils;

/**
 * Data value filtering based on XPath expressions.
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
public class XPathExpressionFilter extends AbstractExpressionFilter<Object> {

	/**
	 * Constructs a new XPathExpressionFilter. Handle type is set to
	 * {@link com.jkoolcloud.tnt4j.streams.filters.HandleType#INCLUDE}.
	 *
	 * @param filterExpression
	 *            filter expression string
	 */
	public XPathExpressionFilter(String filterExpression) {
		super(filterExpression);
	}

	/**
	 * Constructs a new XPathExpressionFilter.
	 * 
	 * @param handleType
	 *            filter {@link com.jkoolcloud.tnt4j.streams.filters.HandleType} name
	 * @param filterExpression
	 *            filter expression string
	 */
	public XPathExpressionFilter(String handleType, String filterExpression) {
		super(handleType, filterExpression);
	}

	@Override
	public boolean doFilter(Object value) throws FilterException {
		if (value == null) {
			return false;
		}

		XPath xPath = StreamsXMLUtils.getStreamsXPath();
		xPath.setXPathVariableResolver(new StreamsVariableResolver(value));

		try {
			boolean match = "true".equals(xPath.evaluate(getExpression(), (Object) null)); // NON-NLS

			return isFilteredOut(getHandleType(), match);
		} catch (Exception exc) {
			throw new FilterException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ExpressionFilter.filtering.failed", filterExpression), exc);
		}
	}

	private static class StreamsVariableResolver implements XPathVariableResolver {
		private Object fieldValue;

		private StreamsVariableResolver(Object fieldValue) {
			this.fieldValue = fieldValue;
		}

		@Override
		public Object resolveVariable(QName variableName) {
			if (variableName.equals(new QName(FIELD_VALUE_VARIABLE_NAME))) {
				return fieldValue;
			}

			return null;
		}
	}
}
