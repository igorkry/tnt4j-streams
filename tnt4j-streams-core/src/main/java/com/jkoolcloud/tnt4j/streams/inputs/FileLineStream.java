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
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.ArrayUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.state.FileStreamStateHandler;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * <p>
 * Implements a files lines activity stream, where each line of the file is
 * assumed to represent a single activity or event which should be recorded.
 * Stream reads changes from defined files every "FileReadDelay" property
 * defined seconds (default is 15sec.).
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>FileName - concrete file name or file name pattern defined using
 * characters '*' and '?'</li>
 * <li>FilePolling - flag {@code true}/{@code false} indicating whether files
 * should be polled for changes or not. If not, then files are read from oldest
 * to newest sequentially one single time. Default value - {@code false}.
 * (Optional)</li>
 * <li>StartFromLatest - flag {@code true}/{@code false} indicating that
 * streaming should be performed from latest file entry line. If {@code false} -
 * then all lines from available files are streamed on startup. Actual just if
 * 'FilePolling' property is set to {@code true}. Default value - {@code true}.
 * (Optional)</li>
 * <li>FileReadDelay - delay is seconds between file reading iterations. Actual
 * just if 'FilePolling' property is set to {@code true}. Default value - 15sec.
 * (Optional)</li>
 * <li>RestoreState - flag {@code true}/{@code false} indicating whether files
 * read state should be stored and restored on stream restart. Default value -
 * {@code true}. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 2 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see WildcardFileFilter#WildcardFileFilter(String)
 */
