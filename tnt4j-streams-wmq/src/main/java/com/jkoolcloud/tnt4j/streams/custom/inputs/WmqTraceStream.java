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

package com.jkoolcloud.tnt4j.streams.custom.inputs;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.ibm.mq.pcf.*;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.WmqStreamProperties;
import com.jkoolcloud.tnt4j.streams.inputs.WmqStreamPCF;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants;

/**
 * Implements a WebSphere MQ activity traces stream, where activity data is {@link PCFMessage} contained PCF parameters
 * and MQ activity trace entries (as {@link MQCFGR}). Same PCF message will be returned as next item until all trace
 * entries are processed (message gets 'consumed') and only then new PCF message is retrieved from MQ server. Stream
 * 'marks' PCF message contained trace entry as 'processed' by setting custom PCF parameter {@link #TRACE_MARKER}. Using
 * this PCF parameter parser "knows" which trace entry to process.
 * <p>
 * Stream also performs traced operations filtering using 'TraceOperations' property. I.e. setting value to
 * 'MQXF_(GET|PUT|CLOSE)' will stream only traces for 'MQXF_GET', 'MQXF_PUT' and 'MQXF_CLOSE' MQ operations.
 * <p>
 * This activity stream requires parsers that can support {@link PCFMessage} data. But primarily it is meant to be used
 * in common with {@link com.jkoolcloud.tnt4j.streams.custom.parsers.WmqTraceParser}.
 * <p>
 * This activity stream supports the following properties (in addition to those supported by {@link WmqStreamPCF}):
 * <ul>
 * <li>TraceOperations - defines traced MQ operations name filter mask (wildcard or RegEx) to process only traces of MQ
 * operations which names matches this mask. Default value - ''. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class WmqTraceStream extends WmqStreamPCF {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(WmqTraceStream.class);

	private static final int TRACES_COUNT = 919191919;
	/**
	 * Custom PCF parameter identifier to store processed PCF message trace entry index.
	 */
	public static final int TRACE_MARKER = 929292929;

	private PCFMessage pcfMessage;

	private String opName = null;
	private Pattern opNameMatcher = null;

	/**
	 * Constructs an empty WmqTraceStream. Requires configuration settings to set input source.
	 */
	public WmqTraceStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (WmqStreamProperties.PROP_TRACE_OPERATIONS.equalsIgnoreCase(name)) {
				opName = value;

				if (StringUtils.isNotEmpty(value)) {
					opNameMatcher = Pattern.compile(Utils.wildcardToRegex2(opName));
				}
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (WmqStreamProperties.PROP_TRACE_OPERATIONS.equalsIgnoreCase(name)) {
			return opName;
		}

		return super.getProperty(name);
	}

	@Override
	public PCFMessage getNextItem() throws Exception {
		if (isPCFMessageConsumed(pcfMessage)) {
			pcfMessage = super.getNextItem();

			if (pcfMessage != null) {
				boolean hasMatchingTraces = initTrace(pcfMessage);

				if (!hasMatchingTraces) {
					pcfMessage = null;
					return getNextItem();
				}
			}
		}

		return pcfMessage;
	}

	private boolean isPCFMessageConsumed(PCFMessage pcfMsg) {
		if (pcfMsg == null) {
			logger().log(OpLevel.DEBUG, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
					"WmqTraceStream.msg.consumption.null"));
			return true;
		}

		MQCFIN tcp = (MQCFIN) pcfMsg.getParameter(TRACES_COUNT);
		int tc = tcp == null ? 0 : tcp.getIntValue();
		MQCFIN tmp = (MQCFIN) pcfMsg.getParameter(TRACE_MARKER);
		int ti = tmp == null ? 0 : tmp.getIntValue();

		logger().log(OpLevel.TRACE, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
				"WmqTraceStream.msg.consumption.marker.found"), ti, tc);

		if (ti >= tc) {
			ti = -1;
		} else {
			ti = getNextMatchingTrace(pcfMsg, ti);
		}

		if (ti == -1 || ti > tc) {
			logger().log(OpLevel.DEBUG, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
					"WmqTraceStream.msg.consumption.done"));
			return true;
		} else {
			logger().log(OpLevel.DEBUG, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
					"WmqTraceStream.msg.consumption.marker.new"), ti, tc);
			tmp.setIntValue(ti);
			return false;
		}
	}

	private boolean initTrace(PCFMessage pcfMsg) {
		int trC = 0;
		int trM = 0;
		boolean opFound = false;

		Enumeration<?> prams = pcfMsg.getParameters();
		while (prams.hasMoreElements()) {
			Object param = prams.nextElement();
			if (param instanceof MQCFGR) {
				MQCFGR trace = (MQCFGR) param;
				// collectAttrs(trace);
				trC++;

				if (!opFound && opNameMatch(trace)) {
					opFound = true;
					trM = trC;
				}
			}

		}

		if (opFound) {
			pcfMsg.addParameter(TRACES_COUNT, trC);
			pcfMsg.addParameter(TRACE_MARKER, trM);

			logger().log(OpLevel.DEBUG, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
					"WmqTraceStream.trace.init.marker"), trM, trC);
		} else {
			logger().log(OpLevel.DEBUG, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
					"WmqTraceStream.trace.init.no.traces"));
		}

		return opFound;
	}

	private int getNextMatchingTrace(PCFMessage pcfMsg, int marker) {
		Enumeration<?> prams = pcfMsg.getParameters();
		int trI = 0;
		while (prams.hasMoreElements()) {
			Object param = prams.nextElement();

			if (param instanceof MQCFGR) {
				trI++;

				if (trI > marker) {
					MQCFGR trace = (MQCFGR) param;
					if (opNameMatch(trace)) {
						return trI;
					}
				}
			}
		}

		return -1;
	}

	private boolean opNameMatch(MQCFGR trace) {
		String operationName = getOpName(trace);
		boolean match = opNameMatcher == null || opNameMatcher.matcher(operationName).matches();

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqTraceStream.trace.name.match"),
				operationName, match);

		return match;
	}

	private static String getOpName(MQCFGR trace) {
		String opName = null;
		PCFParameter op = trace.getParameter(PCFConstants.MQIACF_OPERATION_ID);
		if (op != null) {
			opName = PCFConstants.lookup(((MQCFIN) op).getIntValue(), "MQXF_.*"); // NON-NLS
		}

		return opName;
	}

	// ---- R&D UTILITY CODE ---
	// private Map<String, Set<String>> tracesMap = new HashMap<String, Set<String>>();
	//
	// private void collectAttrs(MQCFGR trace) {
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
	// FileUtils.write(f, str, "UTF-8");
	// } catch (Exception exc) {
	// exc.printStackTrace();
	// }
	// }
	// ---- R&D UTILITY CODE ---
}
