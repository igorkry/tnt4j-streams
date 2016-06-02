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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.TestFileList;

/**
 * @author akausinis
 * @version 1.0
 */
public class ZipLineStreamTest extends InputTestBase {

	ZipLineStream zs;

	@Before
	public void prepare() {
		zs = new ZipLineStream();
	}

	@Test
	public void testProperties() throws Exception {
		final Collection<Entry<String, String>> properties = getPropertyList()
				.add(StreamProperties.PROP_FILENAME, "test.zip").add(StreamProperties.PROP_ARCH_TYPE, "ZIP").build();
		zs.setProperties(properties);
		testPropertyList(zs, properties);
	}

	@Test
	public void initializeTest() throws Exception {
		TestFileList testFiles = new TestFileList(true);
		byte[] buffer = new byte[1024];
		final File zipFile = File.createTempFile("testZip", ".zip");
		zipFile.deleteOnExit();
		OutputStream os = new FileOutputStream(zipFile);
		ZipOutputStream zos = new ZipOutputStream(os);
		for (File testfile : testFiles) {
			final ZipEntry zipEntry = new ZipEntry(testfile.getName());
			zos.putNextEntry(zipEntry);
			FileInputStream fis = new FileInputStream(testfile);
			int length;
			while ((length = fis.read(buffer)) > 0) {
				zos.write(buffer, 0, length);
			}
			zipEntry.setSize(buffer.length);
			zos.closeEntry();
			fis.close();
		}
		zos.close();
		zs.setProperties(getPropertyList().add(StreamProperties.PROP_FILENAME, zipFile.getAbsolutePath())
				.add(StreamProperties.PROP_ARCH_TYPE, "ZIP").build());
		zs.initialize();
		assertEquals("TEST0", zs.getNextItem().toString());
		assertEquals("TEST1", zs.getNextItem().toString());
		assertEquals("TEST2", zs.getNextItem().toString());
		assertEquals("TEST3", zs.getNextItem().toString());
		assertEquals("TEST4", zs.getNextItem().toString());
		zs.cleanup();
	}

}
