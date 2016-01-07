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

import static com.jkool.tnt4j.streams.inputs.InputPropertiesTestUtils.testInputPropertySetAndGet;

import org.junit.Test;

import com.jkool.tnt4j.streams.configure.StreamsConfig;

/**
 * @author akausinis
 * @version 1.0
 */
public class WmqStreamTest {
	WmqStream wmqStream = new WmqStream();

	@Test
	public void propertiesSetTest() throws Throwable {
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_QMGR_NAME, "TEST");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_QUEUE_NAME, "TEST");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_TOPIC_NAME, "TEST");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_SUB_NAME, "TEST");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_TOPIC_STRING, "TEST");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_QMGR_NAME, "TEST");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_HOST, "TEST");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_PORT, 8080);
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_CHANNEL_NAME, "TEST");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_STRIP_HEADERS, false);
	}

}
