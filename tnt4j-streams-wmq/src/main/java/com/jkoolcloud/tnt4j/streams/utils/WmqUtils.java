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

import java.util.NoSuchElementException;

import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.MQCFGR;
import com.ibm.mq.pcf.MQCFIN;
import com.ibm.mq.pcf.PCFContent;
import com.ibm.mq.pcf.PCFParameter;

/**
 * WMQ utility methods used by TNT4J-Streams-WMQ module.
 *
 * @version $Revision: 1 $
 */
public class WmqUtils {
	// ---- R&D UTILITY CODE ---
	// private static Map<String, Set<String>> tracesMap = new HashMap<String, Set<String>>();
	//
	// public static void collectAttrs(MQCFGR trace) {
	// boolean changed = false;
	// String opKey = getOpName(trace);
	//
	// Set<String> opParamSet = tracesMap.get(opKey);
	// if (opParamSet == null) {
	// opParamSet = new HashSet<String>();
	// tracesMap.put(opKey, opParamSet);
	// changed = true;
	// }
	//
	// Set<String> allOpsSet = tracesMap.get("ALL_OPERATIONS_SET");
	// if (allOpsSet == null) {
	// allOpsSet = new HashSet<String>();
	// tracesMap.put("ALL_OPERATIONS_SET", allOpsSet);
	// changed = true;
	// }
	//
	// Enumeration<?> prams = trace.getParameters();
	// while (prams.hasMoreElements()) {
	// PCFParameter param = (PCFParameter) prams.nextElement();
	// String pString = PCFConstants.lookupParameter(param.getParameter());
	//
	// if (!opParamSet.contains(pString)) {
	// opParamSet.add(pString);
	// changed = true;
	// }
	//
	// if (!allOpsSet.contains(pString)) {
	// allOpsSet.add(pString);
	// changed = true;
	// }
	// }
	//
	// if (changed) {
	// write(tracesMap);
	// }
	// }
	//
	// private static void write(Map<String, Set<String>> map) {
	// String str = "";
	//
	// for (Map.Entry<String, Set<String>> e : map.entrySet()) {
	// str += e.getKey() + "\n";
	// for (String s : e.getValue()) {
	// str += " " + s + "\n";
	// }
	// }
	//
	// File f = new File("TRACES_MAP1.log");
	// try {
	// FileUtils.write(f, str, Utils.UTF8);
	// } catch (Exception exc) {
	// exc.printStackTrace();
	// }
	// }
	// ---- R&D UTILITY CODE ---

	/**
	 * Checks whether provided PCF parameter contains MQ activity trace data.
	 *
	 * @param param
	 *            PCF parameter to check
	 * @return {@code true} if parameter is of type {@link com.ibm.mq.pcf.MQCFGR} and parameter's parameter field value
	 *         is {@code MQGACF_ACTIVITY_TRACE}, {@code false} - otherwise
	 */
	public static boolean isTraceParameter(PCFParameter param) {
		return param.getParameter() == MQConstants.MQGACF_ACTIVITY_TRACE && param instanceof MQCFGR;
	}

	/**
	 * Resolves operation id parameter {@link MQConstants#MQIACF_OPERATION_ID} value from WMQ activity trace PCF data
	 * object and translates it to WMQ operation name constant {@code "MQXF_"}.
	 *
	 * @param trace
	 *            wmq activity trace PCF data
	 * @return resolved operation name, or {@code null} if no operation id parameter found in PCF content
	 *
	 * @see MQConstants#lookup(int, String)
	 */
	public static String getOpName(PCFContent trace) {
		String opName = null;
		PCFParameter op = trace.getParameter(MQConstants.MQIACF_OPERATION_ID);
		if (op != null) {
			opName = MQConstants.lookup(((MQCFIN) op).getIntValue(), "MQXF_.*"); // NON-NLS
		}

		return opName;
	}

	/**
	 * Resolves reason code parameter {@link MQConstants#MQIACF_REASON_CODE} value from WMQ activity trace PCF data
	 * object.
	 * 
	 * @param trace
	 *            wmq activity trace PCF data
	 * @return resolved reason code, or {@code null} if no reson code parameter found in PCF content
	 */
	public static Integer getRC(PCFContent trace) {
		Integer traceRC = null;
		PCFParameter rc = trace.getParameter(MQConstants.MQIACF_REASON_CODE);
		if (rc != null) {
			traceRC = ((MQCFIN) rc).getIntValue();
		}

		return traceRC;
	}

	/**
	 * Translates PCF parameter MQ constant name to constant numeric value.
	 *
	 * @param paramIdStr
	 *            PCF parameter MQ constant name
	 *
	 * @return PCF parameter MQ constant numeric value
	 */
	public static Integer getParamId(String paramIdStr) throws NoSuchElementException {
		try {
			return Integer.parseInt(paramIdStr);
		} catch (NumberFormatException nfe) {
			return MQConstants.getIntValue(paramIdStr);
		}
	}
}
