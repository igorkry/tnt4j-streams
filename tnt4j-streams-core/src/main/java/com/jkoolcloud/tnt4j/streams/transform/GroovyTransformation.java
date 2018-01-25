/*
 * Copyright 2014-2018 JKOOL, LLC.
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
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
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
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(GroovyTransformation.class);

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
	protected EventSink getLogger() {
		return LOGGER;
	}

	@Override
	protected String getHandledLanguage() {
		return StreamsScriptingUtils.GROOVY_LANG;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object transform(Object value, ActivityInfo ai) throws TransformationException {
		Binding binding = new Binding();
		binding.setVariable(StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR, value);

		if (ai != null && CollectionUtils.isNotEmpty(exprVars)) {
			for (String eVar : exprVars) {
				Property eKV = resolveFieldKeyAndValue(eVar, ai);

				binding.setVariable(eKV.getKey(), eKV.getValue());
			}
		}

		GroovyShell shell = new GroovyShell(binding, StreamsScriptingUtils.getDefaultGroovyCompilerConfig());

		try {
			Object tValue = shell.evaluate(getExpression(),
					StringUtils.isEmpty(getName()) ? "GroovyTransformScript" : getName()); // NON-NLS

			logEvaluationResult(binding.getVariables(), tValue);

			return tValue;
		} catch (Exception exc) {
			throw new TransformationException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ValueTransformation.transformation.failed", getName(), getPhase()), exc);
		}
	}
}
