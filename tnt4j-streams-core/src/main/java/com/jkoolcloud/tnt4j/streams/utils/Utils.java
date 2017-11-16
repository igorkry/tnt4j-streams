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

import java.io.*;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Paths;
import java.util.*;
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
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.internal.LinkedTreeMap;
import com.jkoolcloud.tnt4j.core.OpType;

/**
 * General utility methods used by TNT4J-Streams.
 *
 * @version $Revision: 1 $
 */
public final class Utils extends com.jkoolcloud.tnt4j.utils.Utils {

	private static final String TAG_DELIM = ","; // NON-NLS
	private static final String VALUE_DELIM = "\\|"; // NON-NLS
	private static final String HEX_PREFIX = "0x"; // NON-NLS
	private static final Pattern LINE_ENDINGS_PATTERN = Pattern.compile("(\\r\\n|\\r|\\n)"); // NON-NLS
	private static final Pattern UUID_PATTERN = Pattern
			.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"); // NON-NLS

	/**
	 * Variable expression placeholder definition start token.
	 */
	public static final String VAR_EXP_START_TOKEN = "${"; // NON-NLS
	private static final String VAR_EXP_END_TOKEN = "}"; // NON-NLS
	private static final Pattern CFG_VAR_PATTERN = Pattern.compile("\\$\\{[\\w.]+\\}"); // NON-NLS
	private static final Pattern EXPR_VAR_PATTERN = CFG_VAR_PATTERN;// Pattern.compile("\\$(\\w+)"); // NON-NLS

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
	 * Base64 encodes the specified sequence of bytes as string.
	 *
	 * @param src
	 *            byte sequence to encode
	 * @return encoded string
	 */
	public static String base64EncodeStr(byte[] src) {
		return Base64.encodeBase64String(src);
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
	 * Base64 decodes the specified encoded data string.
	 *
	 * @param src
	 *            base64 encoded string to decode
	 * @return decoded byte sequence
	 */
	public static byte[] base64Decode(String src) {
		return Base64.decodeBase64(src);
	}

	/**
	 * Converts an array of bytes into a string representing the hexadecimal bytes values.
	 *
	 * @param src
	 *            byte sequence to encode
	 * @return encoded bytes hex string
	 */
	public static String encodeHex(byte[] src) {
		return Hex.encodeHexString(src);
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
			if (str != null && str.startsWith(HEX_PREFIX)) {
				str = str.substring(HEX_PREFIX.length());
			}
			ba = Hex.decodeHex((str == null ? "" : str).toCharArray());
		} catch (DecoderException e) {
		}
		return ba;
	}

	/**
	 * Maps OpType enumeration constant from string or numeric value.
	 *
	 * @param opType
	 *            object to be mapped to OpType enumeration constant
	 * @return OpType mapping, or {@code null} if mapping not found
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
			if (opType.equalsIgnoreCase("END") || opType.equalsIgnoreCase("FINISH") // NON-NLS
					|| opType.equalsIgnoreCase("DISCONNECT") || opType.equalsIgnoreCase("COMMIT") // NON-NLS
					|| opType.equalsIgnoreCase("BACK")) { // NON-NLS
				return OpType.STOP;
			}
			if (opType.equalsIgnoreCase("CONNECT") || opType.equalsIgnoreCase("BEGIN")) { // NON-NLS
				return OpType.START;
			}
			if (opType.equalsIgnoreCase("CALLBACK") || opType.equalsIgnoreCase("GET")) { // NON-NLS
				return OpType.RECEIVE;
			}
			if (opType.equalsIgnoreCase("PUT")) { // NON-NLS
				return OpType.SEND;
			}
			if (StringUtils.endsWithIgnoreCase(opType, "OPEN")) { // NON-NLS
				return OpType.OPEN;
			}
			if (StringUtils.endsWithIgnoreCase(opType, "CLOSE")) { // NON-NLS
				return OpType.CLOSE;
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
	 *
	 * @see #wildcardToRegex(String)
	 */
	public static String wildcardToRegex2(String str) {
		return isWildcardString(str) ? wildcardToRegex(str) : str;
	}

	/**
	 * Counts text lines available in input.
	 *
	 * @param reader
	 *            a {@link java.io.Reader} object to provide the underlying input stream
	 * @return number of lines currently available in input
	 * @throws java.io.IOException
	 *             If an I/O error occurs
	 * @deprecated use {@link #countLines(java.io.InputStream)} instead
	 */
	@Deprecated
	public static int countLines(Reader reader) throws IOException {
		int lCount = 0;
		LineNumberReader lineReader = null;
		try {
			lineReader = new LineNumberReader(reader);
			lineReader.skip(Long.MAX_VALUE);
			// NOTE: Add 1 because line index starts at 0
			lCount = lineReader.getLineNumber() + 1;
		} finally {
			close(lineReader);
		}

		return lCount;
	}

