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

import static com.jkool.tnt4j.streams.inputs.InputPropertiesTestUtils.makeTestPropertiesSet;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.I0Itec.zkclient.exception.ZkTimeoutException;
import org.junit.Test;

import com.jkool.tnt4j.streams.configure.StreamsConfig;

/**
 * @author akausinis
 * @version 1.0
 */
public class KafkaStreamTest {
	KafkaStream input;

	@Test
	public void testProperties() throws Throwable {
		input = new KafkaStream();

		final Collection<Entry<String, String>> properties = makeTestPropertiesSet(StreamsConfig.PROP_TOPIC_NAME,
				"TEST");
		properties.addAll(makeTestPropertiesSet("Topic", "TEST"));
		properties.addAll(makeTestPropertiesSet("zookeeper.connect", "127.0.0.1:2181"));
		properties.addAll(makeTestPropertiesSet("group.id", "TEST"));
		input.setProperties(properties);
		for (Map.Entry<String, String> property : properties) {
			assertEquals("Fail for property: " + property.getKey(), property.getValue(),
					input.getProperty(property.getKey()));
		}
	}

	@Test(expected = ZkTimeoutException.class)
	public void testInitialize() throws Throwable {
		testProperties();
		input.initialize();
	}

}
