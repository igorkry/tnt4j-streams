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

package com.jkool.tnt4j.streams.inputs;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
import com.jkool.tnt4j.streams.utils.StreamsResources;
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
 * <p>
 * This activity stream requires parsers that can support {@code InputStream}s
 * or {@code Reader}s as the source for activity data.
 *
 * NOTE: there can be only one parser referenced with this kind of stream!
 * Because next item returned by this stream is {@code BufferedReader} and
 * parseable value is retrieved inside parser there is no way to rewind reader
 * position if first parser fails to parse RAW activity data.
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>FileName - concrete file name. (Required - just one 'FileName' or 'Port')
 * </li>
 * <li>Port - port number to accept character stream over TCP/IP. (Required -
 * just one 'FileName' or 'Port')</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class CharacterStream extends TNTInputStream<BufferedReader> {
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
	 * 
	 * @param logger
	 *            logger used by activity stream
	 */
	protected CharacterStream(EventSink logger) {
		super(logger);
	}

	/**
	 * Construct empty CharacterStream. Requires configuration settings to set
	 * input stream source.
	 */
	public CharacterStream() {
		super(LOGGER);
	}

	/**
	 * Constructs a new CharacterStream to obtain activity data from the
	 * specified InputStream.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	public CharacterStream(InputStream stream) {
		super(LOGGER);
		this.rawStream = stream;
	}

	/**
	 * Constructs a new CharacterStream to obtain activity data from the
	 * specified Reader.
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
	public void addParser(ActivityParser parser) throws IllegalStateException {
		if (!parsersMap.isEmpty()) {
			StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"CharacterStream.cannot.have.multiple.parsers");
		}

		super.addParser(parser);
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
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamsConfig.PROP_FILENAME.equalsIgnoreCase(name)) {
				if (socketPort != null) {
					throw new IllegalStateException(StreamsResources.getStringFormatted(
							StreamsResources.RESOURCE_BUNDLE_CORE, "CharacterStream.cannot.set.both",
							StreamsConfig.PROP_FILENAME, StreamsConfig.PROP_PORT));
				}
				fileName = value;
			} else if (StreamsConfig.PROP_PORT.equalsIgnoreCase(name)) {
				if (StringUtils.isNotEmpty(fileName)) {
					throw new IllegalStateException(StreamsResources.getStringFormatted(
							StreamsResources.RESOURCE_BUNDLE_CORE, "CharacterStream.cannot.set.both",
							StreamsConfig.PROP_FILENAME, StreamsConfig.PROP_PORT));
				}
				socketPort = Integer.valueOf(value);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize() throws Exception {
		super.initialize();

		if (rawStream == null && rawReader == null) {
			if (StringUtils.isEmpty(fileName) && socketPort == null) {
				throw new IllegalStateException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_CORE, "TNTInputStream.property.undefined.one.of",
						StreamsConfig.PROP_FILENAME, StreamsConfig.PROP_PORT));
			}

			if (fileName != null) {
				rawStream = new FileInputStream(fileName);
			} else if (socketPort != null) {
				svrSocket = new ServerSocket(socketPort);
			} else {
				throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
						"CharacterStream.no.stream.source"));
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
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
						"CharacterStream.waiting.for.connection", socketPort));
				socket = svrSocket.accept();
				rawStream = socket.getInputStream();
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
						"CharacterStream.accepted.connection", socket));
				// only accept one connection, close down server socket
				Utils.close(svrSocket);
				svrSocket = null;
			}
		}
		if (rawStream == null && rawReader == null) {
			throw new IOException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"CharacterStream.no.stream.reader"));
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
	 */
	@Override
	public BufferedReader getNextItem() throws Exception {
		if (dataReader == null) {
			startDataStream();
		}
		if (dataReader.isClosed() || dataReader.hasError()) {
			return null;
		}
		LOGGER.log(OpLevel.TRACE,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "CharacterStream.stream.still.open"));
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
	 * @version $Revision: 1 $
	 *
	 * @see BufferedReader
	 * @see InputStreamReader
	 */
	private static class FeedReader extends BufferedReader {
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
		 *
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
		 *
		 * @see BufferedReader#BufferedReader(Reader)
		 */
		public FeedReader(Reader in) {
			super(in);
		}

		/**
		 * Constructs a new FeedReader, buffering the specified InputStream,
		 * using an internal buffer with the given size.
		 *
		 * @param in
		 *            InputStream to buffer
		 * @param size
		 *            buffer size
		 *
		 * @see BufferedReader#BufferedReader(Reader, int)
		 */
		public FeedReader(InputStream in, int size) {
			this(new InputStreamReader(in), size);
		}

		/**
		 * Constructs a new FeedReader, buffering the specified InputStream.
		 *
		 * @param in
		 *            InputStream to buffer
		 *
		 * @see BufferedReader#BufferedReader(Reader)
		 */
		public FeedReader(InputStream in) {
			this(new InputStreamReader(in));
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String readLine() throws IOException {
			try {
				String line = super.readLine();

				LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
						"CharacterStream.read.line", line));

				if (line == null) {
					close();
				}

				return line;
			} catch (EOFException exc) {
				throw exc;
			} catch (IOException ioe) {
				error = true;
				throw ioe;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int read(char cbuf[], int off, int len) throws IOException {
			try {
				int total = super.read(cbuf, off, len);

				String line = total == -1 ? "EOF" : new String(cbuf, off, total); // NON-NLS

				LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
						"CharacterStream.read.line", line));

				if (total == -1) {
					close();
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
