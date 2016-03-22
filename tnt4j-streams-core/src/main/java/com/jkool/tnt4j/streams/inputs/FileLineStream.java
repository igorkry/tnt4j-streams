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
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang.ArrayUtils;

import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a files activity stream, where each line of the file is assumed to
 * represent a single activity or event which should be recorded. Files to
 * stream are defined using "FileName" property in stream configuration.
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>FileName - concrete file name or file name pattern defined using
 * characters '*' and '?'</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see WildcardFileFilter#WildcardFileFilter(String)
 */
public class FileLineStream extends AbstractFileLineStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(FileLineStream.class);

	private File[] activityFiles = null;

	/**
	 * Constructs a new FileLineStream.
	 */
	public FileLineStream() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadFiles() {
		if (Utils.isWildcardFileName(fileName)) {
			activityFiles = searchFiles(fileName);
		} else {
			activityFiles = new File[] { new File(fileName) };
		}

		int[] totals = getFilesTotals(activityFiles);
		totalBytesCount = totals[0];
		totalLinesCount = totals[1];
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
	private static File[] searchFiles(String namePattern) {
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
		return new FileReader(activityFiles[fileNumber]);
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getFileName(int fileNumber) {
		return activityFiles[fileNumber].getName();
	}
}
