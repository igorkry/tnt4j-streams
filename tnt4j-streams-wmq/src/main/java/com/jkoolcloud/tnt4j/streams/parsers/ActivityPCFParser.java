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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.exits.MQCD;
import com.ibm.mq.exits.MQCSP;
import com.ibm.mq.jmqi.*;
import com.ibm.mq.jmqi.internal.JmqiStructureFormatter;
import com.ibm.mq.jmqi.internal.MqiStructure;
import com.ibm.mq.jmqi.system.JmqiTls;
import com.ibm.mq.pcf.*;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.WmqParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Implements an activity data parser that assumes each activity data item is an {@link PCFContent} ({@link PCFMessage}
 * or {@link MQCFGR}) where each field is represented by a PCF parameter and the PCF parameter MQ constant name/value is
 * used to map each field into its corresponding activity field.
 * <p>
 * PCF message can have grouped parameters - all message will have header {@link MQCFH} and may have {@link MQCFGR} type
 * parameters. To access PCF message header fields use 'MQCFH' expression with header field name separated using
 * {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM} (e.g., 'MQCFH.CompCode'). To access
 * inner {@link MQCFGR} (or inner inner and so on) parameters use group parameter MQ constant name/value with grouped
 * parameter MQ constant name/value separated using
 * {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM} (e.g.,
 * 'MQGACF_ACTIVITY_TRACE.MQIACF_COMP_CODE').
 * <p>
 * This parser supports the following configuration properties (in addition to those supported by
 * {@link GenericActivityParser}):
 * <ul>
 * <li>TranslateNumValues - indicates that parser should translate resolved numeric values to corresponding MQ constant
 * names if possible and field/locator data type is 'String' (meaning translated value can be assigned to field). If
 * value of particular field should be left as number (e.g., {@code ReasonCode}), use field/locator attribute
 * {@code datatype="Number"}. (Optional)</li>
 * <li>SignatureDelim - signature fields delimiter. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class ActivityPCFParser extends GenericActivityParser<PCFContent> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityPCFParser.class);

	private static final String HEAD_MQCFH = "MQCFH"; // NON-NLS
	private static final Pattern STRUCT_ATTR_PATTERN = Pattern.compile("MQB\\w+_MQ\\w+_STRUCT"); // NON-NLS
	private static final String MQ_TMP_CTX_STRUCT_PREF = "MQ_TMP_CTX_"; // NON-NLS

	private boolean translateNumValues = true;
	private String sigDelim = DEFAULT_DELIM;

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
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		super.setProperties(props);

		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				String name = prop.getKey();
				String value = prop.getValue();

				if (WmqParserProperties.PROP_TRANSLATE_NUM_VALUES.equalsIgnoreCase(name)) {
					translateNumValues = BooleanUtils.toBoolean(value);

					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"ActivityParser.setting", name, value);
				} else if (WmqParserProperties.PROP_SIG_DELIM.equalsIgnoreCase(name)) {
					if (StringUtils.isNotEmpty(value)) {
						sigDelim = value;
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"ActivityParser.setting", name, value);
					}
				}
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
	 *            PCF message parsing context data
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 *
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();
		String[] path = Utils.getNodePath(locStr, StreamsConstants.DEFAULT_PATH_DELIM);
		val = getParamValue(locator.getDataType(), path, cData.getData(), 0, cData);

		logger().log(OpLevel.TRACE, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
				"ActivityPCFParser.resolved.pcf.value", locStr, toString(val));

		return val;
	}

	/**
	 * Resolves PCF parameter value from provided {@link PCFContent}: {@link PCFMessage} or {@link MQCFGR}.
	 *
	 * @param fDataType
	 *            field data type
	 * @param path
	 *            parameter path as array of MQ PCF/MQI parameter identifiers
	 * @param pcfContent
	 *            PCF content data (message or parameters group)
	 * @param i
	 *            processed locator path element index
	 * @param cData
	 *            PCF message parsing context data
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 *
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value
	 */
	protected Object getParamValue(ActivityFieldDataType fDataType, String[] path, PCFContent pcfContent, int i,
			ActivityContext cData) throws ParseException {
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

				if (!isLastPathToken(path, i) && param instanceof MQCFGR) {
					val = getParamValue(fDataType, path, (MQCFGR) param, ++i, cData);
				} else {
					if (isMqiStructParam(paramStr) && !isLastPathToken(path, i)) {
						val = resolveMqiStructValue(fDataType, param, path, i, pcfContent, cData);
					} else {
						val = resolvePCFParamValue(fDataType, param);
					}
				}
			} catch (NoSuchElementException exc) {
				throw new ParseException(StreamsResources.getStringFormatted(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
						"ActivityPCFParser.unresolved.pcf.parameter", paramStr), getPCFPosition(pcfContent));
			}
		}

		return val;
	}

	private static boolean isLastPathToken(String[] path, int i) {
		return i >= path.length - 1;
	}

	private static boolean isMqiStructParam(String paramStr) {
		return STRUCT_ATTR_PATTERN.matcher(paramStr).matches();
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
		return translateNumValues
				&& (fDataType == ActivityFieldDataType.String || fDataType == ActivityFieldDataType.Generic);
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
				break;
			case MQConstants.MQIA_CODED_CHAR_SET_ID:
				val = MQConstants.lookup(val, "MQCCSI_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_ENCODING:
				val = MQConstants.lookup(val, "MQENC_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_EXPIRY:
				val = MQConstants.lookup(val, "MQEI_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_FEEDBACK:
				val = MQConstants.lookup(val, "MQFB_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_MSG_FLAGS:
				val = MQConstants.decodeOptions((int) val, "MQMF_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_ORIGINAL_LENGTH:
				val = MQConstants.lookup(val, "MQOL_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_PERSISTENCE:
				val = MQConstants.lookup(val, "MQPER_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_PRIORITY:
				val = MQConstants.lookup(val, "MQPRI_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_REPORT:
				val = MQConstants.decodeOptions((int) val, "MQRO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_VERSION:
				val = MQConstants.lookup(val, "MQMD_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_OPEN_OPTIONS:
				val = MQConstants.decodeOptions((int) val, "MQOO_.*"); // NON-NLS
				break;
			// case MQConstants.MQIACF_OPTIONS:
			// break;
			// case MQConstants.MQIACF_BROKER_OPTIONS:
			// break;
			case MQConstants.MQIACF_REGISTRATION_OPTIONS:
				val = MQConstants.decodeOptions((int) val, "MQREGO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_PUBLICATION_OPTIONS:
				val = MQConstants.decodeOptions((int) val, "MQPUBO_.*"); // NON-NLS
				break;
			// case MQConstants.MQIACF_REG_REG_OPTIONS:
			// break;
			case MQConstants.MQIACF_DELETE_OPTIONS:
				val = MQConstants.decodeOptions((int) val, "MQDELO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_CONNECT_OPTIONS:
				val = MQConstants.decodeOptions((int) val, "MQCNO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_AUTH_OPTIONS:
				val = MQConstants.decodeOptions((int) val, "MQAUTHOPT_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_SUB_OPTIONS:
				val = MQConstants.decodeOptions((int) val, "MQSO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_MQCB_OPTIONS:
				val = MQConstants.decodeOptions((int) val, "MQCBO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_CLOSE_OPTIONS:
				val = MQConstants.decodeOptions((int) val, "MQCO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_GET_OPTIONS:
				val = MQConstants.decodeOptions((int) val, "MQGMO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_PUT_OPTIONS:
				val = MQConstants.decodeOptions((int) val, "MQPMO_.*"); // NON-NLS
				break;
			case MQConstants.MQIACF_SUBRQ_OPTIONS:
				val = MQConstants.decodeOptions((int) val, "MQSRO_.*"); // NON-NLS
				break;
			default:
				break;
			}
		}

		return val;

	}

	/**
	 * Builds MQI structure from PCF parameter contained binary data and resolves field locator referenced value from
	 * that structure.
	 *
	 * @param fDataType
	 *            field data type
	 * @param param
	 *            PCF parameter to use raw data to build MQI structure
	 * @param path
	 *            parameter path as array of MQ PCF/MQI parameter identifiers
	 * @param i
	 *            processed locator path element index
	 * @param pcfContent
	 *            PCF content data (message or parameters group)
	 * @param cData
	 *            PCF message parsing context data
	 * @return resolved MQI structure value
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value
	 */
	protected Object resolveMqiStructValue(ActivityFieldDataType fDataType, PCFParameter param, String[] path, int i,
			PCFContent pcfContent, ActivityContext cData) throws ParseException {
		if (param == null) {
			return null;
		}

		String ctxStructKey = MQ_TMP_CTX_STRUCT_PREF + param.getParameterName();
		MqiStructure mqiStruct = (MqiStructure) cData.get(ctxStructKey);
		boolean exception = false;
		if (mqiStruct == null) {
			try {
				mqiStruct = buildMqiStructureFromBinData(param);
				cData.put(ctxStructKey, mqiStruct);
			} catch (Exception exc) {
				Utils.logThrowable(logger(), OpLevel.ERROR,
						StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
						"ActivityPCFParser.structure.build.failed", exc);
				exception = true;
			}

			if (mqiStruct == null || exception) {
				throw new ParseException(
						StreamsResources.getStringFormatted(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
								"ActivityPCFParser.can.not.build.structure", getFullPath(path, i)),
						getPCFPosition(pcfContent));
			}
		}

		return resolveMqiStructParamValue(mqiStruct, path, ++i, param.getJmqiEnv(), fDataType);
	}

	private MqiStructure buildMqiStructureFromBinData(PCFParameter structParam) throws JmqiException {
		if (structParam == null) {
			return null;
		}

		byte[] structData = (byte[]) structParam.getValue();
		JmqiEnvironment env = structParam.getJmqiEnv();
		MqiStructure mqiStruct = null;

		switch (structParam.getParameter()) {
		case MQConstants.MQBACF_MQBO_STRUCT:
			mqiStruct = env.newMQBO();
			break;
		case MQConstants.MQBACF_MQCBC_STRUCT:
			mqiStruct = env.newMQCBC();
			break;
		case MQConstants.MQBACF_MQCBD_STRUCT:
			mqiStruct = env.newMQCBD();
			break;
		case MQConstants.MQBACF_MQCD_STRUCT:
			mqiStruct = env.newMQCD();
			break;
		case MQConstants.MQBACF_MQCNO_STRUCT:
			mqiStruct = env.newMQCNO();
			break;
		case MQConstants.MQBACF_MQGMO_STRUCT:
			mqiStruct = env.newMQGMO();
			break;
		case MQConstants.MQBACF_MQMD_STRUCT:
			mqiStruct = env.newMQMD();
			break;
		case MQConstants.MQBACF_MQPMO_STRUCT:
			mqiStruct = env.newMQPMO();
			break;
		case MQConstants.MQBACF_MQSD_STRUCT:
			mqiStruct = env.newMQSD();
			break;
		case MQConstants.MQBACF_MQSTS_STRUCT:
			mqiStruct = env.newMQSTS();
			break;
		default:
		}

		if (mqiStruct != null) {
			mqiStruct.readFromBuffer(structData, 0, 4, true, env.getNativeCharSet(), new JmqiTls());
			if (logger().isSet(OpLevel.DEBUG)) {
				logger().log(OpLevel.DEBUG, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
						"ActivityPCFParser.built.structure", PCFConstants.lookupParameter(structParam.getParameter()),
						mqiToString(mqiStruct, env));
			}

			return mqiStruct;
		}

		return mqiStruct;
	}

	private static String mqiToString(MqiStructure mqiStruct, JmqiEnvironment env) {
		JmqiStructureFormatter fmt = new JmqiStructureFormatter(env, 24, 2, "\n"); // NON-NLS
		mqiStruct.addFieldsToFormatter(fmt);

		return fmt.toString();
	}

	private static String getFullPath(String[] path, int i) {
		StringBuilder sb = new StringBuilder();

		for (int pi = 0; pi <= i; pi++) {
			sb.append(path[pi]);

			if (pi < i) {
				sb.append(StreamsConstants.DEFAULT_PATH_DELIM);
			}
		}

		return sb.toString();
	}

	private Object resolveMqiStructParamValue(MqiStructure mqiStruct, String[] path, int i, JmqiEnvironment env,
			ActivityFieldDataType fDataType) throws ParseException {
		Object val = null;

		if (path[i].equalsIgnoreCase(mqiStruct.getClass().getSimpleName())) {
			if (isLastPathToken(path, i)) {
				val = mqiToString(mqiStruct, env);
			} else {
				val = resolveMqiStructParamValue(mqiStruct, path, i + 1, fDataType);

				if (val instanceof String) {
					val = ((String) val).trim();
				} else if (val instanceof MqiStructure) {
					val = mqiToString((MqiStructure) val, env);
				}
			}
		}

		return val;
	}

	private Object resolveMqiStructParamValue(MqiStructure mqiSruct, String[] path, int i,
			ActivityFieldDataType fDataType) throws ParseException {
		Object val = null;
		if (mqiSruct instanceof MQMD) {
			val = resolveMQMDValue((MQMD) mqiSruct, path[i], fDataType);
		} else if (mqiSruct instanceof MQPMO) {
			val = resolveMQPMOValue((MQPMO) mqiSruct, path[i], fDataType);
		} else if (mqiSruct instanceof MQGMO) {
			val = resolveMQGMOValue((MQGMO) mqiSruct, path[i], fDataType);
		} else if (mqiSruct instanceof MQCNO) {
			val = resolveMQCNOValue((MQCNO) mqiSruct, path[i], fDataType);
		} else if (mqiSruct instanceof MQCD) {
			val = resolveMQCDValue((MQCD) mqiSruct, path[i], fDataType);
		} else if (mqiSruct instanceof MQCBD) {
			val = resolveMQCBDValue((MQCBD) mqiSruct, path[i], fDataType);
		} else if (mqiSruct instanceof MQCBC) {
			val = resolveMQCBCValue((MQCBC) mqiSruct, path[i], fDataType);
		} else if (mqiSruct instanceof MQBO) {
			val = resolveMQBOValue((MQBO) mqiSruct, path[i], fDataType);
		} else if (mqiSruct instanceof MQSD) {
			val = resolveMQSDValue((MQSD) mqiSruct, path[i], fDataType);
		} else if (mqiSruct instanceof MQSTS) {
			val = resolveMQSTSValue((MQSTS) mqiSruct, path[i], fDataType);
		} else if (mqiSruct instanceof MQCSP) {
			val = resolveMQCSPValue((MQCSP) mqiSruct, path[i], fDataType);
		} else if (mqiSruct instanceof MQSCO) {
			val = resolveMQSCOValue((MQSCO) mqiSruct, path[i], fDataType);
		} else {
			try {
				val = Utils.getFieldValue(path, mqiSruct, i);
			} catch (Exception exc) {
				Utils.logThrowable(logger(), OpLevel.ERROR,
						StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
						"ActivityPCFParser.structure.value.resolution.failed", exc);

				// throw new ParseException(
				// StreamsResources.getStringFormatted(WmqStreamConstants.RESOURCE_BUNDLE_NAME,
				// "ActivityPCFParser.can.not.resolve.structure.value", getFullPath(path, i)),
				// getPCFPosition(pcfContent));
			}
		}

		return val;
	}

	private Object resolveMQMDValue(MQMD mqmd, String fName, ActivityFieldDataType fDataType) {
		Object val = null;

		switch (fName.toLowerCase()) {
		case "accountingtoken": // NON-NLS
			val = mqmd.getAccountingToken();
			break;
		case "applidentitydata": // NON-NLS
			val = mqmd.getApplIdentityData();
			break;
		case "applorigindata": // NON-NLS
			val = mqmd.getApplOriginData();
			break;
		case "backoutcount": // NON-NLS
			val = mqmd.getBackoutCount();
			break;
		case "codedcharsetid": // NON-NLS
			val = mqmd.getCodedCharSetId();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQCCSI_.*"); // NON-NLS
			}
			break;
		case "correlid": // NON-NLS
			val = mqmd.getCorrelId();
			break;
		case "encoding": // NON-NLS
			val = mqmd.getEncoding();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQENC_.*"); // NON-NLS
			}
			break;
		case "expiry": // NON-NLS
			val = mqmd.getExpiry();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQEI_.*"); // NON-NLS
			}
			break;
		case "feedback": // NON-NLS
			val = mqmd.getFeedback();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQFB_.*"); // NON-NLS
			}
			break;
		case "format": // NON-NLS
			val = mqmd.getFormat();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQFMT_.*"); // NON-NLS
			}
			break;
		case "groupid": // NON-NLS
			val = mqmd.getGroupId();

			break;
		case "msgflags": // NON-NLS
			val = mqmd.getMsgFlags();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.decodeOptions((int) val, "MQMF_.*"); // NON-NLS
			}
			break;
		case "msgid": // NON-NLS
			val = mqmd.getMsgId();
			break;
		case "msgseqnumber": // NON-NLS
			val = mqmd.getMsgSeqNumber();
			break;
		case "msgtype": // NON-NLS
			val = mqmd.getMsgType();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQMT_.*"); // NON-NLS
			}
			break;
		case "offset": // NON-NLS
			val = mqmd.getOffset();
			break;
		case "originallength": // NON-NLS
			val = mqmd.getOriginalLength();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQOL_.*"); // NON-NLS
			}
			break;
		case "persistence": // NON-NLS
			val = mqmd.getPersistence();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQPER_.*"); // NON-NLS
			}
			break;
		case "priority": // NON-NLS
			val = mqmd.getPriority();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQPRI_.*"); // NON-NLS
			}
			break;
		case "putapplname": // NON-NLS
			val = mqmd.getPutApplName();
			break;
		case "putappltype": // NON-NLS
			val = mqmd.getPutApplType();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQAT_.*"); // NON-NLS
			}
			break;
		case "putdate": // NON-NLS
			val = mqmd.getPutDate();
			break;
		case "puttime": // NON-NLS
			val = mqmd.getPutTime();
			break;
		case "replytoq": // NON-NLS
			val = mqmd.getReplyToQ();
			break;
		case "replytoqmgr": // NON-NLS
			val = mqmd.getReplyToQMgr();
			break;
		case "report": // NON-NLS
			val = mqmd.getReport();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.decodeOptions((int) val, "MQRO_.*"); // NON-NLS
			}
			break;
		case "useridentifier": // NON-NLS
			val = mqmd.getUserIdentifier();
			break;
		case "version": // NON-NLS
			val = mqmd.getVersion();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQMD_VERSION_.*"); // NON-NLS
			}
			break;
		default:
			break;
		}

		return val;
	}

	private Object resolveMQPMOValue(MQPMO mqpmo, String fName, ActivityFieldDataType fDataType) {
		Object val = null;

		switch (fName.toLowerCase()) {
		case "action": // NON-NLS
			val = mqpmo.getAction();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQACTP_.*"); // NON-NLS
			}
			break;
		case "context": // NON-NLS
			val = mqpmo.getContext();
			// if (isValueTranslatable(fDataType)) {
			// val = MQConstants.lookup(val, "MQACTP_.*"); // NON-NLS
			// }
			break;
		case "invaliddestcount": // NON-NLS
			val = mqpmo.getInvalidDestCount();
			break;
		case "knowndestcount": // NON-NLS
			val = mqpmo.getKnownDestCount();
			break;
		case "newmsghandle": // NON-NLS
			val = mqpmo.getNewMsgHandle();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQHM_.*"); // NON-NLS
			}
			break;
		case "options": // NON-NLS
			val = mqpmo.getOptions();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.decodeOptions((int) val, "MQPMO_.*"); // NON-NLS
			}
			break;
		case "originalmsghandle": // NON-NLS
			val = mqpmo.getOriginalMsgHandle();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQHM_.*"); // NON-NLS
			}
			break;
		case "sublevel": // NON-NLS
			val = mqpmo.getSubLevel();
			break;
		case "putmsgrecfields": // NON-NLS
			val = mqpmo.getPutMsgRecFields();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.decodeOptions((int) val, "MQPMRF_.*"); // NON-NLS
			}
			break;
		case "recspresent": // NON-NLS
			val = mqpmo.getRecsPresent();
			break;
		case "resolvedqmgrname": // NON-NLS
			val = mqpmo.getResolvedQMgrName();
			break;
		case "resolvedqname": // NON-NLS
			val = mqpmo.getResolvedQName();
			break;
		case "timeout": // NON-NLS
			// val = mqpmo.get();
			break;
		case "unknowndestcount": // NON-NLS
			val = mqpmo.getUnknownDestCount();
			break;
		case "version": // NON-NLS
			val = mqpmo.getVersion();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQPMO_VERSION_.*"); // NON-NLS
			}
			break;
		default:
			break;
		}

		return val;
	}

	private Object resolveMQGMOValue(MQGMO mqgmo, String fName, ActivityFieldDataType fDataType) {
		Object val = null;

		switch (fName.toLowerCase()) {
		case "groupstatus": // NON-NLS
			val = mqgmo.getGroupStatus();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQGS_.*"); // NON-NLS
			}
			break;
		case "matchoptions": // NON-NLS
			val = mqgmo.getMatchOptions();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.decodeOptions((int) val, "MQMO_.*"); // NON-NLS
			}
			break;
		case "msghandle": // NON-NLS
			val = mqgmo.getMessageHandle();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQHM_.*"); // NON-NLS
			}
			break;
		case "msgtoken": // NON-NLS
			val = mqgmo.getMsgToken();
			break;
		case "options": // NON-NLS
			val = mqgmo.getOptions();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.decodeOptions((int) val, "MQGMO_.*"); // NON-NLS
			}
			break;
		case "resolvedqname": // NON-NLS
			val = mqgmo.getResolvedQName();
			break;
		case "returnedlength": // NON-NLS
			val = mqgmo.getReturnedLength();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQRL_.*"); // NON-NLS
			}
			break;
		case "segmentation": // NON-NLS
			val = mqgmo.getSegmentation();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQSEG_.*"); // NON-NLS
			}
			break;
		case "segmentstatus": // NON-NLS
			val = mqgmo.getSegmentStatus();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQSS_.*"); // NON-NLS
			}
			break;
		case "signal2": // NON-NLS
			val = mqgmo.getSignal2();
			break;
		case "waitinterval": // NON-NLS
			val = mqgmo.getWaitInterval();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQWI_.*"); // NON-NLS
			}
			break;
		case "version": // NON-NLS
			val = mqgmo.getVersion();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQGMO_VERSION_.*"); // NON-NLS
			}
			break;
		default:
			break;
		}

		return val;
	}

	private Object resolveMQCNOValue(MQCNO mqcno, String fName, ActivityFieldDataType fDataType) {
		Object val = null;

		switch (fName.toLowerCase()) {
		case "clientconn": // NON-NLS
			val = mqcno.getClientConn();
			break;
		case "securityparams": // NON-NLS
			val = mqcno.getSecurityParms();
			break;
		case "sslconfig": // NON-NLS
			val = mqcno.getSslConfig();
			break;
		case "connectionid": // NON-NLS
			val = mqcno.getConnectionId();
			break;
		case "conntag": // NON-NLS
			val = mqcno.getConnTag();
			break;
		case "options": // NON-NLS
			val = mqcno.getOptions();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.decodeOptions((int) val, "MQCNO_.*"); // NON-NLS
			}
			break;
		case "version": // NON-NLS
			val = mqcno.getVersion();
			if (isValueTranslatable(fDataType)) {
				val = MQConstants.lookup(val, "MQCNO_VERSION_.*"); // NON-NLS
			}
			break;
		default:
			break;
		}

		return val;
	}

	private Object resolveMQCDValue(MQCD mqcd, String fName, ActivityFieldDataType fDataType) {
		return mqcd; // TODO
	}

	private Object resolveMQCBDValue(MQCBD mqcbd, String fName, ActivityFieldDataType fDataType) {
		return mqcbd; // TODO
	}

	private Object resolveMQCBCValue(MQCBC mqcbc, String fName, ActivityFieldDataType fDataType) {
		return mqcbc; // TODO
	}

	private Object resolveMQBOValue(MQBO mqbo, String fName, ActivityFieldDataType fDataType) {
		return mqbo; // TODO
	}

	private Object resolveMQSDValue(MQSD mqsd, String fName, ActivityFieldDataType fDataType) {
		return mqsd; // TODO
	}

	private Object resolveMQSTSValue(MQSTS mqsts, String fName, ActivityFieldDataType fDataType) {
		return mqsts; // TODO
	}

	private Object resolveMQCSPValue(MQCSP mqcsp, String fName, ActivityFieldDataType fDataType) {
		return mqcsp; // TODO
	}

	private Object resolveMQSCOValue(MQSCO mqsco, String fName, ActivityFieldDataType fDataType) {
		return mqsco; // TODO
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

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method applies custom handling for setting field values. This method will construct the signature to use for
	 * the message from the specified value, which is assumed to be a string containing the inputs required for the
	 * message signature calculation, with each input separated by the delimiter specified in property
	 * {@code SignatureDelim}.
	 * <p>
	 * To initiate signature calculation, {@code field} "value type" attribute must be set to
	 * {@value com.jkoolcloud.tnt4j.streams.utils.WmqStreamConstants#VT_SIGNATURE}.
	 * <p>
	 * The signature items MUST be specified in the following order (meaning of field value is not so important, but
	 * data types must match):
	 * <ol>
	 * <li>Message Type - {@link Integer}</li>
	 * <li>Message Format - {@link String}</li>
	 * <li>Message ID - {@code byte[]} or {@link String}</li>
	 * <li>Message User - {@link String}</li>
	 * <li>Message Application Type - {@link String}</li>
	 * <li>Message Application Name - {@link String}</li>
	 * <li>Message Date - {@link String}</li>
	 * <li>Message Time - {@link String}</li>
	 * <li>Correlator ID - {@code byte[]} or {@link String}</li>
	 * </ol>
	 * <p>
	 * Individual items can be omitted, but must contain a place holder (except for trailing items).
	 *
	 * @see WmqUtils#computeSignature(Object, String, EventSink)
	 */
	@Override
	protected void applyFieldValue(ActivityInfo ai, ActivityField field, Object value) throws ParseException {
		StreamFieldType fieldType = field.getFieldType();
		if (fieldType != null && WmqStreamConstants.VT_SIGNATURE.equalsIgnoreCase(field.getValueType())) {
			switch (fieldType) {
			case Correlator:
			case TrackingId:
				value = WmqUtils.computeSignature(value, sigDelim, logger());
				break;
			default:
				break;
			}
		}

		super.applyFieldValue(ai, field, value);
	}
}
