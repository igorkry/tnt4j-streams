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

import javax.xml.namespace.QName;
import javax.xml.xpath.*;

import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsXMLUtils;

/**
 * Data value transformation based on XPath function expressions.
 *
 * @version $Revision: 1 $
 *
 * @see XPathFactory
 * @see XPathVariableResolver
 * @see XPathFunctionResolver
 * @see javax.xml.namespace.NamespaceContext
 * @see XPathFunction
 * @see XPath#evaluate(String, Object)
 */
public class XPathTransformation extends AbstractScriptTransformation<Object> {

	/**
	 * Constructs a new XPathTransformation.
	 *
	 * @param name
	 *            transformation name
	 * @param scriptCode
	 *            XPath expression code
	 */
	public XPathTransformation(String name, String scriptCode) {
		super(name, scriptCode);
	}

	@Override
	public Object transform(Object value) throws TransformationException {
		if (value == null) {
			return value;
		}

		XPath xPath = StreamsXMLUtils.getStreamsXPath();
		xPath.setXPathVariableResolver(new StreamsVariableResolver(value));

		try {
			return xPath.evaluate(getScriptCode(), (Object) null);
		} catch (Exception exc) {
			throw new TransformationException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ValueTransformation.transformation.failed", getName()), exc);
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
