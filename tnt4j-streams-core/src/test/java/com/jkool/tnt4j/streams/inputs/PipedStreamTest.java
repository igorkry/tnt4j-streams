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
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.io.Reader;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class PipedStreamTest {

	@Test(timeout = 1000L)
	public void testPipeFlow() throws Exception {
		System.setIn(new FileInputStream("./samples/piping-stream/orders.log"));

		PipedStream pipedStream = new PipedStream();
		pipedStream.initialize();
		String line = pipedStream.getNextItem();
		pipedStream.cleanup();

		assertNotNull("Next item line is null", line);
		assertEquals("Expected next item line length does not match", 97, line.length());
	}

	@Test(expected = IllegalStateException.class)
	public void testNullRawInput() throws Exception {
		PipedStream pipedStream = new PipedStream((Reader) null);
		pipedStream.initialize();
		pipedStream.cleanup();
	}

	@Test(expected = IllegalStateException.class)
	public void testNextItemAfterCleanup() throws Exception {
		PipedStream pipedStream = new PipedStream();
		pipedStream.initialize();
		pipedStream.cleanup();
		String line = pipedStream.getNextItem();
	}
}