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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.Property;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

/**
 * Data value transformation based on Groovy code/expressions.
 *
 * @version $Revision: 1 $
 *
 * @see Binding
 * @see GroovyShell#evaluate(String, String)
 */
public class GroovyTransformation extends AbstractScriptTransformation<Object> {

	/**
	 * Constructs a new GroovyTransformation.
	 *
	 * @param name
	 *            transformation name
	 * @param scriptCode
	 *            transformation script code
	 */
	public GroovyTransformation(String name, String scriptCode) {
		super(name, scriptCode);
	}

	/**
	 * Constructs a new GroovyTransformation.
	 *
	 * @param name
	 *            transformation name
	 * @param scriptCode
	 *            transformation script code
	 * @param phase
	 *            activity data value resolution phase
	 */
	public GroovyTransformation(String name, String scriptCode, Phase phase) {
		super(name, scriptCode, phase);
	}

	@Override
	public Object transform(Object value, ActivityInfo ai) throws TransformationException {
		Binding binding = new Binding();
		binding.setVariable(FIELD_VALUE_VARIABLE_EXPR, value);

		if (ai != null && CollectionUtils.isNotEmpty(exprVars)) {
			for (String eVar : exprVars) {
				Property eKV = resolveFieldKeyAndValue(eVar, ai);

				binding.setVariable(eKV.getKey(), eKV.getValue());
			}
		}

		GroovyShell shell = new GroovyShell(binding, StreamsScriptingUtils.getDefaultGroovyCompilerConfig());

		try {
			return shell.evaluate(getScriptCode(),
					StringUtils.isEmpty(getName()) ? "GroovyTransformScript" : getName()); // NON-NLS
		} catch (Exception exc) {
			throw new TransformationException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ValueTransformation.transformation.failed", getName(), getPhase()), exc);
		}
	}
}
