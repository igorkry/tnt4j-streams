/*
 * Copyright 2014-2017 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.matchers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class XPathMatcherTest {
	private static String xml;

	@BeforeClass
	public static void initXMLData() throws Exception {
		try {
			xml = new String(Files.readAllBytes(Paths.get("./tnt-streams-core/samples/swift/msg.xml")));
		} catch (Exception exc) {
			xml = new String(Files.readAllBytes(Paths.get("./samples/swift/msg.xml")));
		}
	}

	@Test
	public void evaluateTrue() throws Exception {
		assertTrue(Matchers.evaluate("xpath:/Request/header[currency='EUR']/messageId", xml));
		assertTrue(Matchers.evaluate("xpath:boolean(/Request)", xml));
	}

	@Test
	public void evaluateFalse() throws Exception {
		assertFalse(Matchers.evaluate("xpath:/Request/header[currency='USD']/messageId", xml));
		assertFalse(Matchers.evaluate("xpath:boolean(/Request1)", xml));
	}

	@Test
	public void evaluateAdvanced() throws Exception {
		assertFalse(Matchers.evaluate("xpath:/Request/genericTransFields/receiver", xml));
		assertTrue(Matchers.evaluate("xpath:contains(/Request/messageDetails/tag21, 'VOLUME')", xml));
		assertFalse(Matchers.evaluate("xpath:contains(/Request/messageDetails/tag21, 'V0LUME')", xml));
	}
}
