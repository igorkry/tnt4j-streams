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

package com.jkool.tnt4j.streams.configure;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author akausinis
 * @version 1.0
 */
public class StreamsConfigTest {
	private static final boolean DEFAULT_CONFIG = true;
	private static final String TEST_FILE_NAME = ".\\samples\\http-stream\\tnt-data-source.xml";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File testfile = new File(TEST_FILE_NAME);
		assertTrue("Test configuration failure: XML file doesn't exist ", testfile.exists());
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testStreamsConfig() throws SAXException, ParserConfigurationException, IOException {
		StreamsConfig streamsConfig = new StreamsConfig();
		assertNotNull(streamsConfig);
		isStreamsAndParsersLoaded(streamsConfig);
	}

	private void isStreamsAndParsersLoaded(StreamsConfig streamsConfig) {
		if (DEFAULT_CONFIG) {
			assertNotNull("Has no parsers", streamsConfig.getParsers());
			assertNotNull("Has no parsers", streamsConfig.getStreams());
		}
	}

	@Test
	public void testStreamsConfigConfigWithFileName() throws SAXException, ParserConfigurationException, IOException {
		StreamsConfig streamsConfig = new StreamsConfig(TEST_FILE_NAME);
		assertNotNull(streamsConfig);
		isStreamsAndParsersLoaded(streamsConfig);
	}

	@Test
	public void testStreamsConfigSAXException() throws SAXException, ParserConfigurationException, IOException {
		final File tempFile = File.createTempFile("testStreams", null);
		File configFile = new File(TEST_FILE_NAME);
		FileWriter fw = new FileWriter(tempFile);
		FileReader fr = new FileReader(configFile);
		final int filSize = new Long(configFile.length()).intValue();
		char[] buffer = new char[filSize];
		fr.read(buffer);
		fw.write(buffer);
		fw.write("ERROR<>");
		com.jkool.tnt4j.streams.utils.Utils.close(fw);
		com.jkool.tnt4j.streams.utils.Utils.close(fr);
		new StreamsConfig(TEST_FILE_NAME);
	}

	@Test(expected = IOException.class)
	public void testStreamsConfigConfigWithFileNameNotFound()
			throws SAXException, ParserConfigurationException, IOException {
		new StreamsConfig(TEST_FILE_NAME + "ERROR");
	}

	@Test
	public void testStreamsConfigFile() throws SAXException, ParserConfigurationException, IOException {
		File configFile = new File(TEST_FILE_NAME);
		StreamsConfig streamsConfig = new StreamsConfig(configFile);
		assertNotNull(streamsConfig);
		isStreamsAndParsersLoaded(streamsConfig);
	}

	@Test
	public void testStreamsConfigReader() throws SAXException, ParserConfigurationException, IOException {
		File configFile = new File(TEST_FILE_NAME);
		FileReader fileReader = new FileReader(configFile);
		StreamsConfig streamsConfig = new StreamsConfig(fileReader);
		assertNotNull(streamsConfig);
		isStreamsAndParsersLoaded(streamsConfig);
	}

}
