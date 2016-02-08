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

package com.jkool.tnt4j.streams.samples.custom;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.Map;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Sample custom stream.
 */
public class SampleStream extends TNTInputStream<String> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(SampleStream.class);

	private String fileName;
	private File activityFile;
	private LineNumberReader lineReader;
	private int lineNumber;

	private SampleStream() {
		super(LOGGER);
	}

	/**
	 * Get value of specified property.
	 *
	 * @param name
	 *            name of property whose value is to be retrieved
	 *
	 * @return value for property, or {@code null} if property does not exist
	 */
	@Override
	public Object getProperty(String name) {
		if (StreamsConfig.PROP_FILENAME.equalsIgnoreCase(name)) {
			return fileName;
		}
		return super.getProperty(name);
	}

	/**
	 * Set properties for activity stream.
	 *
	 * @param props
	 *            properties to set
	 *
	 * @throws Exception
	 *             indicates error with properties
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
	 * Initialize the stream.
	 *
	 * @throws Exception
	 *             indicates that stream is not configured properly and cannot
	 *             continue.
	 */
	@Override
	public void initialize() throws Exception {
		super.initialize();
		if (fileName == null) {
			throw new IllegalStateException("SampleStream: File name not defined");
		}
		LOGGER.log(OpLevel.DEBUG, "Opening file: {0}", fileName);
		activityFile = new File(fileName);
		lineReader = new LineNumberReader(new FileReader(activityFile));
	}

	/**
	 * Returns the next line in the file.
	 *
	 * @returns string containing next line in file.
	 */
	@Override
	public String getNextItem() throws Exception {
		if (lineReader == null) {
			throw new IllegalStateException("SampleStream: File is not opened for reading");
		}
		String line = lineReader.readLine();
		lineNumber = lineReader.getLineNumber();
		return line;
	}

	/**
	 * Returns line number of the file last read.
	 */
	@Override
	public int getActivityPosition() {
		return lineNumber;
	}

	/**
	 * Closes file being read.
	 */
	@Override
	protected void cleanup() {
		Utils.close(lineReader);

		lineReader = null;
		activityFile = null;

		super.cleanup();
	}
}
