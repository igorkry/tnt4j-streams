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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.outputs.JKCloudJsonOutput;
import com.jkoolcloud.tnt4j.streams.utils.*;

/**
 * Implements a redirecting activity stream, where activity data is prepared by other TNT4J based streaming libraries
 * (i.e. tnt4j-stream-jmx, tnt4j-stream-gc) using {@link com.jkoolcloud.tnt4j.format.JSONFormatter} to format activity
 * data. Redirected activities JSON data ban be read from the specified InputStream-based stream or Reader-based reader.
 * This class wraps the raw {@link InputStream} or {@link Reader} with a {@link BufferedReader}. Input source also can
 * be {@link File} descriptor or {@link ServerSocket} connections.
 * <p>
 * In case input source is {@link ServerSocket} connections, there is stream property 'RestartOnInputClose' allowing to
 * restart {@link ServerSocket} (open new {@link ServerSocket} instance) if listened {@link ServerSocket} gets closed or
 * fails to accept connections.
 * <p>
 * This activity stream requires parsers that can support {@link String} activity data.
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>FileName - the system-dependent file name. (Required - just one 'FileName' or 'Port')</li>
 * <li>Port - port number to accept character stream over TCP/IP. (Required - just one 'FileName' or 'Port')</li>
 * <li>RestartOnInputClose - flag indicating to restart {@link ServerSocket} (open new {@link ServerSocket} instance) if
 * listened server socked gets closed or fails to accept connection. (Optional)</li>
 * <li>BufferSize - maximal buffer queue capacity. Default value - 512. (Optional)</li>
 * <li>BufferOfferTimeout - how long to wait if necessary for space to become available when adding data item to buffer
 * queue. Default value - 45sec. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see ArrayBlockingQueue
 * @see BlockingQueue#offer(Object, long, TimeUnit)
 * @see RedirectTNT4JStreamFormatter
 */
