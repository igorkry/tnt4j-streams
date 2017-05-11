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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.ArrayUtils;

import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.*;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.WmqParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldDataType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Implements an activity data parser that assumes each activity data item is an {@link PCFContent} ({@link PCFMessage}
 * or {@link MQCFGR}) where each field is represented by a PCF parameter and the PCF parameter MQ constant name/value is
 * used to map each field onto its corresponding activity field.
 * <p>
 * PCF message can have grouped parameters - all message will have header {@link MQCFH} and may have {@link MQCFGR} type
 * parameters. To access PCF message header fields use 'MQCFH' expression with header field name separated using
 * '{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM}' (i.e. 'MQCFH.CompCode'). To access
 * inner {@link MQCFGR} (or inner inner and so on) parameters use group parameter MQ constant name/value with grouped
 * parameter MQ constant name/value separated using
 * '{@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM}' (i.e.
 * 'MQGACF_ACTIVITY_TRACE.MQIACF_COMP_CODE').
 * <p>
 * This parser supports the following properties (in addition to those supported by {@link GenericActivityParser}):
 * <ul>
 * <li>TranslateNumValues - indicates that parser should translate resolved numeric values to corresponding MQ constant
 * names if possible and field/locator data type is 'String' (meaning translated value can be assigned to field). If
 * value of particular field should be left as number (i.e. {@code ReasonCode}), use field/locator attribute
 * {@code datatype="Number"}. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityPCFParser extends GenericActivityParser<PCFContent> {
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

		super.setProperties(props);

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
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link com.ibm.mq.pcf.PCFContent}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return PCFContent.class.isInstance(data);
	}

	/**
	 * Gets field raw data value resolved by locator.
	 *
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            PCF message representing activity object data
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 *
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ContextData cData,
			AtomicBoolean formattingNeeded) throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();
		String[] path = Utils.getNodePath(locStr, StreamsConstants.DEFAULT_PATH_DELIM);
		val = getParamValue(locator.getDataType(), path, cData.getData(), 0);

		logger().log(OpLevel.TRACE, StreamsResources.getString(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
				"ActivityPCFParser.resolved.pcf.value"), locStr, toString(val));

		return val;
	}

	/**
	 * Resolves PCF parameter value from provided {@link PCFContent}: {@link PCFMessage} or {@link MQCFGR}.
	 *
	 * @param fDataType
	 *            field data type
	 * @param path
	 *            parameter path as array of PCF parameter identifiers
	 * @param pcfContent
	 *            PCF content data (message or parameters group)
	 * @param i
	 *            processed path element index
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 *
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value
	 */
	protected Object getParamValue(ActivityFieldDataType fDataType, String[] path, PCFContent pcfContent, int i)
			throws ParseException {
		if (ArrayUtils.isEmpty(path) || pcfContent == null) {
			return null;
		}

		Object val = null;
		String paramStr = path[i];

		if (i == 0 && paramStr.equals(HEAD_MQCFH)) {
			val = resolvePCFHeaderValue(fDataType, path[i + 1], (PCFMessage) pcfContent);
		} else {
			try {
				Integer paramId = WmqUtils.getParamId(paramStr);

				PCFParameter param = pcfContent.getParameter(paramId);

				if (i < path.length - 1 && param instanceof MQCFGR) {
					val = getParamValue(fDataType, path, (MQCFGR) param, ++i);
				} else {
					val = resolvePCFParamValue(fDataType, param);
				}
			} catch (NoSuchElementException exc) {
				throw new ParseException(StreamsResources.getStringFormatted(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
						"ActivityPCFParser.unresolved.pcf.parameter", paramStr), getPCFPosition(pcfContent));
			}
		}

		return val;
	}

	/**
	 * Returns PCF content position identifier to know where event has occurred.
	 * 
	 * @param pcfContent
	 *            PCF content to get position value
	 * @return PCF message sequence number or {@link MQCFGR} parameter value
	 */
	protected int getPCFPosition(PCFContent pcfContent) {
		return pcfContent instanceof PCFMessage ? ((PCFMessage) pcfContent).getMsgSeqNumber()
				: ((MQCFGR) pcfContent).getParameter();
	}

	private Object resolvePCFHeaderValue(ActivityFieldDataType fDataType, String hAttrName, PCFMessage pcfMsg) {
		Object val = null;
		if ("command".equals(hAttrName.toLowerCase())) { // NON-NLS
			val = pcfMsg.getCommand();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQCMD_.*"); // NON-NLS
			}
		} else if ("msgseqnumber".equals(hAttrName.toLowerCase())) { // NON-NLS
			val = pcfMsg.getMsgSeqNumber();
		} else if ("control".equals(hAttrName.toLowerCase())) { // NON-NLS
			val = pcfMsg.getControl();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQCFC_.*"); // NON-NLS
			}
		} else if ("compcode".equals(hAttrName.toLowerCase())) { // NON-NLS
			val = pcfMsg.getCompCode();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookupCompCode((Integer) val);
			}
		} else if ("reason".equals(hAttrName.toLowerCase())) { // NON-NLS
			val = pcfMsg.getReason();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookupReasonCode((Integer) val);
			}
		} else if ("parametercount".equals(hAttrName.toLowerCase())) { // NON-NLS
			val = pcfMsg.getParameterCount();
		}

		return val;
	}

	private boolean isValueTranslatable(ActivityFieldDataType fDataType) {
		return translateNumValues && fDataType == ActivityFieldDataType.String;
	}

	/**
	 * Resolves PCF parameter value. If parser property `TranslateNumValues` is set to {@code true} - then if possible,
	 * resolved numeric value gets translated to corresponding MQ constant name.
	 *
	 * @param fDataType
	 *            field data type
	 * @param param
	 *            PCF parameter to resolve value from
	 *
	 * @return resolved PCF parameter value
	 */
	protected Object resolvePCFParamValue(ActivityFieldDataType fDataType, PCFParameter param) {
		if (param == null) {
			return null;
		}

		Object val = param.getValue();

		if (val instanceof String) {
			val = ((String) val).trim();
		}

		if (isValueTranslatable(fDataType)) {
			switch (param.getParameter()) {
			case MQConstants.MQIA_APPL_TYPE:
				val = MQConstants.lookup(val, "MQAT_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_API_CALLER_TYPE:
				val = MQConstants.lookup(val, "MQXACT_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_API_ENVIRONMENT:
				val = MQConstants.lookup(val, "MQXE_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_APPL_FUNCTION_TYPE:
				val = MQConstants.lookup(val, "MQFUN_.*"); // NON-NLS
				break;
			case MQConstants.MQIA_PLATFORM:
				val = MQConstants.lookup(val, "MQPL_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_OPERATION_ID:
				val = MQConstants.lookup(val, "MQXF_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_OBJECT_TYPE:
			case MQConstants.MQIACF_RESOLVED_TYPE:
				val = MQConstants.lookup(val, "MQOT_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_COMP_CODE:
				val = MQConstants.lookup(val, "MQCC_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_MSG_TYPE:
				val = MQConstants.lookup(val, "MQMT_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_REASON_CODE:
				val = MQConstants.lookupReasonCode((Integer) val);
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
