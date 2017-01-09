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

package com.jkoolcloud.tnt4j.streams.custom.parsers;

import java.text.ParseException;
import java.util.Enumeration;

import org.apache.commons.lang3.ArrayUtils;

import com.ibm.mq.pcf.MQCFGR;
import com.ibm.mq.pcf.PCFContent;
import com.ibm.mq.pcf.PCFMessage;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.custom.inputs.WmqTraceStream;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityPCFParser;

/**
 * Implements an activity data parser that assumes each activity data item is an IBM MQ activity trace
 * {@link PCFMessage}.
 * <p>
 * Parser uses {@link PCFMessage} contained parameter {@link WmqTraceStream#TRACE_MARKER} to determine which trace entry
 * to process.
 *
 * @version $Revision: 1 $
 */
public class WmqTraceParser extends ActivityPCFParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(WmqTraceParser.class);

	private static final String MQGACF_ACTIVITY_TRACE = "MQGACF_ACTIVITY_TRACE"; // NON-NLS

	/**
	 * Constructs a new WmqTraceParser.
	 */
	public WmqTraceParser() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	protected Object getParamValue(ActivityFieldDataType fDataType, String[] path, PCFMessage pcfMsg,
			PCFContent pcfContent, int i) throws ParseException {
		if (ArrayUtils.isEmpty(path) || (pcfMsg == null && pcfContent == null)) {
			return null;
		}

		String paramStr = path[i];

		if (paramStr.equals(MQGACF_ACTIVITY_TRACE)) {
			Integer traceM = (Integer) pcfMsg.getParameterValue(WmqTraceStream.TRACE_MARKER);
			PCFContent traceData = getActivityTraceGroupParameter(traceM, pcfMsg);

			return super.getParamValue(fDataType, path, pcfMsg, traceData, ++i);
		} else {
			return super.getParamValue(fDataType, path, pcfMsg, pcfContent, i);
		}
	}

	private static PCFContent getActivityTraceGroupParameter(Integer traceIndex, PCFMessage pcfMsg) {
		if (traceIndex != null) {
			Enumeration<?> prams = pcfMsg.getParameters();
			int trI = 0;
			while (prams.hasMoreElements()) {
				Object param = prams.nextElement();
				if (param instanceof MQCFGR) {
					trI++;
					if (trI == traceIndex) {
						return (MQCFGR) param;
					}
				}
			}
		}

		return pcfMsg;
	}

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - ACTIVITY TRACE MESSAGE
	 */
	@Override
	protected String getActivityDataType() {
		return "ACTIVITY TRACE MESSAGE"; // NON-NLS
	}
}
