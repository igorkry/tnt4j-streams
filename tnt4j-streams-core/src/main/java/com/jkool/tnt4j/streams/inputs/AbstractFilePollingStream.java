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

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;

import com.jkool.tnt4j.streams.configure.StreamProperties;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Base class for log files polling activity stream, where each line of the file
 * is assumed to represent a single activity or event which should be recorded.
 * Stream reads changes form defined log files every "FileReadDelay" property
 * defined seconds (default is 15sec.).
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>FileName - concrete file name or file name pattern defined using
 * characters '*' and '?'. (Required)</li>
 * <li>StartFromLatest - flag {@code true}/{@code false} indicating that
 * streaming should be performed from latest log entry. If {@code false} - then
 * latest log file is streamed from beginning. (Optional)</li>
 * <li>FileReadDelay - delay is seconds between log file reading iterations.
 * (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public abstract class AbstractFilePollingStream extends AbstractBufferedStream<String> {
	private static final long DEFAULT_DELAY_PERIOD = TimeUnit.SECONDS.toMillis(15);

	/**
	 * Stream attribute defining file name.
	 */
	protected String fileName = null;

	/**
	 * Stream attribute defining if streaming should be performed from log
	 * position found on stream initialization. If {@code false} - then
	 * streaming is performed from beginning of the file.
	 */
	protected boolean startFromLatestActivity = true;

	private long logWatcherDelay = DEFAULT_DELAY_PERIOD;

	private LogWatcher logWatcher;

	/**
	 * Constructs a new AbstractFilePollingStream.
	 * 
	 * @param logger
	 *            logger used by activity stream
	 */
	protected AbstractFilePollingStream(EventSink logger) {
		super(logger);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			return fileName;
		}
		if (StreamProperties.PROP_START_FROM_LATEST.equalsIgnoreCase(name)) {
			return startFromLatestActivity;
		}
		if (StreamProperties.PROP_FILE_READ_DELAY.equalsIgnoreCase(name)) {
			return logWatcherDelay;
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
			if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
				fileName = value;
			} else if (StreamProperties.PROP_START_FROM_LATEST.equalsIgnoreCase(name)) {
				startFromLatestActivity = Boolean.parseBoolean(value);
			} else if (StreamProperties.PROP_FILE_READ_DELAY.equalsIgnoreCase(name)) {
				logWatcherDelay = TimeUnit.SECONDS.toMillis(Long.parseLong(value));
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialize() throws Exception {
		super.initialize();
		if (StringUtils.isEmpty(fileName)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"TNTInputStream.property.undefined", StreamProperties.PROP_FILENAME));
		}
		logger.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
				"FileLineStream.initializing.stream", fileName));

		logWatcher = createLogWatcher();
		logWatcher.initialize();
		logWatcher.start();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanup() {
		logWatcher.shutdown();

		super.cleanup();
	}

	/**
	 * Constructs a new log watcher instance specific for this stream.
	 *
	 * @return log watcher instance
	 */
	protected abstract LogWatcher createLogWatcher();

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isInputEnded() {
		return logWatcher.isInputEnded();
	}

	/**
	 * Base class containing common log watcher features.
	 */
	protected abstract class LogWatcher extends InputProcessor {

		/**
		 * Log monitor attribute storing line number marker of polled log file.
		 */
		protected int lineNumber = -1;

		/**
		 * Log monitor attribute storing modification time of polled log file.
		 */
		protected long lastModifTime = -1;

		/**
		 * Log monitor attribute identifying that last (or latest) log file is
		 * polled. If {@code false}, then no delay is performed between log file
		 * changes reading.
		 */
		protected boolean readingLatestLogFile = true;

		/**
		 * Constructs a new LogWatcher.
		 * 
		 * @param name
		 *            the name of log watcher thread
		 */
		LogWatcher(String name) {
			super(name);
		}

		/**
		 * Initializes log watcher thread. Picks log file matching user defined
		 * file name to monitor. If user defined to start streaming from latest
		 * logged activity, then count of lines in log file is calculated to
		 * mark latest activity position.
		 *
		 * @throws Exception
		 *             indicates that stream is not configured properly and log
		 *             files monitoring can't initialize and continue.
		 */
		protected abstract void initialize() throws Exception;

		/**
		 * Performs continuous file monitoring until stream thread is halted or
		 * monitoring is interrupted. Log file monitoring is performed with
		 * {@link #logWatcherDelay} defined delays between iterations.
		 */
		@Override
		public void run() {
			while (!isStopping()) {
				readLogChanges();

				if (readingLatestLogFile && !isStopping()) {
					try {
						Thread.sleep(logWatcherDelay);
					} catch (InterruptedException exc) {
					}
				}
			}
		}

		/**
		 * Performs log changes reading.
		 */
		protected abstract void readLogChanges();

		/**
		 * Reads new log lines and adds them to changed lines buffer.
		 *
		 * @param lnr
		 *            line number reader
		 * 
		 * @throws IOException
		 *             if error occurs when reading log line
		 */
		protected void readNewLogLines(LineNumberReader lnr) throws IOException {
			String line;
			while ((line = lnr.readLine()) != null) {
				addInputToBuffer(line);
				lineNumber = lnr.getLineNumber();
			}
		}
	}
}
