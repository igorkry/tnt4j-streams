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

import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Test;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.utils.MqttStreamConstants;
import com.jkool.tnt4j.streams.utils.StreamsResources;

/**
 * @author akausinis
 * @version 1.0
 */
public class MqttStreamTest {
	MqttStream input;

	@Test
	public void testProperties() throws Throwable {
		input = new MqttStream();
		InputPropertiesTestUtils.testInputPropertySetAndGet(input, StreamsConfig.PROP_SERVER_URI,
				"tcp://localhost:1883");
		InputPropertiesTestUtils.testInputPropertySetAndGet(input, StreamsConfig.PROP_USERNAME, "");
		InputPropertiesTestUtils.testInputPropertySetAndGet(input, StreamsConfig.PROP_PASSWORD, "");
		InputPropertiesTestUtils.testInputPropertySetAndGet(input, StreamsConfig.PROP_TOPIC_STRING, "TEST");
		InputPropertiesTestUtils.testInputPropertySetAndGet(input, StreamsConfig.PROP_USE_SSL, false);
		InputPropertiesTestUtils.testInputPropertySetAndGet(input, StreamsConfig.PROP_KEYSTORE, "");
		InputPropertiesTestUtils.testInputPropertySetAndGet(input, StreamsConfig.PROP_KEYSTORE_PASS, "");
	}

	@Test(expected = MqttException.class)
	public void testInitialize() throws Throwable {
		testProperties();
		input.initialize();
	}

	@Test(expected = IllegalStateException.class)
	public void testInitializeFailDueToNotAllPropertiesSet() throws Throwable {
		input = new MqttStream();
		input.initialize();
	}

	@Test(expected = IllegalStateException.class)
	public void testInitializeFailDueToNotAllPropertiesSet2() throws Throwable {
		input = new MqttStream();
		input.setProperties(
				InputPropertiesTestUtils.makeTestPropertiesSet(StreamsConfig.PROP_SERVER_URI, "tcp://localhost:1883"));
		input.initialize();
	}

	@Test
	public void testRB() {
		String keyModule = "MqttStream.stream.ready";
		String keyCore = "ActivityField.field.type.name.empty";

		String rbs1 = StreamsResources.getString(MqttStreamConstants.RESOURCE_BUNDLE_MQTT, keyModule);
		assertNotEquals("Mqtt resource bundle entry not found", rbs1, keyModule);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, keyModule);
		assertEquals("Mqtt resource bundle entry found in core", rbs1, keyModule);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, keyCore);
		assertNotEquals("Core resource bundle entry not found", rbs1, keyCore);
		rbs1 = StreamsResources.getString(MqttStreamConstants.RESOURCE_BUNDLE_MQTT, keyCore);
		assertEquals("Core resource bundle entry found in mqtt", rbs1, keyCore);
	}
}
