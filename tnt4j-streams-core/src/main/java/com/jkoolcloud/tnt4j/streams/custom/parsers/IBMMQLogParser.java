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

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.parsers.AbstractActivityMapParser;
import com.jkoolcloud.tnt4j.streams.utils.CharBufferParser;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Implements an activity data parser that assumes each activity data item is an IBM MQ error log entry {@link String}.
 * Parser resolved log entry string fields are put into {@link Map} afterwards mapped into activity fields and
 * properties according to defined parser configuration.
 * <p>
 * This parser resolved data map may contain such entries:
 * <ul>
 * <li>Date - resolved log entry date string</li>
 * <li>Time - resolved log entry time string</li>
 * <li>Process - resolved log entry process identifier</li>
 * <li>User - resolved log entry user name</li>
 * <li>Program - resolved log entry program (application) name</li>
 * <li>Host - resolved log entry host name IBM MQ is running on</li>
 * <li>Installation - resolved log entry IBM MQ installation name</li>
 * <li>VRMF - resolved log entry running IBM MQ version descriptor</li>
 * <li>QMgr - resolved log entry Queue manager error occurred on</li>
 * <li>ErrCode - resolved log entry IBM MQ error code string</li>
 * <li>ErrText - resolved log entry IBM MQ error message text</li>
 * <li>Explanation - resolved log entry IBM MQ error explanation message text</li>
 * <li>Action - resolved log entry IBM MQ error fix action message text</li>
 * <li>Where - resolved log entry error descriptor location string containing source code file name and line number</li>
 * </ul>
 *
 * This activity parser supports properties from {@link AbstractActivityMapParser} (and higher hierarchy parsers).
 *
 * @version $Revision: 1 $
 */
public class IBMMQLogParser extends AbstractActivityMapParser {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(IBMMQLogParser.class);

	private static final String RAW_ERR_LOG_ENTRY_KEY = "RAW_ERR_LOG_ENTRY"; // NON-NLS
	private static final String ENTRIES_DELIM = "-----"; // NON-NLS

	private final IBMMQErrParser errEntryParser;

	/**
	 * Constructs a new IBMMQLogParser.
	 */
	public IBMMQLogParser() {
		errEntryParser = new IBMMQErrParser();
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

		// for (Map.Entry<String, String> prop : props) {
		// String name = prop.getKey();
		// String value = prop.getValue();
		//
		// // no any additional properties are required yet.
		// if (false) {
		// logger().log(OpLevel.DEBUG,
		// StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
		// name, value);
		// }
		// }
	}

