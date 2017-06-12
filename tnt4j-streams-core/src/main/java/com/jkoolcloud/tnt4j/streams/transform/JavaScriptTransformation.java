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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils;

/**
 * Data value transformation based on JavaScript code/expressions.
 * 
 * @version $Revision: 1 $
 *
 * @see ScriptEngineManager
 * @see ScriptEngine#eval(String)
 */
public class JavaScriptTransformation extends AbstractScriptTransformation<Object> {

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

	@Override
	public Object transform(Object value) throws TransformationException {
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName(JAVA_SCRIPT_LANG);
		factory.put(FIELD_VALUE_VARIABLE_EXPR, value);

		try {
			return engine.eval(StreamsScriptingUtils.addDefaultJSScriptImports(getScriptCode()));
		} catch (Exception exc) {
			throw new TransformationException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"ValueTransformation.transformation.failed", getName()), exc);
		}
	}
}
