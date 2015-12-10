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
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a HDFS log files polling activity stream, where each line of the
 * file is assumed to represent a single activity or event which should be
 * recorded. Stream reads changes form defined log files every "FileReadDelay"
 * property defined seconds (default is 15sec.).
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
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see WildcardFileFilter#WildcardFileFilter(String)
 */
public class HdfsFilePollingStream extends AbstractFilePollingStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(HdfsFilePollingStream.class);

	/**
	 * Constructs an HdfsFilePollingStream.
	 */
	public HdfsFilePollingStream() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected LogWatcher createLogWatcher() {
		return new HdfsLogWatcher();
	}

	/**
	 * Searches for files matching name pattern. Name pattern also may contain
	 * path of directory, where file search should be performed i.e.
	 * C:/Tomcat/logs/localhost_access_log.*.txt. If no path is defined (just
	 * file name pattern) then files are searched in
	 * {@code System.getProperty("user.dir")}. Files describing {@code Path}
	 * array is ordered by file modification timestamp in descending order.
	 *
	 * @param path
	 *            path of file
	 * @param fs
	 *            file system
	 *
	 * @return array of found files paths.
	 * @throws IOException
	 *             if files can't be listed by file system.
	 *
	 * @see FileSystem#listStatus(Path, PathFilter)
	 * @see FilenameUtils#wildcardMatch(String, String, IOCase)
	 */
	private static Path[] searchFiles(Path path, FileSystem fs) throws IOException {
		FileStatus[] dir = fs.listStatus(path.getParent(), new PathFilter() {
			@Override
			public boolean accept(Path path) {
				final String name = path.getName();
				return FilenameUtils.wildcardMatch(name, "*", IOCase.INSENSITIVE);
			}
		});

		Path[] activityFiles = new Path[dir == null ? 0 : dir.length];
		if (dir != null) {
			Arrays.sort(dir, new Comparator<FileStatus>() {
				@Override
				public int compare(FileStatus o1, FileStatus o2) {
					return Long.valueOf(o1.getModificationTime()).compareTo(Long.valueOf(o2.getModificationTime()))
							* (-1);
				}
			});

			for (int i = 0; i < dir.length; i++) {
				activityFiles[i] = dir[i].getPath();
			}
		}

		return activityFiles;
	}

	/**
	 * HDFS Log files changes watcher thread. It reads changes from defined log
	 * HDFS files using last modification timestamp of log file.
	 */
	private class HdfsLogWatcher extends LogWatcher {

		private Path pollingFile = null;

		/**
		 * Constructs an HdfsLogWatcher.
		 */
		HdfsLogWatcher() {
			super("HdfsFilePollingStream.HdfsLogWatcher"); // NON-NLS
		}

		/**
		 * Initializes log files watcher thread. Picks log file matching user
		 * defined file name to monitor. If user defined to start streaming from
		 * latest logged activity, then count of lines in log file is calculated
		 * to mark latest activity position.
		 *
		 * @throws Throwable
		 *             indicates that stream is not configured properly and log
		 *             files monitoring can't initialize and continue.
		 */
		protected void initialize() throws Exception {
			final URI fileUri = new URI(fileName);
			FileSystem fs = FileSystem.get(fileUri, new Configuration());
			Path filePath = new Path(fileUri);

			if (Utils.isWildcardFileName(fileName)) {
				Path[] activityFiles = searchFiles(filePath, fs);

				pollingFile = ArrayUtils.isEmpty(activityFiles) ? null : activityFiles[0];
			} else {
				pollingFile = filePath;
			}

			if (startFromLatestActivity && pollingFile != null) {
				lastModifTime = getModificationTime(pollingFile, fs);
				LineNumberReader lineReader = null;
				try {
					lineReader = new LineNumberReader(new InputStreamReader(fs.open(pollingFile)));
					lineReader.skip(Long.MAX_VALUE);
					// NOTE: Add 1 because line index starts at 0
					lineNumber = lineReader.getLineNumber() + 1;
				} finally {
					Utils.close(lineReader);
				}
			}

			Utils.close(fs);
		}

		/**
		 * Reads HDFS log file changes since last read iteration (or from stream
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
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getStringFormatted("FilePollingStream.reading.changes", pollingFile.toString()));
			readingLatestLogFile = true;

			FileSystem fs = null;
			try {
				final URI uri = new URI(fileName);
				fs = FileSystem.get(uri, new Configuration());
				FileStatus fStatus = fs.getFileStatus(pollingFile);

				if (!canRead(fStatus)) {
					LOGGER.log(OpLevel.WARNING, StreamsResources.getString("FilePollingStream.cant.access"));

					boolean swapped = swapToNextAvailableLogFile(fs);

					if (!swapped) {
						LOGGER.log(OpLevel.ERROR, StreamsResources.getString("FilePollingStream.next.not.found"));

						interrupted = true;
						return;
					}
				} else {
					long flm = fStatus.getModificationTime();
					if (flm > lastModifTime) {
						LOGGER.log(OpLevel.DEBUG,
								StreamsResources.getStringFormatted("FilePollingStream.file.updated",
										TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - flm),
										TimeUnit.MILLISECONDS.toSeconds(flm - lastModifTime)));

						lastModifTime = flm;
					} else {
						boolean swapped = swapToNextAvailableLogFile(fs);

						if (!swapped) {
							LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("FilePollingStream.no.changes"));

							return;
						}
					}
				}

				LineNumberReader lnr = null;

				try {
					lnr = rollToCurrentLine(fs);
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
			} catch (Throwable exc) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getString("HdfsFilePollingStream.error.reading.changes"),
						exc);
			} finally {
				Utils.close(fs);
			}
		}

		private LineNumberReader rollToCurrentLine(FileSystem fs) throws Exception {
			LineNumberReader lnr;
			try {
				lnr = new LineNumberReader(new InputStreamReader(fs.open(pollingFile)));
			} catch (Exception exc) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getString("FilePollingStream.reader.error"));

				interrupted = true;
				return null;
			}

			return skipOldLines(lnr, fs);
		}

		private LineNumberReader skipOldLines(LineNumberReader lnr, FileSystem fs) throws Exception {
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
				boolean swapped = swapToPrevLogFile(fs);

				if (swapped) {
					Utils.close(lnr);

					return rollToCurrentLine(fs);
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

		private boolean swapToPrevLogFile(FileSystem fs) throws Exception {
			if (Utils.isWildcardFileName(fileName)) {
				URI fileUri = new URI(fileName);
				Path filePath = new Path(fileUri);
				Path[] foundFiles = searchFiles(filePath, fs);
				Path prevFile = foundFiles == null || foundFiles.length < 2 ? null : foundFiles[1];

				if (prevFile != null) {
					pollingFile = prevFile;
					lastModifTime = getModificationTime(prevFile, fs);
					readingLatestLogFile = false;

					LOGGER.log(OpLevel.INFO, StreamsResources
							.getStringFormatted("FilePollingStream.changing.to.previous", prevFile.toUri()));
				}

				return prevFile != null;
			}

			return false;
		}

		private boolean swapToNextAvailableLogFile(FileSystem fs) throws Exception {
			if (Utils.isWildcardFileName(fileName)) {
				URI fileUri = new URI(fileName);
				Path filePath = new Path(fileUri);
				Path[] foundFiles = searchFiles(filePath, fs);
				Path nextFile = getFirstNewer(foundFiles, lastModifTime, fs);

				if (nextFile != null) {
					pollingFile = nextFile;
					lastModifTime = getModificationTime(nextFile, fs);
					lineNumber = 0;
					readingLatestLogFile = nextFile.equals(foundFiles[0]);

					LOGGER.log(OpLevel.INFO, StreamsResources.getStringFormatted("FilePollingStream.changing.to.next",
							nextFile.toUri()));
				}

				return nextFile != null;
			}

			return false;
		}

		private Path getFirstNewer(Path[] files, Long lastModif, FileSystem fs) throws IOException {
			Path last = null;

			if (ArrayUtils.isNotEmpty(files)) {
				for (Path f : files) {
					FileStatus fStatus = fs.getFileStatus(f);
					if (canRead(fStatus)) {
						if (lastModif == null) {
							last = f;
							break;
						} else {
							if (fStatus.getModificationTime() > lastModif) {
								last = f;
							} else {
								break;
							}
						}
					}
				}
			}

			return last;
		}

		private long getModificationTime(Path file, FileSystem fs) throws IOException {
			FileStatus fStatus = fs.getFileStatus(file);
			return fStatus.getModificationTime();
		}

		private boolean canRead(FileStatus fs) {
			return fs != null && ((fs.getPermission().toShort() & 0444) == 0444);
		}
	}
}