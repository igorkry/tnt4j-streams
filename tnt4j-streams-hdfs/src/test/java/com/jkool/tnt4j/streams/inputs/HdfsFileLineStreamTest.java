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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.*;

import org.apache.hadoop.fs.*;
import org.junit.Test;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.utils.HdfsStreamConstants;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.TestFileList;

/**
 * @author akausinis
 * @version 1.0
 */
public class HdfsFileLineStreamTest {

	@Test()
	public void test() throws Throwable {
		FileSystem fs = mock(FileSystem.class);
		HdfsFileLineStream stream = new HdfsFileLineStream();

		TestFileList files = new TestFileList();

		final String fileName = (files.get(0).getParentFile() + File.separator + "TEST*").replace("\\", "/");

		Collection<Map.Entry<String, String>> props = new ArrayList<Map.Entry<String, String>>() {
			{
				add(new AbstractMap.SimpleEntry(StreamsConfig.PROP_FILENAME, fileName));
			}
		};

		when(fs.open(any(Path.class))).thenReturn(mock(FSDataInputStream.class));
		final FileStatus fileStatusMock = mock(FileStatus.class);
		final FileStatus[] array = new FileStatus[10];
		Arrays.fill(array, fileStatusMock);
		when(fs.listStatus(any(Path.class), any(PathFilter.class))).thenReturn(array);
		when(fileStatusMock.getModificationTime()).thenReturn(new Long(1L), new Long(2L), 3L);
		when(fileStatusMock.getPath()).thenReturn(mock(Path.class));

		stream.setFs(fs);
		stream.setProperties(props);

		stream.initialize();
		verify(fileStatusMock, atLeastOnce()).getModificationTime();
		verify(fileStatusMock, atLeastOnce()).getPath();
		verify(fs, atLeastOnce()).listStatus(any(Path.class), any(PathFilter.class));

		stream.cleanup();
	}

	@Test
	public void testRB() {
		// String keyModule = "ZorkaConnector.received.null.hello.packet";
		String keyCore = "ActivityField.field.type.name.empty";

		// String rbs1 =
		// StreamsResources.getString(HdfsStreamConstants.RESOURCE_BUNDLE_HDFS,
		// keyModule);
		// assertNotEquals("Hdfs resource bundle entry not found", rbs1,
		// keyModule);
		// rbs1 =
		// StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
		// keyModule);
		// assertEquals("Hdfs resource bundle entry found in core", rbs1,
		// keyModule);
		String rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, keyCore);
		assertNotEquals("Core resource bundle entry not found", rbs1, keyCore);
		rbs1 = StreamsResources.getString(HdfsStreamConstants.RESOURCE_BUNDLE_HDFS, keyCore);
		assertEquals("Core resource bundle entry found in hdfs", rbs1, keyCore);
	}

}
