/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jkool.tnt4j.streams.utils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.internal.LinkedTreeMap;
import com.jkool.tnt4j.streams.parsers.MessageType;
import com.nastel.jkool.tnt4j.core.OpType;

/**
 * General utility methods.
 *
 * @version $Revision: 1 $
 */
public final class Utils extends com.nastel.jkool.tnt4j.utils.Utils {

	private static final String TAG_DELIM = ","; // NON-NLS
	private static final Pattern LINE_ENDINGS_PATTERN = Pattern.compile("(\\r\\n|\\r|\\n)"); // NON-NLS
	private static final Pattern UUID_PATTERN = Pattern
			.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"); // NON-NLS

	/**
	 * Default floating point numbers equality comparison difference tolerance
	 * {@value}.
	 */
	public static final double DEFAULT_EPSILON = 0.000001;

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
			ba = Hex.decodeHex((str == null ? "" : str).toCharArray());
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
	 * <p>
	 * It is up to the individual stream to determine which of these attributes
	 * is available/required to uniquely identify a message. In order to
	 * identify a message within two different transports, the streams for each
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
	 * <p>
	 * It is up to the individual stream to determine which of these attributes
	 * is available/required to uniquely identify a message. In order to
	 * identify a message within two different transports, the streams for each
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
	 * @return OpType mapping or {@code null} if mapping not found
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
		try {
			return OpType.valueOf(opType.toUpperCase());
		} catch (IllegalArgumentException exc) {
			if (opType.equalsIgnoreCase("END")) { // NON-NLS
				return OpType.STOP;
			}
		}

