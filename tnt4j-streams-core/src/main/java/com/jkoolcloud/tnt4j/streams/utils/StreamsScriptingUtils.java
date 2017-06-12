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

package com.jkoolcloud.tnt4j.streams.utils;

import java.util.*;

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
			Properties p = Utils.loadPropertiesResources("scripting.properties"); // NON-NLS

			for (String pName : p.stringPropertyNames()) {
				if (pName.endsWith(".scripting.import.packages")) { // NON-NLS
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
}
