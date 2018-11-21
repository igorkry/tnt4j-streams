/*
 * Copyright 2014-2018 JKOOL, LLC.
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

import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;

/**
 * Implements a JSR-203 compliant {@link java.nio.file.FileSystem} provided files lines activity stream, where each line
 * of the file is assumed to represent a single activity or event which should be recorded. Stream reads changes from
 * defined files every "FileReadDelay" property defined seconds (default is 15sec.).
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 * <p>
 * List of supported file systems can be found in {@link com.jkoolcloud.tnt4j.streams.inputs.FileSystemAdapter}
 * documentation.
 * <p>
 * This activity stream supports configuration properties from {@link FileLineStream} (and higher hierarchy streams) in
 * combination with properties from {@link com.jkoolcloud.tnt4j.streams.inputs.FileSystemAdapter}.
 *
 * @version $Revision: 1 $
 *
 * @see FileSystemAdapter
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class FileSystemLineStream extends FileLineStream {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(FileSystemLineStream.class);

	private FileSystemAdapter fsAdapter;

	/**
	 * Constructs a new FileSystemLineStream.
	 */
	public FileSystemLineStream() {
		super();

		fsAdapter = new FileSystemAdapter();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public void setProperty(String name, String value) {
		super.setProperty(name, value);

		fsAdapter.setProperty(name, value);
	}

	@Override
	public Object getProperty(String name) {
		Object pVal = fsAdapter.getProperty(name);
		return pVal == null ? super.getProperty(name) : pVal;
	}

	@Override
	protected FileWatcher createFileWatcher() throws Exception {
		return new CommonFileWatcher(fsAdapter.getFileSystem());
	}

	@Override
	public void initializeFs() throws Exception {
		super.initializeFs();

		fileName = fsAdapter.initialize(fileName);
	}
}