		return OpType.OTHER;
	}

	private static OpType mapOpType(int opType) {
		try {
			return OpType.valueOf(opType);
		} catch (IllegalArgumentException exc) {
		}

		return OpType.OTHER;
	}

	private static OpType mapOpTypeTW(int opType) {
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
	 * @return {@code true} if file name contains wildcard characters
	 */
	public static boolean isWildcardFileName(String fileName) {
		if (StringUtils.isNotEmpty(fileName)) {
			return fileName.contains("*") || fileName.contains("?"); // NON-NLS
		}
		return false;
	}

	/**
	 * Counts text lines available in input.
	 *
	 * @param reader
	 *            a {@link Reader} object to provide the underlying input stream
	 *
	 * @return number of lines currently available in input
	 *
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	public static int countLines(Reader reader) throws IOException {
		int lCount = 0;
		LineNumberReader lineReader = null;
		try {
			lineReader = new LineNumberReader(reader);
			lineReader.skip(Long.MAX_VALUE);
			// NOTE: Add 1 because line index starts at 0
			lCount = lineReader.getLineNumber() + 1;
		} finally {
			Utils.close(lineReader);
		}

		return lCount;
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
				if (f.canRead()) {
					if (lastModif == null) {
						last = f;
						break;
					} else {
						if (f.lastModified() > lastModif) {
							last = f;
						} else {
							break;
						}
					}
				}
			}
		}

		return last;
	}

	/**
	 * Deserializes JSON data object ({@link String}, {@link Reader},
	 * {@link InputStream}) into map structured data.
	 *
	 * @param jsonData
	 *            JSON format data object
	 * @param jsonAsLine
	 *            flag indicating that complete JSON data package is single line
	 *
	 * @return data map parsed from JSON data object
	 *
	 * @throws com.google.gson.JsonSyntaxException
	 *             if there was a problem reading from the Reader
	 * @throws com.google.gson.JsonIOException
	 *             if json is not a valid representation for an object of type
	 *
	 * @see Gson#fromJson(String, Class)
	 * @see Gson#fromJson(Reader, Class)
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, ?> fromJsonToMap(Object jsonData, boolean jsonAsLine) {
		Map<String, ?> map = new LinkedTreeMap<String, Object>();
		Gson gson = new Gson();

		if (jsonAsLine) {
			try {
				map = (Map<String, ?>) gson.fromJson(getStringLine(jsonData), map.getClass());
			} catch (IOException ioe) {
				throw new JsonIOException(ioe);
			}
		} else {
			if (jsonData instanceof String) {
				map = (Map<String, ?>) gson.fromJson((String) jsonData, map.getClass());
			} else if (jsonData instanceof byte[]) {
				map = (Map<String, ?>) gson.fromJson(getString((byte[]) jsonData), map.getClass());
			} else if (jsonData instanceof Reader) {
				map = (Map<String, ?>) gson.fromJson((Reader) jsonData, map.getClass());
			} else if (jsonData instanceof InputStream) {
				map = (Map<String, ?>) gson.fromJson(new BufferedReader(new InputStreamReader((InputStream) jsonData)),
						map.getClass());
			}
		}

		return map;
	}

	/**
	 * Returns string line read from data source. Data source object can be
	 * {@link String}, {@link Reader} or {@link InputStream}.
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

		BufferedReader rdr = null;
		boolean autoClose = false;
		if (data instanceof String) {
			rdr = new BufferedReader(new StringReader((String) data));
			autoClose = true;
		} else if (data instanceof byte[]) {
			rdr = new BufferedReader(new StringReader(getString((byte[]) data)));
			autoClose = true;
		} else if (data instanceof BufferedReader) {
			rdr = (BufferedReader) data;
		} else if (data instanceof Reader) {
			rdr = new BufferedReader((Reader) data);
		} else if (data instanceof InputStream) {
			rdr = new BufferedReader(new InputStreamReader((InputStream) data));
		}

		try {
			String str = rdr == null ? null : rdr.readLine();

			return str;
		} finally {
			if (autoClose) {
				close(rdr);
			}
		}
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
	@SuppressWarnings("unchecked")
	public static String[] getTags(Object tagsData) {
		if (tagsData instanceof String) {
			return ((String) tagsData).split(TAG_DELIM);
		} else if (tagsData instanceof String[]) {
			return (String[]) tagsData;
		} else if (tagsData instanceof Collection) {
			Collection<String> tagsList = (Collection<String>) tagsData;
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
	 * @param <T>
	 *            type of raw activity data
	 *
	 * @return raw activity data without 'new line' symbols
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cleanActivityData(T activityData) {
		if (activityData instanceof String) {
			String ads = (String) activityData;

			return (T) LINE_ENDINGS_PATTERN.matcher(ads).replaceAll(""); // NON-NLS
		}

		return activityData;
	}

	/**
	 * Makes a new {@link String} by decoding the specified array of bytes using
	 * the UTF-8 charset. If {@link String} can't be constructed using UTF-8
	 * charset, then the platform's default charset is used.
	 *
	 * @param strBytes
	 *            The bytes to be decoded into characters
	 *
	 * @return string constructed from specified byte array
	 *
	 * @see String#String(byte[], Charset)
	 * @see String#String(byte[], String)
	 * @see String#String(byte[])
	 */
	public static String getString(byte[] strBytes) {
		try {
			return new String(strBytes, Charset.forName("UTF-8"));
		} catch (UnsupportedCharsetException uce) {
			try {
				return new String(strBytes, "UTF-8");
			} catch (UnsupportedEncodingException uee) {
				return new String(strBytes);
			}
		}
	}

	/**
	 * Returns UUID found in provided string.
	 *
	 * @param str
	 *            string to find UUID
	 * 
	 * @return found uuid
	 */
	public static UUID findUUID(String str) {
		Matcher m = UUID_PATTERN.matcher(str);
		String fnUUID = null;
		if (m.find()) {
			fnUUID = m.group();
		}

		return StringUtils.isEmpty(fnUUID) ? null : UUID.fromString(fnUUID);
	}

	/**
	 * Transforms (serializes) XML DOM document to string.
	 *
	 * @param doc
	 *            document to transform to string
	 *
	 * @return XML string representation of document
	 *
	 * @throws TransformerException
	 *             If an exception occurs while transforming XML DOM document to
	 *             string
	 */
	public static String documentToString(Document doc) throws TransformerException {
		StringWriter sw = new StringWriter();
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no"); // NON-NLS
		transformer.setOutputProperty(OutputKeys.METHOD, "xml"); // NON-NLS
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // NON-NLS
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); // NON-NLS

		transformer.transform(new DOMSource(doc), new StreamResult(sw));

		return sw.toString(); // sw.toString().replaceAll("\n|\r", ""); //NOTE:
								// if single line
	}

	/**
	 * Checks equality of two double numbers with difference tolerance
	 * {@value #DEFAULT_EPSILON}.
	 *
	 * @param d1
	 *            first double to compare
	 * @param d2
	 *            second double to compare
	 *
	 * @return {@code true} if difference is less than epsilon, {@code false} -
	 *         otherwise
	 */
	public static boolean equals(double d1, double d2) {
		return equals(d1, d2, DEFAULT_EPSILON);
	}

	/**
	 * Checks equality of two double numbers with given difference tolerance
	 * {@code epsilon}.
	 * 
	 * @param d1
	 *            first double to compare
	 * @param d2
	 *            second double to compare
	 * @param epsilon
	 *            value of difference tolerance
	 *
	 * @return {@code true} if difference is less than epsilon, {@code false} -
	 *         otherwise
	 */
	public static boolean equals(double d1, double d2, double epsilon) {
		return Math.abs(d1 - d2) < epsilon;
	}

	/**
	 * Close an socket without exceptions.
	 * <p>
	 * For Java 6 backward comparability.
	 *
	 * @param socket
	 *            socket to close
	 */
	public static void close(Socket socket) {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException exc) {

			}
		}
	}

	/**
	 * Close an server socket without exceptions.
	 * <p>
	 * For Java 6 backward comparability.
	 *
	 * @param socket
	 *            server socket to close
	 */
	public static void close(ServerSocket socket) {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException exc) {

			}
		}
	}

	/**
	 * Returns first non empty text string available to read from defined
	 * reader.
	 *
	 * @param reader
	 *            reader to use for reading
	 *
	 * @return non empty text string, or {@code null} if the end of the stream
	 *         has been reached
	 *
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	public static String getNonEmptyLine(BufferedReader reader) throws IOException {
		String line;

		while ((line = reader.readLine()) != null) {
			if (line.trim().isEmpty()) {
				// empty line
			} else {
				break;
			}
		}

		return line;
	}

	/**
	 * Reads text lines into one string from provided input stream.
	 *
	 * @param is
	 *            input stream to read
	 * @param separateLines
	 *            flag indicating whether to make string lines separated
	 *
	 * @return string read from input stream
	 */
	public static String readInput(InputStream is, boolean separateLines) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder builder = new StringBuilder();
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(separateLines ? System.getProperty("line.separator") : " ");
			}
		} catch (IOException exc) {
		} finally {
			Utils.close(reader);
		}

		return builder.toString();
	}
}
