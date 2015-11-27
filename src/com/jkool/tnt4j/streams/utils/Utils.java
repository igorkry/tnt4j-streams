/*
 * Copyright (c) 2015 jKool, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * jKool, LLC. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with jKool, LLC.
 *
 * JKOOL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. JKOOL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 *
 */

package com.jkool.tnt4j.streams.utils;

import java.io.*;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.jkool.tnt4j.streams.parsers.MessageType;
import com.nastel.jkool.tnt4j.core.OpType;

/**
 * General utility methods.
 *
 * @version $Revision: 16 $
 */
public final class Utils extends com.nastel.jkool.tnt4j.utils.Utils {

	private static final String TAG_DELIM = ","; // NON-NLS

	private Utils() {
	}

	/**
	 * Base64 encodes the specified sequence of bytes.
	 *
	 * @param src
	 *            byte sequence to encode
	 *
	 * @return encoded byte sequence
	 */
	public static byte[] base64Encode(byte[] src) {
		return Base64.encodeBase64(src);
	}

	/**
	 * Base64 decodes the specified sequence of bytes.
	 *
	 * @param src
	 *            byte sequence to decode
	 *
	 * @return decoded byte sequence
	 */
	public static byte[] base64Decode(byte[] src) {
		return Base64.decodeBase64(src);
	}

	/**
	 * Converts an array of bytes into an array of characters representing the
	 * hexadecimal values.
	 *
	 * @param src
	 *            byte sequence to encode
	 *
	 * @return encoded character sequence
	 */
	public static char[] encodeHex(byte[] src) {
		return Hex.encodeHex(src);
	}

	/**
	 * Converts a string representing hexadecimal values into an array of bytes.
	 *
	 * @param str
	 *            String to convert
	 *
	 * @return decoded byte sequence
	 */
	public static byte[] decodeHex(String str) {
		byte[] ba = null;
		try {
			ba = Hex.decodeHex(str.toCharArray());
		} catch (DecoderException e) {
		}
		return ba;
	}

	private static final MessageDigest MSG_DIGEST = getMD5Digester();

	/**
	 * <p>
	 * Generates a new unique message signature. This signature is expected to
	 * be used for creating a new message instance, and is intended to uniquely
	 * identify the message regardless of which application is processing it.
	 * </p>
	 * <p>
	 * It is up to the individual stream to determine which of these attributes
	 * is available/required to uniquely identify a message. In order to
	 * identify a message within two different transports, the streams for each
	 * transport must provide the same values.
	 * </p>
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
	 *
	 * @return unique message signature
	 */
	public static String computeSignature(MessageType msgType, String msgFormat, byte[] msgId, String userId,
			String putApplType, String putApplName, String putDate, String putTime) {
		synchronized (MSG_DIGEST) {
			return computeSignature(MSG_DIGEST, msgType, msgFormat, msgId, userId, putApplType, putApplName, putDate,
					putTime);
		}
	}

	/**
	 * <p>
	 * Generates a new unique message signature. This signature is expected to
	 * be used for creating a new message instance, and is intended to uniquely
	 * identify the message regardless of which application is processing it.
	 * </p>
	 * <p>
	 * It is up to the individual stream to determine which of these attributes
	 * is available/required to uniquely identify a message. In order to
	 * identify a message within two different transports, the streams for each
	 * transport must provide the same values.
	 * </p>
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
	 *
	 * @return unique message signature
	 */
	public static String computeSignature(MessageDigest _msgDigest, MessageType msgType, String msgFormat, byte[] msgId,
			String userId, String putApplType, String putApplName, String putDate, String putTime) {
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
		return new String(base64Encode(_msgDigest.digest()));
	}

	/**
	 * Maps OpType enumeration constant from string or numeric value.
	 *
	 * @param opType
	 *            object to be mapped to OpType enumeration constant
	 *
	 * @return OpType mapping or {@code null} if mapping not found.
	 */
	public static OpType mapOpType(Object opType) {
		if (opType == null) {
			return null;
		}
		if (opType instanceof Number) {
			return mapOpType(((Number) opType).intValue());
		}
		return mapOpType(opType.toString());
	}

