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

package com.jkoolcloud.tnt4j.streams.utils;

import java.io.*;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.security.MessageDigest;
import java.util.Arrays;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.HexDump;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.internal.LinkedTreeMap;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.streams.parsers.MessageType;

/**
 * General utility methods.
 *
 * @version $Revision: 1 $
 */
public final class Utils extends com.jkoolcloud.tnt4j.utils.Utils {

	private static final String TAG_DELIM = ","; // NON-NLS
	private static final Pattern LINE_ENDINGS_PATTERN = Pattern.compile("(\\r\\n|\\r|\\n)"); // NON-NLS
	private static final Pattern UUID_PATTERN = Pattern
			.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"); // NON-NLS
	private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{(\\w+)\\}"); // NON-NLS

	/**
	 * Default floating point numbers equality comparison difference tolerance {@value}.
	 */
	public static final double DEFAULT_EPSILON = 0.000001;

	/**
	 * Constant for system dependent line separator symbol.
	 */
	public static final String NEW_LINE = System.getProperty("line.separator");

	private Utils() {
	}

	/**
	 * Base64 encodes the specified sequence of bytes.
	 *
	 * @param src
	 *            byte sequence to encode
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
	 * @return decoded byte sequence
	 */
	public static byte[] base64Decode(byte[] src) {
		return Base64.decodeBase64(src);
	}

