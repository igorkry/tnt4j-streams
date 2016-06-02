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

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;

/**
 * This class extends {@link BufferedReader}, wrapping the specified
 * {@link Reader} or {@link InputStream} (via {@link InputStreamReader}), and
 * adding the ability to detect if the underlying object has been closed or
 * reading input was erroneous.
 *
 * @version $Revision: 1 $
 *
 * @see BufferedReader
 * @see InputStreamReader
 */
public abstract class FeedReader extends BufferedReader {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(FeedReader.class);

	private boolean closed = false;
	private boolean error = false;

	/**
	 * Constructs a new FeedReader, buffering the specified Reader, using an
	 * internal buffer with the given size.
	 *
	 * @param in
	 *            Reader to buffer
	 * @param size
	 *            buffer size
	 * @see BufferedReader#BufferedReader(Reader, int)
	 */
	public FeedReader(Reader in, int size) {
		super(in, size);
	}

	/**
	 * Constructs a new FeedReader, buffering the specified Reader.
	 *
	 * @param in
	 *            Reader to buffer
	 * @see BufferedReader#BufferedReader(Reader)
	 */
	public FeedReader(Reader in) {
		super(in);
	}

	/**
	 * Constructs a new FeedReader, buffering the specified {@link InputStream},
	 * using an internal buffer with the given size.
	 *
	 * @param in
	 *            InputStream to buffer
	 * @param size
	 *            buffer size
	 * @see BufferedReader#BufferedReader(Reader, int)
	 */
	public FeedReader(InputStream in, int size) {
		this(new InputStreamReader(in), size);
	}

	/**
	 * Constructs a new FeedReader, buffering the specified {@link InputStream}.
	 *
	 * @param in
	 *            InputStream to buffer
	 * @see BufferedReader#BufferedReader(Reader)
	 */
	public FeedReader(InputStream in) {
		this(new InputStreamReader(in));
	}

	@Override
	public String readLine() throws IOException {
		try {
			String line = super.readLine();

			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "FeedReader.read.line"), line);

			if (line == null) {
				close();
			} else {
				notifyBytesRead(line.getBytes().length);
			}

			return line;
		} catch (EOFException exc) {
			throw exc;
		} catch (IOException ioe) {
			error = true;
			throw ioe;
		}
	}

	@Override
	public int read(char cbuf[], int off, int len) throws IOException {
		try {
			int total = super.read(cbuf, off, len);

			String line = total == -1 ? "EOF" : new String(cbuf, off, total); // NON-NLS

			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "FeedReader.read.line"), line);

			if (total == -1) {
				close();
			} else {
				notifyBytesRead(total);
			}

			return total;
		} catch (EOFException exc) {
			throw exc;
		} catch (IOException ioe) {
			error = true;
			throw ioe;
		}
	}

	/**
	 * Returns whether or not an error occurred on the stream.
	 *
	 * @return {@code true} if error occurred, {@code false} if not
	 */
	public boolean hasError() {
		return error;
	}

	@Override
	public void close() throws IOException {
		closed = true;
		super.close();
	}

	/**
	 * Returns whether or not the stream has been closed.
	 *
	 * @return {@code true} if stream is closed, {@code false} if still open
	 */
	public boolean isClosed() {
		return closed;
	}

	/**
	 * Makes notification on amount of bytes successfully read from input.
	 *
	 * @param bCount
	 *            read bytes count
	 */
	protected abstract void notifyBytesRead(int bCount);
}
