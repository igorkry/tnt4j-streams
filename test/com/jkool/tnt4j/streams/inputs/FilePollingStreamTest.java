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

import java.io.File;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.inputs.AbstractFilePollingStream.LogWatcher;
import com.jkool.tnt4j.streams.utils.TestFileList;

/**
 * @author akausinis
 * @version 1.0
 */
public class FilePollingStreamTest {

	@Test
	@SuppressWarnings({ "serial", "unchecked", "rawtypes" })
	public void searchFilesTest() throws Throwable {
		TestFileList files = new TestFileList();
		FilePollingStream fps = new FilePollingStream();
		int count = TestFileList.TEST_FILE_LIST_SIZE;
		final String fileName = files.get(0).getParentFile() + File.separator + "TEST*";

		Collection<Map.Entry<String, String>> props = new ArrayList<Map.Entry<String, String>>() {
			{
				add(new AbstractMap.SimpleEntry(StreamsConfig.PROP_FILENAME, fileName));
			}
		};

		fps.setProperties(props);
		fps.initialize();
		final LogWatcher logWatcher = fps.createLogWatcher();
		// TODO assert smth

		fps.cleanup();
		files.cleanup();
	}

}