public class RedirectTNT4JStream extends TNTInputStream<String, String> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(RedirectTNT4JStream.class);

	private static final int DEFAULT_INPUT_BUFFER_SIZE = 512;
	private static final int DEFAULT_INPUT_BUFFER_OFFER_TIMEOUT = 3 * 15; // NOTE:
																			// sec.
	private static final Object DIE_MARKER = new Object();

	private int bufferSize = DEFAULT_INPUT_BUFFER_SIZE;
	private int bufferOfferTimeout = DEFAULT_INPUT_BUFFER_OFFER_TIMEOUT;
	private boolean restartOnInputClose = false;

	private Reader rawReader;
	private String fileName = null;
	private Integer socketPort = null;

	private FeedersProducer feedsProducer;

	protected BlockingQueue<Object> inputBuffer;

	/**
	 * Constructs an empty RedirectTNT4JStream. Requires configuration settings to set input stream source.
	 *
	 * @param logger
	 *            logger used by activity stream
	 */
	protected RedirectTNT4JStream(EventSink logger) {
		super(logger);
	}

	/**
	 * Constructs an empty RedirectTNT4JStream. Requires configuration settings to set input stream source.
	 */
	public RedirectTNT4JStream() {
		super(LOGGER);
	}

	/**
	 * Constructs a new RedirectTNT4JStream to obtain activity data from the specified {@link InputStream}.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	public RedirectTNT4JStream(InputStream stream) {
		super(LOGGER);
		setStream(stream);
	}

	/**
	 * Constructs a new RedirectTNT4JStream to obtain activity data from the specified {@link Reader}.
	 *
	 * @param reader
	 *            reader to read data from
	 */
	public RedirectTNT4JStream(Reader reader) {
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
	public void setDefaultStreamOutput() {
		setOutput(new JKCloudJsonOutput());
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
			} else if (StreamProperties.PROP_BUFFER_SIZE.equalsIgnoreCase(name)) {
				bufferSize = Integer.parseInt(value);
			} else if (StreamProperties.PROP_OFFER_TIMEOUT.equalsIgnoreCase(name)) {
				bufferOfferTimeout = Integer.parseInt(value);
			}
		}
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
		if (StreamProperties.PROP_BUFFER_SIZE.equalsIgnoreCase(name)) {
			return bufferSize;
		}
		if (StreamProperties.PROP_OFFER_TIMEOUT.equalsIgnoreCase(name)) {
			return bufferOfferTimeout;
		}

		return super.getProperty(name);
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		inputBuffer = new ArrayBlockingQueue<Object>(bufferSize, true);

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
				feedsProducer = new ServerSocketFeedsProducer(socketPort);
			} else {
				throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"CharacterStream.no.stream.source"));
			}
		}

		startDataStream();
	}

	/**
	 * Sets up the input data stream or reader to prepare it for reading.
	 *
	 * @throws IOException
	 *             if an I/O error preparing the stream
	 */
	protected void startDataStream() throws IOException {
		if (rawReader == null && feedsProducer == null) {
			throw new IOException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"CharacterStream.no.stream.reader"));
		}

		if (rawReader != null) {
			feedsProducer = new RawReaderFeedsProducer(rawReader);
		}

		feedsProducer.start();
	}

	private boolean addInputToBuffer(String inputData) {
		if (inputData != null && !isHalted()) {
			try {
				boolean added = inputBuffer.offer(inputData, bufferOfferTimeout, TimeUnit.SECONDS);

				if (!added) {
					LOGGER.log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"AbstractBufferedStream.changes.buffer.limit"), bufferOfferTimeout, inputData);
				}

				return added;
			} catch (InterruptedException exc) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"AbstractBufferedStream.offer.interrupted"), inputData);
			}
		}

		return false;
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
	public String getNextItem() throws Exception {
		if (inputBuffer == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"AbstractBufferedStream.changes.buffer.uninitialized"));
		}

		// Buffer is empty and producer input is ended. No more items going to
		// be available.
		if (inputBuffer.isEmpty() && isInputEnded()) {
			return null;
		}

		Object qe = inputBuffer.take();

		// Producer input was slower than consumer, but was able to put "DIE"
		// marker object to queue. No more items going to be available.
		if (DIE_MARKER.equals(qe)) {
			return null;
		}

		String activityInput = (String) qe;

		addStreamedBytesCount(activityInput == null ? 0 : activityInput.getBytes().length);

		return activityInput;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Redirects raw activity data (already JSON formatted by TNT4J based producers) from input to output.
	 */
	@Override
	protected void processActivityItem(String item, AtomicBoolean failureFlag) throws Exception {
		notifyProgressUpdate(incrementCurrentActivitiesCount(), getTotalActivities());

		LOGGER.log(OpLevel.TRACE,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "RedirectTNT4JStream.sending.item"),
				item);

		getOutput().logItem(item);
	}

	private boolean isInputEnded() {
		return feedsProducer == null || feedsProducer.isStopRunning();
	}

	@Override
	protected void cleanup() {
		cleanupStreamInternals();

		super.cleanup();
	}

	private void cleanupStreamInternals() {
		if (feedsProducer != null) {
			feedsProducer.halt();
		}

		inputBuffer.clear();
	}

	private class CharFeedReader extends FeedReader {
		CharFeedReader(Reader in) {
			super(in);
		}

		CharFeedReader(InputStream in) {
			super(in);
		}

		@Override
		protected void notifyBytesRead(int bCount) {
			// addStreamedBytesCount(bCount);
		}
	}

	private abstract class FeedersProducer extends StreamsThread {
		List<ActivitiesFeeder> activeFeedersList = new ArrayList<ActivitiesFeeder>();

		void removeInactiveFeeder(ActivitiesFeeder conn) {
			activeFeedersList.remove(conn);
		}

		public void close() {
			for (ActivitiesFeeder f : activeFeedersList) {
				f.halt();
			}

			activeFeedersList.clear();

			inputBuffer.offer(DIE_MARKER);
		}

		@Override
		public void halt() {
			super.halt();
			close();
		}
	}

	private class ServerSocketFeedsProducer extends FeedersProducer {
		private int srvSocketPort;
		private ServerSocket srvSocket;

		ServerSocketFeedsProducer(int srvSocketPort) throws IOException {
			this.srvSocketPort = srvSocketPort;
			srvSocket = new ServerSocket(srvSocketPort);
		}

		@Override
		public void run() {
			while (!isStopRunning() && !srvSocket.isClosed()) {
				Socket connSocket = null;
				try {
					LOGGER.log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"CharacterStream.waiting.for.connection"), srvSocketPort);
					connSocket = srvSocket.accept();
					LOGGER.log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"CharacterStream.accepted.connection"), connSocket);
				} catch (Exception e) {
					LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"RedirectTNT4JStream.failed.accept.connection"), e.getLocalizedMessage(), e);

					boolean recovered = restartOnInputClose && resetDataStream();

					if (!recovered) {
						halt();
					}
				}

				if (!isStopRunning() && connSocket != null) {
					try {
						ActivitiesFeeder feeder = new ActivitiesFeeder(connSocket);
						activeFeedersList.add(feeder);
						feeder.start();
					} catch (Exception e) {
						LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
								"RedirectTNT4JStream.socket.initialization"), e.getLocalizedMessage(), e);
					}
				}
			}

			close();
		}

		private boolean resetDataStream() {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"RedirectTNT4JStream.resetting.stream"), getName());

			Utils.close(srvSocket);

			try {
				srvSocket = new ServerSocket(srvSocketPort);

				LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"CharacterStream.stream.reset"), srvSocketPort);
			} catch (Exception exc) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"RedirectTNT4JStream.resetting.failed"), getName(), exc);

				return false;
			}

			return true;
		}

		@Override
		public void close() {
			Utils.close(srvSocket);
		}
	}

	private class RawReaderFeedsProducer extends FeedersProducer {
		private ActivitiesFeeder rawFeeder;

		RawReaderFeedsProducer(Reader rawReader) {
			this.rawFeeder = new ActivitiesFeeder(rawReader);
		}

		@Override
		public void run() {
			activeFeedersList.add(rawFeeder);
			rawFeeder.start();
		}
	}

	private class ActivitiesFeeder extends StreamsThread {
		private Socket socket = null;

		/**
		 * BufferedReader that wraps {@link Socket#getInputStream()} or {@link Reader}
		 */
		protected FeedReader dataReader = null;

		ActivitiesFeeder(Socket socket) throws IOException {
			this.socket = socket;
			this.dataReader = new CharFeedReader(socket.getInputStream());
		}

		ActivitiesFeeder(Reader reader) {
			this.dataReader = new CharFeedReader(reader);
		}

		@Override
		public void run() {
			while (!isStopRunning() && !(dataReader.isClosed() || dataReader.hasError())) {
				try {
					String line = dataReader.readLine();

					if (line == null) {
						LOGGER.log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
								"RedirectTNT4JStream.feeder.data.ended"));
						halt(); // no more data items to process
					} else {
						if (line.isEmpty()) {
							LOGGER.log(OpLevel.WARNING, StreamsResources.getString(
									StreamsResources.RESOURCE_BUNDLE_NAME, "RedirectTNT4JStream.redirect.empty.input"));
							incrementSkippedActivitiesCount();
							notifyStreamEvent(OpLevel.WARNING,
									StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
											"RedirectTNT4JStream.redirect.empty.input", line),
									line);
						} else {
							addInputToBuffer(line);
						}
					}
				} catch (IOException ioe) {
					LOGGER.log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"RedirectTNT4JStream.feeder.failure"), ioe.getLocalizedMessage());
					halt();
				} catch (Exception e) {
					LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"RedirectTNT4JStream.feeder.failure"), e.getLocalizedMessage(), e);
				}
			}

			close();
		}

		public void close() {
			Utils.close(dataReader);
			dataReader = null;

			if (socket != null) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"CharacterStream.closing.stream.connection"), socket);

				Utils.close(socket);
				socket = null;
			}

			if (feedsProducer != null) {
				feedsProducer.removeInactiveFeeder(this);
			}
		}

		@Override
		public void halt() {
			super.halt();
			close();
		}
	}
}
