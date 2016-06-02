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

import java.io.File;
import java.util.Collection;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;

/**
 * @author akausinis
 * @version 1.0
 */
public class RedirectStreamTest extends InputTestBase {

	RedirectStream rs;

	@Before
	public void prepareTests() {
		rs = new RedirectStream();
	}

	@Test
	public void setPropertiesFileTest() throws Exception {
		final File tempFile = File.createTempFile("Test", ".test");
		tempFile.deleteOnExit();
		final Collection<Entry<String, String>> properties = getPropertyList()
				.add(StreamProperties.PROP_FILENAME, tempFile.getAbsolutePath())
				.add(StreamProperties.PROP_BUFFER_SIZE, "55").add(StreamProperties.PROP_OFFER_TIMEOUT, "10").build();
		rs.setProperties(properties);
		testPropertyList(rs, properties);
		rs.initialize();
	}

	@Test
	public void setPropertiesSocketTest() throws Exception {

		final Collection<Entry<String, String>> properties = getPropertyList().add(StreamProperties.PROP_PORT, "9010")
				.build();
		rs.setProperties(properties);
		testPropertyList(rs, properties);
		rs.initialize();
	}

	@Test(expected = IllegalStateException.class)
	public void setPropertiesSetShouldFailTest() throws Exception {
		final Collection<Entry<String, String>> properties = getPropertyList()
				.add(StreamProperties.PROP_FILENAME, "TestFile").add(StreamProperties.PROP_RESTART_ON_CLOSE, "true")
				.add(StreamProperties.PROP_PORT, "9009").build();
		rs.setProperties(properties);
	}

}
