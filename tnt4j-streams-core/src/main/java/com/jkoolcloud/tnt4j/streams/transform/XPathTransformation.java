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

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xpath.*;

import com.jkoolcloud.tnt4j.streams.utils.NamespaceMap;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

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

	private static final String STREAMS_NS = "ts"; // NON-NLS
	private static final String STREAMS_NS_URI = "http://github.com/Nastel/tnt4j-streams"; // NON-NLS

	private static NamespaceMap tsContext;
	private static StreamsFunctionResolver tsFunctionResolver;

	static {
		tsContext = new NamespaceMap();
		tsFunctionResolver = new StreamsFunctionResolver();

		tsContext.addPrefixUriMapping(STREAMS_NS, STREAMS_NS_URI);
	}

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
		XPath xPath = XPathFactory.newInstance().newXPath();

		xPath.setNamespaceContext(tsContext);
		xPath.setXPathFunctionResolver(tsFunctionResolver);
		xPath.setXPathVariableResolver(new StreamsVariableResolver(value));

		try {
			return xPath.evaluate(getScriptCode(), (Object) null);
		} catch (Exception exc) {
			throw new TransformationException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ValueTransformation.transformation.failed", getName()), exc);
		}
	}

	/**
	 * Adds XPath function to custom functions registry.
	 * <p>
	 * To call these functions from script code use 'ts:' prefix: i.e. 'ts:getFileName()'
	 *
	 * @param functionName
	 *            name of custom function to be used
	 * @param function
	 *            custom XPath function to add to registry
	 * @return previously registered function for defined function name, or {@code null} if function was not previously
	 *         registered.
	 */
	public static XPathFunction registerCustomFunction(String functionName, XPathFunction function) {
		return tsFunctionResolver.fMap.put(functionName, function);
	}

	private static class StreamsFunctionResolver implements XPathFunctionResolver {
		private static Map<String, XPathFunction> fMap = new HashMap<String, XPathFunction>(2);

		static {
			fMap.put(FuncGetFileName.FUNCTION_NAME, new FuncGetFileName());
		}

		@Override
		public XPathFunction resolveFunction(QName functionName, int arity) {
			if (functionName.getNamespaceURI().equalsIgnoreCase(STREAMS_NS_URI)) {
				return fMap.get(functionName.getLocalPart());
			}

			return null;
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
