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

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.collections4.CollectionUtils;

import com.jkoolcloud.tnt4j.core.Property;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.LoggerUtils;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils;

/**
 * Data value transformation based on JavaScript code/expressions.
 * 
 * @version $Revision: 2 $
 *
 * @see com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils#compileJSScript(String)
 * @see javax.script.CompiledScript#eval(javax.script.Bindings)
 */
public class JavaScriptTransformation extends AbstractScriptTransformation<Object> {
	private static final EventSink LOGGER = LoggerUtils.getLoggerSink(JavaScriptTransformation.class);

	private CompiledScript script;

	/**
	 * Constructs a new JavaScriptTransformation.
	 *
	 * @param name
	 *            transformation name
	 * @param scriptCode
	 *            transformation script code
	 */
	public JavaScriptTransformation(String name, String scriptCode) {
		super(name, scriptCode);
	}

	/**
	 * Constructs a new JavaScriptTransformation.
	 *
	 * @param name
	 *            transformation name
	 * @param scriptCode
	 *            transformation script code
	 * @param phase
	 *            activity data value resolution phase
	 */
	public JavaScriptTransformation(String name, String scriptCode, Phase phase) {
		super(name, scriptCode, phase);
	}

	@Override
	protected EventSink getLogger() {
		return LOGGER;
	}

	@Override
	protected String getHandledLanguage() {
		return StreamsScriptingUtils.JAVA_SCRIPT_LANG;
	}

	@Override
	protected void initTransformation() {
		super.initTransformation();

		try {
			script = StreamsScriptingUtils.compileJSScript(getExpression());
		} catch (ScriptException exc) {
			throw new IllegalArgumentException(
					StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ScriptTransformation.invalid.script", getName(), getScriptCode()),
					exc);
		}
	}

	@Override
	public Object transform(Object value, ActivityInfo ai) throws TransformationException {
		Bindings bindings = new SimpleBindings();
		bindings.put(StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR, value);

		if (ai != null && CollectionUtils.isNotEmpty(exprVars)) {
			for (String eVar : exprVars) {
				Property eKV = resolveFieldKeyAndValue(eVar, ai);

				bindings.put(eKV.getKey(), eKV.getValue());
			}
		}

		try {
			Object tValue = script.eval(bindings);

			logEvaluationResult(bindings, tValue);

			return tValue;
		} catch (Exception exc) {
			throw new TransformationException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ValueTransformation.transformation.failed", getName(), getPhase()), exc);
		}
	}
}
