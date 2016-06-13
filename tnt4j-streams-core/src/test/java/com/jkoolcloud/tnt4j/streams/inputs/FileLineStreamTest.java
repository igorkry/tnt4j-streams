/*
 * Copyright 2014-2016 JKOOL, LLC.
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.PropertiesTestBase;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.inputs.AbstractFileLineStream.FileWatcher;
import com.jkoolcloud.tnt4j.streams.utils.TestFileList;

/**
 * @author akausinis
 * @version 1.0
 */
public class FileLineStreamTest extends PropertiesTestBase {

	FileLineStream fls;

	@Before
	public void prepareTest() {
		fls = new FileLineStream();

		StreamThread st = mock(StreamThread.class);
		st.setName("FileLineStreamTestThreadName");
		fls.setOwnerThread(st);
	}

	@Test
	@SuppressWarnings({ "serial", "unchecked" })
	public void searchFilesTest() throws Exception {
		TestFileList files = new TestFileList(false);

		final String fileName = files.get(0).getParentFile() + File.separator + files.getPrefix() + "*.TST";

		Collection<Map.Entry<String, String>> props = getPropertyList().add(StreamProperties.PROP_FILENAME, fileName)
				.add(StreamProperties.PROP_RESTORE_STATE, "false").build();
		fls.setProperties(props);
		fls.initialize();
		final FileWatcher fileWatcher = fls.createFileWatcher();
		// TODO assert smth
		fileWatcher.shutdown();

		fls.cleanup();
		files.cleanup();
	}

	@Test
	public void testProperties() throws Exception {
		TestFileList testFiles = new TestFileList(true);
		Collection<Map.Entry<String, String>> props = getPropertyList()
				.add(StreamProperties.PROP_FILENAME, testFiles.getWildcardName())
				.add(StreamProperties.PROP_START_FROM_LATEST, "false").add(StreamProperties.PROP_FILE_READ_DELAY, "0")
				.add(StreamProperties.PROP_FILE_POLLING, "false").add(StreamProperties.PROP_RESTORE_STATE, "false")
				.add(StreamProperties.PROP_USE_EXECUTOR_SERVICE, "false").build();
		fls.setProperties(props);
		testPropertyList(fls, props);
		fls.initialize();
		assertEquals("TEST0", fls.getNextItem().toString());
		assertEquals("TEST1", fls.getNextItem().toString());
		assertEquals("TEST2", fls.getNextItem().toString());
		assertEquals("TEST3", fls.getNextItem().toString());
		assertEquals("TEST4", fls.getNextItem().toString());
	}

}
