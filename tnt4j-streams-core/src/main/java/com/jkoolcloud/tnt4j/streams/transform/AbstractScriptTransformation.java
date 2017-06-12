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

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Base class for abstract script based data value transformation.
 *
 * @param <V>
 *            the type of transformed data value
 *
 * @version $Revision: 1 $
 *
 * @see JavaScriptTransformation
 * @see GroovyTransformation
 * @see XPathTransformation
 */
public abstract class AbstractScriptTransformation<V> extends AbstractValueTransformation<V, Object> {
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

	private String scriptCode;

	/**
	 * Constructs a new AbstractScriptTransformation.
	 *
	 * @param name
	 *            transformation name
	 * @param scriptCode
	 *            transformation script code
	 */
	protected AbstractScriptTransformation(String name, String scriptCode) {
		this.scriptCode = scriptCode;
		setName(StringUtils.isEmpty(name) ? scriptCode : name);
	}

	/**
	 * Returns transformation script/expression code.
	 *
	 * @return transformation script code
	 */
	public String getScriptCode() {
		return scriptCode;
	}

	/**
	 * Creates transformation instance based on provided parameters.
	 *
	 * @param name
	 *            transformation name
	 * @param lang
	 *            scripting/expression language: '{@value #GROOVY_LANG}', '{@value #JAVA_SCRIPT_LANG}' ('js', 'jscript')
	 *            or '{@value #XPATH_SCRIPT_LANG}'
	 * @param code
	 *            transformation script code
	 * @return created transformation instance
	 *
	 * @throws IllegalArgumentException
	 *             if transformation can not be created for provided language
	 */
	public static ValueTransformation<Object, Object> createScriptTransformation(String name, String lang, String code)
			throws IllegalArgumentException {
		if (StringUtils.isEmpty(lang)) {
			lang = JAVA_SCRIPT_LANG;
		}

		if (GROOVY_LANG.equalsIgnoreCase(lang)) {
			return new GroovyTransformation(name, code);
		} else if (JAVA_SCRIPT_LANG.equalsIgnoreCase(lang) || "js".equalsIgnoreCase(lang)
				|| "jscript".equalsIgnoreCase(lang)) // NON-NLS
		{
			return new JavaScriptTransformation(name, code);
		} else if (XPATH_SCRIPT_LANG.equalsIgnoreCase(lang)) {
			return new XPathTransformation(name, code);
		}

		throw new IllegalArgumentException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
				"ScriptTransformation.unknown.language", lang));
	}
}
