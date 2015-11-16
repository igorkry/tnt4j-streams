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

package com.jkool.tnt4j.streams.inputs;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a character arrays based activity stream, where activity data is
 * read from the specified InputStream-based stream or Reader-based reader. This
 * class wraps the raw {@code InputStream} or {@code Reader} with a
 * {@code BufferedReader}.
 * </p>
 * <p>
 * This activity stream requires parsers that can support {@code InputStream}s
 * or {@code Reader}s as the source for activity data.
 * </p>
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>FileName</li>
 * <li>Port</li>
 * </ul>
 *
 * @version $Revision: 4 $
 * @see ActivityParser#isDataClassSupported(Object)
 */
public class CharacterStream extends TNTInputStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(CharacterStream.class);

	private String fileName = null;
	private Integer socketPort = null;

	private ServerSocket svrSocket = null;
	private Socket socket = null;

	/**
	 * InputStream from which activity data is read
	 */
	protected InputStream rawStream = null;

	/**
	 * Reader from which activity data is read
	 */
	protected Reader rawReader = null;

	/**
	 * BufferedReader that wraps {@link #rawStream} or {@link #rawReader}
	 */
	protected FeedReader dataReader = null;

	/**
	 * Construct empty CharacterStream. Requires configuration settings to set
	 * input stream source.
	 */
	public CharacterStream() {
		super(LOGGER);
	}

	/**
	 * Constructs CharacterStream to obtain activity data from the specified
	 * InputStream.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	public CharacterStream(InputStream stream) {
		super(LOGGER);
		this.rawStream = stream;
	}

	/**
	 * Constructs CharacterStream to obtain activity data from the specified
	 * Reader.
	 *
	 * @param reader
	 *            reader to read data from
	 */
	public CharacterStream(Reader reader) {
		super(LOGGER);
		this.rawReader = reader;
	}

	/**
	 * Sets stream from which activity data should be read.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	public void setStream(InputStream stream) {
		rawStream = stream;
	}

	/**
	 * Sets reader from which activity data should be read.
	 *
	 * @param reader
	 *            reader to read data from
	 */
	public void setReader(Reader reader) {
		rawReader = reader;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getProperty(String name) {
		if (StreamsConfig.PROP_FILENAME.equalsIgnoreCase(name)) {
			return fileName;
		}
		if (StreamsConfig.PROP_PORT.equalsIgnoreCase(name)) {
			return socketPort;
		}
		return super.getProperty(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws IllegalStateException {
		if (props == null) {
			return;
		}
		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamsConfig.PROP_FILENAME.equalsIgnoreCase(name)) {
				if (socketPort != null) {
					throw new IllegalStateException(
							"Cannot set both " + StreamsConfig.PROP_FILENAME + " and " + StreamsConfig.PROP_PORT);
				}
				fileName = value;
			} else if (StreamsConfig.PROP_PORT.equalsIgnoreCase(name)) {
				if (StringUtils.isNotEmpty(fileName)) {
					throw new IllegalStateException(
							"Cannot set both " + StreamsConfig.PROP_FILENAME + " and " + StreamsConfig.PROP_PORT);
				}
				socketPort = Integer.valueOf(value);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize() throws Throwable {
		super.initialize();
		if (rawStream == null && rawReader == null) {
			if (fileName != null) {
				rawStream = new FileInputStream(fileName);
			} else if (socketPort != null) {
				svrSocket = new ServerSocket(socketPort);
			} else {
				throw new IllegalStateException("CharacterStream: Input stream source type not specified");
			}
		}
	}

	/**
	 * Sets up the input data stream or reader to prepare it for reading.
	 *
	 * @throws IOException
	 *             if an I/O error preparing the stream
	 */
	protected void startDataStream() throws IOException {
		if (rawStream == null && rawReader == null) {
			if (svrSocket != null) {
				LOGGER.log(OpLevel.DEBUG, "Waiting for socket connection on port: {0}", socketPort);
				socket = svrSocket.accept();
				rawStream = socket.getInputStream();
				LOGGER.log(OpLevel.DEBUG, "Accepted connection, reading data from socket: {0}", socket);
				// only accept one connection, close down server socket
				Utils.close(svrSocket);
				svrSocket = null;
			}
		}
		if (rawStream == null && rawReader == null) {
			throw new IOException("raw stream or reader is not set");
		}
		dataReader = rawReader != null ? new FeedReader(rawReader) : new FeedReader(rawStream);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method does not actually return the next item, but the
	 * {@link BufferedReader} from which the next item should be read. This is
	 * useful for parsers that accept {@code Reader}s that are using underlying
	 * classes to process the data from an input stream. The parser, or its
	 * underlying data reader needs to handle all I/O, along with any associated
	 * errors.
	 * </p>
	 */
	@Override
	public Object getNextItem() throws Throwable {
		if (dataReader == null) {
			startDataStream();
		}
		if (dataReader.isClosed() || dataReader.hasError()) {
			return null;
		}
		LOGGER.log(OpLevel.TRACE, "Stream is still open");
		return dataReader;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanup() {
		Utils.close(socket);
		Utils.close(svrSocket);
		Utils.close(rawStream);
		Utils.close(rawReader);
		Utils.close(dataReader);

		socket = null;
		svrSocket = null;
		rawStream = null;
		rawReader = null;
		dataReader = null;

		super.cleanup();
	}

	/**
	 * This class extends {@link BufferedReader}, wrapping the specified
	 * {@link InputStream} (via {@link InputStreamReader}) or {@link Reader},
	 * and adding the ability to detect if the underlying object has been
	 * closed.
	 *
	 * @version $Revision: 4 $
	 * @see BufferedReader
	 * @see InputStreamReader
	 */
	private static class FeedReader extends BufferedReader {
		private boolean closed = false;
		private boolean error = false;

		/**
		 * Constructs a FeedReader, buffering the specified Reader, using an
		 * internal buffer with the given size.
		 *
		 * @param in
		 *            Reader to buffer
		 * @param size
		 *            buffer size
		 *
		 * @see BufferedReader#BufferedReader(Reader, int)
		 */
		public FeedReader(Reader in, int size) {
			super(in, size);
		}

		/**
		 * Constructs a FeedReader, buffering the specified Reader.
		 *
		 * @param in
		 *            Reader to buffer
		 *
		 * @see BufferedReader#BufferedReader(Reader)
		 */
		public FeedReader(Reader in) {
			super(in);
		}

		/**
		 * Constructs a FeedReader, buffering the specified InputStream, using
		 * an internal buffer with the given size.
		 *
		 * @param in
		 *            InputStream to buffer
		 * @param size
		 *            buffer size
		 *
		 * @see BufferedReader#BufferedReader(Reader, int)
		 */
		public FeedReader(InputStream in, int size) {
			super(new InputStreamReader(in), size);
		}

		/**
		 * Constructs a FeedReader, buffering the specified InputStream.
		 *
		 * @param in
		 *            InputStream to buffer
		 *
		 * @see BufferedReader#BufferedReader(Reader)
		 */
		public FeedReader(InputStream in) {
			super(new InputStreamReader(in));
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String readLine() throws IOException {
			try {
				return super.readLine();
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

		/**
		 * {@inheritDoc}
		 */
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
	}
}