	/**
	 * Converts an array of bytes into an array of characters representing the hexadecimal values.
	 *
	 * @param src
	 *            byte sequence to encode
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
	 * @return OpType mapping or {@code null} if mapping not found
	 */
	public static OpType mapOpType(Object opType) {
		if (opType == null) {
			return null;
		}
		if (opType instanceof Number) {
			return mapOpType(((Number) opType).intValue());
		}
		return mapOpType(String.valueOf(opType));
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

	/**
	 * Checks if provided string contains wildcard characters.
	 *
	 * @param str
	 *            string to check if it contains wildcard characters
	 * @return {@code true} if string contains wildcard characters
	 */
	public static boolean isWildcardString(String str) {
		if (StringUtils.isNotEmpty(str)) {
			return str.contains("*") || str.contains("?"); // NON-NLS
		}
		return false;
	}

	/**
	 * Transforms wildcard mask string to regex ready string.
	 *
	 * @param str
	 *            wildcard string
	 * @return regex ready string
	 */
	public static String wildcardToRegex(String str) {
		return StringUtils.isEmpty(str) ? str : str.replace("?", ".?").replace("*", ".*?"); // NON-NLS
	}

	/**
	 * Checks if string is wildcard mask string and if {@code true} then transforms it to regex ready string.
	 *
	 * @param str
	 *            string to check and transform
	 * @return regex ready string
	 * @see #wildcardToRegex(String)
	 */
	public static String wildcardToRegex2(String str) {
		return isWildcardString(str) ? wildcardToRegex(str) : str;
	}

	/**
	 * Counts text lines available in input.
	 *
	 * @param reader
	 *            a {@link Reader} object to provide the underlying input stream
	 * @return number of lines currently available in input
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
	 * Returns first readable file from {@code files} array which has last modification timestamp newer than
	 * {@code lastModif}. If {@code lastModif} is {@code null}, then newest readable file is returned.
	 *
	 * @param files
	 *            last modification timestamp ordered files array
	 * @param lastModif
	 *            last modification time to compare. If {@code null} - then newest file is returned
	 * @return first file from array which has modification time later than {@code lastModif}, or {@code null} if
	 *         {@code files} is empty or does not contain readable file with last modification time later than
	 *         {@code lastModif}
	 */
	public static File getFirstNewer(File[] files, Long lastModif) {
		File last = null;

		if (ArrayUtils.isNotEmpty(files)) {
			boolean changeDir = (files[0].lastModified() - files[files.length - 1].lastModified()) < 0;

			for (int i = changeDir ? files.length - 1 : 0; changeDir ? i >= 0
					: i < files.length; i = changeDir ? i - 1 : i + 1) {
				File f = files[i];
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
	 * Deserializes JSON data object ({@link String}, {@link Reader}, {@link InputStream}) into map structured data.
	 *
	 * @param jsonData
	 *            JSON format data object
	 * @param jsonAsLine
	 *            flag indicating that complete JSON data package is single line
	 * @return data map parsed from JSON data object
	 * @throws com.google.gson.JsonSyntaxException
	 *             if there was a problem reading from the Reader
	 * @throws com.google.gson.JsonIOException
	 *             if json is not a valid representation for an object of type
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
	 * Returns string line read from data source. Data source object can be {@link String}, {@link Reader} or
	 * {@link InputStream}.
	 *
	 * @param data
	 *            data source object to read string line
	 * @return string line read from data source
	 * @throws IOException
	 *             If an I/O error occurs while reading line
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
			return rdr == null ? null : rdr.readLine();
		} finally {
			if (autoClose) {
				close(rdr);
			}
		}
	}

	/**
	 * Returns tag strings array retrieved from provided data object. Data object an be string (tags delimiter ','),
	 * strings collection or strings array.
	 *
	 * @param tagsData
	 *            tags data object
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
	 * Cleans raw activity data. If activity data is string, removes 'new line' symbols from it. Returns same object
	 * otherwise.
	 *
	 * @param <T>
	 *            type of raw activity data
	 * @param activityData
	 *            raw activity data
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
	 * Makes a new {@link String} by decoding the specified array of bytes using the UTF-8 charset. If {@link String}
	 * can't be constructed using UTF-8 charset, then the platform's default charset is used.
	 *
	 * @param strBytes
	 *            The bytes to be decoded into characters
	 * @return string constructed from specified byte array
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
	 * @return XML string representation of document
	 * @throws TransformerException
	 *             If an exception occurs while transforming XML DOM document to string
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

		return sw.toString();
		// NOTE: if return as single line
		// return sw.toString().replaceAll("\n|\r", ""); // NON-NLS
	}

	/**
	 * Checks equality of two double numbers with difference tolerance {@value #DEFAULT_EPSILON}.
	 *
	 * @param d1
	 *            first double to compare
	 * @param d2
	 *            second double to compare
	 * @return {@code true} if difference is less than epsilon, {@code false} - otherwise
	 */
	public static boolean equals(double d1, double d2) {
		return equals(d1, d2, DEFAULT_EPSILON);
	}

	/**
	 * Checks equality of two double numbers with given difference tolerance {@code epsilon}.
	 *
	 * @param d1
	 *            first double to compare
	 * @param d2
	 *            second double to compare
	 * @param epsilon
	 *            value of difference tolerance
	 * @return {@code true} if difference is less than epsilon, {@code false} - otherwise
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
	 * Returns first non empty text string available to read from defined reader.
	 *
	 * @param reader
	 *            reader to use for reading
	 * @return non empty text string, or {@code null} if the end of the stream has been reached
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
	 * Reads text lines into one string from provided {@link InputStream}.
	 *
	 * @param is
	 *            input stream to read
	 * @param separateLines
	 *            flag indicating whether to make string lines separated
	 * @return string read from input stream
	 * @see #readInput(BufferedReader, boolean)
	 */
	public static String readInput(InputStream is, boolean separateLines) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String text = null;
		try {
			text = readInput(reader, separateLines);
		} catch (IOException exc) {
		} finally {
			Utils.close(reader);
		}

		return text;
	}

	/**
	 * Reads text lines into one string from provided {@link BufferedReader}.
	 *
	 * @param reader
	 *            reader to read string
	 * @param separateLines
	 *            flag indicating whether to make string lines separated
	 * @return string read from reader
	 */
	public static String readInput(BufferedReader reader, boolean separateLines) throws IOException {
		StringBuilder builder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append(separateLines ? NEW_LINE : " ");
		}

		return builder.toString();
	}

