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
	 * Set for variables of transformation expression contained activity fields.
	 */
	protected Set<String> exprVars;
	/**
	 * Pre-processed transformation expression.
	 */
	protected String ppExpression;
	/**
	 * Map for variable placeholders of transformation expression contained activity fields.
	 */
	protected Map<String, String> placeHoldersMap;

	/**
	 * Constructs a new AbstractScriptTransformation.
	 *
	 * @param name
	 *            transformation name
	 * @param scriptCode
	 *            transformation script code
	 */
	protected AbstractScriptTransformation(String name, String scriptCode) {
		this(name, scriptCode, null);
	}

	/**
	 * Constructs a new AbstractScriptTransformation.
	 *
	 * @param name
	 *            transformation name
	 * @param scriptCode
	 *            transformation script code
	 * @param phase
	 *            activity data value resolution phase
	 */
	protected AbstractScriptTransformation(String name, String scriptCode, Phase phase) {
		this.scriptCode = scriptCode;
		setName(StringUtils.isEmpty(name) ? scriptCode : name);
		setPhase(phase);

		initTransformation();
	}

	/**
	 * Returns transformation script code string.
	 *
	 * @return transformation script code
	 */
	public String getScriptCode() {
		return scriptCode;
	}

	/**
	 * Returns transformation expression code string.
	 *
	 * @return transformation expression
	 */
	public String getExpression() {
		return StringUtils.isEmpty(ppExpression) ? scriptCode : ppExpression;
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
	 * @param phaseName
	 *            data resolution phase name, when to apply transformation: '{@link Phase#RAW}',
	 *            '{@link Phase#FORMATTED}' (default) or '{@link Phase#AGGREGATED}'
	 * @return created transformation instance
	 *
	 * @throws IllegalArgumentException
	 *             if transformation can not be created for provided language
	 */
	public static ValueTransformation<Object, Object> createScriptTransformation(String name, String lang, String code,
			String phaseName) throws IllegalArgumentException {
		if (StringUtils.isEmpty(lang)) {
			lang = JAVA_SCRIPT_LANG;
		}

		Phase phase = null;
		if (StringUtils.isNotEmpty(phaseName)) {
			try {
				phase = Phase.valueOf(phaseName.toUpperCase());
			} catch (Exception exc) {
				throw new IllegalArgumentException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "ScriptTransformation.unknown.phase", phase));
			}
		}

		if (GROOVY_LANG.equalsIgnoreCase(lang)) {
			return new GroovyTransformation(name, code, phase);
		} else if (JAVA_SCRIPT_LANG.equalsIgnoreCase(lang) || "js".equalsIgnoreCase(lang)
				|| "jscript".equalsIgnoreCase(lang)) // NON-NLS
		{
			return new JavaScriptTransformation(name, code, phase);
		} else if (XPATH_SCRIPT_LANG.equalsIgnoreCase(lang)) {
			return new XPathTransformation(name, code, phase);
		}

		throw new IllegalArgumentException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
				"ScriptTransformation.unknown.language", lang));
	}

	/**
	 * Performs transformation initialization: resolves script defined expression variables.
	 */
	protected void initTransformation() {
		exprVars = new HashSet<>();
		placeHoldersMap = new HashMap<>();
		Utils.resolveExpressionVariables(exprVars, scriptCode);

		String expString = scriptCode;
		if (CollectionUtils.isNotEmpty(exprVars)) {
			String varPlh;
			int idx = 0;
			for (String eVar : exprVars) {
				varPlh = "$TNT4J_ST_TRSF_PLH" + (idx++); // NON-NLS
				expString = expString.replace(eVar, varPlh);
				placeHoldersMap.put(eVar, varPlh);
			}
		}

		ppExpression = expString;
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
