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

import java.io.FileReader;
import java.util.*;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.junit.Test;

import com.jkoolcloud.tnt4j.config.TrackerConfigStore;

/**
 * @author akausinis
 * @version 1.0
 */
public class InterceptionsManagerTest {

	@Test
	public void interceptionsTest() throws Exception {
		final Consumer<String, String> consumer = initConsumer();

		final Thread pt = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					kafkaProducerTest();
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
		});

		Thread ct = new Thread(new Runnable() {
			@Override
			public void run() {
				consume(consumer);
			}
		});
		ct.start();

		pt.start();
		pt.join();
		consumer.wakeup();
		ct.join();
	}

	@Test
	public void kafkaProducerTest() throws Exception {
		Producer<String, String> producer = initProducer();
		produce(producer, "page_visits", 10);
	}

	@Test
	public void kafkaConsumerTest() throws Exception {
		Consumer<String, String> consumer = initConsumer();
		consume(consumer);
	}

	private Producer<String, String> initProducer() throws Exception {
		System.setProperty(TrackerConfigStore.TNT4J_PROPERTIES_KEY, "./config/tnt4j_dev.properties");
		System.setProperty("log4j.configuration", "file:../config/log4j_dev.properties");

		Properties props = new Properties();
		props.load(new FileReader("../config/tnt4j-kafka.properties"));
		props.put("interceptor.classes", "com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor");

		Producer<String, String> producer = new KafkaProducer<>(props);

		return producer;
	}

	private void produce(Producer<String, String> producer, String topic, int eventCount) {
		Random rnd = new Random();

		for (long ei = 0; ei < eventCount; ei++) {
			long runtime = System.currentTimeMillis();
			String ip = "192.168.2." + rnd.nextInt(255);
			String msg = runtime + ",www.example.com," + ip;
			ProducerRecord<String, String> data = new ProducerRecord<>(topic, ip, msg);
			producer.send(data);
		}

		producer.close();
	}

	private Consumer<String, String> initConsumer() throws Exception {
		System.setProperty(TrackerConfigStore.TNT4J_PROPERTIES_KEY, "./config/tnt4j_dev.properties");
		System.setProperty("log4j.configuration", "file:../config/log4j_dev.properties");

		Properties props = new Properties();
		props.load(new FileReader("./config/consumer.properties"));
		props.put("interceptor.classes", "com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor");

		String topic = "page_visits";

		Consumer<String, String> consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Collections.singletonList(topic));

		return consumer;
	}

	private void consume(Consumer<String, String> consumer) {
		boolean halt = false;
		while (!halt) {
			try {
				ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
				for (ConsumerRecord<String, String> record : records) {
					Map<String, Object> data = new HashMap<>();
					data.put("partition", record.partition());
					data.put("offset", record.offset());
					data.put("value", record.value());
					System.out.println(consumer.hashCode() + ": " + data);
				}
			} catch (WakeupException exc) {
				halt = true;
			}
		}

		consumer.close();
	}
}