	/**
	 * Gets a string representation of specified object for use in debugging, which includes the value of each object
	 * field.
	 *
	 * @param obj
	 *            object to get debugging info
	 * @return debugging string representation
	 */
	public static String getDebugString(Object obj) {
		if (obj == null) {
			return "null";
		}

		StringBuilder sb = new StringBuilder();
		Class<?> objClass = obj.getClass();
		sb.append(objClass.getSimpleName()).append('{');

		try {
			Field[] valueObjFields = objClass.getDeclaredFields();

			for (int i = 0; i < valueObjFields.length; i++) {
				String fieldName = valueObjFields[i].getName();
				valueObjFields[i].setAccessible(true);

				String valueStr = toString(valueObjFields[i].get(obj));

				sb.append(fieldName).append("='").append(valueStr).append('\'') // NON-NLS
						.append(i < valueObjFields.length - 1 ? ", " : ""); // NON-NLS
			}
		} catch (Exception exc) {
			sb.append(exc.toString());
		}

		sb.append('}');

		return sb.toString();
	}

	/**
	 * Clears string builder content.
	 *
	 * @param sb
	 *            string builder to clear content
	 * @return empty string builder
	 */
	public static StringBuilder clear(StringBuilder sb) {
		return sb == null ? null : sb.delete(0, sb.length());
	}

	/**
	 * Clears string buffer content.
	 *
	 * @param sb
	 *            string buffer to clear content
	 * @return empty string buffer
	 */
	public static StringBuffer clear(StringBuffer sb) {
		return sb == null ? null : sb.delete(0, sb.length());
	}

	/**
	 * Checks if number object is {@code null} or has value equal to {@code 0}.
	 *
	 * @param number
	 *            number object to check
	 * @return {@code true} if number is {@code null} or number value is {@code 0}, {@code false} - otherwise
	 */
	public static boolean isZero(Number number) {
		return number == null || number.intValue() == 0;
	}

	/**
	 * Finds variable expressions like '${varName}' in provided array of strings and puts into collection.
	 *
	 * @param vars
	 *            collection to add resolved variable expression
	 * @param attrs
	 *            array of {@link String}s to find variable expressions
	 */
	public static void resolveVariables(Collection<String> vars, String... attrs) {
		if (attrs != null) {
			for (String attr : attrs) {
				if (StringUtils.isNotEmpty(attr)) {
					Matcher m = VAR_PATTERN.matcher(attr);
					while (m.find()) {
						vars.add(m.group());
					}
				}
			}
		}
	}

	/**
	 * Makes {@link Object} type array from provided object instance.
	 * <p>
	 * If obj is {@code Object[]}, then simple casting is performed. If obj is {@link Collection}, then method
	 * {@link Collection#toArray()} is invoked. In all other cases - new single item array is created.
	 *
	 * @param obj
	 *            object instance to make an array
	 * @return array made of provided object, or {@code null} if obj is {@code null}
	 */
	public static Object[] makeArray(Object obj) {
		if (obj == null) {
			return null;
		}

		return obj instanceof Object[] ? (Object[]) obj
				: obj instanceof Collection ? ((Collection<?>) obj).toArray() : new Object[] { obj };
	}

	/**
	 * Checks if provided object is {@link Collection} or {@code Object[]}.
	 *
	 * @param obj
	 *            object to check
	 * @return {@code true} if obj is {@link Collection} or {@code Object[]}, {@code false} - otherwise.
	 */
	public static boolean isCollection(Object obj) {
		return obj instanceof Object[] || obj instanceof Collection;
	}

	/**
	 * Checks if provided class represents an array class or is implementation of {@link Collection}.
	 *
	 * @param cls
	 *            class to check
	 * @return {@code true} if cls is implementation of {@link Collection} or represents an array class, {@code false} -
	 *         otherwise.
	 */
	public static boolean isCollectionType(Class<?> cls) {
		return cls.isArray() || Collection.class.isAssignableFrom(cls);
	}

	/**
	 * Wraps provided object item seeking by index.
	 * <p>
	 * If obj is not {@link Collection} or {@code Object[]}, then same object is returned.
	 * <p>
	 * In case of {@link Collection} - it is transformed to {@code Object[]}.
	 * <p>
	 * When obj is {@code Object[]} - array item referenced by index is returned if {@code index < array.length}, first
	 * array item if {@code array.length == 1} or {@code null} in all other cases.
	 *
	 * @param obj
	 *            object instance containing item
	 * @param index
	 *            item index
	 * @return item found, or {@code null} if obj is {@code null} or {@code index >= array.length}
	 */
	public static Object getItem(Object obj, int index) {
		if (obj instanceof Collection) {
			obj = ((Collection<?>) obj).toArray();
		}

		if (obj instanceof Object[]) {
			Object[] array = (Object[]) obj;

			return index < array.length ? array[index] : array.length == 1 ? array[0] : null;
		}

		return obj;
	}