	/**
	 * Counts text lines available in input.
	 *
	 * @param is
	 *            a {@link java.io.InputStream} object to provide the underlying file input stream
	 * @return number of lines currently available in input
	 * @throws java.io.IOException
	 *             If an I/O error occurs
	 */
	public static int countLines(InputStream is) throws IOException {
		InputStream bis = is instanceof BufferedInputStream ? (BufferedInputStream) is : new BufferedInputStream(is);
		try {
			byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			boolean endsWithoutNewLine = false;
			while ((readChars = bis.read(c)) != -1) {
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
				endsWithoutNewLine = (c[readChars - 1] != '\n');
			}
			if (endsWithoutNewLine) {
				++count;
			}
			return count;
		} finally {
			close(bis);
		}
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
	 * Deserializes JSON data object ({@link String}, {@link java.io.Reader}, {@link java.io.InputStream}) into map
	 * structured data.
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
	 *
	 * @see com.google.gson.Gson#fromJson(String, Class)
	 * @see com.google.gson.Gson#fromJson(java.io.Reader, Class)
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
	 * Returns string line read from data source. Data source object can be {@link String}, {@link java.io.Reader} or
	 * {@link java.io.InputStream}.
	 *
	 * @param data
	 *            data source object to read string line
	 * @return string line read from data source
	 * @throws java.io.IOException
	 *             If an I/O error occurs while reading line
	 *
	 * @see java.io.BufferedReader#readLine()
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
		} else if (data instanceof ByteBuffer) {
			ByteBuffer bb = (ByteBuffer) data;
			rdr = new BufferedReader(new StringReader(getString(bb.array())));
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
	 * Returns tag strings array retrieved from provided data object. Data object can be string (tags delimiter ','),
	 * strings collection or strings array.
	 *
	 * @param tagsData
	 *            tags data object
	 * @return tag strings array, or {@code null} if arrays can't be made
	 */
	public static String[] getTags(Object tagsData) {
		if (tagsData instanceof byte[]) {
			return new String[] { encodeHex((byte[]) tagsData) };
		} else if (tagsData instanceof String) {
			return ((String) tagsData).split(TAG_DELIM);
		} else if (tagsData instanceof String[]) {
			return (String[]) tagsData;
		} else if (isCollection(tagsData)) {
			Object[] tagsArray = makeArray(tagsData);

			if (ArrayUtils.isNotEmpty(tagsArray)) {
				List<String> tags = new ArrayList<>(tagsArray.length);
				for (Object aTagsArray : tagsArray) {
					String[] ta = getTags(aTagsArray);

					if (ArrayUtils.isNotEmpty(ta)) {
						Collections.addAll(tags, ta);
					}
				}

				return tags.toArray(new String[tags.size()]);
			}
		}

		return null;
	}

	/**
	 * Makes Hex string representation (starting '0x') of byte array.
	 *
	 * @param bytes
	 *            byte array to represent as string
	 * @return hex string representation of byte array
	 *
	 * @see #encodeHex(byte[])
	 */
	public static String toHexString(byte[] bytes) {
		return HEX_PREFIX + encodeHex(bytes);
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
	 *
	 * @see String#String(byte[], java.nio.charset.Charset)
	 * @see String#String(byte[], String)
	 * @see String#String(byte[])
	 */
	public static String getString(byte[] strBytes) {
		try {
			return new String(strBytes, Charset.forName(UTF8));
		} catch (UnsupportedCharsetException uce) {
			try {
				return new String(strBytes, UTF8);
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
	 * @throws javax.xml.transform.TransformerException
	 *             If an exception occurs while transforming XML DOM document to string
	 */
	public static String documentToString(Node doc) throws TransformerException {
		StringWriter sw = new StringWriter();
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no"); // NON-NLS
		transformer.setOutputProperty(OutputKeys.METHOD, "xml"); // NON-NLS
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // NON-NLS
		transformer.setOutputProperty(OutputKeys.ENCODING, UTF8);

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
	 * @throws java.io.IOException
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
	 * Reads all text availalbe to read from defined <tt>reader</tt>.
	 *
	 * @param reader
	 *            reader to use for reading
	 * @return tests string raed from <tt>reader</tt>
	 * @throws IOException
	 *             If an I/O error occurs
	 */
	public static String readAll(BufferedReader reader) throws IOException {
		StringBuilder sb = new StringBuilder();

		String line;
		while ((line = reader.readLine()) != null) {
			if (sb.length() != 0) {
				sb.append("\n");
			}
			sb.append(line);
		}

		return sb.toString();
	}

	/**
	 * Reads text lines into one string from provided {@link java.io.InputStream}.
	 *
	 * @param is
	 *            input stream to read
	 * @param separateLines
	 *            flag indicating whether to make string lines separated
	 * @return string read from input stream
	 *
	 * @see #readInput(java.io.BufferedReader, boolean)
	 */
	public static String readInput(InputStream is, boolean separateLines) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String text = null;
		try {
			text = readInput(reader, separateLines);
		} catch (IOException exc) {
		} finally {
			close(reader);
		}

		return text;
	}

	/**
	 * Reads text lines into one string from provided {@link java.io.BufferedReader}.
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

				sb.append(fieldName).append("=").append(sQuote(valueStr)) // NON-NLS
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
	public static void resolveCfgVariables(Collection<String> vars, String... attrs) {
		if (attrs != null) {
			for (String attr : attrs) {
				if (StringUtils.isNotEmpty(attr)) {
					Matcher m = CFG_VAR_PATTERN.matcher(attr);
					while (m.find()) {
						vars.add(m.group());
					}
				}
			}
		}
	}

	/**
	 * Finds variable expressions like '${VarName}' in provided string and puts into collection.
	 *
	 * @param vars
	 *            collection to add resolved variable expression
	 * @param exprStr
	 *            expression string
	 */
	public static void resolveExpressionVariables(Collection<String> vars, String exprStr) {
		if (StringUtils.isNotEmpty(exprStr)) {
			Matcher m = EXPR_VAR_PATTERN.matcher(exprStr);
			while (m.find()) {
				vars.add(m.group(0));
			}
		}
	}

	/**
	 * Checks provided expression string contains variable placeholders having {@code "${VAR_NAME}"} format.
	 *
	 * @param exp
	 *            expression string to check
	 * @return {@code true} if expression contains variable placeholders, {@code false} - otherwise
	 */
	public static boolean isVariableExpression(String exp) {
		return exp != null && EXPR_VAR_PATTERN.matcher(exp).find();
	}

	/**
	 * Makes expressions used variable placeholder representation.
	 *
	 * @param varName
	 *            variable name
	 * @return variable name surround by expression tokens
	 */
	public static String makeExpVariable(String varName) {
		return VAR_EXP_START_TOKEN + varName + VAR_EXP_END_TOKEN;
	}

	/**
	 * Makes {@link Object} type array from provided object instance.
	 * <p>
	 * If obj is {@code Object[]}, then simple casting is performed. If obj is {@link java.util.Collection}, then method
	 * {@link java.util.Collection#toArray()} is invoked. In all other cases - new single item array is created.
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
	 * Checks if provided object is {@link java.util.Collection} or {@code Object[]}.
	 *
	 * @param obj
	 *            object to check
	 * @return {@code true} if obj is {@link java.util.Collection} or {@code Object[]}, {@code false} - otherwise
	 */
	public static boolean isCollection(Object obj) {
		return obj instanceof Object[] || obj instanceof Collection;
	}

	/**
	 * Checks if provided class represents an array class or is implementation of {@link java.util.Collection}.
	 *
	 * @param cls
	 *            class to check
	 * @return {@code true} if cls is implementation of {@link java.util.Collection} or represents an array class,
	 *         {@code false} - otherwise
	 */
	public static boolean isCollectionType(Class<?> cls) {
		return cls != null && (cls.isArray() || Collection.class.isAssignableFrom(cls));
	}

	/**
	 * Wraps provided object item seeking by index.
	 * <p>
	 * If obj is not {@link java.util.Collection} or {@code Object[]}, then same object is returned.
	 * <p>
	 * In case of {@link java.util.Collection} - it is transformed to {@code Object[]}.
	 * <p>
	 * When obj is {@code Object[]} - array item referenced by index is returned if {@code index < array.length}, first
	 * array item if {@code array.length == 1}, or {@code null} in all other cases.
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
		if (value instanceof int[]) {
			return Arrays.toString((int[]) value);
		}
		if (value instanceof byte[]) {
			return getString((byte[]) value);
			// return Arrays.toString((byte[]) value);
		}
		if (value instanceof char[]) {
			return new String((char[]) value);
			// return Arrays.toString((char[]) value);
		}
		if (value instanceof long[]) {
			return Arrays.toString((long[]) value);
		}
		if (value instanceof float[]) {
			return Arrays.toString((float[]) value);
		}
		if (value instanceof short[]) {
			return Arrays.toString((short[]) value);
		}
		if (value instanceof double[]) {
			return Arrays.toString((double[]) value);
		}
		if (value instanceof boolean[]) {
			return Arrays.toString((boolean[]) value);
		}
		if (value instanceof Object[]) {
			return toStringDeep((Object[]) value);
		}
		if (value instanceof Collection) {
			Collection<?> c = (Collection<?>) value;
			return toString(c.toArray());
		}
		if (value instanceof Map) {
			Map<?, ?> m = (Map<?, ?>) value;
			return toString(m.entrySet());
		}
		if (value instanceof Node) {
			return ((Node) value).getTextContent();
		}

		return String.valueOf(value);
	}

	/**
	 * Returns the appropriate string representation for the specified array.
	 *
	 * @param args
	 *            array to convert to string representation
	 * @return string representation of array
	 *
	 * @see #toStringDeep(Object[])
	 */
	public static String arrayToString(Object... args) {
		return toStringDeep(args);
	}

	/**
	 * Returns single object (first item) if list/array contains single item, makes an array from
	 * {@link java.util.Collection}, or returns same value as parameter in all other cases.
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
		if (a == null) {
			return "null"; // NON-NLS
		}

		int iMax = a.length - 1;
		if (iMax == -1) {
			return "[]"; // NON-NLS
		}

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0;; i++) {
			b.append(toString(a[i]));
			if (i == iMax) {
				return b.append(']').toString();
			}
			b.append(", "); // NON-NLS
		}
	}

	/**
	 * Makes a HEX dump string representation of provided bytes array. Does all the same as
	 * {@link #toHexDump(byte[], int)} setting {@code offset} parameter to {@code 0}.
	 *
	 * @param b
	 *            bytes array make HEX dump
	 * @return returns HEX dump representation of provided bytes array
	 *
	 * @see #toHexDump(byte[], int, int)
	 */
	public static String toHexDump(byte[] b) {
		return toHexDump(b, 0);
	}

	/**
	 * Makes a HEX dump string representation of provided bytes array. Does all the same as
	 * {@link #toHexDump(byte[], int, int)} setting {@code len} parameter to {@code 0}.
	 *
	 * @param b
	 *            bytes array make HEX dump
	 * @param offset
	 *            offset at which to start dumping bytes
	 * @return returns HEX dump representation of provided bytes array
	 *
	 * @see #toHexDump(byte[], int, int)
	 */
	public static String toHexDump(byte[] b, int offset) {
		return toHexDump(b, offset, 0);
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
	public static String toHexDump(byte[] b, int offset, int len) {
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
			hexStr = NEW_LINE + bos.toString(UTF8);
			bos.close();
		} catch (Exception exc) {
			hexStr = "HEX FAIL: " + exc.getLocalizedMessage(); // NON-NLS
		}

		return hexStr;
	}

	/**
	 * Splits object identification path expression into path nodes array.
	 * <p>
	 * If empty path node is found, it is removed from path.
	 *
	 * @param path
	 *            object identification path
	 * @param nps
	 *            path nodes separator
	 * @return array of path nodes, or {@code null} if path is empty
	 */
	public static String[] getNodePath(String path, String nps) {
		if (StringUtils.isNotEmpty(path)) {
			String[] pArray = StringUtils.isEmpty(nps) ? new String[] { path } : path.split(Pattern.quote(nps));
			List<String> pList = new ArrayList<>(pArray.length);
			for (String pe : pArray) {
				if (StringUtils.isNotEmpty(pe)) {
					pList.add(pe);
				}
			}

			pArray = new String[pList.size()];
			return pList.toArray(pArray);
		}

		return null;
	}

	/**
	 * Checks if provided object is an array - of primitives or objects.
	 *
	 * @param obj
	 *            object to check
	 * @return {@code true} if obj is an array, {@code false} - otherwise
	 */
	public static boolean isArray(Object obj) {
		return obj instanceof Object[] || (obj != null && obj.getClass().isArray());
	}

	/**
	 * Sleeps current running thread and silently consumes thrown {@link InterruptedException}.
	 *
	 * @param millis
	 *            the length of time to sleep in milliseconds
	 *
	 * @see Thread#sleep(long)
	 */
	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException exc) {
		}
	}

	/**
	 * Sleeps current running thread and silently consumes thrown {@link InterruptedException}.
	 *
	 * @param millis
	 *            the length of time to sleep in milliseconds
	 * @param nanos
	 *            {@code 0-999999} additional nanoseconds to sleep
	 *
	 * @see Thread#sleep(long, int)
	 */
	public static void sleep(long millis, int nanos) {
		try {
			Thread.sleep(millis, nanos);
		} catch (InterruptedException exc) {
		}
	}

	/**
	 * Loads properties from file referenced by provided system property.
	 *
	 * @param propKey
	 *            system property key referencing properties file path
	 * @return properties loaded from file
	 * @throws java.io.IOException
	 *             if an error occurred when reading properties file
	 *
	 * @see #loadPropertiesFile(String)
	 */
	public static Properties loadPropertiesFor(String propKey) throws IOException {
		String propFile = System.getProperty(propKey);

		return loadPropertiesFile(propFile);
	}

	/**
	 * Loads properties from file.
	 *
	 * @param propFile
	 *            properties file path
	 * @return properties loaded from file
	 * @throws java.io.IOException
	 *             if an error occurred when reading properties file
	 *
	 * @see java.util.Properties#load(java.io.InputStream)
	 */
	public static Properties loadPropertiesFile(String propFile) throws IOException {
		Properties fProps = new Properties();

		try (InputStream is = new FileInputStream(new File(propFile))) {
			fProps.load(is);
		}

		return fProps;
	}

	/**
	 * Loads properties from resource with given name.
	 *
	 * @param name
	 *            the resource name
	 * @return properties loaded from resource
	 * @throws java.io.IOException
	 *             if an error occurred when reading properties file
	 *
	 * @see java.lang.ClassLoader#getResourceAsStream(String)
	 * @see java.util.Properties#load(java.io.InputStream)
	 */
	public static Properties loadPropertiesResource(String name) throws IOException {
		Properties rProps = new Properties();
		ClassLoader loader = Thread.currentThread().getContextClassLoader();

		try (InputStream ins = loader.getResourceAsStream(name)) {
			rProps.load(ins);
		}

		return rProps;
	}

	/**
	 * Loads properties from all resource with given name.
	 *
	 * @param name
	 *            the resource name
	 * @return properties loaded from all found resources
	 * @throws java.io.IOException
	 *             if an error occurred when reading properties file
	 *
	 * @see java.lang.ClassLoader#getResources(String)
	 * @see java.util.Properties#load(java.io.InputStream)
	 */
	public static Properties loadPropertiesResources(String name) throws IOException {
		Properties rProps = new Properties();

		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Enumeration<URL> rEnum = loader.getResources(name);

		while (rEnum.hasMoreElements()) {
			try (InputStream ins = rEnum.nextElement().openStream()) {
				rProps.load(ins);
			}
		}

		return rProps;
	}

	/**
	 * Creates {@link java.io.BufferedReader} instance to read provided bytes data.
	 *
	 * @param data
	 *            bytes data to read
	 * @return bytes data reader
	 */
	public static BufferedReader bytesReader(byte[] data) {
		return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
	}

	/**
	 * Creates file according to provided file descriptor - file path or file URI.
	 *
	 * @param fileDescriptor
	 *            string representing file path or URI
	 * @return file instance
	 * @throws IllegalArgumentException
	 *             if {@code fileDescriptor} is {@code null} or empty
	 * @throws java.net.URISyntaxException
	 *             if {@code fileDescriptor} defines malformed URI
	 */
	public static File createFile(String fileDescriptor) throws URISyntaxException {
		if (StringUtils.isEmpty(fileDescriptor)) {
			throw new IllegalArgumentException(
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "Utils.empty.file.descriptor"));
		}

		if (fileDescriptor.startsWith("file:/")) { // NON-NLS
			URI fUri = new URI(fileDescriptor);
			return Paths.get(fUri).toFile();
		} else {
			return Paths.get(fileDescriptor).toFile();
		}
	}

	/**
	 * Checks if provided numeric value match the mask.
	 *
	 * @param v
	 *            numeric value
	 * @param mask
	 *            mask to check
	 * @return {@code true} if value match the mask, {@code false} - otherwise
	 */
	public static boolean matchMask(int v, int mask) {
		return (v & mask) == mask;
	}

	/**
	 * Checks if provided numeric value match any bit of the mask.
	 *
	 * @param v
	 *            numeric value
	 * @param mask
	 *            mask to check
	 * @return {@code true} if value match any bit from the mask, {@code false} - otherwise
	 */
	public static boolean matchAny(int v, int mask) {
		return (v & mask) != 0;
	}

	/**
	 * Removes {@code null} elements from array.
	 *
	 * @param array
	 *            array to cleanup
	 * @param <T>
	 *            type of array elements
	 * @return new array instance without {@code null} elements
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] tidy(T[] array) {
		if (array == null) {
			return null;
		}

		List<T> oList = new ArrayList<>(array.length);
		for (T obj : array) {
			if (obj != null) {
				oList.add(obj);
			}
		}

		return (T[]) oList.toArray();
	}

	/**
	 * Removes {@code null} elements from collection.
	 *
	 * @param coll
	 *            collection to clean
	 * @param <T>
	 *            type of collection elements
	 * @return new {@link java.util.List} instance without {@code null} elements
	 */
	public static <T> List<T> tidy(Collection<T> coll) {
		if (coll == null) {
			return null;
		}

		List<T> oList = new ArrayList<>(coll.size());
		for (T obj : coll) {
			if (obj != null) {
				oList.add(obj);
			}
		}

		return oList;
	}

	/**
	 * Casts provided number value to desired number type.
	 *
	 * @param num
	 *            number value to cast
	 * @param clazz
	 *            number class to cast number to
	 * @param <T>
	 *            desired number type
	 * @return number value cast to desired numeric type
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Number> T castNumber(Number num, Class<T> clazz) {
		Number cNum = 0;

		if (clazz.isAssignableFrom(Long.class)) {
			cNum = num.longValue();
		} else if (clazz.isAssignableFrom(Integer.class)) {
			cNum = num.intValue();
		} else if (clazz.isAssignableFrom(Byte.class)) {
			cNum = num.byteValue();
		} else if (clazz.isAssignableFrom(Float.class)) {
			cNum = num.floatValue();
		} else if (clazz.isAssignableFrom(Double.class)) {
			cNum = num.doubleValue();
		} else if (clazz.isAssignableFrom(Short.class)) {
			cNum = num.shortValue();
		}

		return (T) cNum;
	}

	/**
	 * Sets system property value.
	 * <p>
	 * If property name is {@code null} - does nothing. If property value is {@code null} - removes property from system
	 * properties list.
	 *
	 * @param pName
	 *            the name of the system property
	 * @param pValue
	 *            the value of the system property
	 * @return the previous string value of the system property, or {@code null} if name is {@code null} or there was no
	 *         property with that name.
	 */
	public static String setSystemProperty(String pName, String pValue) {
		if (StringUtils.isEmpty(pName)) {
			return null;
		}

		if (pValue != null) {
			return System.setProperty(pName, pValue);
		} else {
			return System.clearProperty(pName);
		}
	}

	/**
	 * Returns enumeration entry based on entry name value ignoring case.
	 *
	 * @param enumClass
	 *            enumeration instance class
	 * @param name
	 *            name of enumeration entry
	 * @param <E>
	 *            type of enumeration
	 * @return enumeration object having provided name
	 * @throws IllegalArgumentException
	 *             if name is not a valid enumeration entry name or is {@code null}
	 */
	public static <E extends Enum<E>> E valueOfIgnoreCase(Class<E> enumClass, String name)
			throws IllegalArgumentException {
		if (StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException(
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "Utils.name.empty"));
		}

		E[] enumConstants = enumClass.getEnumConstants();

		for (E ec : enumConstants) {
			if (ec.name().equalsIgnoreCase(name)) {
				return ec;
			}
		}

		throw new IllegalArgumentException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
				"Utils.no.enum.constant", name, enumClass.getSimpleName()));
	}

	/**
	 * Returns a quoted string, surrounded with single quote.
	 *
	 * @param str
	 *            string handle
	 * @return a quoted string, surrounded with single quote
	 *
	 * @see #surround(String, String)
	 */
	public static String sQuote(String str) {
		return surround(str, "'"); // NON-NLS
	}

	/**
	 * Returns a quoted object representation string, surrounded with single quote.
	 *
	 * @param obj
	 *            object handle
	 * @return a quoted object representation string, surrounded with single quote
	 *
	 * @see #sQuote(String)
	 * @see #toString(Object)
	 */
	public static String sQuote(Object obj) {
		return sQuote(toString(obj));
	}

	/**
	 * Splits string contained values delimited using '|' delimiter into array of separate values.
	 *
	 * @param value
	 *            string contained values to split
	 * @return array of split string values
	 */
	public static String[] splitValue(String value) {
		return StringUtils.isEmpty(value) ? new String[] { value } : value.split(VALUE_DELIM);
	}

	/**
	 * Searches for files matching name pattern. Name pattern also may contain path of directory, where file search
	 * should be performed, e.g., C:/Tomcat/logs/localhost_access_log.*.txt. If no path is defined (just file name
	 * pattern) then files are searched in {@code System.getProperty("user.dir")}. Files array is ordered by file
	 * modification timestamp in ascending order.
	 *
	 * @param namePattern
	 *            name pattern to find files
	 *
	 * @return array of found files
	 *
	 * @see WildcardFileFilter#WildcardFileFilter(String)
	 * @see File#listFiles(FilenameFilter)
	 */
	public static File[] searchFiles(String namePattern) {
		File f = new File(namePattern);
		File dir = f.getAbsoluteFile().getParentFile();
		File[] activityFiles = dir.listFiles((FilenameFilter) new WildcardFileFilter(f.getName()));

		if (activityFiles != null) {
			Arrays.sort(activityFiles, new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					long f1ct = o1.lastModified();
					long f2ct = o2.lastModified();
					// NOTE: we want files to be sorted oldest->newest (ASCENDING)
					return f1ct < f2ct ? -1 : (f1ct == f2ct ? 0 : 1);
				}
			});

			return activityFiles;
		} else {
			return new File[0];
		}
	}

	/**
	 * Returns list of files matching provided file name. If file name contains wildcard symbols, then
	 * {@link #searchFiles(String)} is invoked.
	 *
	 * @param fileName
	 *            file name pattern to list matching files
	 * @return array of files matching file name
	 *
	 * @see #searchFiles(String)
	 */
	public static File[] listFilesByName(String fileName) {
		if (isWildcardString(fileName)) {
			return searchFiles(fileName);
		} else {
			return new File[] { new File(fileName) };
		}
	}

	/**
	 * Resolves map contained value by provided map keys path.
	 * <p>
	 * Path delimiter value is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM} and path
	 * level value is {@code 0}.
	 *
	 * @param path
	 *            map keys path string referencing wanted value
	 * @param dataMap
	 *            data map to get value from
	 * @return path resolved map contained value
	 *
	 * @see #getNodePath(String, String)
	 * @see #getMapValueByPath(String, Map, int)
	 */
	public static Object getMapValueByPath(String path, Map<String, ?> dataMap) {
		return getMapValueByPath(path, dataMap, 0);
	}

	/**
	 * Resolves map contained value by provided map keys path.
	 * <p>
	 * Path delimiter value is {@value com.jkoolcloud.tnt4j.streams.utils.StreamsConstants#DEFAULT_PATH_DELIM}.
	 *
	 * @param path
	 *            map keys path string referencing wanted value
	 * @param dataMap
	 *            data map to get value from
	 * @param level
	 *            path level
	 * @return path resolved map contained value
	 *
	 * @see #getNodePath(String, String)
	 * @see #getMapValueByPath(String, String, Map, int)
	 */
	public static Object getMapValueByPath(String path, Map<String, ?> dataMap, int level) {
		return getMapValueByPath(path, StreamsConstants.DEFAULT_PATH_DELIM, dataMap, level);
	}

	/**
	 * Resolves map contained value by provided map keys path.
	 * <p>
	 * Path level value is {@code 0}.
	 *
	 * @param path
	 *            map keys path string referencing wanted value
	 * @param pathDelim
	 *            path delimiter
	 * @param dataMap
	 *            data map to get value from
	 * @return path resolved map contained value
	 *
	 * @see #getNodePath(String, String)
	 * @see #getMapValueByPath(String, String, Map, int)
	 */
	public static Object getMapValueByPath(String path, String pathDelim, Map<String, ?> dataMap) {
		return getMapValueByPath(path, pathDelim, dataMap, 0);
	}

	/**
	 * Resolves map contained value by provided map keys path.
	 *
	 * @param path
	 *            map keys path string referencing wanted value
	 * @param pathDelim
	 *            path delimiter
	 * @param dataMap
	 *            data map to get value from
	 * @param level
	 *            path level
	 * @return path resolved map contained value
	 *
	 * @see #getNodePath(String, String)
	 * @see #getMapValueByPath(String[], Map, int)
	 */
	public static Object getMapValueByPath(String path, String pathDelim, Map<String, ?> dataMap, int level) {
		return getMapValueByPath(getNodePath(path, pathDelim), dataMap, level);
	}

	/**
	 * Resolves map contained value by provided map keys path.
	 * <p>
	 * Path level value is {@code 0}.
	 *
	 * @param path
	 *            map keys path tokens array referencing wanted value
	 * @param dataMap
	 *            data map to get value from
	 * @return path resolved map contained value
	 *
	 * @see #getMapValueByPath(String[], Map, int)
	 */
	public static Object getMapValueByPath(String[] path, Map<String, ?> dataMap) {
		return getMapValueByPath(path, dataMap, 0);
	}

	/**
	 * Resolves map contained value by provided map keys path.
	 *
	 * @param path
	 *            map keys path tokens array referencing wanted value
	 * @param dataMap
	 *            data map to get value from
	 * @param level
	 *            path level
	 * @return path resolved map contained value
	 */
	@SuppressWarnings("unchecked")
	public static Object getMapValueByPath(String[] path, Map<String, ?> dataMap, int level) {
		if (ArrayUtils.isEmpty(path) || dataMap == null) {
			return null;
		}

		if (StreamsConstants.MAP_NODE_TOKEN.equals(path[level])) {
			return dataMap;
		}

		Object val = dataMap.get(path[level]);

		if (level < path.length - 1 && val instanceof Map) {
			val = getMapValueByPath(path, (Map<String, ?>) val, ++level);
		} else if (level < path.length - 2 && val instanceof List) {
			try {
				int lii = Integer.parseInt(getItemIndexStr(path[level + 1]));
				val = getMapValueByPath(path, (Map<String, ?>) ((List<?>) val).get(lii), level + 2);
			} catch (NumberFormatException exc) {
			}
		}

		return val;
	}

	private static String getItemIndexStr(String indexToken) {
		return indexToken.replaceAll("\\D+", ""); // NON-NLS
	}

	/**
	 * Extracts variable name from provided variable placeholder string <tt>varPlh</tt>.
	 *
	 * @param varPlh
	 *            variable placeholder string
	 * @return variable name found within placeholder string
	 */
	public static String getVarName(String varPlh) {
		return StringUtils.isEmpty(varPlh) ? varPlh
				: varPlh.substring(VAR_EXP_START_TOKEN.length(), varPlh.length() - VAR_EXP_END_TOKEN.length());
	}

	/**
	 * Checks if <tt>array</tt> and all its elements are empty.
	 *
	 * @param array
	 *            array instance to check
	 * @return {@code true} if <tt>array</tt> is empty or all its elements are empty, {@code false} - otherwise
	 *
	 * @see #isEmpty(Object)
	 */
	public static boolean isEmptyContent(Object[] array) {
		if (ArrayUtils.isEmpty(array)) {
			return true;
		}

		for (Object ao : array) {
			if (!isEmpty(ao)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Checks if <tt>collection</tt> and all its elements are empty.
	 *
	 * @param collection
	 *            collection instance to check
	 * @return {@code true} if <tt>collection</tt> is empty or all its elements are empty, {@code false} - otherwise
	 *
	 * @see #isEmpty(Object)
	 */
	public static boolean isEmptyContent(Collection<?> collection) {
		if (CollectionUtils.isEmpty(collection)) {
			return true;
		}

		for (Object co : collection) {
			if (!isEmpty(co)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Checks if <tt>obj</tt> is empty considering when it is:
	 * <ul>
	 * <li>{@link String} - is {@code null} or equal to {@code ""}</li>
	 * <li>{@code Array} - is {@code null} or length is {@code 0}</li>
	 * <li>{@link Collection} - is {@code null} or length is {@code 0}</li>
	 * <li>{@link Object} - is {@code null}</li>
	 * </ul>
	 *
	 * @param obj
	 *            object to check
	 * @return {@code true} if <tt>obj</tt> is empty, {@code false} - otherwise
	 */
	public static boolean isEmpty(Object obj) {
		if (obj == null) {
			return true;
		}
		if (obj instanceof String) {
			return StringUtils.isEmpty((String) obj);
		}
		if (isArray(obj)) {
			return ArrayUtils.getLength(obj) > 0;
		}
		if (obj instanceof Collection) {
			return CollectionUtils.isEmpty((Collection<?>) obj);
		}

		return false;
	}

	/**
	 * Resolves {@link java.io.InputStream} <tt>is</tt> referenced file name path.
	 * <p>
	 * Supported {@link java.io.InputStream} types to resolve file path:
	 * <ul>
	 * <li>{@link java.io.FileInputStream}</li>
	 * <li>{@link org.apache.commons.io.input.ReaderInputStream}</li>
	 * <li>{@link java.io.FilterInputStream}</li>
	 * </ul>
	 *
	 * @param is
	 *            input stream instance to resolve file path
	 * @return the path of the <tt>is</tt> referenced file
	 */
	public static String resolveInputFilePath(InputStream is) {
		if (is instanceof FileInputStream) {
			return resolveInputFilePath((FileInputStream) is);
		} else if (is instanceof ReaderInputStream) {
			return resolveInputFilePath((ReaderInputStream) is);
		} else if (is instanceof FilterInputStream) {
			return resolveInputFilePath((FilterInputStream) is);
		}

		return null;
	}

	private static String resolveInputFilePath(FileInputStream fis) {
		try {
			Field pathField = FileInputStream.class.getDeclaredField("path");
			pathField.setAccessible(true);
			return (String) pathField.get(fis);
		} catch (Exception exc) {
		}

		return null;
	}

	private static String resolveInputFilePath(ReaderInputStream ris) {
		try {
			Field readerField = ReaderInputStream.class.getDeclaredField("reader");
			readerField.setAccessible(true);
			Reader reader = (Reader) readerField.get(ris);

			return resolveReaderFilePath(reader);
		} catch (Exception exc) {
		}

		return null;
	}

	private static String resolveInputFilePath(FilterInputStream fis) {
		try {
			Field inField = FilterInputStream.class.getDeclaredField("in");
			inField.setAccessible(true);
			InputStream is = (InputStream) inField.get(fis);

			return resolveInputFilePath(is);
		} catch (Exception exc) {
		}

		return null;
	}

	/**
	 * Resolves {@link java.io.Reader} <tt>rdr</tt> referenced file name path.
	 * <p>
	 * Reader referenced file path can be resolved only if reader lock object is instance of {@link java.io.InputStream}
	 * and falls under {@link #resolveInputFilePath(java.io.InputStream)} conditions.
	 *
	 * @param rdr
	 *            reader instance to resolve file path
	 * @return the path of the <tt>rdr</tt> referenced file
	 *
	 * @see #resolveInputFilePath(java.io.InputStream)
	 */
	public static String resolveReaderFilePath(Reader rdr) {
		try {
			Field lockField = Reader.class.getDeclaredField("lock");
			lockField.setAccessible(true);
			Object lock = (InputStream) lockField.get(rdr);

			if (lock instanceof InputStream) {
				return resolveInputFilePath((InputStream) lock);
			}
		} catch (Exception exc) {
		}

		return null;
	}

	/**
	 * Resolves Java object (POJO) instance field value defined by <tt>dataObj</tt> fields names <tt>path<tt> array.
	 * <p>
	 * If <tt>path</tt> level resolved value is primitive type ({@link Class#isPrimitive()}), then value resolution
	 * terminates at that level.
	 * <p>
	 * Value resolution also terminates if path element index <tt>i</tt> is {@code i >= path.length}.
	 *
	 * @param path
	 *            fields path as array of objects field names
	 * @param dataObj
	 *            Java object instance to resolve value
	 * @param i
	 *            processed locator path element index
	 * @return resolved Java object field value, or {@code null} if value is not resolved
	 * @throws java.lang.RuntimeException
	 *             if field can't be found or accessed
	 *
	 * @see #getNodePath(String, String)
	 * @see Class#isPrimitive()
	 */
	public static Object getFieldValue(String[] path, Object dataObj, int i) throws RuntimeException {
		if (ArrayUtils.isEmpty(path) || dataObj == null) {
			return null;
		}

		if (i >= path.length || dataObj.getClass().isPrimitive()) {
			return dataObj;
		}

		try {
			Field f = dataObj.getClass().getDeclaredField(path[i]);
			f.setAccessible(true);

			return getFieldValue(path, f.get(dataObj), i + 1);
		} catch (Exception exc) {
			throw new RuntimeException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"Utils.could.not.get.declared.field", path[i], dataObj.getClass().getSimpleName(),
					toString(dataObj)), exc);
		}
	}

	/**
	 * Converts provided object type <tt>value</tt> to a Boolean.
	 * <p>
	 * <tt>value</tt> will be converted to real boolean value only if string representation of it is {@code "true"} or
	 * {@code "false"} (case insensitive). In all other cases {@code null} is returned.
	 *
	 * @param value
	 *            object value to convert to a Boolean
	 * @return boolean value resolved from provided <tt>value</tt>, or {@code null} if <tt>value</tt> does not represent
	 *         boolean
	 */
	public static Boolean getBoolean(Object value) {
		if (value != null) {
			String vStr = toString(value);

			if ("true".equalsIgnoreCase(vStr)) { // NON-NLS
				return true;
			}

			if ("false".equalsIgnoreCase(vStr)) { // NON-NLS
				return false;
			}
		}

		return null;
	}
}