	@Override
	public boolean canHaveDelimitedLocators() {
		return false;
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link java.lang.String}</li>
	 * <li>{@code byte[]}</li>
	 * <li>{@link java.nio.ByteBuffer}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.InputStream}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return String.class.isInstance(data) || byte[].class.isInstance(data) || ByteBuffer.class.isInstance(data)
				|| Reader.class.isInstance(data) || InputStream.class.isInstance(data);
	}

	@Override
	protected Map<String, ?> getDataMap(Object data) {
		if (data == null) {
			return null;
		}

		String logEntry = getNextActivityString(data);
		if (StringUtils.isEmpty(logEntry)) {
			return null;
		}
		Map<String, Object> dataMap = new HashMap<>();
		dataMap.put(RAW_ERR_LOG_ENTRY_KEY, logEntry);

		try {
			synchronized (errEntryParser) {
				dataMap.putAll(errEntryParser.parse(logEntry));
			}
		} catch (Exception exc) {
			logger().log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"IBMMQLogParser.log.entry.parse.failed"), exc);
		}

		return dataMap;
	}

	/**
	 * Retrieves RAW IBM MQ error log entry string to be put to activity field
	 * {@link com.jkoolcloud.tnt4j.streams.fields.StreamFieldType#Message}. This is used when no field
	 * {@link com.jkoolcloud.tnt4j.streams.fields.StreamFieldType#Message} mapping defined in parser configuration.
	 * 
	 * @param dataMap
	 *            IBM MQ error log entry fields map
	 * @return RAW IBM MQ error log entry string retrieved from map entry {@code "RAW_ERR_LOG_ENTRY"}
	 */
	@Override
	protected String getRawDataAsMessage(Map<String, ?> dataMap) {
		return dataMap == null ? null : (String) dataMap.get(RAW_ERR_LOG_ENTRY_KEY);
	}

	/**
	 * Reads RAW IBM MQ error log entry string from {@link BufferedReader}. If the data input source contains multiple
	 * error log entries, then each document must start with {@code "-----"}, and be separated by a new line.
	 * 
	 * @param rdr
	 *            reader to use for reading
	 * @return non empty RAW IBM MQ error log entry string, or {@code null} if the end of the stream has been reached
	 */
	@Override
	protected String readNextActivity(BufferedReader rdr) {
		String entryString = null;
		StringBuilder entryBuffer = new StringBuilder(1024);

		synchronized (NEXT_LOCK) {
			try {
				for (String line; entryString == null && (line = rdr.readLine()) != null;) {
					entryBuffer.append(line);
					if (line.startsWith(ENTRIES_DELIM)) {
						if (entryBuffer.length() > 0) {
							entryString = entryBuffer.toString();
							entryBuffer.setLength(0);
						}
					}
				}
			} catch (EOFException eof) {
				logger().log(OpLevel.DEBUG,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.data.end"),
						getActivityDataType(), eof);
			} catch (IOException ioe) {
				logger().log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityParser.error.reading"), getActivityDataType(), ioe);
			}
		}

		if (entryString == null && entryBuffer.length() > 0) {
			entryString = entryBuffer.toString();
		}

		return entryString;
	}

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - IBM MQ ERR LOG
	 */
	@Override
	protected String getActivityDataType() {
		return "IBM MQ ERR LOG"; // NON-NLS
	}

	/**
	 * IBM MQ error log entries parser.
	 */
	private static class IBMMQErrParser extends CharBufferParser<String, Map<String, ?>> {
		private static final char CB = ')';
		private static final char COLON = ':';
		private static final char MINUS = '-';

		/**
		 * Construct a new IBMMQErrParser.
		 */
		public IBMMQErrParser() {
			super();
		}

		/**
		 * Parse IBM MQ error log entry string making map of parsed fields.
		 * 
		 * @param logEntry
		 *            IBM MQ error log entry
		 * @return a map, or {@code null} if line is empty.
		 * @throws Exception
		 *             if the underlying stream fails, or unexpected chars occurs.
		 */
		@Override
		public Map<String, Object> parse(String logEntry) throws Exception {
			CharBuffer cb = stringToBuffer(logEntry);

			String date = readWord(cb, 9);
			skipSpaces(cb);
			String time = readWord(cb, 8);
			readChars(cb, 3);
			String process = readBetween(cb, "Process(", CB); // NON-NLS
			String user = readBetween(cb, "User(", CB); // NON-NLS
			String program = readBetween(cb, "Program(", CB); // NON-NLS
			String host = readBetween(cb, "Host(", CB); // NON-NLS
			String install = readBetween(cb, "Installation(", CB); // NON-NLS
			String vrmf = readBetween(cb, "VRMF(", CB); // NON-NLS
			String qMgr = readBetween(cb, "QMgr(", CB); // NON-NLS

			String errCode = readUntil(cb, COLON);
			skipSpaces(cb);
			String errText = readUntil(cb, "EXPLANATION:"); // NON-NLS
			skipSpaces(cb);
			String explanation = readUntil(cb, "ACTION:"); // NON-NLS
			skipSpaces(cb);
			String action = readUntil(cb, ENTRIES_DELIM);

			String where = null;
			int c = peek(cb);
			if (c == SPACE) {
				where = readUntil(cb, MINUS);
			}

			return createFieldMap(date, time, process, user, program, host, install, vrmf, qMgr, errCode, errText,
					explanation, action, where);
		}

		private static Map<String, Object> createFieldMap(String date, String time, String process, String user,
				String program, String host, String install, String vrmf, String qMgr, String errCode, String errText,
				String exp, String action, String where) {
			Map<String, Object> map = new HashMap<>(14);

			map.put("Date", date); // NON-NLS
			map.put("Time", time); // NON-NLS
			map.put("Process", process); // NON-NLS
			map.put("User", user); // NON-NLS
			map.put("Program", program); // NON-NLS
			map.put("Host", host); // NON-NLS
			map.put("Installation", install); // NON-NLS
			map.put("VRMF", vrmf); // NON-NLS
			map.put("QMgr", qMgr); // NON-NLS
			map.put("ErrCode", errCode); // NON-NLS
			map.put("ErrText", StringUtils.trim(errText)); // NON-NLS
			map.put("Explanation", StringUtils.trim(exp)); // NON-NLS
			map.put("Action", StringUtils.trim(action)); // NON-NLS
			map.put("Where", StringUtils.trim(where)); // NON-NLS

			return map;
		}

		private static String readBetween(CharBuffer cb, String pre, char term) throws IOException {
			expect(cb, pre);
			String str = readUntil(cb, term);
			skipSpaces(cb);

			return str;
		}

		private static String readUntil(CharBuffer cb, char tc) {
			StringBuilder sb = new StringBuilder(16);
			int c;

			while ((c = read(cb)) != tc && c != -1) {
				sb.append((char) c);
			}

			return sb.toString();
		}
	}
}
