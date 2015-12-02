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
import java.util.Comparator;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.jkool.tnt4j.streams.parsers.ActivityParser;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a files activity stream, where each line of the file is assumed to
 * represent a single activity or event which should be recorded. Files to
 * stream are defined using "FileName" property in stream configuration.
 * </p>
 * <p>
 * This activity stream requires parsers that can support {@code String} data.
 * </p>
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>FileName - concrete file name or file name pattern defined using
 * characters '*' and '?'</li>
 * </ul>
 *
 * @version $Revision: 4 $
 *
 * @see ActivityParser#isDataClassSupported(Object)
 * @see WildcardFileFilter#WildcardFileFilter(String)
 */
public class FileLineStream extends AbstractFileLineStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(FileLineStream.class);

	private File[] activityFiles = null;

	/**
	 * Constructs an FileLineStream.
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
	}

	/**
	 * Searches for files matching name pattern. Name pattern also may contain
	 * path of directory, where file search should be performed i.e.
	 * C:/Tomcat/logs/localhost_access_log.*.txt. If no path is defined (just
	 * file name pattern) then files are searched in
	 * {@code System.getProperty("user.dir")}. Files array is ordered by file
	 * create timestamp in descending order.
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
						return bfa1.creationTime().compareTo(bfa2.creationTime()) * (-1);
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