	private static OpType mapOpType(String opType) {
		if (opType.equalsIgnoreCase("OTHER")) { // NON-NLS
			return OpType.OTHER;
		}
		if (opType.equalsIgnoreCase("START")) { // NON-NLS
			return OpType.START;
		}
		if (opType.equalsIgnoreCase("OPEN")) { // NON-NLS
			return OpType.OPEN;
		}
		if (opType.equalsIgnoreCase("SEND")) { // NON-NLS
			return OpType.SEND;
		}
		if (opType.equalsIgnoreCase("RECEIVE")) { // NON-NLS
			return OpType.RECEIVE;
		}
		if (opType.equalsIgnoreCase("CLOSE")) { // NON-NLS
			return OpType.CLOSE;
		}
		if (opType.equalsIgnoreCase("END")) { // NON-NLS
			return OpType.STOP;
		}
		if (opType.equalsIgnoreCase("INQUIRE")) { // NON-NLS
			return OpType.INQUIRE;
		}
		if (opType.equalsIgnoreCase("SET")) { // NON-NLS
			return OpType.SET;
		}
		if (opType.equalsIgnoreCase("CALL")) { // NON-NLS
			return OpType.CALL;
		}
		if (opType.equalsIgnoreCase("URL")) { // NON-NLS
			return OpType.OTHER;
		}
		if (opType.equalsIgnoreCase("BROWSE")) { // NON-NLS
			return OpType.BROWSE;
		}
		return OpType.OTHER;
	}

	private static OpType mapOpType(int opType) {
		switch (opType) {
		case 0:
			return OpType.OTHER;
		case 1:
			return OpType.START;
		case 2:
			return OpType.OPEN;
		case 3:
			return OpType.SEND;
		case 4:
			return OpType.RECEIVE;
		case 5:
			return OpType.CLOSE;
		case 6:
			return OpType.STOP;
		case 7:
			return OpType.INQUIRE;
		case 8:
			return OpType.SET;
		case 9:
			return OpType.CALL;
		case 10:// URL
			return OpType.OTHER;
		case 11:
			return OpType.BROWSE;
		default:
			return OpType.OTHER;
		}
	}

	/**
	 * Checks if properties defined file name contains wildcard characters and
	 * directory scanning for matching files is required.
	 *
	 * @param fileName
	 *            file name to check if it contains wildcard characters
	 *
	 * @return {@code true} if file name con
	 *
	 */
	public static boolean isWildcardFileName(String fileName) {
		if (StringUtils.isNotEmpty(fileName)) {
			return fileName.contains("*") || fileName.contains("?"); // NON-NLS
		}
		return false;
	}

	/**
	 * Counts text lines available in file.
	 * 
	 * @param filename
	 *            file name to count lines
	 *
	 * @return number of lines currently available in file
	 */
	public static int countLines(String fileName) {
		int count = 0;
		InputStream is = null;

		try {
			is = new BufferedInputStream(new FileInputStream(fileName));
			byte[] c = new byte[1024];
			int readChars = 0;
			boolean endsWithoutNewLine = false;
			while ((readChars = is.read(c)) != -1) {
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n')
						++count;
				}
				endsWithoutNewLine = (c[readChars - 1] != '\n');
			}
			if (endsWithoutNewLine) {
				++count;
			}
		} catch (IOException exc) {

		} finally {
			close(is);
		}

