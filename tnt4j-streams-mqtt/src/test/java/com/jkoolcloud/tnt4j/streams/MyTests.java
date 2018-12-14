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

package com.jkoolcloud.tnt4j.streams;

import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser;
import com.jkoolcloud.tnt4j.streams.utils.MqttStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsConstants;

@Ignore
public class MyTests {

	@Test
	public void testMQTT() throws Exception {
		StreamsConfigLoader sc = new StreamsConfigLoader("./samples/mqtt-json/tnt-data-source.xml"); // NON-NLS
		ActivityMapParser p = (ActivityMapParser) sc.getParser("MqttMessageParser"); // NON-NLS

		Map<String, Object> msgMap = new HashMap<>();
		msgMap.put(StreamsConstants.TOPIC_KEY, "JSONTest"); // NON-NLS
		msgMap.put(StreamsConstants.ACTIVITY_DATA_KEY,
				"{\"temperature\": 75, \"id\": \"sensor02\", \"room\": \"livingroom\", \"connected\": true}"); // NON-NLS
		msgMap.put(StreamsConstants.TRANSPORT_KEY, MqttStreamConstants.TRANSPORT_MQTT);

		ActivityInfo ai = p.parse(sc.getStream("SampleMQTTStream"), msgMap); // NON-NLS
		ai.toString();
	}

}
