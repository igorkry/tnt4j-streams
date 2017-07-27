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

package com.jkoolcloud.tnt4j.streams.transform;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;

import org.apache.commons.collections4.CollectionUtils;

import com.jkoolcloud.tnt4j.core.Property;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsXMLUtils;

/**
 * Data value transformation based on XPath function expressions.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.utils.StreamsXMLUtils#getStreamsXPath()
 * @see XPathVariableResolver
 * @see XPathFunctionResolver
 * @see javax.xml.namespace.NamespaceContext
 * @see XPathFunction
 * @see XPath#evaluate(String, Object)
 */
public class XPathTransformation extends AbstractScriptTransformation<Object> {
	private static final String OWN_FIELD_VALUE_KEY = "<!TNT4J_XPATH_TRSF_FLD_VALUE!>"; // NON-NLS;

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
	public Object transform(Object value, ActivityInfo ai) throws TransformationException {
		if (value == null) {
			return value;
		}

		Map<String, Object> valuesMap = new HashMap<>();
		valuesMap.put(OWN_FIELD_VALUE_KEY, value);

		if (ai != null && CollectionUtils.isNotEmpty(exprVars)) {
			for (String eVar : exprVars) {
				Property eKV = resolveFieldKeyAndValue(eVar, ai);

				valuesMap.put(eKV.getKey(), eKV.getValue());
			}
		}

		XPath xPath = StreamsXMLUtils.getStreamsXPath();
		xPath.setXPathVariableResolver(new StreamsVariableResolver(valuesMap));

		try {
			return xPath.evaluate(getScriptCode(), (Object) null);
		} catch (Exception exc) {
			throw new TransformationException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ValueTransformation.transformation.failed", getName()), exc);
		}
	}

	private static class StreamsVariableResolver implements XPathVariableResolver {
		private Map<String, Object> valuesMap;

		private StreamsVariableResolver(Map<String, Object> valuesMap) {
			this.valuesMap = valuesMap;
		}

		@Override
		public Object resolveVariable(QName variableName) {
			if (variableName.equals(new QName(FIELD_VALUE_VARIABLE_NAME))) {
				return valuesMap.get(OWN_FIELD_VALUE_KEY);
			}

			String varNameStr = "$" + variableName.toString(); // NON-NLS

			return valuesMap.get(varNameStr);
		}
	}
}
