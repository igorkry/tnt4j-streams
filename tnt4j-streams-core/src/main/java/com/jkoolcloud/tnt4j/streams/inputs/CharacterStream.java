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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.FeedReader;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements a character arrays based activity stream, where activity data is read from the specified InputStream-based
 * stream or Reader-based reader. This class wraps the raw {@link InputStream} or {@link Reader} with a
 * {@link BufferedReader}. Input source also can be {@link File} descriptor or {@link ServerSocket} accepted
 * {@link Socket} connection.
 * <p>
 * In case input source is {@link ServerSocket} connection, stream allows only one {@link Socket} connection at a time.
 * After accepting connection from {@link ServerSocket}, server socket gets closed and no more connections can be
 * accepted. But there is stream property 'RestartOnInputClose' allowing to restart (reset) stream and open new server
 * socket instance if already streamed connection socket gets closed.
 * <p>
 * This activity stream requires parsers that can support {@link Reader}s as the source for activity data.
 *
 * NOTE: there can be only one parser referenced with this kind of stream! Because next item returned by this stream is
 * {@link BufferedReader} and parseable value is retrieved inside parser there is no way to rewind reader position if
 * first parser fails to parse RAW activity data.
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>FileName - the system-dependent file name. (Required - just one 'FileName' or 'Port')</li>
 * <li>Port - port number to accept character stream over TCP/IP. (Required - just one 'FileName' or 'Port')</li>
 * <li>RestartOnInputClose - flag indicating to restart stream if input socked gets closed. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see ActivityParser#isDataClassSupported(Object)
 */
public class CharacterStream extends TNTParseableInputStream<BufferedReader> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(CharacterStream.class);

	private String fileName = null;
	private Integer socketPort = null;

	private ServerSocket svrSocket = null;
	private Socket socket = null;

	/**
	 * {@link Reader} from which activity data is read
	 */
	protected Reader rawReader = null;

	/**
	 * BufferedReader that wraps {@link #rawReader}
	 */
	protected FeedReader dataReader = null;

	/**
	 * Indicates whether stream should restart listening for incoming data if RAW data socket gets closed (set by
	 * {@code RestartOnInputClose} property) - default: {@code false}
	 */
	protected boolean restartOnInputClose = false;

	/**
	 * Constructs an empty CharacterStream. Requires configuration settings to set input stream source.
	 *
	 * @param logger
	 *            logger used by activity stream
	 */
	protected CharacterStream(EventSink logger) {
		super(logger);
	}

	/**
	 * Constructs an empty CharacterStream. Requires configuration settings to set input stream source.
	 */
	public CharacterStream() {
		super(LOGGER);
	}

	/**
	 * Constructs a new CharacterStream to obtain activity data from the specified {@link InputStream}.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	public CharacterStream(InputStream stream) {
		super(LOGGER);
		setStream(stream);
	}

	/**
	 * Constructs a new CharacterStream to obtain activity data from the specified {@link Reader}.
	 *
	 * @param reader
	 *            reader to read data from
	 */
	public CharacterStream(Reader reader) {
		super(LOGGER);
		setReader(reader);
	}

	/**
	 * Sets {@link InputStream} from which activity data should be read.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	public void setStream(InputStream stream) {
		setReader(new InputStreamReader(stream));
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
	public void addParser(ActivityParser parser) throws IllegalStateException {
		if (!parsersMap.isEmpty()) {
			StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"CharacterStream.cannot.have.multiple.parsers");
		}

		super.addParser(parser);
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			return fileName;
		}
		if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			return socketPort;
		}
		if (StreamProperties.PROP_RESTART_ON_CLOSE.equalsIgnoreCase(name)) {
			return restartOnInputClose;
		}
		return super.getProperty(name);
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
				if (socketPort != null) {
					throw new IllegalStateException(StreamsResources.getStringFormatted(
							StreamsResources.RESOURCE_BUNDLE_NAME, "CharacterStream.cannot.set.both",
							StreamProperties.PROP_FILENAME, StreamProperties.PROP_PORT));
				}
				fileName = value;
			} else if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
				if (StringUtils.isNotEmpty(fileName)) {
					throw new IllegalStateException(StreamsResources.getStringFormatted(
							StreamsResources.RESOURCE_BUNDLE_NAME, "CharacterStream.cannot.set.both",
							StreamProperties.PROP_FILENAME, StreamProperties.PROP_PORT));
				}
				socketPort = Integer.valueOf(value);
			} else if (StreamProperties.PROP_RESTART_ON_CLOSE.equalsIgnoreCase(name)) {
				restartOnInputClose = Boolean.parseBoolean(value);
			}
		}
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		initializeStreamInternals();
	}

	private void initializeStreamInternals() throws Exception {
		if (rawReader == null) {
			if (StringUtils.isEmpty(fileName) && socketPort == null) {
				throw new IllegalStateException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.property.undefined.one.of",
						StreamProperties.PROP_FILENAME, StreamProperties.PROP_PORT));
			}

			if (fileName != null) {
				setStream(new FileInputStream(fileName));
			} else if (socketPort != null) {
				svrSocket = new ServerSocket(socketPort);
			} else {
				throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
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
		if (rawReader == null) {
			if (svrSocket != null) {
				LOGGER.log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"CharacterStream.waiting.for.connection"), socketPort);
				socket = svrSocket.accept();
				setStream(socket.getInputStream());
				LOGGER.log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"CharacterStream.accepted.connection"), socket);
				// only accept one connection, close down server socket
				Utils.close(svrSocket);
				svrSocket = null;
			}
		}
		if (rawReader == null) {
			throw new IOException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"CharacterStream.no.stream.reader"));
		}
		dataReader = new CharFeedReader(rawReader);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method does not actually return the next item, but the {@link BufferedReader} from which the next item
	 * should be read. This is useful for parsers that accept {@link Reader}s that are using underlying classes to
	 * process the data from an input stream. The parser, or its underlying data reader needs to handle all I/O, along
	 * with any associated errors.
	 */
	@Override
	public BufferedReader getNextItem() throws Exception {
		if (dataReader == null) {
			startDataStream();
		}
		if (dataReader.isClosed() || dataReader.hasError()) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"CharacterStream.reader.terminated"));
			if (restartOnInputClose && socketPort != null) {
				resetDataStream();
				return getNextItem();
			} else {
				return null;
			}
		}
		LOGGER.log(OpLevel.TRACE,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "CharacterStream.stream.still.open"));
		return dataReader;
	}

	private void resetDataStream() throws Exception {
		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "CharacterStream.resetting.stream"),
				socket);

		cleanupStreamInternals();

		initializeStreamInternals();

		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "CharacterStream.stream.reset"),
				socketPort);
	}

	@Override
	protected void cleanup() {
		cleanupStreamInternals();

		super.cleanup();
	}

	private void cleanupStreamInternals() {
		Utils.close(rawReader);
		Utils.close(dataReader);
		Utils.close(socket);
		Utils.close(svrSocket);

		rawReader = null;
		dataReader = null;
		socket = null;
		svrSocket = null;
	}

	private class CharFeedReader extends FeedReader {
		CharFeedReader(Reader in) {
			super(in);
		}

		@Override
		protected void notifyBytesRead(int bCount) {
			addStreamedBytesCount(bCount);
		}
	}

}
