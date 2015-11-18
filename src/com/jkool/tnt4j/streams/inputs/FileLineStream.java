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

import org.apache.commons.io.filefilter.WildcardFileFilter;

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
 * @version $Revision: 3 $
 * @see ActivityParser#isDataClassSupported(Object)
 * @see WildcardFileFilter#WildcardFileFilter(String)
 */
public class FileLineStream extends TNTInputStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(FileLineStream.class);

	private String fileName = null;
	private File[] activityFiles = null;
	private LineNumberReader lineReader = null;

	private int fileNumber = -1;
	private int lineNumber = 0;

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
	public Object getProperty(String name) {
		if (StreamsConfig.PROP_FILENAME.equalsIgnoreCase(name)) {
			return fileName;
		}
		return super.getProperty(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		if (props == null) {
			return;
		}
		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamsConfig.PROP_FILENAME.equalsIgnoreCase(name)) {
				fileName = value;
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
		if (Utils.isWildcardFileName(fileName)) {
			activityFiles = searchFiles(fileName);
		} else {
			activityFiles = new File[] { new File(fileName) };
		}

		hasNextFile();
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
	 * @return files with name matching name pattern ordered by file create
	 *         timestamp in descending order.
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
	 * <p>
	 * This method returns a string containing the contents of the next line in
	 * the file.
	 * </p>
	 */
	@Override
	public Object getNextItem() throws Throwable {
		if (lineReader == null) {
			throw new IllegalStateException(StreamsResources.getString("FileLineStream.file.not.opened"));
		}

		String line = lineReader.readLine();
		lineNumber = lineReader.getLineNumber();

		if (line == null && hasNextFile()) {
			line = (String) getNextItem();
		}

		return line;
	}

	/**
	 * Returns {@code true} if the stream configuration defined activity files
	 * array has more files.
	 *
	 * @return {@code true} if there is more activity files available
	 *
	 * @throws IOException
	 *
	 * @see FileReader#FileReader(File)
	 */
	private boolean hasNextFile() throws IOException {
		fileNumber++;
		if (activityFiles != null && fileNumber < activityFiles.length) {
			File activityFile = activityFiles[fileNumber];
			lineReader = new LineNumberReader(new FileReader(activityFile));
			lineNumber = 0;
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("FileLineStream.opening.file"),
					activityFile.getName());

			return true;
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns line number of the file last read.
	 * </p>
	 */
	@Override
	public int getActivityPosition() {
		return lineNumber;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanup() {
		Utils.close(lineReader);

		lineReader = null;
		activityFiles = null;

		super.cleanup();
	}
}
