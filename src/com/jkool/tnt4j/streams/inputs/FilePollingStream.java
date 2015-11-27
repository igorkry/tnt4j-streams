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
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.ArrayUtils;
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
 * Implements a file activity stream, where each line of the file is assumed to
 * represent a single activity or event which should be recorded. Stream reads
 * changes form defined log files every "FileReadDelay" property defined seconds
 * (default is 15sec.).
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
 * @see WildcardFileFilter#WildcardFileFilter(String)
 */
public class FilePollingStream extends TNTInputStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(FilePollingStream.class);

	private static final long DEFAULT_DELAY_PERIOD = TimeUnit.SECONDS.toMillis(15);
	private static final int CHANGES_BUFFER_SIZE = 1024 * 10;

	private String fileName = null;
	private boolean startFromLatestActivity = true;
	private long logWatcherDelay = DEFAULT_DELAY_PERIOD;

	private BlockingQueue<String> changedLinesBuffer;

	/**
	 * Constructs an FilePollingStream.
	 */
	public FilePollingStream() {
		super(LOGGER);
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
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("FileLineStream.initializing.stream"), fileName);

		changedLinesBuffer = new ArrayBlockingQueue<String>(CHANGES_BUFFER_SIZE, true);

		LogWatcher lw = new LogWatcher();
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
	 * Searches for files matching name pattern. Name pattern also may contain
	 * path of directory, where file search should be performed i.e.
	 * C:/Tomcat/logs/localhost_access_log.*.txt. If no path is defined (just
	 * file name pattern) then files are searched in
	 * {@code System.getProperty("user.dir")}. Files array is ordered by file
	 * last modification timestamp in descending order.
	 *
	 * @param namePattern
	 *            name pattern to find files
	 *
	 * @return files with name matching name pattern ordered by file last
	 *         modification timestamp in descending order.
	 *
	 * @see WildcardFileFilter#WildcardFileFilter(String)
	 * @see File#listFiles(FilenameFilter)
	 */
	private static File[] searchFiles(String namePattern) {
		File f = new File(namePattern);
		File dir = f.getAbsoluteFile().getParentFile();
		File[] activityFiles = dir.listFiles((FilenameFilter) new WildcardFileFilter(f.getName()));

		if (activityFiles != null) {
			Arrays.sort(activityFiles, new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					try {
						BasicFileAttributes bfa1 = Files.readAttributes(o1.toPath(), BasicFileAttributes.class);
						BasicFileAttributes bfa2 = Files.readAttributes(o2.toPath(), BasicFileAttributes.class);

						// NOTE: we want files to be sorted from oldest
						return bfa1.lastModifiedTime().compareTo(bfa2.lastModifiedTime()) * (-1);
					} catch (IOException exc) {
						return 0;
					}
				}
			});
		}

		return activityFiles;
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
	 * Log changes watcher thread. It reads changes from defined log files using
	 * last modification timestamp of log file.
	 */
	private class LogWatcher extends Thread {

		private String polledFileName = null;
		private int lineNumber = -1;
		private long lastModifTime = -1;

		private boolean readingLatestLogFile = true;

		private boolean interrupted = false;

		/**
		 * Constructs an LogWatcher.
		 */
		LogWatcher() {
			super("FilePollingStream.LogWatcher"); // NON-NLS

			setDaemon(true);
		}

		/**
		 * Initializes log files watcher thread. Picks log file matching user
		 * defined file name to monitor. If user defined to start streaming from
		 * latest logged activity, then count of lines in log file is calculated
		 * to mark latest activity position.
		 *
		 * @throws Throwable
		 *             indicates that stream is not configured properly and lof
		 *             files monitoring can't initialize and continue.
		 */
		void initialize() throws Throwable {
			if (Utils.isWildcardFileName(fileName)) {
				File[] activityFiles = searchFiles(fileName);

				polledFileName = ArrayUtils.isEmpty(activityFiles) ? null : activityFiles[0].getAbsolutePath();
			} else {
				polledFileName = fileName;
			}

			if (startFromLatestActivity && StringUtils.isNotEmpty(polledFileName)) {
				File activityFile = new File(polledFileName);
				lastModifTime = activityFile.lastModified();
				LineNumberReader lineReader = null;
				try {
					lineReader = new LineNumberReader(new FileReader(activityFile));
					lineReader.skip(Long.MAX_VALUE);
					// NOTE: Add 1 because line index starts at 0
					lineNumber = lineReader.getLineNumber() + 1;
				} finally {
					Utils.close(lineReader);
				}

				// NOTE: this one should be "faster"
				// lineNumber = Utils.countLines(polledFileName);
			}
		}

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

		private void readLogChanges() {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("FilePollingStream.reading.changes"), polledFileName);
			readingLatestLogFile = true;

			File currLogFile = new File(polledFileName);

			if (!currLogFile.canRead()) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getString("FilePollingStream.cant.access"));

				currLogFile = getNextAvailableLogFile();

				if (currLogFile == null) {
					LOGGER.log(OpLevel.ERROR, StreamsResources.getString("FilePollingStream.next.not.found"));

					interrupted = true;
					return;
				}
			} else {
				long flm = currLogFile.lastModified();
				if (flm > lastModifTime) {
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("FilePollingStream.file.updated"),
							TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - flm),
							TimeUnit.MILLISECONDS.toSeconds(flm - lastModifTime));

					lastModifTime = flm;
				} else {
					File nextLog = getNextAvailableLogFile();

					if (nextLog != null) {
						currLogFile = nextLog;
					} else {
						LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("FilePollingStream.no.changes"));

						return;
					}
				}
			}

			LineNumberReader lnr = null;

			try {
				lnr = rollToCurrentLine(currLogFile);
			} catch (IOException exc) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getString("FilePollingStream.error.rolling"), exc);
			}

			if (lnr != null) {
				try {
					readNewLogLines(lnr);
				} catch (IOException exc) {
					LOGGER.log(OpLevel.ERROR, StreamsResources.getString("FilePollingStream.error.reading"), exc);
				} finally {
					Utils.close(lnr);
				}
			}
		}

		private File getNextAvailableLogFile() {
			if (Utils.isWildcardFileName(fileName)) {
				File[] foundFiles = searchFiles(fileName);
				File nextFile = Utils.getFirstNewer(foundFiles, lastModifTime);

				if (nextFile != null) {
					polledFileName = nextFile.getAbsolutePath();
					lastModifTime = nextFile.lastModified();
					lineNumber = 0;
					readingLatestLogFile = nextFile.equals(foundFiles[0]);

					LOGGER.log(OpLevel.INFO, StreamsResources.getString("FilePollingStream.changing.to.next"),
							polledFileName);
				}

				return nextFile;
			}

			return null;
		}

		private LineNumberReader rollToCurrentLine(File logFile) throws IOException {
			LineNumberReader lnr = openLogReader(logFile);

			if (lnr == null) {
				return null;
			}

			return skipOldLines(lnr);
		}

		private LineNumberReader openLogReader(File logFile) {
			try {
				return new LineNumberReader(new FileReader(logFile));
			} catch (Exception exc) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getString("FilePollingStream.reader.error"));

				interrupted = true;
				return null;
			}
		}

		private LineNumberReader skipOldLines(LineNumberReader lnr) throws IOException {
			lnr.mark(0);
			boolean skipFail = false;

			for (int i = 0; i < lineNumber; i++) {
				if (lnr.readLine() == null) {
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("FilePollingStream.log.shorter"));

					skipFail = true;
					break;
				}
			}

			if (skipFail) {
				File prevLogFile = getPrevLogFile();

				if (prevLogFile != null) {
					Utils.close(lnr);

					return rollToCurrentLine(prevLogFile);
				} else {
					if (lnr.markSupported()) {
						LOGGER.log(OpLevel.INFO,
								StreamsResources.getStringFormatted("FilePollingStream.resetting.reader", 0));

						lnr.reset();
					}
				}

			}

			return lnr;
		}

		private File getPrevLogFile() {
			if (Utils.isWildcardFileName(fileName)) {
				File[] foundFiles = searchFiles(fileName);
				File prevFile = foundFiles == null || foundFiles.length < 2 ? null : foundFiles[1];

				if (prevFile != null) {
					polledFileName = prevFile.getAbsolutePath();
					lastModifTime = prevFile.lastModified();
					readingLatestLogFile = false;

					LOGGER.log(OpLevel.INFO, StreamsResources.getString("FilePollingStream.changing.to.previous"),
							polledFileName);
				}

				return prevFile;
			}

			return null;
		}

		private void readNewLogLines(LineNumberReader lnr) throws IOException {
			String line;
			while ((line = lnr.readLine()) != null) {
				addChangedLineToBuffer(line);
				lineNumber = lnr.getLineNumber();
			}
		}

		private void addChangedLineToBuffer(String line) {
			if (StringUtils.isNotEmpty(line)) {
				boolean added = changedLinesBuffer.offer(line);

				if (!added) {
					LOGGER.log(OpLevel.WARNING, StreamsResources.getString("FilePollingStream.changes.buffer.limit"),
							line);
				}
			}
		}
	}
}