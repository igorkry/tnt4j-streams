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

package com.jkoolcloud.tnt4j.streams.custom.parsers;

import java.text.ParseException;
import java.util.Enumeration;

import org.apache.commons.lang3.ArrayUtils;

import com.ibm.mq.pcf.MQCFGR;
import com.ibm.mq.pcf.PCFContent;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFParameter;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityPCFParser;
import com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.WmqUtils;

/**
 * Implements an activity data parser that assumes each activity data item is an IBM MQ {@link PCFMessage} activity
 * trace parameters group ({@link MQCFGR} having parameter value {@code MQGACF_ACTIVITY_TRACE}).
 * <p>
 * Parser uses {@link PCFMessage} contained parameter {@link WmqStreamConstants#TRACE_MARKER} to determine which trace
 * entry to process.
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
	protected Object getRawDataAsMessage(PCFContent pcfContent) {
		PCFContent traceContent = strip(pcfContent, getTraceMarker(pcfContent));

		return traceContent.toString();
	}

	private static Integer getTraceMarker(PCFContent pcfContent) {
		return (Integer) pcfContent.getParameterValue(WmqStreamConstants.TRACE_MARKER);
	}

	@Override
	protected Object getParamValue(ActivityFieldDataType fDataType, String[] path, PCFContent pcfContent, int i)
			throws ParseException {
		if (ArrayUtils.isEmpty(path) || pcfContent == null) {
			return null;
		}

		String paramStr = path[i];

		if (paramStr.equals(MQGACF_ACTIVITY_TRACE)) {
			PCFContent traceData = getActivityTraceGroupParameter(getTraceMarker(pcfContent), pcfContent);

			return super.getParamValue(fDataType, path, traceData, ++i);
		} else {
			return super.getParamValue(fDataType, path, pcfContent, i);
		}
	}

	/**
	 * Returns PCF content position identifier to know where event has occurred.
	 *
	 * @param pcfContent
	 *            PCF content to get position value
	 * @return {@link WmqStreamConstants#TRACE_MARKER} parameter value or does same as
	 *         {@link ActivityPCFParser#getPCFPosition(PCFContent)} if parameter value is {@code null}
	 */
	@Override
	protected int getPCFPosition(PCFContent pcfContent) {
		Integer traceM = getTraceMarker(pcfContent);

		return traceM == null ? super.getPCFPosition(pcfContent) : traceM;
	}

	private static PCFContent getActivityTraceGroupParameter(Integer traceIndex, PCFContent pcfContent) {
		if (traceIndex != null) {
			Enumeration<?> prams = pcfContent.getParameters();
			int trI = 0;
			while (prams.hasMoreElements()) {
				PCFParameter param = (PCFParameter) prams.nextElement();
				if (WmqUtils.isTraceParameter(param)) {
					trI++;
					if (trI == traceIndex) {
						return (MQCFGR) param;
					}
				}
			}
		}

		return pcfContent;
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

	/**
	 * Strips off PCF message MQ activity trace parameters leaving only one - corresponding trace marker value.
	 *
	 * @param pcfContent
	 *            PCF message containing MQ activity traces
	 * @param traceMarker
	 *            processed MQ activity trace marker
	 * @return PCF message copy containing only one MQ activity trace marked by trace marker
	 */
	private static PCFContent strip(PCFContent pcfContent, int traceMarker) {
		if (pcfContent instanceof PCFMessage) {
			PCFMessage pcfMsg = (PCFMessage) pcfContent;
			PCFMessage msgCpy = new PCFMessage(pcfMsg.getType(), pcfMsg.getCommand(), pcfMsg.getMsgSeqNumber(),
					pcfMsg.getControl() == 1);

			Enumeration<?> params = pcfContent.getParameters();
			int trI = 0;
			while (params.hasMoreElements()) {
				PCFParameter param = (PCFParameter) params.nextElement();

				if (param.getParameter() == WmqStreamConstants.TRACE_MARKER
						|| param.getParameter() == WmqStreamConstants.TRACES_COUNT) {
					continue;
				}

				if (WmqUtils.isTraceParameter(param)) {
					trI++;
					if (trI != traceMarker) {
						continue;
					}
				}

				msgCpy.addParameter(param);
			}

			return msgCpy;
		} else {
			return pcfContent;
		}
	}
}
