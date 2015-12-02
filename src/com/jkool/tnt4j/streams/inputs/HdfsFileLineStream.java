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
import java.io.Reader;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import com.jkool.tnt4j.streams.parsers.ActivityParser;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a HDFS files activity stream, where each line of the file is
 * assumed to represent a single activity or event which should be recorded.
 * Files to stream are defined using "FileName" property in stream
 * configuration.
 * </p>
 * <p>
 * This activity stream requires parsers that can support {@code String} data.
 * </p>
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>FileName - URI of concrete file name or file name pattern defined using
 * characters '*' and '?'</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see ActivityParser#isDataClassSupported(Object)
 * @see FilenameUtils#wildcardMatch(String, String, IOCase)
 */
public class HdfsFileLineStream extends AbstractFileLineStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(HdfsFileLineStream.class);

	private Path[] activityFiles = null;
	private FileSystem fs = null;

	/**
	 * Constructs an FileLineStream.
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
		fs = FileSystem.get(fileUri, new Configuration());
		Path filePath = new Path(fileUri);

		if (Utils.isWildcardFileName(fileName)) {
			activityFiles = searchFiles(filePath, fs);
		} else {
			activityFiles = new Path[] { filePath };
		}

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
}
