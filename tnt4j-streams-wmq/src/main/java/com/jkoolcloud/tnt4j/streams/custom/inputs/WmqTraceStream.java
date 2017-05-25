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

package com.jkoolcloud.tnt4j.streams.custom.inputs;

import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.MQCFGR;
import com.ibm.mq.pcf.MQCFIN;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFParameter;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.WmqStreamProperties;
import com.jkoolcloud.tnt4j.streams.inputs.WmqStreamPCF;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.WmqUtils;

/**
 * Implements a WebSphere MQ activity traces stream, where activity data is {@link PCFMessage} contained PCF parameters
 * and MQ activity trace entries (as {@link MQCFGR}). Same PCF message will be returned as next item until all trace
 * entries are processed (message gets 'consumed') and only then new PCF message is retrieved from MQ server. Stream
 * 'marks' PCF message contained trace entry as 'processed' by setting custom PCF parameter
 * {@link WmqStreamConstants#TRACE_MARKER}. Using this PCF parameter parser "knows" which trace entry to process.
 * <p>
 * Stream also performs traced operations filtering using 'TraceOperations' and 'ExcludedRC' properties:
 * <ul>
 * <li>setting 'TraceOperations' property value to 'MQXF_(GET|PUT|CLOSE)' will stream only traces for 'MQXF_GET',
 * 'MQXF_PUT' and 'MQXF_CLOSE' MQ operations.</li>
 * <li>setting 'ExcludedRC' property value to 'MQRC_NO_MSG_AVAILABLE' will not stream MQ operations (i.e. 'MQXF_GET')
 * traces when there was no messages available in queue.</li>
 * </ul>
 * <p>
 * This activity stream requires parsers that can support {@link PCFMessage} data. But primarily it is meant to be used
 * in common with {@link com.jkoolcloud.tnt4j.streams.custom.parsers.WmqTraceParser}.
 * <p>
 * This activity stream supports the following properties (in addition to those supported by {@link WmqStreamPCF}):
 * <ul>
 * <li>TraceOperations - defines traced MQ operations name filter mask (wildcard or RegEx) to process only traces of MQ
 * operations which names matches this mask. Default value - {@code "*"}. (Optional)</li>
 * <li>ExcludedRC - defines set of excluded MQ trace events reason codes (delimited using '|' character) to process only
 * MQ trace events having reason codes not contained in this set. Set entries may be defined using both numeric and MQ
 * constant name values. Default value - {@code ""}. (Optional)</li>
 * <li>SuppressBrowseGets - flag indicating whether to exclude WMQ BROWSE type GET operation traces from streaming.
 * Default value - {@code false}. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class WmqTraceStream extends WmqStreamPCF {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(WmqTraceStream.class);

	private PCFMessage pcfMessage;

	private String opName = null;
	private Pattern opNameMatcher = null;

	private String rcExclude = null;
	private Set<Integer> excludedRCs = null;

	private boolean suppressBrowseGets = false;

	// private Map<String, MQCFGR> dupIds = new HashMap<>(); //TODO: duplicate traces handling if such may occur

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
			} else if (WmqStreamProperties.PROP_EXCLUDED_REASON_CODES.equalsIgnoreCase(name)) {
				rcExclude = value;

				if (StringUtils.isNotEmpty(value)) {
					String[] erca = Utils.splitValue(rcExclude);

					excludedRCs = new HashSet<>(erca.length);

					Integer eRC;
					for (String erc : erca) {
						try {
							eRC = WmqUtils.getParamId(erc);
						} catch (NoSuchElementException exc) {
							logger().log(OpLevel.WARNING, StreamsResources.getString(
									WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqTraceStream.invalid.rc"), erc);
							continue;
						}

						excludedRCs.add(eRC);
					}
				}
			} else if (WmqStreamProperties.PROP_SUPPRESS_BROWSE_GETS.equalsIgnoreCase(name)) {
				suppressBrowseGets = Boolean.parseBoolean(value);
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (WmqStreamProperties.PROP_TRACE_OPERATIONS.equalsIgnoreCase(name)) {
			return opName;
		}

		if (WmqStreamProperties.PROP_EXCLUDED_REASON_CODES.equalsIgnoreCase(name)) {
			return rcExclude;
		}

		if (WmqStreamProperties.PROP_SUPPRESS_BROWSE_GETS.equalsIgnoreCase(name)) {
			return suppressBrowseGets;
		}

		return super.getProperty(name);
	}

	@Override
	public PCFMessage getNextItem() throws Exception {
		while (true) {
			if (isPCFMessageConsumed(pcfMessage)) {
				pcfMessage = super.getNextItem();

				if (pcfMessage != null) {
					boolean hasMatchingTraces = initTrace(pcfMessage);

					if (!hasMatchingTraces) {
						pcfMessage = null;
						continue;
					}
				}
			}

			return pcfMessage;
		}
	}

	private boolean isPCFMessageConsumed(PCFMessage pcfMsg) {
		if (pcfMsg == null) {
			logger().log(OpLevel.DEBUG, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
					"WmqTraceStream.msg.consumption.null"));
			return true;
		}

		MQCFIN tcp = (MQCFIN) pcfMsg.getParameter(WmqStreamConstants.TRACES_COUNT);
		int tc = tcp == null ? 0 : tcp.getIntValue();
		MQCFIN tmp = (MQCFIN) pcfMsg.getParameter(WmqStreamConstants.TRACE_MARKER);
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
			PCFParameter param = (PCFParameter) prams.nextElement();
			if (WmqUtils.isTraceParameter(param)) {
				MQCFGR trace = (MQCFGR) param;
				// WmqUtils.collectAttrs(trace);
				trC++;

				if (!opFound && isTraceRelevant(trace)) {
					opFound = true;
					trM = trC;
				}
			}

		}

		if (opFound) {
			pcfMsg.addParameter(WmqStreamConstants.TRACES_COUNT, trC);
			pcfMsg.addParameter(WmqStreamConstants.TRACE_MARKER, trM);

			logger().log(OpLevel.DEBUG, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
					"WmqTraceStream.trace.init.marker"), trM, trC);
		} else {
			logger().log(OpLevel.DEBUG, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
					"WmqTraceStream.trace.init.no.traces"));
		}

		// if (!dupIds.isEmpty()) {
		// dupIds.clear();
		// }

		return opFound;
	}

	private int getNextMatchingTrace(PCFMessage pcfMsg, int marker) {
		Enumeration<?> prams = pcfMsg.getParameters();
		int trI = 0;
		while (prams.hasMoreElements()) {
			PCFParameter param = (PCFParameter) prams.nextElement();

			if (WmqUtils.isTraceParameter(param)) {
				trI++;

				if (trI > marker) {
					MQCFGR trace = (MQCFGR) param;
					if (isTraceRelevant(trace)) {
						return trI;
					}
				}
			}
		}

		return -1;
	}

	private boolean isTraceRelevant(MQCFGR trace) {
		boolean relevant = opNameMatch(trace) && rcMatch(trace) && !isBrowseGetSuppressed(trace);

		if (!relevant) {
			logger().log(OpLevel.TRACE, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
					"WmqTraceStream.trace.suppressed"), trace);
		}

		return relevant;
	}

	private boolean opNameMatch(MQCFGR trace) {
		// Object msgId = trace.getParameterValue(PCFConstants.MQBACF_MSG_ID);
		// String msgIdStr = msgId instanceof byte[] ? Utils.toHexString((byte[]) msgId) : "<EMPTY>";
		//
		// MQCFGR dTrace = dupIds.put(msgIdStr, trace);
		// if (dTrace != null) {
		// logger().log(OpLevel.ERROR, "Duplicate message found: {0} \n----\n{1} \n----\n{2}\n-----", msgIdStr,
		// dTrace.toString(), trace.toString());
		// }

		String operationName = WmqUtils.getOpName(trace);
		boolean match = operationName == null || opNameMatcher == null
				|| opNameMatcher.matcher(operationName).matches();

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqTraceStream.trace.name.match"),
				operationName, match);

		return match;
	}

	private boolean rcMatch(MQCFGR trace) {
		Integer traceRC = WmqUtils.getRC(trace);

		boolean match = traceRC == null || excludedRCs == null || !excludedRCs.contains(traceRC);

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME, "WmqTraceStream.trace.rc.match"),
				traceRC == null ? "null" : MQConstants.lookupReasonCode(traceRC), match);

		return match;
	}

	private boolean isBrowseGetSuppressed(MQCFGR trace) {
		if (!suppressBrowseGets) {
			return false;
		}

		boolean browseGet = false;
		PCFParameter go = trace.getParameter(MQConstants.MQIACF_GET_OPTIONS);
		if (go != null) {
			int getOptions = ((MQCFIN) go).getIntValue();

			browseGet = Utils.matchAny(getOptions, MQConstants.MQGMO_BROWSE_FIRST | MQConstants.MQGMO_BROWSE_NEXT
					| MQConstants.MQGMO_BROWSE_MSG_UNDER_CURSOR);

			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
							"WmqTraceStream.trace.browse.get"),
					getOptions, MQConstants.decodeOptions(getOptions, "MQGMO_.*"), browseGet); // NON-NLS
		}

		return browseGet;
	}
}
