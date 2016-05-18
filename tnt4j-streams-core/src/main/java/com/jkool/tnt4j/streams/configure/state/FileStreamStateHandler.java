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

package com.jkool.tnt4j.streams.configure.state;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import javax.xml.bind.JAXBException;

import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * Implements streamed files access state handler.
 *
 * @version $Revision: 1 $
 */
public class FileStreamStateHandler extends AbstractFileStreamStateHandler<File> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(FileStreamStateHandler.class);

	/**
	 * Constructs a new FileStreamStateHandler.
	 */
	FileStreamStateHandler() {
		super(LOGGER);
	}

	/**
	 * Constructs a new FileStreamStateHandler. Performs search of persisted
	 * streaming state and loads it if such is available.
	 *
	 * @param activityFiles
	 *            files processed by stream
	 * @param streamName
	 *            stream name
	 */
	public FileStreamStateHandler(File[] activityFiles, String streamName) {
		super(LOGGER, activityFiles, streamName);
	}

	@Override
	String getParent(File[] activityFiles) {
		return activityFiles[0].getParent();
	}

	@Override
	Reader openFile(File file) throws IOException {
		return new FileReader(file);
	}

	@Override
	public boolean isStreamedFileAvailable() {
		return file != null && file.exists() && file.isFile();
	}

	@Override
	FileAccessState loadState(String path, String streamName) throws IOException, JAXBException {
		// Case 1: state file in same dir as streamed files
		FileAccessState fas = loadStateFile(path, streamName);
		if (fas == null) {
			// Case 2: state file is in temp. dir
			fas = super.loadState(path, streamName);
		}

		return fas;
	}
}
