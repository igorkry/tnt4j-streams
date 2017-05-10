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

import java.io.*;
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
 * Implements a files lines activity stream, where each line of the file is assumed to represent a single activity or
 * event which should be recorded. Stream reads changes from defined files every "FileReadDelay" property defined
 * seconds (default is 15sec.).
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 * <p>
 * This activity stream supports properties from {@link AbstractFileLineStream} (and higher hierarchy streams).
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
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	protected FileWatcher createFileWatcher() {
		return new CommonFileWatcher();
	}

	private static int[] getFilesTotals(File[] activityFiles) {
		int tbc = 0;
		int tlc = 0;
		if (ArrayUtils.isNotEmpty(activityFiles)) {
			for (File f : activityFiles) {
				tbc += f.length();
				try {
					tlc += Utils.countLines(new FileInputStream(f));
				} catch (IOException exc) {
				}
			}
		}

		return new int[] { tbc, tlc };
	}

	/**
	 * Files changes watcher thread. It reads changes from defined files using last modification timestamp of file.
	 */
	private class CommonFileWatcher extends FileWatcher {

		/**
		 * Constructs a new CommonFileWatcher.
		 */
		CommonFileWatcher() {
			super("FileLineStream.FileWatcher"); // NON-NLS
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
			availableFiles = Utils.listFilesByName(fileName);

			updateDataTotals(availableFiles);

			File file;
			stateHandler = storeState ? new FileStreamStateHandler(availableFiles, FileLineStream.this.getName())
					: null;
			if (isStoredStateAvailable()) {
				file = stateHandler.getFile();
				lineNumber = stateHandler.getLineNumber();
				// lastModifTime = stateHandler.getReadTime();
			} else {
				file = ArrayUtils.isEmpty(availableFiles) ? null
						: startFromLatestActivity ? availableFiles[availableFiles.length - 1] : availableFiles[0];
			}

			setFileToRead(file);

			if (startFromLatestActivity && fileToRead != null) {
				lastModifTime = fileToRead.lastModified();
				lineNumber = Utils.countLines(new FileInputStream(fileToRead));
			}
		}

		/**
		 * Reads defined file changes since last read iteration (or from stream initialization if it is first monitor
		 * invocation).
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
			logger().log(OpLevel.DEBUG,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "FileLineStream.reading.changes"),
					fileToRead.getAbsolutePath());

			if (!fileToRead.canRead()) {
				logger().log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"FileLineStream.cant.access"));

				boolean swapped = swapToNextFile();

				if (!swapped) {
					logger().log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"FileLineStream.next.not.found"));

					shutdown();
					return;
				}
			} else {
				long flm = fileToRead.lastModified();
				if (flm > lastModifTime) {
					logger().log(OpLevel.DEBUG,
							StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
									"FileLineStream.file.updated"),
							TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - flm),
							getLastReadTimeToLog(flm));

					lastModifTime = flm;
				} else {
					boolean swapped = swapToNextFile();

					if (!swapped) {
						logger().log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
								"FileLineStream.no.changes"));

						return;
					}
				}
			}

			LineNumberReader lnr = null;

			try {
				lnr = rollToCurrentLine();
			} catch (IOException exc) {
				logger().log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"FileLineStream.error.rolling"), exc);
			}

			if (lnr != null) {
				try {
					readNewFileLines(lnr);
				} catch (IOException exc) {
					logger().log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"FileLineStream.error.reading"), exc);
				} finally {
					Utils.close(lnr);
				}
			}

			logger().log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
					"FileLineStream.changes.read.end"), fileToRead.getAbsolutePath());
		}

		private LineNumberReader rollToCurrentLine() throws IOException {
			LineNumberReader lnr;
			try {
				lnr = new LineNumberReader(new FileReader(fileToRead));
			} catch (Exception exc) {
				logger().log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
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
					logger().log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
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
						logger().log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
								"FileLineStream.resetting.reader"), 0);

						lnr.reset();
					}
				}
			}

			return lnr;
		}

		private boolean swapToPrevFile() {
			if (Utils.isWildcardString(fileName)) {
				availableFiles = Utils.searchFiles(fileName);
				updateDataTotals(availableFiles);

				File prevFile = ArrayUtils.getLength(availableFiles) < 2 ? null
						: availableFiles[availableFiles.length - 2];

				if (prevFile != null) {
					setFileToRead(prevFile);
					lastModifTime = prevFile.lastModified();

					logger().log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"FileLineStream.changing.to.previous"), prevFile.getAbsolutePath());
				}

				return prevFile != null;
			}

			return false;
		}

		private boolean swapToNextFile() {
			if (Utils.isWildcardString(fileName)) {
				availableFiles = Utils.searchFiles(fileName);
				updateDataTotals(availableFiles);

				File nextFile = Utils.getFirstNewer(availableFiles, lastModifTime);

				if (nextFile != null) {
					setFileToRead(nextFile);
					lastModifTime = nextFile.lastModified();
					lineNumber = 0;

					logger().log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
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