	/**
	 * Returns the appropriate string representation for the specified object.
	 *
	 * @param value
	 *            object to convert to string representation
	 *
	 * @return string representation of object
	 */
	public static String toString(Object value) {
		if (value instanceof byte[]) {
			return getString((byte[]) value);
		}
		if (value instanceof char[]) {
			return new String((char[]) value);
		}
		if (value instanceof Object[]) {
			return toStringDeep((Object[]) value);
		}
		return String.valueOf(value);
	}

	/**
	 * Returns single object (first item) if list/array contains single item, makes an array from {@link Collection}, or
	 * returns same value as parameter in all other cases.
	 *
	 * @param value
	 *            object value to simplify
	 * @return same value as parameter if it is not array or collection; first item of list/array if it contains single
	 *         item; array of values if list/array contains more than one item; {@code null} if parameter object is
	 *         {@code null} or list/array is empty
	 */
	public static Object simplifyValue(Object value) {
		if (value instanceof Collection) {
			return simplifyValue((Collection<?>) value);
		}
		if (value instanceof Object[]) {
			return simplifyValue((Object[]) value);
		}

		return value;
	}

	private static Object simplifyValue(Collection<?> valuesList) {
		if (CollectionUtils.isEmpty(valuesList)) {
			return null;
		}

		return valuesList.size() == 1 ? valuesList.iterator().next() : valuesList.toArray();
	}

	private static Object simplifyValue(Object[] valuesArray) {
		if (ArrayUtils.isEmpty(valuesArray)) {
			return null;
		}

		return valuesArray.length == 1 ? valuesArray[0] : valuesArray;
	}

	/**
	 * Returns the appropriate string representation for the specified array.
	 *
	 * @param a
	 *            array to convert to string representation
	 * @return string representation of array
	 */
	public static String toStringDeep(Object[] a) {
		if (a == null)
			return "null"; // NON-NLS

		int iMax = a.length - 1;
		if (iMax == -1)
			return "[]"; // NON-NLS

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0;; i++) {
			b.append(toString(a[i]));
			if (i == iMax)
				return b.append(']').toString();
			b.append(", "); // NON-NLS
		}
	}

	/**
	 * Makes a HEX dump string representation of provided bytes array. Does all the same as
	 * {@link #toHex(byte[], int, int)} setting {@code len} parameter to {@code 0}.
	 * 
	 * @param b
	 *            bytes array make HEX dump
	 * @param offset
	 *            offset at which to start dumping bytes
	 * @return returns HEX dump representation of provided bytes array
	 *
	 * @see #toHex(byte[], int, int)
	 */
	public static String toHex(byte[] b, int offset) {
		return toHex(b, offset, 0);
	}

	/**
	 * Makes a HEX dump string representation of provided bytes array.
	 *
	 * @param b
	 *            bytes array make HEX dump
	 * @param offset
	 *            offset at which to start dumping bytes
	 * @param len
	 *            maximum number of bytes to dump
	 * @return returns HEX dump representation of provided bytes array
	 */
	public static String toHex(byte[] b, int offset, int len) {
		if (b == null) {
			return "<EMPTY>"; // NON-NLS
		}

		String hexStr;
		ByteArrayOutputStream bos = new ByteArrayOutputStream(b.length * 2);
		try {
			if (len > 0 && len < b.length) {
				byte[] bc = Arrays.copyOfRange(b, offset, offset + len);
				HexDump.dump(bc, 0, bos, offset);
			} else {
				HexDump.dump(b, 0, bos, offset);
			}
			hexStr = NEW_LINE + bos.toString("UTF-8"); // NON-NLS
			bos.close();
		} catch (Exception exc) {
			hexStr = "HEX FAIL: " + exc.getLocalizedMessage(); // NON-NLS
		}

		return hexStr;
	}
}
