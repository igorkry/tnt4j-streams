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

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Base class for log files polling activity stream, where each line of the file
 * is assumed to represent a single activity or event which should be recorded.
 * Stream reads changes form defined log files every "FileReadDelay" property
 * defined seconds (default is 15sec.).
 * </p>
 * <p>
 * This activity stream requires parsers that can support {@code String} data.
 * </p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>FileName - concrete file name or file name pattern defined using
 * characters '*' and '?'. (Required)</li>
 * <li>StartFromLatest - flag {@code true}/{@code false} indicating that
 * streaming should be performed from latest log entry. If {@code false} - then
 * latest log file is streamed from beginning</li>
 * <li>FileReadDelay - delay is seconds between log file reading iterations</li>
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
		if (StreamsConfig.PROP_FILENAME.equalsIgnoreCase(name)) {
			return fileName;
		}
		if (StreamsConfig.PROP_START_FROM_LATEST.equalsIgnoreCase(name)) {
			return startFromLatestActivity;
		}
		if (StreamsConfig.PROP_FILE_READ_DELAY.equalsIgnoreCase(name)) {
			return logWatcherDelay;
		}
		return super.getProperty(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Throwable {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamsConfig.PROP_FILENAME.equalsIgnoreCase(name)) {
				fileName = value;
			} else if (StreamsConfig.PROP_START_FROM_LATEST.equalsIgnoreCase(name)) {
				startFromLatestActivity = Boolean.parseBoolean(value);
			} else if (StreamsConfig.PROP_FILE_READ_DELAY.equalsIgnoreCase(name)) {
				logWatcherDelay = TimeUnit.SECONDS.toMillis(Long.parseLong(value));
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void initialize() throws Throwable {
		super.initialize();
		if (StringUtils.isEmpty(fileName)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted("TNTInputStream.property.undefined",
					StreamsConfig.PROP_FILENAME));
		}
		logger.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("FileLineStream.initializing.stream", fileName));

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
		 * {@code logWatcherDelay} defined delays between iterations.
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
