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

import java.io.OutputStream;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CharacterStreamTest {
	private static final int PORT = (int) (Math.random() * (1 << 16));

	@Test
	public void settingsTest() throws Exception {
		CharacterStream cStream = new CharacterStream();

		InputPropertiesTestUtils.testInputPropertySetAndGet(cStream, StreamProperties.PROP_PORT, PORT);
		InputPropertiesTestUtils.testInputPropertySetAndGet(cStream, StreamProperties.PROP_HALT_ON_PARSER, false);

		cStream.cleanup();
	}

	@Test(expected = IllegalStateException.class)
	public void settingsFailToSetBothTest() throws Exception {
		CharacterStream cStream = new CharacterStream();

		InputPropertiesTestUtils.testInputPropertySetAndGet(cStream, StreamProperties.PROP_FILENAME, "TestFileName");
		InputPropertiesTestUtils.testInputPropertySetAndGet(cStream, StreamProperties.PROP_PORT, PORT);

		cStream.cleanup();
	}

	@Test
	public void startDataStreamTest() throws Exception {
		CharacterStream cStream = new CharacterStream();
		cStream.setName("TEST_CHAR_STREAM");

		Collection<Map.Entry<String, String>> props = new ArrayList<Map.Entry<String, String>>();
		props.add(new AbstractMap.SimpleEntry<String, String>(StreamProperties.PROP_PORT, String.valueOf(PORT)));
		props.add(new AbstractMap.SimpleEntry<String, String>(StreamProperties.PROP_HALT_ON_PARSER,
				String.valueOf(false)));
		cStream.setProperties(props);

		StreamThread thread = new StreamThread(cStream);
		thread.start();

		Thread.sleep(250);
		Socket socket = new Socket("localhost", PORT);
		final OutputStream outputStream = socket.getOutputStream();
		outputStream.write(55);
		outputStream.flush();

		Utils.close(outputStream);
		Utils.close(socket);

		Thread.sleep(50);

		cStream.halt();

		Thread.sleep(50);

		assertEquals("No activities processed", 0, cStream.getCurrentActivity() - cStream.getSkippedActivitiesCount());
	}
}
