/*
 * Copyright 2014-2017 JKOOL, LLC.
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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.state.HdfsFileStreamStateHandler;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Implements a HDFS files lines activity stream, where each line of the file is assumed to represent a single activity
 * or event which should be recorded. Stream reads changes from defined files every "FileReadDelay" property defined
 * seconds (default is 15sec.).
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 * <p>
 * This activity stream supports configuration properties from {@link AbstractFileLineStream} (and higher hierarchy
 * streams).
 *
 * @version $Revision: 2 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see WildcardFileFilter#WildcardFileFilter(String)
 */
public class HdfsFileLineStream extends AbstractFileLineStream<Path> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(HdfsFileLineStream.class);

	/**
	 * Constructs a new HdfsFileLineStream.
	 */
	public HdfsFileLineStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	protected FileWatcher createFileWatcher() {
		return new HdfsFileWatcher();
	}

	/**
	 * Searches for files matching name pattern. Name pattern also may contain path of directory, where file search
	 * should be performed, e.g., C:/Tomcat/logs/localhost_access_log.*.txt. If no path is defined (just file name
	 * pattern) then files are searched in {@code System.getProperty("user.dir")}. Files array is ordered by file create
	 * timestamp in descending order.
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
	public static Path[] searchFiles(Path path, FileSystem fs) throws IOException {
		FileStatus[] dir = fs.listStatus(path.getParent(), new PathFilter() {
			@Override
			public boolean accept(Path path) {
				String name = path.getName();
				return FilenameUtils.wildcardMatch(name, "*", IOCase.INSENSITIVE); // NON-NLS
			}
		});

		Path[] activityFiles = new Path[dir == null ? 0 : dir.length];
		if (dir != null) {
			Arrays.sort(dir, new Comparator<FileStatus>() {
				@Override
				public int compare(FileStatus o1, FileStatus o2) {
					return Long.valueOf(o1.getModificationTime()).compareTo(o2.getModificationTime()) * (-1);
				}
			});

			for (int i = 0; i < dir.length; i++) {
				activityFiles[i] = dir[i].getPath();
			}
		}

		return activityFiles;
	}

	private static int[] getFilesTotals(FileSystem fs, Path[] activityFiles) {
		int tbc = 0;
		int tlc = 0;
		if (ArrayUtils.isNotEmpty(activityFiles)) {
			for (Path f : activityFiles) {
				try {
					ContentSummary cSummary = fs.getContentSummary(f);
					tbc += cSummary.getLength();
					tlc += Utils.countLines(fs.open(f));
				} catch (IOException exc) {
				}
			}
		}

		return new int[] { tbc, tlc };
	}

	/**
	 * HDFS files changes watcher thread. It reads changes from defined HDFS files using last modification timestamp of
	 * file.
	 */
	private class HdfsFileWatcher extends FileWatcher {

		private FileSystem fs;

		/**
		 * Constructs a new HdfsFileWatcher.
		 */
		HdfsFileWatcher() {
			super("HdfsFileLineStream.HdfsFileWatcher"); // NON-NLS
		}

		/**
		 * Initializes files watcher thread. Picks file matching user defined file name to monitor. If user defined to
		 * start streaming from latest file line, then count of lines in file is calculated to mark latest activity
		 * position.
		 *
		 * @param params
		 *            initialization parameters array
		 *
		 * @throws Exception
		 *             indicates that stream is not configured properly and files monitoring can't initialize and
		 *             continue
		 */
		@Override
		protected void initialize(Object... params) throws Exception {
			URI fileUri = new URI(fileName);
			fs = FileSystem.get(fileUri, new Configuration());
			Path filePath = new Path(fileUri);

			if (Utils.isWildcardString(fileName)) {
				availableFiles = searchFiles(filePath, fs);
			} else {
				availableFiles = new Path[] { filePath };
			}

			updateDataTotals(availableFiles, fs);

			stateHandler = storeState
					? new HdfsFileStreamStateHandler(availableFiles, fs, HdfsFileLineStream.this.getName()) : null;
			if (isStoredStateAvailable()) {
				filePath = stateHandler.getFile();
				lineNumber = stateHandler.getLineNumber();
				// lastModifTime = stateHandler.getReadTime();
			} else {
				filePath = ArrayUtils.isEmpty(availableFiles) ? null
						: startFromLatestActivity ? availableFiles[availableFiles.length - 1] : availableFiles[0];
			}

			setFileToRead(filePath);

			if (startFromLatestActivity && fileToRead != null) {
				lastModifTime = getModificationTime(fileToRead, fs);
				lineNumber = Utils.countLines(fs.open(fileToRead));
			}
		}

		/**
		 * Reads defined HDFS file changes since last read iteration (or from stream initialization if it is first
		 * monitor invocation).
		 * <p>
		 * Monitor checks if it can read defined file. If not then tries to swap to next available file. If swap can't
		 * be done (no newer readable file) then file monitoring is interrupted.
		 * <p>
		 * If defined file is readable, then monitor checks modification timestamp. If it is newer than
		 * {@link #lastModifTime} value, file gets opened for reading. If not, monitor tries to swap to next available
		 * file. If swap can'e be done (no newer readable file) then file reading is skipped until next monitor
		 * invocation.
		 * <p>
		 * When file gets opened for reading reader is rolled to file marked by {@link #lineNumber} attribute. If turns
		 * out that file got smaller in lines count, then monitor tries to swap to previous file. If no previous
		 * readable file is available, then reader is reset to first file line.
		 * <p>
		 * Reader reads all file lines until end of file and puts them to changed lines buffer.
		 */
		@Override
		protected void readFileChanges() {
			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"FileLineStream.reading.changes", fileToRead.toString());

			try {
				if (fs == null) {
					URI uri = new URI(fileName);
					fs = FileSystem.get(uri, new Configuration());
				}
				FileStatus fStatus = fs.getFileStatus(fileToRead);

				if (!canRead(fStatus)) {
					logger().log(OpLevel.WARNING, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"FileLineStream.cant.access");

					boolean swapped = swapToNextFile(fs);

					if (!swapped) {
						logger().log(OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"FileLineStream.next.not.found");

						shutdown();
						return;
					}
				} else {
					long flm = fStatus.getModificationTime();
					if (flm > lastModifTime) {
						logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"FileLineStream.file.updated",
								TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - flm),
								getLastReadTimeToLog(flm));

						lastModifTime = flm;
					} else {
						boolean swapped = swapToNextFile(fs);

						if (!swapped) {
							logger().log(OpLevel.DEBUG,
									StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
									"FileLineStream.no.changes");

							return;
						}
					}
				}

				LineNumberReader lnr = null;

				try {
					lnr = rollToCurrentLine(fs);
				} catch (IOException exc) {
					logger().log(OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"FileLineStream.error.rolling", exc);
				}

				if (lnr != null) {
					try {
						readNewFileLines(lnr);
					} catch (IOException exc) {
						logger().log(OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"FileLineStream.error.reading", exc);
					} finally {
						Utils.close(lnr);
					}
				}
			} catch (Exception exc) {
				logger().log(OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"FileLineStream.error.reading.changes", exc);
			}

			logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
					"FileLineStream.changes.read.end", fileToRead.toString());
		}

		private LineNumberReader rollToCurrentLine(FileSystem fs) throws Exception {
			LineNumberReader lnr;
			try {
				lnr = new LineNumberReader(new InputStreamReader(fs.open(fileToRead)));
			} catch (Exception exc) {
				logger().log(OpLevel.ERROR, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
						"FileLineStream.reader.error");

				shutdown();
				return null;
			}

			return skipOldLines(lnr, fs);
		}

		private LineNumberReader skipOldLines(LineNumberReader lnr, FileSystem fs) throws Exception {
			lnr.mark(0);
			boolean skipFail = false;

			for (int i = 0; i < lineNumber; i++) {
				if (lnr.readLine() == null) {
					logger().log(OpLevel.DEBUG, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"FileLineStream.file.shorter");

					skipFail = true;
					break;
				}
			}

			if (skipFail) {
				boolean swapped = swapToPrevFile(fs);

				if (swapped) {
					Utils.close(lnr);

					return rollToCurrentLine(fs);
				} else {
					if (lnr.markSupported()) {
						logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
								"FileLineStream.resetting.reader", 0);

						lnr.reset();
					}
				}
			}

			return lnr;
		}

		private boolean swapToPrevFile(FileSystem fs) throws Exception {
			if (Utils.isWildcardString(fileName)) {
				URI fileUri = new URI(fileName);
				Path filePath = new Path(fileUri);

				availableFiles = searchFiles(filePath, fs);
				updateDataTotals(availableFiles, fs);

				Path prevFile = ArrayUtils.getLength(availableFiles) < 2 ? null
						: availableFiles[availableFiles.length - 2];

				if (prevFile != null) {
					setFileToRead(prevFile);
					lastModifTime = getModificationTime(prevFile, fs);

					logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"FileLineStream.changing.to.previous", prevFile.toUri());
				}

				return prevFile != null;
			}

			return false;
		}

		private boolean swapToNextFile(FileSystem fs) throws Exception {
			if (Utils.isWildcardString(fileName)) {
				URI fileUri = new URI(fileName);
				Path filePath = new Path(fileUri);

				availableFiles = searchFiles(filePath, fs);
				updateDataTotals(availableFiles, fs);

				Path nextFile = getFirstNewer(availableFiles, lastModifTime, fs);

				if (nextFile != null) {
					setFileToRead(nextFile);
					lastModifTime = getModificationTime(nextFile, fs);
					lineNumber = 0;

					logger().log(OpLevel.INFO, StreamsResources.getBundle(StreamsResources.RESOURCE_BUNDLE_NAME),
							"FileLineStream.changing.to.next", nextFile.toUri());
				}

				return nextFile != null;
			}

			return false;
		}

		private Path getFirstNewer(Path[] files, Long lastModif, FileSystem fs) throws IOException {
			Path last = null;

			if (ArrayUtils.isNotEmpty(files)) {
				boolean changeDir = (getModificationTime(files[0], fs)
						- getModificationTime(files[files.length - 1], fs)) < 0;

				for (int i = changeDir ? files.length - 1 : 0; changeDir ? i >= 0
						: i < files.length; i = changeDir ? i - 1 : i + 1) {
					Path f = files[i];
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
			return fs != null && Utils.matchMask(fs.getPermission().toShort(), 0444);
		}

		private void updateDataTotals(Path[] activityFiles, FileSystem fs) {
			int[] totals = getFilesTotals(fs, activityFiles);
			totalBytesCount = totals[0];
			totalLinesCount = totals[1];
		}

		/**
		 * Closes opened {@link FileSystem} and persists file access state.
		 *
		 * @throws Exception
		 *             if fails to close opened resources due to internal error
		 */
		@Override
		void closeInternals() throws Exception {
			Utils.close(fs);

			super.closeInternals();
		}
	}
}