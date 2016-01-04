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
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.ArrayUtils;

import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a log files poling activity stream, where each line of the file is
 * assumed to represent a single activity or event which should be recorded.
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
 * <li>StartFromLatest - flag {@code true}/{@code false} indicating that
 * streaming should be performed from latest log entry. If {@code false} - then
 * latest log file is streamed from beginning</li>
 * <li>FileReadDelay - delay is seconds between log file reading iterations</li>
 * </ul>
 *
 * @version $Revision: 2 $
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see WildcardFileFilter#WildcardFileFilter(String)
 */
public class FilePollingStream extends AbstractFilePollingStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(FilePollingStream.class);

	/**
	 * Constructs a new FilePollingStream.
	 */
	public FilePollingStream() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected LogWatcher createLogWatcher() {
		return new FileLogWatcher();
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
	 * @return array of found files.
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
	 * Log files changes watcher thread. It reads changes from defined log files
	 * using last modification timestamp of log file.
	 */
	private class FileLogWatcher extends LogWatcher {

		private File pollingFile = null;

		/**
		 * Constructs a new FileLogWatcher.
		 */
		FileLogWatcher() {
			super("FilePollingStream.LogWatcher"); // NON-NLS
		}

		/**
		 * Initializes log files watcher thread. Picks log file matching user
		 * defined file name to monitor. If user defined to start streaming from
		 * latest logged activity, then count of lines in log file is calculated
		 * to mark latest activity position.
		 *
		 * @throws Exception
		 *             when file is not found or can't determine number of lines
		 *             in file.
		 */
		@Override
		protected void initialize() throws Exception {
			if (Utils.isWildcardFileName(fileName)) {
				File[] activityFiles = searchFiles(fileName);

				pollingFile = ArrayUtils.isEmpty(activityFiles) ? null : activityFiles[0];
			} else {
				pollingFile = new File(fileName);
			}

			if (startFromLatestActivity && pollingFile != null) {
				lastModifTime = pollingFile.lastModified();
				LineNumberReader lineReader = null;
				try {
					lineReader = new LineNumberReader(new FileReader(pollingFile));
					lineReader.skip(Long.MAX_VALUE);
					// NOTE: Add 1 because line index starts at 0
					lineNumber = lineReader.getLineNumber() + 1;
				} finally {
					Utils.close(lineReader);
				}

				// NOTE: this one should be "faster"
				// lineNumber = Utils.countLines(pollingFile);
			}
		}

		/**
		 * Reads log file changes since last read iteration (or from stream
		 * initialization if it is first monitor invocation).
		 * <p>
		 * Monitor checks if it can read polled file. If not then tries to swap
		 * to next available log file. If swap can't be done (no newer readable
		 * file) then file monitoring is interrupted.
		 * </p>
		 * <p>
		 * If polled file is readable, then monitor checks modification
		 * timestamp. If it is newer than sored in {@code lastModifTime} file
		 * gets opened for reading. If not, monitor tries to swap to next
		 * available log file. If swap can'e be done (no newer readable file)
		 * then file reading is skipped until next monitor invocation.
		 * </p>
		 * <p>
		 * When file gets opened for reading reader is rolled to log file marked
		 * by {@code lineNumber} attribute. If turns out that file got smaller
		 * in lines count, then monitor tries to swap to previous log file. If
		 * no previous readable log file is available, then reader is reset to
		 * first file line.
		 * </p>
		 * <p>
		 * Reader reads all file lines until end of file and puts them to
		 * changed lines buffer.
		 * </p>
		 */
		@Override
		protected void readLogChanges() {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("FilePollingStream.reading.changes",
					pollingFile.getAbsolutePath()));
			readingLatestLogFile = true;

			if (!pollingFile.canRead()) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getString("FilePollingStream.cant.access"));

				boolean swapped = swapToNextAvailableLogFile();

				if (!swapped) {
					LOGGER.log(OpLevel.ERROR, StreamsResources.getString("FilePollingStream.next.not.found"));

					halt();
					return;
				}
			} else {
				long flm = pollingFile.lastModified();
				if (flm > lastModifTime) {
					LOGGER.log(OpLevel.DEBUG,
							StreamsResources.getStringFormatted("FilePollingStream.file.updated",
									TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - flm),
									TimeUnit.MILLISECONDS.toSeconds(flm - lastModifTime)));

					lastModifTime = flm;
				} else {
					boolean swapped = swapToNextAvailableLogFile();

					if (!swapped) {
						LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("FilePollingStream.no.changes"));

						return;
					}
				}
			}

			LineNumberReader lnr = null;

			try {
				lnr = rollToCurrentLine();
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

		private LineNumberReader rollToCurrentLine() throws IOException {
			LineNumberReader lnr;
			try {
				lnr = new LineNumberReader(new FileReader(pollingFile));
			} catch (Exception exc) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getString("FilePollingStream.reader.error"));

				halt();
				return null;
			}

			return skipOldLines(lnr);
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
				boolean swapped = swapToPrevLogFile();

				if (swapped) {
					Utils.close(lnr);

					return rollToCurrentLine();
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

		private boolean swapToPrevLogFile() {
			if (Utils.isWildcardFileName(fileName)) {
				File[] foundFiles = searchFiles(fileName);
				File prevFile = foundFiles == null || foundFiles.length < 2 ? null : foundFiles[1];

				if (prevFile != null) {
					pollingFile = prevFile;
					lastModifTime = prevFile.lastModified();
					readingLatestLogFile = false;

					LOGGER.log(OpLevel.INFO, StreamsResources
							.getStringFormatted("FilePollingStream.changing.to.previous", prevFile.getAbsolutePath()));
				}

				return prevFile != null;
			}

			return false;
		}

		private boolean swapToNextAvailableLogFile() {
			if (Utils.isWildcardFileName(fileName)) {
				File[] foundFiles = searchFiles(fileName);
				File nextFile = Utils.getFirstNewer(foundFiles, lastModifTime);

				if (nextFile != null) {
					pollingFile = nextFile;
					lastModifTime = nextFile.lastModified();
					lineNumber = 0;
					readingLatestLogFile = nextFile.equals(foundFiles[0]);

					LOGGER.log(OpLevel.INFO, StreamsResources.getStringFormatted("FilePollingStream.changing.to.next",
							nextFile.getAbsolutePath()));
				}

				return nextFile != null;
			}

			return false;
		}
	}
}