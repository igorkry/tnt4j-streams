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

import java.security.MessageDigest;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.MQCFGR;
import com.ibm.mq.pcf.MQCFIN;
import com.ibm.mq.pcf.PCFContent;
import com.ibm.mq.pcf.PCFParameter;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.parsers.MessageType;

/**
 * WMQ utility methods used by TNT4J-Streams-WMQ module.
 *
 * @version $Revision: 1 $
 */
public class WmqUtils {

	private static final MessageDigest MSG_DIGEST = Utils.getMD5Digester();

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

	/**
	 * Generates a new unique message signature. This signature is expected to be used for creating a new message
	 * instance, and is intended to uniquely identify the message regardless of which application is processing it.
	 * <p>
	 * It is up to the individual stream to determine which of these attributes is available/required to uniquely
	 * identify a message. In order to identify a message within two different transports, the streams for each
	 * transport must provide the same values.
	 *
	 * @param msgType
	 *            message type
	 * @param msgFormat
	 *            message format
	 * @param msgId
	 *            message identifier
	 * @param userId
	 *            user that originated the message
	 * @param putApplType
	 *            type of application that originated the message
	 * @param putApplName
	 *            name of application that originated the message
	 * @param putDate
	 *            date (GMT) the message was originated
	 * @param putTime
	 *            time (GMT) the message was originated
	 * @param correlId
	 *            message correlator
	 * @return unique message signature
	 */
	public static String computeSignature(MessageType msgType, String msgFormat, byte[] msgId, String userId,
			String putApplType, String putApplName, String putDate, String putTime, byte[] correlId) {
		synchronized (MSG_DIGEST) {
			return computeSignature(MSG_DIGEST, msgType, msgFormat, msgId, userId, putApplType, putApplName, putDate,
					putTime, correlId);
		}
	}

	/**
	 * Generates a new unique message signature. This signature is expected to be used for creating a new message
	 * instance, and is intended to uniquely identify the message regardless of which application is processing it.
	 * <p>
	 * It is up to the individual stream to determine which of these attributes is available/required to uniquely
	 * identify a message. In order to identify a message within two different transports, the streams for each
	 * transport must provide the same values.
	 *
	 * @param _msgDigest
	 *            message type
	 * @param msgType
	 *            message type
	 * @param msgFormat
	 *            message format
	 * @param msgId
	 *            message identifier
	 * @param userId
	 *            user that originated the message
	 * @param putApplType
	 *            type of application that originated the message
	 * @param putApplName
	 *            name of application that originated the message
	 * @param putDate
	 *            date (GMT) the message was originated
	 * @param putTime
	 *            time (GMT) the message was originated
	 * @param correlId
	 *            message correlator
	 * @return unique message signature
	 */
	public static String computeSignature(MessageDigest _msgDigest, MessageType msgType, String msgFormat, byte[] msgId,
			String userId, String putApplType, String putApplName, String putDate, String putTime, byte[] correlId) {
		_msgDigest.reset();
		if (msgType != null) {
			_msgDigest.update(String.valueOf(msgType.value()).getBytes());
		}
		if (msgFormat != null) {
			_msgDigest.update(msgFormat.trim().getBytes());
		}
		if (msgId != null) {
			_msgDigest.update(msgId);
		}
		if (userId != null) {
			_msgDigest.update(userId.trim().toLowerCase().getBytes());
		}
		if (putApplType != null) {
			_msgDigest.update(putApplType.trim().getBytes());
		}
		if (putApplName != null) {
			_msgDigest.update(putApplName.trim().getBytes());
		}
		if (putDate != null) {
			_msgDigest.update(putDate.trim().getBytes());
		}
		if (putTime != null) {
			_msgDigest.update(putTime.trim().getBytes());
		}
		if (correlId != null) {
			_msgDigest.update(correlId);
		}

		return Utils.base64EncodeStr(_msgDigest.digest());
	}

	/**
	 * This method applies custom handling for setting field values. This method will construct the signature to use for
	 * the message from the specified value, which is assumed to be a string containing the inputs required for the
	 * message signature calculation, with each input separated by the delimiter specified using parameter
	 * {@code sigDelim}.
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
	 * @param value
	 *            value object to retrieve signature fields data
	 * @param sigDelim
	 *            signature delimiter
	 * @param logger
	 *            logger to log result trace messages
	 * @return unique message signature, or {@code null} if <tt>value</tt> contained signature calculation items are
	 *         empty
	 *
	 * @see #computeSignature(MessageType, String, byte[], String, String, String, String, String, byte[])
	 */
	public static Object computeSignature(Object value, String sigDelim, EventSink logger) {
		Object[] sigItems = null;
		if (value instanceof Object[]) {
			sigItems = (Object[]) value;
		} else if (value instanceof String) {
			String sigStr = (String) value;
			if (sigStr.contains(sigDelim)) {
				sigItems = sigStr.split(Pattern.quote(sigDelim));
			}
		}

		if (Utils.isEmptyContent(sigItems)) {
			return null;
		}

		MessageType msgType = null;
		String msgFormat = null;
		byte[] msgId = null;
		String msgUser = null;
		String msgApplType = null;
		String msgApplName = null;
		String msgPutDate = null;
		String msgPutTime = null;
		byte[] correlId = null;
		for (int i = 0; i < sigItems.length; i++) {
			Object item = sigItems[i];
			if (item == null) {
				continue;
			}
			switch (i) {
			case 0:
				msgType = MessageType.valueOf(
						item instanceof Number ? ((Number) item).intValue() : Integer.parseInt(item.toString()));
				break;
			case 1:
				msgFormat = item.toString();
				break;
			case 2:
				msgId = item instanceof byte[] ? (byte[]) item : item.toString().getBytes();
				break;
			case 3:
				msgUser = item.toString();
				break;
			case 4:
				msgApplType = item.toString();
				break;
			case 5:
				msgApplName = item.toString();
				break;
			case 6:
				msgPutDate = item.toString();
				break;
			case 7:
				msgPutTime = item.toString();
				break;
			case 8:
				correlId = item instanceof byte[] ? (byte[]) item : item.toString().getBytes();
				break;
			default:
				break;
			}
		}
		value = computeSignature(msgType, msgFormat, msgId, msgUser, msgApplType, msgApplName, msgPutDate, msgPutTime,
				correlId);
		logger.log(OpLevel.TRACE, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
				"MessageActivityXmlParser.msg.signature", value, msgType, msgFormat,
				msgId == null ? "null" : Utils.encodeHex(msgId), msgId == null ? "null" : new String(msgId), msgUser,
				msgApplType, msgApplName, msgPutDate, msgPutTime, correlId == null ? "null" : Utils.encodeHex(correlId),
				correlId == null ? "null" : new String(correlId));

		return value;
	}
}
