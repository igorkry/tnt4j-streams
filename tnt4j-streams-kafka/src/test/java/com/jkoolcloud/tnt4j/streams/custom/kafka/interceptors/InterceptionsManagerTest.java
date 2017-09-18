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

package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors;

import org.junit.Test;

import com.jkoolcloud.tnt4j.config.TrackerConfigStore;

/**
 * @author akausinis
 * @version 1.0
 */
public class InterceptionsManagerTest {

	static {
		System.setProperty(TrackerConfigStore.TNT4J_PROPERTIES_KEY, "./config/intercept/tnt4j_kafka.properties");
		System.setProperty("producer.config", "./config/intercept/producer.properties");
		System.setProperty("consumer.config", "./config/intercept/consumer.properties");
		System.setProperty("interceptors.config", "./config/intercept/interceptors.properties");
		System.setProperty("log4j.configuration", "file:../config/log4j.properties");
	}

	@Test
	public void interceptionsTest() throws Exception {
		InterceptorsTest.interceptionsTest();
	}

	@Test
	public void kafkaProducerTest() throws Exception {
		InterceptorsTest.produce();
	}

	@Test
	public void kafkaConsumerTest() throws Exception {
		InterceptorsTest.consume();
	}
}
