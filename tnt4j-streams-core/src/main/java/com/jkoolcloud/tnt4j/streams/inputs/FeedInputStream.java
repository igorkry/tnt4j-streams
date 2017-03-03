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
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.inputs.feeds.Feed;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityParser;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Base class for feeds based activity stream, where activity data is read from the specified raw input source (i.e.
 * {@link InputStream} or {@link Reader}). This class wraps the raw input source with a {@link Feed}. RAW input source
 * also can be {@link File} descriptor or {@link ServerSocket} accepted {@link Socket} connection.
 * <p>
 * In case input source is {@link ServerSocket} connection, stream allows only one {@link Socket} connection at a time.
 * After accepting connection from {@link ServerSocket}, server socket gets closed and no more connections can be
 * accepted. But there is stream property 'RestartOnInputClose' allowing to restart (reset) stream and open new server
 * socket instance if already streamed connection socket gets closed.
 * <p>
 * This activity stream requires parsers that can support type of activity data feed input as the source for activity
 * data.
 *
 * NOTE: there can be only one parser referenced with this kind of stream! Because next item returned by this stream is
 * raw input source and parseable value is retrieved inside parser, there is no way to rewind reader position if first
 * parser fails to parse RAW activity data.
 * <p>
 * This activity stream supports the following properties (in addition to those supported by
 * {@link TNTParseableInputStream}):
 * <ul>
 * <li>FileName - the system-dependent file name. (Required - just one 'FileName' or 'Port')</li>
 * <li>Port - port number to accept character stream over TCP/IP. (Required - just one 'FileName' or 'Port')</li>
 * <li>RestartOnInputClose - flag indicating to restart stream if input socked gets closed. (Optional)</li>
 * </ul>
 *
 * @param <R>
 *            type of raw input source
 * @param <T>
 *            type of activity data feed input
 *
 * @version $Revision: 1 $
 *
 * @see BytesInputStream
 * @see CharacterStream
 * @see ActivityParser#isDataClassSupported(Object)
 */
public abstract class FeedInputStream<R extends Closeable, T> extends TNTParseableInputStream<T> {
	private String fileName = null;
	private Integer socketPort = null;

	private ServerSocket svrSocket = null;
	private Socket socket = null;

	/**
	 * RAW input source (i.e. {@link java.io.Reader} or {@link InputStream}) from which activity data is read.
	 */
	private R rawInputSource = null;

	/**
	 * Activity data feed that wraps {@link #rawInputSource}
	 */
	private Feed<T> dataFeed = null;

	/**
	 * Indicates whether stream should restart listening for incoming data if RAW data socket gets closed (set by
	 * {@code RestartOnInputClose} property) - default: {@code false}
	 */
	private boolean restartOnInputClose = false;

	/**
	 * Constructs an empty FeedInputStream. Requires configuration settings to set input stream source.
	 */
	public FeedInputStream() {
		super();
	}

	/**
	 * Constructs a new FeedInputStream to obtain activity data from the specified raw input source.
	 *
	 * @param source
	 *            raw input source to read data from
	 */
	public FeedInputStream(R source) {
		this();
		setInputSource(source);
	}

	/**
	 * Sets {@link InputStream} from which activity data should be read.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	public abstract void setInputStream(InputStream stream);

	/**
	 * Sets raw input source from which activity data should be read.
	 *
	 * @param source
	 *            input source to read data from
	 */
	public void setInputSource(R source) {
		this.rawInputSource = source;
	}

	/**
	 * Creates activity data feed instance from provided raw input source
	 *
	 * @param feedSource
	 *            raw input source object
	 * @return instance of activity data feed
	 */
	protected abstract Feed<T> createFeedInput(R feedSource);

	@Override
	public void addParser(ActivityParser parser) throws IllegalStateException {
		if (!parsersMap.isEmpty()) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"FeedInputStream.cannot.have.multiple.parsers"));
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
							StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.cannot.set.both",
							StreamProperties.PROP_FILENAME, StreamProperties.PROP_PORT));
				}
				fileName = value;
			} else if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
				if (StringUtils.isNotEmpty(fileName)) {
					throw new IllegalStateException(StreamsResources.getStringFormatted(
							StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.cannot.set.both",
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
		if (rawInputSource == null) {
			if (StringUtils.isEmpty(fileName) && socketPort == null) {
				throw new IllegalStateException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.property.undefined.one.of",
						StreamProperties.PROP_FILENAME, StreamProperties.PROP_PORT));
			}

			if (fileName != null) {
				setInputStream(new FileInputStream(fileName));
			} else if (socketPort != null) {
				svrSocket = new ServerSocket(socketPort);
			} else {
				throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"FeedInputStream.no.stream.source"));
			}
		}
	}

	/**
	 * Sets up the input data stream to prepare it for reading feeds.
	 *
	 * @throws IOException
	 *             if an I/O error preparing the stream
	 */
	protected void startDataStream() throws IOException {
		if (rawInputSource == null) {
			if (svrSocket != null) {
				logger().log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"FeedInputStream.waiting.for.connection"), socketPort);
				socket = svrSocket.accept();
				setInputStream(socket.getInputStream());
				logger().log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"FeedInputStream.accepted.connection"), socket);
				// only accept one connection, close down server socket
				Utils.close(svrSocket);
				svrSocket = null;
			}
		}
		if (rawInputSource == null) {
			throw new IOException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"FeedInputStream.no.stream.reader"));
		}
		dataFeed = createFeedInput(rawInputSource);
		dataFeed.addFeedListener(new StreamFeedsListener());
	}

	/**
	 * Closes opened data feed.
	 */
	@Override
	protected void stopInternals() {
		Utils.close(dataFeed);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method does not actually return the next item, but the activity data feed from which the next item should be
	 * read. This is useful for parsers that accept {@link InputStream}s or {@link java.io.Reader}s that are using
	 * underlying classes to process the data from an input stream. The parser needs to handle all I/O, along with any
	 * associated errors.
	 */
	@Override
	public T getNextItem() throws Exception {
		while (true) {
			if (dataFeed == null) {
				startDataStream();
			}
			if (dataFeed.isClosed() || dataFeed.hasError()) {
				logger().log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"FeedInputStream.reader.terminated"));
				if (restartOnInputClose && socketPort != null) {
					resetDataStream();
					continue;
				} else {
					return null;
				}
			}
			logger().log(OpLevel.TRACE, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"FeedInputStream.stream.still.open"));
			return dataFeed.getInput();
		}
	}

	private void resetDataStream() throws Exception {
		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "FeedInputStream.resetting.stream"),
				socket);

		cleanupStreamInternals();

		initializeStreamInternals();

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "FeedInputStream.stream.reset"),
				socketPort);
	}

	@Override
	protected void cleanup() {
		cleanupStreamInternals();

		super.cleanup();
	}

	private void cleanupStreamInternals() {
		Utils.close(rawInputSource);
		Utils.close(dataFeed);
		Utils.close(socket);
		Utils.close(svrSocket);

		rawInputSource = null;
		dataFeed = null;
		socket = null;
		svrSocket = null;
	}

	private class StreamFeedsListener implements Feed.FeedListener {
		@Override
		public void bytesReadFromInput(int bCount) {
			addStreamedBytesCount(bCount);
		}
	}
}
