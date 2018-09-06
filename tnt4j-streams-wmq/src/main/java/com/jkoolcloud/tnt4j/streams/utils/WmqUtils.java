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

package com.jkoolcloud.tnt4j.streams.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.CCSID;
import com.ibm.mq.headers.Charsets;
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
	 * @param elements
	 *            elements array to calculate signature
	 * @return unique message signature
	 */
	public static String computeSignature(Object... elements) {
		synchronized (MSG_DIGEST) {
			return computeSignature(MSG_DIGEST, elements);
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
	 * @param elements
	 *            elements array to calculate signature
	 * @return unique message signature
	 */
	public static String computeSignature(MessageDigest _msgDigest, Object... elements) {
		_msgDigest.reset();

		if (elements != null) {
			for (Object element : elements) {
				if (element instanceof MessageType) {
					_msgDigest.update(String.valueOf(((MessageType) element).value()).getBytes());
				} else if (element instanceof byte[]) {
					_msgDigest.update((byte[]) element);
				} else if (element instanceof String) {
					_msgDigest.update(((String) element).trim().getBytes());
				} else if (element.getClass().isEnum()) {
					_msgDigest.update(((Enum<?>) element).name().getBytes());
				} else {
					String elemStr = Utils.toString(element);
					_msgDigest.update(elemStr.trim().getBytes());
				}
			}
		}

		return Utils.base64EncodeStr(_msgDigest.digest());
	}

	/**
	 * This method applies custom handling for setting field values. This method will construct the signature to use for
	 * the message from the specified value, which is assumed to be a string containing the inputs required for the
	 * message signature calculation, with each input separated by the delimiter specified using parameter
	 * {@code sigDelim}.
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
	 * @see #computeSignature(Object...)
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
			logger.log(OpLevel.DEBUG, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
					"WmqUtils.msg.signature.null.elements");
			return null;
		}

		if (isEmptyItems(sigItems)) {
			logger.log(OpLevel.DEBUG, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
					"WmqUtils.msg.signature.empty.elements", sigItems);
			return null;
		}

		value = computeSignature(sigItems);
		logger.log(OpLevel.TRACE, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
				"WmqUtils.msg.signature", value, sigItems.length, Utils.toStringDeep(sigItems));

		if ("1B2M2Y8AsgTpgAmY7PhCfg==".equals(value)) { // NON-NLS
			logger.log(OpLevel.DEBUG, StreamsResources.getBundle(WmqStreamConstants.RESOURCE_BUNDLE_NAME),
					"WmqUtils.msg.signature.md5.default.value", value);
			return null;
		}

		return value;
	}

	private static boolean isEmptyItems(Object... items) {
		if (items != null) {
			for (Object item : items) {
				if (!isEmptyItem(item)) {
					return false;
				}
			}
		}

		return true;
	}

	private static boolean isEmptyItem(Object obj) {
		if (obj == null) {
			return true;
		}
		if (obj instanceof String) {
			return StringUtils.isEmpty((String) obj);
		}
		if (obj instanceof byte[]) {
			return isEmptyId((byte[]) obj);
		}

		return false;
	}

	/**
	 * Checks whether identifier is empty: is {@code null} or contains only {@code 0} values.
	 *
	 * @param id
	 *            identifier to check
	 * @return {@code true} if <tt>is</tt> is {@code null} and contains only {@code 0} elements, {@code false} -
	 *         otherwise
	 */
	private static boolean isEmptyId(byte[] id) {
		if (id != null) {
			for (byte b : id) {
				if (b != 0) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Returns the charset corresponding to the specified {@code ccsid} Coded Charset Identifier.
	 *
	 * @param ccsid
	 *            coded charset identifier, or {@code null} to get default ({@code ccsid=0}) charset name
	 * @return charset name mapped from coded charset identifier
	 * @throws UnsupportedEncodingException
	 *             if there is no charset mapping for the supplied {@code ccsid}
	 */
	public static String getCharsetName(Object ccsid) throws UnsupportedEncodingException {
		return CCSID.getCodepage(getCCSID(ccsid));
	}

	/**
	 * Converts byte array content in the specified {@code ccsid} into a Java {@link java.lang.String}.
	 *
	 * @param strBytes
	 *            the byte array to convert
	 * @param ccsid
	 *            coded charset identifier, or {@code null} to use default ({@code ccsid=0}) charset
	 * @return the string made from provided bytes using defined {@code ccsid}
	 * @throws UnsupportedEncodingException
	 *             if there is no charset mapping for the supplied {@code ccsid} value or the platform cannot convert
	 *             from the charset
	 */
	public static String getString(byte[] strBytes, Object ccsid) throws UnsupportedEncodingException {
		return Charsets.convert(strBytes, getCCSID(ccsid));
	}

	private static int getCCSID(Object ccsidObj) throws UnsupportedEncodingException {
		int ccsid = 0;

		if (ccsidObj instanceof Number) {
			ccsid = ((Number) ccsid).intValue();
		} else if (ccsidObj instanceof String) {
			try {
				ccsid = Integer.parseInt((String) ccsidObj);
			} catch (NumberFormatException exc) {
				UnsupportedEncodingException te = new UnsupportedEncodingException();
				te.initCause(exc);
				throw te;
			}
		}

		return ccsid;
	}
}
