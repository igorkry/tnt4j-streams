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
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.lang.ArrayUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a HDFS files activity stream, where each line of the file is
 * assumed to represent a single activity or event which should be recorded.
 * Files to stream are defined using "FileName" property in stream
 * configuration.
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>FileName - URI of concrete file name or file name pattern defined using
 * characters '*' and '?'. (Required)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see FilenameUtils#wildcardMatch(String, String, IOCase)
 */
public class HdfsFileLineStream extends AbstractFileLineStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(HdfsFileLineStream.class);

	private Path[] activityFiles = null;
	private FileSystem fs = null;

	/**
	 * Constructs a new FileLineStream.
	 */
	public HdfsFileLineStream() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanup() {
		Utils.close(fs);

		super.cleanup();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadFiles() throws Exception {
		final URI fileUri = new URI(fileName);
		if (fs == null) {
			fs = FileSystem.get(fileUri, new Configuration());
		}
		Path filePath = new Path(fileUri);

		if (Utils.isWildcardFileName(fileName)) {
			activityFiles = searchFiles(filePath, fs);
		} else {
			activityFiles = new Path[] { filePath };
		}

		int[] totals = getFilesTotals(fs, activityFiles);
		totalBytesCount = totals[0];
		totalLinesCount = totals[1];
	}

	private static int[] getFilesTotals(FileSystem fs, Path[] activityFiles) {
		int tbc = 0;
		int tlc = 0;
		if (ArrayUtils.isNotEmpty(activityFiles)) {
			for (Path f : activityFiles) {
				try {
					ContentSummary cSummary = fs.getContentSummary(f);
					tbc += cSummary.getLength();
					tlc += Utils.countLines(new InputStreamReader(fs.open(f)));
				} catch (IOException exc) {
				}
			}
		}

		return new int[] { tbc, tlc };
	}

	/**
	 * Searches for files matching name pattern. Name pattern also may contain
	 * path of directory, where file search should be performed i.e.
	 * C:/Tomcat/logs/localhost_access_log.*.txt. If no path is defined (just
	 * file name pattern) then files are searched in
	 * {@code System.getProperty("user.dir")}. Files array is ordered by file
	 * create timestamp in descending order.
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
	 * {@inheritDoc}
	 */
	protected boolean isFileAvailable(int fileNumber) {
		return activityFiles != null && fileNumber < activityFiles.length;
	}

	/**
	 * {@inheritDoc}
	 */
	protected Reader getFileReader(int fileNumber) throws IOException {
		return new InputStreamReader(fs.open(activityFiles[fileNumber]));
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getFileName(int fileNumber) {
		return activityFiles[fileNumber].getName();
	}

	/**
	 * For testing purposes.
	 *
	 * Sets file system object for stream to use.
	 *
	 * @param fs
	 *            file system object
	 */
	protected void setFs(FileSystem fs) {
		this.fs = fs;
	}
}
