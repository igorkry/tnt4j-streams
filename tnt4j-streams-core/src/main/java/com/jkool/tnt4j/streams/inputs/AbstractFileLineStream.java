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
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Base class for files activity stream, where each line of the file is assumed
 * to represent a single activity or event which should be recorded. Files to
 * stream are defined using "FileName" property in stream configuration.
 * <p>
 * This activity stream requires parsers that can support {@code String} data.
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>FileName - concrete file name or file name pattern defined using
 * characters '*' and '?'. (Required)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public abstract class AbstractFileLineStream extends TNTInputStream<String> {

	/**
	 * Stream attribute defining file name.
	 */
	protected String fileName = null;
	private LineNumberReader lineReader = null;

	private int fileNumber = -1;
	private int lineNumber = 0;

	/**
	 * Constructs a new AbstractFileLineStream.
	 *
	 * @param logger
	 *            logger used by activity stream
	 */
	protected AbstractFileLineStream(EventSink logger) {
		super(logger);
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
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		super.setProperties(props);

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
	public void initialize() throws Exception {
		super.initialize();
		if (StringUtils.isEmpty(fileName)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"TNTInputStream.property.undefined", StreamsConfig.PROP_FILENAME));
		}
		logger.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
				"FileLineStream.initializing.stream", fileName));

		loadFiles();

		hasNextFile();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns a string containing the contents of the next line in
	 * the file.
	 */
	@Override
	public String getNextItem() throws Exception {
		if (lineReader == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"FileLineStream.file.not.opened"));
		}

		String line = lineReader.readLine();
		lineNumber = lineReader.getLineNumber();

		if (line == null && hasNextFile()) {
			line = getNextItem();
		}

		return line;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns line number of the file last read.
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

		super.cleanup();
	}

	/**
	 * Load file descriptors matching {@code fileName} into collection.
	 *
	 * @throws Exception
	 *             if any errors occurred while loading file descriptors
	 */
	protected abstract void loadFiles() throws Exception;

	/**
	 * Returns {@code true} if the stream configuration defined activity files
	 * array has more files.
	 *
	 * @return {@code true} if there is more activity files available
	 *
	 * @throws IOException
	 * @see FileReader#FileReader(File)
	 */
	private boolean hasNextFile() throws IOException {
		fileNumber++;
		if (isFileAvailable(fileNumber)) {
			Utils.close(lineReader);

			lineReader = new LineNumberReader(getFileReader(fileNumber));
			lineNumber = 0;
			logger.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"FileLineStream.opening.file", getFileName(fileNumber)));

			return true;
		}

		return false;
	}

	/**
	 * Checks if file descriptors collection has element with index equal to
	 * fileNumber.
	 *
	 * @param fileNumber
	 *            file number in file descriptors collection
	 *
	 * @return {@code true} if file descriptors collection contains element with
	 *         index equal to fileNumber, {@code false} - if file descriptors
	 *         collection is {@code null} or fileNumber is out if collection
	 *         bounds
	 */
	protected abstract boolean isFileAvailable(int fileNumber);

	/**
	 * Returns {@code Reader} object for a file identified by file number in
	 * files collection.
	 *
	 * @param fileNumber
	 *            file number in file descriptors collection
	 *
	 * @return file reader
	 *
	 * @throws IOException
	 *             if reader can't be initialized.
	 */
	protected abstract Reader getFileReader(int fileNumber) throws IOException;

	/**
	 * Returns name of file identified by file number in files collection.
	 *
	 * @param fileNumber
	 *            file number in file descriptors collection
	 *
	 * @return file name
	 */
	protected abstract String getFileName(int fileNumber);
}
