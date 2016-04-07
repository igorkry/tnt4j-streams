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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.net.Socket;

import org.junit.Test;

import com.jkool.tnt4j.streams.configure.StreamProperties;
import com.jkool.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
public class CharacterStreamTest {
	private static final int PORT = 8182;
	CharacterStream cStream = new CharacterStream();

	@Test
	public void settingsTest() throws Exception {
		InputPropertiesTestUtils.testInputPropertySetAndGet(cStream, StreamProperties.PROP_FILENAME, "TestFileName");
		cStream = new CharacterStream();
		InputPropertiesTestUtils.testInputPropertySetAndGet(cStream, StreamProperties.PROP_PORT, 8080);
	}

	@Test(expected = IllegalStateException.class)
	public void settingsFailToSetBothTest() throws Exception {
		InputPropertiesTestUtils.testInputPropertySetAndGet(cStream, StreamProperties.PROP_FILENAME, "TestFileName");
		InputPropertiesTestUtils.testInputPropertySetAndGet(cStream, StreamProperties.PROP_PORT, 8080);
	}

	@Test
	public void startDataStreamTest() throws Exception {
		// InputStream is = mock(InputStream.class);
		// cStream = new CharacterStream(is);
		InputPropertiesTestUtils.testInputPropertySetAndGet(cStream, StreamProperties.PROP_PORT, PORT);
		StreamThread thread = new StreamThread(cStream);
		thread.start();
		Thread.sleep(250);
		Socket socket = new Socket("localhost", PORT);
		socket.getOutputStream().write(55);
		socket.getOutputStream().flush();
		Utils.close(socket);
		Thread.sleep(250);

		assertEquals("No activities processed", 1, cStream.getCurrentActivity());
		assertTrue("Stream is not closed", cStream.isHalted());

		cStream.cleanup();
	}
}
