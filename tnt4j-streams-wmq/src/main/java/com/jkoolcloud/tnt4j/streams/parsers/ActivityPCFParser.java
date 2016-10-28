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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.ArrayUtils;

import com.ibm.mq.pcf.*;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.WmqParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants;

/**
 * Implements an activity data parser that assumes each activity data item is an {@link PCFMessage} where each field is
 * represented by a PCF parameter and the PCF parameter MQ constant name/value is used to map each field onto its
 * corresponding activity field.
 * <p>
 * PCF message can have grouped parameters - all message will have header {@link MQCFH} and may have {@link MQCFGR} type
 * parameters. To access PCF message header fields use {@value #HEAD_MQCFH} expression with header field name separated
 * using '{@value StreamsConstants#DEFAULT_PATH_DELIM}' (i.e. 'MQCFH.CompCode'). To access inner {@link MQCFGR} (or
 * inner inner and so on) parameters use group parameter MQ constant name/value with grouped parameter MQ constant
 * name/value separated using '{@value StreamsConstants#DEFAULT_PATH_DELIM}' (i.e.
 * 'MQGACF_ACTIVITY_TRACE.MQIACF_COMP_CODE').
 * <p>
 * This parser supports the following properties:
 * <ul>
 * <li>TranslateNumValues - indicates that parser should translate resolved numeric values to corresponding MQ constant
 * names if possible. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityPCFParser extends GenericActivityParser<PCFMessage> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityPCFParser.class);

	private static final String HEAD_MQCFH = "MQCFH"; // NON-NLS

	private boolean translateNumValues = true;

	/**
	 * Constructs a new ActivityPCFParser.
	 */
	public ActivityPCFParser() {
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

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();

			if (WmqParserProperties.PROP_TRANSLATE_NUM_VALUES.equalsIgnoreCase(name)) {
				translateNumValues = Boolean.parseBoolean(value);

				logger().log(OpLevel.DEBUG,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
						name, value);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link PCFMessage}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return PCFMessage.class.isInstance(data);
	}

	/**
	 * Gets field raw data value resolved by locator.
	 *
	 * @param locator
	 *            activity field locator
	 * @param data
	 *            PCF message representing activity object data
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 *
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, PCFMessage data, AtomicBoolean formattingNeeded)
			throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();
		String[] path = Utils.getNodePath(locStr, StreamsConstants.DEFAULT_PATH_DELIM);
		val = getParamValue(path, data, data, 0);

		logger().log(OpLevel.TRACE, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
				"ActivityPCFParser.resolved.pcf.value"), locStr, toString(val));

		return val;
	}

	/**
	 * Resolves PCF parameter value from provided {@link PCFMessage} and {@link PCFContent}: {@link PCFMessage} or
	 * {@link MQCFGR}.
	 *
	 * @param path
	 *            parameter path as array of PCF parameter identifiers
	 * @param pcfMsg
	 *            PCF message
	 * @param pcfContent
	 *            PCF content data (message or parameters group)
	 * @param i
	 *            processed path element index
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 *
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value
	 */
	protected Object getParamValue(String[] path, PCFMessage pcfMsg, PCFContent pcfContent, int i)
			throws ParseException {
		if (ArrayUtils.isEmpty(path) || (pcfMsg == null && pcfContent == null)) {
			return null;
		}

		Object val = null;
		String paramStr = path[i];

		if (i == 0 && paramStr.equals(HEAD_MQCFH)) {
			val = resolvePCFHeaderValue(path[i + 1], pcfMsg);
		} else {
			Integer paramId = getParamId(paramStr);

			if (paramId == null) {
				throw new ParseException(StreamsResources.getStringFormatted(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
						"ActivityPCFParser.unresolved.pcf.parameter", paramStr), pcfMsg.getMsgSeqNumber());
			}

			PCFParameter param = pcfContent.getParameter(paramId);

			if (i < path.length - 1 && param instanceof MQCFGR) {
				val = getParamValue(path, pcfMsg, (MQCFGR) param, ++i);
			} else {
				val = resolvePCFParamValue(param);
			}
		}

		return val;
	}

	private Object resolvePCFHeaderValue(String hAttrName, PCFMessage pcfMsg) {
		Object val = null;
		if ("command".equals(hAttrName.toLowerCase())) { // NON-NLS
			val = pcfMsg.getCommand();
			if (translateNumValues) {
				val = PCFConstants.lookup(val, "MQCMD_.*"); // NON-NLS
			}
		} else if ("msgseqnumber".equals(hAttrName.toLowerCase())) { // NON-NLS
			val = pcfMsg.getMsgSeqNumber();
		} else if ("control".equals(hAttrName.toLowerCase())) { // NON-NLS
			val = pcfMsg.getControl();
			if (translateNumValues) {
				val = PCFConstants.lookup(val, "MQCFC_.*"); // NON-NLS
			}
		} else if ("compcode".equals(hAttrName.toLowerCase())) { // NON-NLS
			val = pcfMsg.getCompCode();
			if (translateNumValues && val instanceof Integer) {
				val = PCFConstants.lookupCompCode((Integer) val);
			}
		} else if ("reason".equals(hAttrName.toLowerCase())) { // NON-NLS
			val = pcfMsg.getReason();
			if (translateNumValues && val instanceof Integer) {
				val = PCFConstants.lookupReasonCode((Integer) val);
			}
		} else if ("parametercount".equals(hAttrName.toLowerCase())) { // NON-NLS
			val = pcfMsg.getParameterCount();
		}

		return val;
	}

	/**
	 * Translates PCF parameter MQ constant name to constant numeric value.
	 *
	 * @param paramIdStr
	 *            PCF parameter MQ constant name
	 *
	 * @return PCF parameter MQ constant numeric value
	 */
	protected static Integer getParamId(String paramIdStr) {
		Integer paramId = null;

		try {
			paramId = Integer.parseInt(paramIdStr);
		} catch (NumberFormatException nfe) {
			try {
				paramId = PCFConstants.getIntValue(paramIdStr);
			} catch (NoSuchElementException nsee) {
			}
		}

		return paramId;
	}

	/**
	 * Resolves PCF parameter value. If parser property `TranslateNumValues` is set to {@code true} - then if possible,
	 * resolved numeric value gets translated to corresponding MQ constant name.
	 *
	 * @param param
	 *            PCF parameter to resolve value from
	 *
	 * @return resolved PCF parameter value
	 */
	protected Object resolvePCFParamValue(PCFParameter param) {
		if (param == null) {
			return null;
		}

		Object val = param.getValue();

		if (val instanceof String) {
			val = ((String) val).trim();
		}

		if (translateNumValues) {
			switch (param.getParameter()) {
			case PCFConstants.MQIA_APPL_TYPE:
				val = PCFConstants.lookup(val, "MQAT_.*"); // NON-NLS
				break;
			case PCFConstants.MQIACF_API_CALLER_TYPE:
				val = PCFConstants.lookup(val, "MQXACT_.*"); // NON-NLS
				break;
			case PCFConstants.MQIACF_API_ENVIRONMENT:
				val = PCFConstants.lookup(val, "MQXE_.*"); // NON-NLS
				break;
			case PCFConstants.MQIACF_APPL_FUNCTION_TYPE:
				val = PCFConstants.lookup(val, "MQFUN_.*"); // NON-NLS
				break;
			case PCFConstants.MQIA_PLATFORM:
				val = PCFConstants.lookup(val, "MQPL_.*"); // NON-NLS
				break;
			case PCFConstants.MQIACF_OPERATION_ID:
				val = PCFConstants.lookup(val, "MQXF_.*"); // NON-NLS
				break;
			case PCFConstants.MQIACF_OBJECT_TYPE:
			case PCFConstants.MQIACF_RESOLVED_TYPE:
				val = PCFConstants.lookup(val, "MQOT_.*"); // NON-NLS
				break;
			case PCFConstants.MQIACF_COMP_CODE:
				val = PCFConstants.lookup(val, "MQCC_.*"); // NON-NLS
				break;
			case PCFConstants.MQIACF_MSG_TYPE:
				val = PCFConstants.lookup(val, "MQMT_.*"); // NON-NLS
				break;
			default:
				break;
			}
		}

		return val;
	}

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - PCF MESSAGE
	 */
	@Override
	protected String getActivityDataType() {
		return "PCF MESSAGE"; // NON-NLS
	}
}
