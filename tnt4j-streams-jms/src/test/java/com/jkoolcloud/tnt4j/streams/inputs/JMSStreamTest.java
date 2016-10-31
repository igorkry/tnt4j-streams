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

import static com.jkoolcloud.tnt4j.streams.TestUtils.testPropertyList;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.JMSStreamConstants;

/**
 * @author akausinis
 * @version 1.0
 */
public class JMSStreamTest {
	JMSStream input;

	@Test
	public void testProperties() throws Exception {
		input = new JMSStream();
		Map<String, String> props = new HashMap<String, String>(4);
		props.put(StreamProperties.PROP_SERVER_URI, "localhost"); // NON-NLS
		props.put(StreamProperties.PROP_QUEUE_NAME, "test"); // NON-NLS
		props.put(StreamProperties.PROP_JNDI_FACTORY, "JNDI"); // NON-NLS
		props.put(JMSStreamConstants.PROP_JMS_CONN_FACTORY, "JMS"); // NON-NLS
		input.setProperties(props.entrySet());
		testPropertyList(input, props.entrySet());
	}

	// @Test
	// public void testInitialize() throws Exception {
	// testProperties();
	// input.startStream();
	// assertTrue(input.jmsDataReceiver.isAlive());
	// }
}
