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
import static org.junit.Assert.assertNotEquals;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.I0Itec.zkclient.exception.ZkTimeoutException;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.KafkaStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * @author akausinis
 * @version 1.0
 */
public class KafkaStreamTest {
	KafkaStream input;

	@Test
	public void testProperties() throws Exception {
		input = new KafkaStream();

		final Collection<Entry<String, String>> properties = InputPropertiesTestUtils
				.makeTestPropertiesSet(StreamProperties.PROP_TOPIC_NAME, "TEST");
		properties.addAll(InputPropertiesTestUtils.makeTestPropertiesSet("Topic", "TEST"));
		properties.addAll(InputPropertiesTestUtils.makeTestPropertiesSet("zookeeper.connect", "127.0.0.1:2181"));
		properties.addAll(InputPropertiesTestUtils.makeTestPropertiesSet("group.id", "TEST"));
		input.setProperties(properties);
		for (Map.Entry<String, String> property : properties) {
			assertEquals("Fail for property: " + property.getKey(), property.getValue(),
					input.getProperty(property.getKey()));
		}
	}

	@Test(expected = ZkTimeoutException.class)
	public void testInitialize() throws Exception {
		testProperties();
		input.initialize();
	}

	@Test
	public void testRB() {
		String keyModule = "KafkaStream.stream.ready";
		String keyCore = "ActivityField.field.type.name.empty";

		String rbs1 = StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME, keyModule);
		assertNotEquals("Kafka resource bundle entry not found", rbs1, keyModule);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyModule);
		assertEquals("Kafka resource bundle entry found in core", rbs1, keyModule);
		rbs1 = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, keyCore);
		assertNotEquals("Core resource bundle entry not found", rbs1, keyCore);
		rbs1 = StreamsResources.getString(KafkaStreamConstants.RESOURCE_BUNDLE_NAME, keyCore);
		assertEquals("Core resource bundle entry found in kafka", rbs1, keyCore);
	}

}
