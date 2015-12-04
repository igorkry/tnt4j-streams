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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
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
 * characters '*' and '?'</li>
 * <li>StartFromLatest - flag {@code true/false} indicating that streaming
 * should be performed from latest log entry. If {@code false} - then latest log
 * file is streamed from beginning</li>
 * <li>FileReadDelay - delay is seconds between log file reading iterations</li>
 * </ul>
 *
 * @version $Revision: 1 $
 * @see ActivityParser#isDataClassSupported(Object)
 */
public abstract class AbstractFilePollingStream extends TNTInputStream {
	private static final long DEFAULT_DELAY_PERIOD = TimeUnit.SECONDS.toMillis(15);
	private static final int CHANGES_BUFFER_SIZE = 1024 * 10;

	/**
	 * Stream attribute defining file name.
	 */
	protected String fileName = null;

	/**
	 * Stream attribute defining if streaming should be performed from log
	 * position found on stream initialization. If false - then streaming is
	 * performed from beginning of the file.
	 */
	protected boolean startFromLatestActivity = true;

	private long logWatcherDelay = DEFAULT_DELAY_PERIOD;

	private BlockingQueue<String> changedLinesBuffer;

	/**
	 * Constructs an AbstractFilePollingStream.
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
		if (fileName == null) {
			throw new IllegalStateException(StreamsResources.getString("FileLineStream.undefined.filename"));
		}
		logger.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("FileLineStream.initializing.stream", fileName));

		changedLinesBuffer = new ArrayBlockingQueue<String>(CHANGES_BUFFER_SIZE, true);

		LogWatcher lw = createLogWatcher();
		lw.initialize();
		lw.start();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns a string containing the contents of the next line in
	 * the polled file. Method blocks and waits for lines available in changed
	 * lines buffer. Changed lines buffer is filled by LogWatcher thread.
	 * </p>
	 */
	@Override
	public Object getNextItem() throws Throwable {
		if (changedLinesBuffer == null) {
			throw new IllegalStateException(
					StreamsResources.getString("FilePollingStream.changes.buffer.uninitialized"));
		}

		String line = changedLinesBuffer.take();

		return line;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanup() {
		changedLinesBuffer.clear();

		super.cleanup();
	}

	/**
	 * Constructs new log watcher instance.
	 *
	 * @return log watcher instance
	 */
	protected abstract LogWatcher createLogWatcher();

	/**
	 * Base class containing common log watcher features.
	 */
	protected abstract class LogWatcher extends Thread {

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
		 * polled. If false, then no delay is performed between log file changes
		 * reading.
		 */
		protected boolean readingLatestLogFile = true;

		/**
		 * Log monitor attribute identifying that monitoring is interrupted.
		 */
		protected boolean interrupted = false;

		/**
		 * Constructs an LogWatcher.
		 */
		LogWatcher(String name) {
			super(name);

			setDaemon(true);
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

		private boolean isStopping() {
			return interrupted || isHalted();
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
				addChangedLineToBuffer(line);
				lineNumber = lnr.getLineNumber();
			}
		}

		/**
		 * Adds log line to changes buffer. Line may not be added if buffer size
		 * limit is exceeded.
		 *
		 * @param line
		 *            log line to add to changes buffer
		 *
		 * @see BlockingQueue#offer(Object)
		 */
		private void addChangedLineToBuffer(String line) {
			if (StringUtils.isNotEmpty(line)) {
				boolean added = changedLinesBuffer.offer(line);

				if (!added) {
					logger.log(OpLevel.WARNING,
							StreamsResources.getStringFormatted("FilePollingStream.changes.buffer.limit", line));
				}
			}
		}
	}

}
