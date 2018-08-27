/*
 * Copyright 2014-2018 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.utils;

import static org.junit.Assert.*;

import java.security.MessageDigest;

import org.junit.Test;

import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.streams.parsers.MessageType;

/**
 * @author akausinis
 * @version 1.0
 */
public class WmqUtilsTest {

	@Test
	public void testComputeSignature() throws Exception {
		String expectedSignature = "ecFrCSkZqXWsnKJGGUIliA==";

		String sigMD5 = WmqUtils.computeSignature(MessageType.REQUEST, "MSG_FORMAT", "MSG_ID".getBytes(),
				"USER_ID".toLowerCase(), // NON-NLS
				"APPL_TYPE", "APPL_NAME", "2016-04-18", "13:17:25", "xxxyyyzzz".getBytes()); // NON-NLS

		assertEquals("MD5 signature does not match expected initial value", expectedSignature, sigMD5);

		MessageDigest msgDig = MessageDigest.getInstance("SHA1"); // NON-NLS
		String sigOther = WmqUtils.computeSignature(msgDig, MessageType.REQUEST, "MSG_FORMAT", "MSG_ID".getBytes(), // NON-NLS
				"USER_ID".toLowerCase(), "APPL_TYPE", "APPL_NAME", "2016-04-18", "13:17:25", "xxxyyyzzz".getBytes()); // NON-NLS

		assertNotEquals("Messages signatures should not match", sigMD5, sigOther);

		msgDig = MessageDigest.getInstance("MD5"); // NON-NLS
		sigOther = WmqUtils.computeSignature(msgDig, MessageType.REQUEST, "MSG_FORMAT", "MSG_ID".getBytes(),
				"USER_ID".toLowerCase(), // NON-NLS
				"APPL_TYPE", "APPL_NAME", "2016-04-18", "13:17:25", "xxxyyyzzz".getBytes()); // NON-NLS

		assertEquals("Messages signatures should match", sigMD5, sigOther);
	}

	@Test
	public void testComputeSignatureValueNull() throws Exception {
		assertNull(WmqUtils.computeSignature(null, ",", DefaultEventSinkFactory.defaultEventSink(WmqUtilsTest.class))); // NON-NLS

	}
}
