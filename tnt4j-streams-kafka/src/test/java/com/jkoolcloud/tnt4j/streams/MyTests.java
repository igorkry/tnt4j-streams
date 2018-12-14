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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.Random;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaInterceptor;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor;

/**
 * @author akausinis
 * @version 1.0
 * @created 2017-12-21 13:03
 */
public class MyTests {

	@Test
	public void putKafkaMessageTest() throws Exception {
		Producer<String, String> producer = initProducer();
		produce(producer, "trucking_data_truck", 2);
	}

	private static Producer<String, String> initProducer() throws Exception {
		Properties props = new Properties();
		props.setProperty("bootstrap.servers", "localhost:6667");
		props.setProperty("acks", "all");
		props.setProperty("retries", "0");
		props.setProperty("linger.ms", "1");
		props.setProperty("buffer.memory", "33554432");
		props.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.setProperty("client.id", "kafka-x-ray-test-producer");

		Producer<String, String> producer = new KafkaProducer<>(props);

		return producer;
	}

	private static void produce(Producer<String, String> producer, String topic, int eventCount) {
		Random rnd = new Random();

		for (int ei = 0; ei < eventCount; ei++) {
			long runtime = System.currentTimeMillis();
			String key = "192.168.2." + rnd.nextInt(255); // NON-NLS
			String value = runtime + ",www.example.com," + key + "," + (ei + 1)
					+ Character.valueOf((char) rnd.nextInt(255));// NON-NLS
			ProducerRecord<String, String> data = new ProducerRecord<>(topic, key, value);
			producer.send(data);
			if (ei % 5 == 0) {
				try {
					Thread.sleep((long) (1000 + 2000 * Math.random()));
				} catch (InterruptedException exc) {
				}
			}
		}

		producer.close();
	}

	@Test
	public void testClass() {
		TNTKafkaInterceptor i = new TNTKafkaCInterceptor();
		assertTrue(i.getClass().isAssignableFrom(TNTKafkaCInterceptor.class));
		assertFalse(i.getClass().isAssignableFrom(TNTKafkaPInterceptor.class));

		assertTrue(TNTKafkaCInterceptor.class.isInstance(i));
		assertFalse(TNTKafkaPInterceptor.class.isInstance(i));
	}
}
