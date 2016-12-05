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

package com.jkoolcloud.tnt4j.streams.utils;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionResolver;

import com.jkoolcloud.tnt4j.streams.transform.FuncGetFileName;

/**
 * General XML utility methods used by TNT4J-Streams.
 *
 * @version $Revision: 1 $
 */
public final class StreamsXMLUtils {
	private static final String STREAMS_NS = "ts"; // NON-NLS
	private static final String STREAMS_NS_URI = "http://github.com/Nastel/tnt4j-streams"; // NON-NLS

	private static NamespaceMap tsContext;
	private static StreamsFunctionResolver tsFunctionResolver;

	static {
		tsContext = new NamespaceMap();
		tsFunctionResolver = new StreamsFunctionResolver();

		tsContext.addPrefixUriMapping(STREAMS_NS, STREAMS_NS_URI);
	}

	private StreamsXMLUtils() {
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

	/**
	 * Initiates new instance of {@link XPath} and decorates it with TNT4J-Streams namespace context 'ts:' and custom
	 * functions resolver.
	 *
	 * @return new instance of an {@link XPath}
	 *
	 * @see NamespaceMap
	 * @see FuncGetFileName
	 */
	public static XPath getStreamsXPath() {
		XPath xPath = XPathFactory.newInstance().newXPath();

		xPath.setNamespaceContext(tsContext);
		xPath.setXPathFunctionResolver(tsFunctionResolver);

		return xPath;
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

}