public class FileLineStream extends AbstractFileLineStream<File> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(FileLineStream.class);

	/**
	 * Constructs a new FileLineStream.
	 */
	public FileLineStream() {
		super(LOGGER);
	}

	@Override
	protected FileWatcher createFileWatcher() {
		return new CommonFileWatcher();
	}

	/**
	 * Searches for files matching name pattern. Name pattern also may contain
	 * path of directory, where file search should be performed i.e.
	 * C:/Tomcat/logs/localhost_access_log.*.txt. If no path is defined (just
	 * file name pattern) then files are searched in
	 * {@code System.getProperty("user.dir")}. Files array is ordered by file
	 * modification timestamp in ascending order.
	 *
	 * @param namePattern
	 *            name pattern to find files
	 *
	 * @return array of found files.
	 *
	 * @see WildcardFileFilter#WildcardFileFilter(String)
	 * @see File#listFiles(FilenameFilter)
	 */
	public static File[] searchFiles(String namePattern) {
		File f = new File(namePattern);
		File dir = f.getAbsoluteFile().getParentFile();
		File[] activityFiles = dir.listFiles((FilenameFilter) new WildcardFileFilter(f.getName()));

		if (activityFiles != null) {
			Arrays.sort(activityFiles, new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					long f1ct = o1.lastModified();
					long f2ct = o2.lastModified();
					// NOTE: we want files to be sorted oldest->newest
					// (ASCENDING)
					return f1ct < f2ct ? -1 : (f1ct == f2ct ? 0 : 1);
				}
			});
		}

		return activityFiles;
	}

	private static int[] getFilesTotals(File[] activityFiles) {
		int tbc = 0;
		int tlc = 0;
		if (ArrayUtils.isNotEmpty(activityFiles)) {
			for (File f : activityFiles) {
				tbc += f.length();
				try {
					tlc += Utils.countLines(new FileReader(f));
				} catch (IOException exc) {
				}
			}
		}

		return new int[] { tbc, tlc };
	}

	/**
	 * Files changes watcher thread. It reads changes from defined files using
	 * last modification timestamp of file.
	 */
	private class CommonFileWatcher extends FileWatcher {

		/**
		 * Constructs a new CommonFileWatcher.
		 */
		CommonFileWatcher() {
			super("FileLineStream.FileWatcher"); // NON-NLS
		}

		/**
		 * Initializes files watcher thread. Picks file matching user defined
		 * file name to monitor. If user defined to start streaming from latest
		 * file line, then count of lines in file is calculated to mark latest
		 * activity position.
		 *
		 * @throws Exception
		 *             indicates that stream is not configured properly and
		 *             files monitoring can't initialize and continue.
		 */
		@Override
		protected void initialize() throws Exception {
			if (Utils.isWildcardFileName(fileName)) {
				availableFiles = searchFiles(fileName);
			} else {
				availableFiles = new File[] { new File(fileName) };
			}

			updateDataTotals(availableFiles);

			File file;
			stateHandler = storeState ? new FileStreamStateHandler(availableFiles, FileLineStream.this.getName())
					: null;
			if (stateHandler != null && stateHandler.isStreamedFileAvailable()) {
				file = stateHandler.getFile();
				lineNumber = stateHandler.getLineNumber();
			} else {
				file = ArrayUtils.isEmpty(availableFiles) ? null
						: startFromLatestActivity ? availableFiles[availableFiles.length - 1] : availableFiles[0];
			}

			setFileToRead(file);

			if (startFromLatestActivity && fileToRead != null) {
				lastModifTime = fileToRead.lastModified();
				lineNumber = Utils.countLines(new FileReader(fileToRead));
			}
		}

		/**
		 * Reads defined file changes since last read iteration (or from stream
		 * initialization if it is first monitor invocation).
		 * <p>
		 * Monitor checks if it can read defined file. If not then tries to swap
		 * to next available file. If swap can't be done (no newer readable
		 * file) then file monitoring is interrupted.
		 * <p>
		 * If defined file is readable, then monitor checks modification
		 * timestamp. If it is newer than {@link #lastModifTime} value, file
		 * gets opened for reading. If not, monitor tries to swap to next
		 * available file. If swap can'e be done (no newer readable file) then
		 * file reading is skipped until next monitor invocation.
		 * <p>
		 * When file gets opened for reading reader is rolled to file marked by
		 * {@link #lineNumber} attribute. If turns out that file got smaller in
		 * lines count, then monitor tries to swap to previous file. If no
		 * previous readable file is available, then reader is reset to first
		 * file line.
		 * <p>
		 * Reader reads all file lines until end of file and puts them to
		 * changed lines buffer.
		 */
		@Override
		protected void readFileChanges() {
			LOGGER.log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "FileLineStream.reading.changes"),
					fileToRead.getAbsolutePath());

			if (!fileToRead.canRead()) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"FileLineStream.cant.access"));

				boolean swapped = swapToNextFile();

				if (!swapped) {
					LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"FileLineStream.next.not.found"));

					shutdown();
					return;
				}
			} else {
				long flm = fileToRead.lastModified();
				if (flm > lastModifTime) {
					LOGGER.log(OpLevel.DEBUG,
							StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
									"FileLineStream.file.updated"),
							TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - flm),
							TimeUnit.MILLISECONDS.toSeconds(flm - lastModifTime));

					lastModifTime = flm;
				} else {
					boolean swapped = swapToNextFile();

					if (!swapped) {
						LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
								"FileLineStream.no.changes"));

						return;
					}
				}
			}

			LineNumberReader lnr = null;

			try {
				lnr = rollToCurrentLine();
			} catch (IOException exc) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"FileLineStream.error.rolling"), exc);
			}

			if (lnr != null) {
				try {
					readNewFileLines(lnr);
				} catch (IOException exc) {
					LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"FileLineStream.error.reading"), exc);
				} finally {
					Utils.close(lnr);
				}
			}

			LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"FileLineStream.changes.read.end"), fileToRead.getAbsolutePath());
		}

		private LineNumberReader rollToCurrentLine() throws IOException {
			LineNumberReader lnr;
			try {
				lnr = new LineNumberReader(new FileReader(fileToRead));
			} catch (Exception exc) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"FileLineStream.reader.error"));

				shutdown();
				return null;
			}

			return skipOldLines(lnr);
		}

		private LineNumberReader skipOldLines(LineNumberReader lnr) throws IOException {
			lnr.mark(0);
			boolean skipFail = false;

			for (int i = 0; i < lineNumber; i++) {
				if (lnr.readLine() == null) {
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"FileLineStream.file.shorter"));

					skipFail = true;
					break;
				}
			}

			if (skipFail) {
				boolean swapped = swapToPrevFile();

				if (swapped) {
					Utils.close(lnr);

					return rollToCurrentLine();
				} else {
					if (lnr.markSupported()) {
						LOGGER.log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
								"FileLineStream.resetting.reader"), 0);

						lnr.reset();
					}
				}
			}

			return lnr;
		}

		private boolean swapToPrevFile() {
			if (Utils.isWildcardFileName(fileName)) {
				availableFiles = searchFiles(fileName);
				updateDataTotals(availableFiles);

				File prevFile = availableFiles == null || availableFiles.length < 2 ? null
						: availableFiles[availableFiles.length - 2];

				if (prevFile != null) {
					setFileToRead(prevFile);
					lastModifTime = prevFile.lastModified();

					LOGGER.log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"FileLineStream.changing.to.previous"), prevFile.getAbsolutePath());
				}

				return prevFile != null;
			}

			return false;
		}

		private boolean swapToNextFile() {
			if (Utils.isWildcardFileName(fileName)) {
				availableFiles = searchFiles(fileName);
				updateDataTotals(availableFiles);

				File nextFile = Utils.getFirstNewer(availableFiles, lastModifTime);

				if (nextFile != null) {
					setFileToRead(nextFile);
					lastModifTime = nextFile.lastModified();
					lineNumber = 0;

					LOGGER.log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"FileLineStream.changing.to.next"), nextFile.getAbsolutePath());
				}

				return nextFile != null;
			}

			return false;
		}

		private void updateDataTotals(File[] activityFiles) {
			int[] totals = getFilesTotals(activityFiles);
			totalBytesCount = totals[0];
			totalLinesCount = totals[1];
		}
	}
}