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

package com.jkoolcloud.tnt4j.streams.utils;

import java.util.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

/**
 * General scripting utility methods used by TNT4J-Streams.
 * 
 * @version $Revision: 1 $
 */
public final class StreamsScriptingUtils {
	/**
	 * Constant for field value variable name used in script/expression code.
	 */
	public static final String FIELD_VALUE_VARIABLE_NAME = "fieldValue"; // NON-NLS
	/**
	 * Constant for field value variable expression used in script/expression code.
	 */
	public static final String FIELD_VALUE_VARIABLE_EXPR = '$' + FIELD_VALUE_VARIABLE_NAME;

	/**
	 * Constant for name of scripting/expression language {@value}.
	 */
	public static final String GROOVY_LANG = "groovy"; // NON-NLS
	/**
	 * Constant for name of scripting/expression language {@value}.
	 */
	public static final String JAVA_SCRIPT_LANG = "javascript"; // NON-NLS
	/**
	 * Constant for name of scripting/expression language {@value}.
	 */
	public static final String XPATH_SCRIPT_LANG = "xpath"; // NON-NLS

	private static final String SCRIPTING_CFG_PROPERTIES = "scripting.properties"; // NON-NLS
	private static final String IMPORT_PACKAGES_PROP_KEY_SUFFIX = ".scripting.import.packages"; // NON-NLS

	private static CompilerConfiguration DEFAULT_GROOVY_CONFIGURATION;
	private static String DEFAULT_JS_CODE_IMPORTS;

	private static final Set<String> DEFAULT_IMPORT_PACKAGES = new HashSet<>();

	static {
		initDefaultImportPackages();
	}

	private StreamsScriptingUtils() {
	}

	/**
	 * Initiates default set of Java API imported packages.
	 */
	private static void initDefaultImportPackages() {
		try {
			Properties p = Utils.loadPropertiesResources(SCRIPTING_CFG_PROPERTIES);

			for (String pName : p.stringPropertyNames()) {
				if (pName.endsWith(IMPORT_PACKAGES_PROP_KEY_SUFFIX)) {
					String importPackages = p.getProperty(pName);

					if (StringUtils.isNotEmpty(importPackages)) {
						String[] pArray = importPackages.split(";");

						Collections.addAll(DEFAULT_IMPORT_PACKAGES, pArray);
					}
				}
			}
		} catch (Exception exc) {
		}
	}

	/**
	 * Returns Groovy compiler configuration containing default set of Java API imported packages.
	 * 
	 * @return Groovy compiler compiler configuration containing default set of Java API imported packages
	 */
	public static CompilerConfiguration getDefaultGroovyCompilerConfig() {
		if (DEFAULT_GROOVY_CONFIGURATION == null) {
			DEFAULT_GROOVY_CONFIGURATION = initDefaultGroovyCompilerConfig();
		}

		return DEFAULT_GROOVY_CONFIGURATION;
	}

	private static CompilerConfiguration initDefaultGroovyCompilerConfig() {
		CompilerConfiguration cc = new CompilerConfiguration();

		// inject default imports
		ImportCustomizer ic = new ImportCustomizer();

		for (String pckg : DEFAULT_IMPORT_PACKAGES) {
			ic.addStarImports(pckg);
		}

		cc.addCompilationCustomizers(ic);

		return cc;
	}

	/**
	 * Wraps JS script code with default set of Java API imported packages.
	 *
	 * @param script
	 *            script code to wrap with default imports
	 * @return default imports wrapped JS script code
	 */
	public static String addDefaultJSScriptImports(String script) {
		if (DEFAULT_JS_CODE_IMPORTS == null) {
			DEFAULT_JS_CODE_IMPORTS = initDefaultJSScriptImports();
		}

		return DEFAULT_JS_CODE_IMPORTS + script;
	}

	private static String initDefaultJSScriptImports() {
		StringBuilder sb = new StringBuilder();

		for (String pckg : DEFAULT_IMPORT_PACKAGES) {
			sb.append("importPackage(").append(pckg).append(");\n"); // NON-NLS
		}

		return sb.toString();
	}

	/**
	 * Adds package name to default set of Java API imported packages.
	 * 
	 * @param pckg
	 *            package name to add
	 */
	public static void registerDefaultImportPackage(String pckg) {
		DEFAULT_IMPORT_PACKAGES.add(pckg);

		if (DEFAULT_GROOVY_CONFIGURATION != null) {
			List<CompilationCustomizer> ccList = DEFAULT_GROOVY_CONFIGURATION.getCompilationCustomizers();

			for (CompilationCustomizer cc : ccList) {
				if (cc instanceof ImportCustomizer) {
					((ImportCustomizer) cc).addStarImports(pckg);
				}
			}
		}

		if (DEFAULT_JS_CODE_IMPORTS != null) {
			StringBuilder sb = new StringBuilder(DEFAULT_JS_CODE_IMPORTS);

			sb.append("importPackage(").append(pckg).append(");\n"); // NON-NLS

			DEFAULT_JS_CODE_IMPORTS = sb.toString();
		}
	}

	/**
	 * Builds string, describing script based evaluation expression.
	 *
	 * @param userExpression
	 *            used defined evaluation expression
	 * @param vars
	 *            variables binding map
	 * @return string describing script based evaluation expression
	 */
	public static String describeExpression(String userExpression, Map<String, Object> vars, String lang,
			Collection<String> expVars, Map<String, String> phMap) {
		StringBuilder expDescStr = new StringBuilder();
		expDescStr.append("'").append(lang).append(":").append(userExpression).append("'"); // NON-NLS

		StringBuilder varStr = new StringBuilder();
		if (CollectionUtils.isNotEmpty(expVars)) {
			for (String eVar : expVars) {
				String vph = phMap.get(eVar);
				StreamsScriptingUtils.appendVariable(varStr, eVar, vars.get(vph));
			}
		}

		Object fValue = vars.get(FIELD_VALUE_VARIABLE_EXPR);
		if (fValue != null) {
			StreamsScriptingUtils.appendVariable(varStr, FIELD_VALUE_VARIABLE_EXPR, fValue);
		}

		if (varStr.length() > 0) {
			expDescStr.append(" (").append(varStr).append(")"); // NON-NLS
		}

		return expDescStr.toString();
	}

	/**
	 * Appends variable definition to expression description string builder <tt>sb</tt>.
	 *
	 * @param sb
	 *            string builder to append
	 * @param varName
	 *            variable name
	 * @param varValue
	 *            variable value
	 */
	public static void appendVariable(StringBuilder sb, String varName, Object varValue) {
		if (sb.length() > 0) {
			sb.append("; "); // NON-NLS
		}

		sb.append(Utils.getVarName(varName)).append("=").append(toString(varValue)); // NON-NLS
	}

	/**
	 * Returns the appropriate string representation for the specified object.
	 * <p>
	 * If <tt>obj</tt> is {@link String}, it gets surrounded by {@code "} chars. If <tt>obj</tt> is
	 * {@link java.lang.Character}, it gets surrounded by {@code '} character.
	 *
	 * @param obj
	 *            object to convert to string representation
	 * @return string representation of object
	 */
	public static String toString(Object obj) {
		if (obj instanceof String) {
			return Utils.surround(String.valueOf(obj), "\""); // NON-NLS
		}

		if (obj instanceof Character) {
			return Utils.surround(String.valueOf(obj), "'"); // NON-NLS
		}

		return Utils.toString(obj);
	}
}
