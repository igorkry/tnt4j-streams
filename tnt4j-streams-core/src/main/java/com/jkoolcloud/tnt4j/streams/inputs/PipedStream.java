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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.io.*;

import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * <p>
 * Implements piped activity stream, where each piped RAW data line is assumed
 * to represent a single activity or event which should be recorded. This class
 * wraps the RAW {@link InputStream} or {@link Reader} with a
 * {@link BufferedReader}. Default RAW input stream is {@link System#in}.
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 * <p>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class PipedStream extends TNTParseableInputStream<String> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(PipedStream.class);

	/**
	 * {@link Reader} from which activity data is read
	 */
	protected Reader rawReader = null;

	/**
	 * BufferedReader that wraps {@link #rawReader}
	 */
	protected LineNumberReader dataReader = null;

	private int lineNumber = 0;

	/**
	 * Constructs an empty PipedStream. Default input stream is
	 * {@link System#in}.
	 *
	 * @param logger
	 *            logger used by activity stream
	 */
	protected PipedStream(EventSink logger) {
		super(logger);
		setReader(new InputStreamReader(System.in));
	}

	/**
	 * Constructs an empty PipedStream. Default input stream is
	 * {@link System#in}.
	 */
	public PipedStream() {
		this(System.in);
	}

	/**
	 * Constructs a new PipedStream to obtain activity data from the specified
	 * {@link InputStream}.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	public PipedStream(InputStream stream) {
		this(new InputStreamReader(stream));
	}

	/**
	 * Constructs a new PipedStream to obtain activity data from the specified
	 * {@link Reader}.
	 *
	 * @param reader
	 *            reader to read data from
	 */
	public PipedStream(Reader reader) {
		super(LOGGER);
		setReader(reader);
	}

	/**
	 * Sets {@link Reader} from which activity data should be read.
	 *
	 * @param reader
	 *            reader to read data from
	 */
	public void setReader(Reader reader) {
		rawReader = reader;
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		if (rawReader == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"CharacterStream.no.stream.reader"));
		}

		dataReader = new LineNumberReader(new BufferedReader(rawReader));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns a string containing the contents of the next line in
	 * the piped RAW input.
	 */
	@Override
	public String getNextItem() throws Exception {
		if (dataReader == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"PipedStream.raw.stream.not.opened"));
		}

		String line = Utils.getNonEmptyLine(dataReader);
		lineNumber = dataReader.getLineNumber();

		if (line != null) {
			addStreamedBytesCount(line.getBytes().length);
		}

		return line;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns line number last read from pipe.
	 */
	@Override
	public int getActivityPosition() {
		return lineNumber;
	}

	@Override
	protected void cleanup() {
		// NOTE: we just pipe input and did not open System.in or other RAW
		// input source enforcing the rule that who opens it, closes it - leave
		// the closing to opener.

		// Utils.close(rawReader);
		// Utils.close(dataReader);

		rawReader = null;
		dataReader = null;

		super.cleanup();
	}
}