		return count;
	}

	/**
	 * Returns first readable file from {@code files} array which has last
	 * modification timestamp newer than {@code lastModif}. If {@code lastModif}
	 * is {@code null}, then newest readable file is returned.
	 * 
	 * @param files
	 *            last modification timestamp ordered files array
	 * @param lastModif
	 *            last modification time to compare. If {@code null} - then
	 *            newest file is returned
	 *
	 * @return first file from array which has modification time later than
	 *         {@code lastModif}, or {@code null} if {@code files} is empty or
	 *         does not contain readable file with last modification time later
	 *         than {@code lastModif}
	 */
	public static File getFirstNewer(File[] files, Long lastModif) {
		File last = null;

		if (ArrayUtils.isNotEmpty(files)) {
			for (File f : files) {
				if (lastModif == null && f.canRead()) {
					last = f;
					break;
				} else {
					if (f.lastModified() > lastModif && f.canRead()) {
						last = f;
					} else {
						break;
					}
				}
			}
		}

		return last;
	}

	/**
	 * Deserializes JSON data object ({@code String}, {@code Reader},
	 * {@code InputStream}) into map structured data.
	 *
	 * @param jsonData
	 *            JSON format data object
	 *
	 * @return data map parsed from JSON data object
	 *
	 * @throws JsonSyntaxException
	 *             if there was a problem reading from the Reader
	 * @throws JsonIOException
	 *             if json is not a valid representation for an object of type
	 *
	 * @see Gson#fromJson(String, Class)
	 * @see Gson#fromJson(Reader, Class)
	 */
	public static Map<String, ?> fromJsonToMap(Object jsonData) {
		Map<String, ?> map = new LinkedTreeMap<String, Object>();
		Gson gson = new Gson();

		if (jsonData instanceof String) {
			map = (Map<String, ?>) gson.fromJson((String) jsonData, map.getClass());
		} else if (jsonData instanceof Reader) {
			map = (Map<String, ?>) gson.fromJson((Reader) jsonData, map.getClass());
		} else if (jsonData instanceof InputStream) {
			map = (Map<String, ?>) gson.fromJson(new BufferedReader(new InputStreamReader((InputStream) jsonData)),
					map.getClass());
		}

		return map;
	}

	/**
	 * Returns string line read from data source. Data source object can be
	 * {@code String}, {@code Reader} or {@code InputStream}.
	 *
	 * @param data
	 *            data source object to read string line
	 *
	 * @return string line read from data source
	 *
	 * @throws IOException
	 *             If an I/O error occurs while reading line
	 *
	 * @see BufferedReader#readLine()
	 */
	public static String getStringLine(Object data) throws IOException {
		if (data == null) {
			return null;
		}
		if (data instanceof String) {
			return (String) data;
		}

		BufferedReader rdr = null;
		if (data instanceof BufferedReader) {
			rdr = (BufferedReader) data;
		} else if (data instanceof Reader) {
			rdr = new BufferedReader((Reader) data);
		} else if (data instanceof InputStream) {
			rdr = new BufferedReader(new InputStreamReader((InputStream) data));
		}

		String str = rdr == null ? null : rdr.readLine();

		return str;
	}

	/**
	 * Returns tag strings array retrieved from provided data object. Data
	 * object an be string (tags delimiter ','), strings collection or strings
	 * array.
	 * 
	 * @param tagsData
	 *            tags data object
	 *
	 * @return tag strings array, or {@code null} if arrays can't be made
	 */
	public static String[] getTags(Object tagsData) {
		if (tagsData instanceof String) {
			return ((String) tagsData).split(TAG_DELIM);
		} else if (tagsData instanceof String[]) {
			return (String[]) tagsData;
		} else if (tagsData instanceof Collection) {
			Collection<?> tagsList = (Collection<?>) tagsData;
			if (!tagsList.isEmpty()) {
				String[] tags = new String[tagsList.size()];
				tags = tagsList.toArray(tags);

				return tags;
			}
		}

		return null;
	}

	/**
	 * Cleans raw activity data. If activity data is string, removes 'new line'
	 * symbols from it. Returns same object otherwise.
	 *
	 * @param activityData
	 *            raw activity data
	 *
	 * @return raw activity data without 'new line' symbols.
	 */
	public static Object cleanActivityData(Object activityData) {
		if (activityData instanceof String) {
			String ads = (String) activityData;

			return ads.replaceAll("(\\r\\n|\\r|\\n)", "");
		}

		return activityData;
	}
}
