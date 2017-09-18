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

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;

/**
 * @author akausinis
 * @version 1.0
 * @created 2017-09-18 14:49
 */
public class InterceptorsTest {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(InterceptorsTest.class);

	private static int eventsToProduce = 10;
	private static String topicName = "tnt4j_streams_kafka_intercept_test_page_visits"; // NON-NLS

	public static void main(String... args) {
		try {
			interceptionsTest();
		} catch (Exception exc) {
			LOGGER.log(OpLevel.ERROR, "InterceptorsTest.interceptionsTest failed to complete!..", exc);
		}
	}

	public static void interceptionsTest() throws Exception {
		final Consumer<String, String> consumer = initConsumer();

		final Thread pt = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					produce();
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

	public static void produce() throws Exception {
		Producer<String, String> producer = initProducer();
		produce(producer, topicName, eventsToProduce);
	}

	public static void consume() throws Exception {
		Consumer<String, String> consumer = initConsumer();
		consume(consumer);
	}

	private static Producer<String, String> initProducer() throws Exception {
		Properties props = new Properties();
		props.load(new FileReader(System.getProperty("producer.config")));// NON-NLS

		eventsToProduce = Integer.parseInt(props.getProperty("events.count", "10"));
		props.remove("events.count");
		topicName = props.getProperty("test.app.topic.name", "tnt4j_streams_kafka_intercept_test_page_visits"); // NON-NLS
		props.remove("test.app.topic.name");

		Producer<String, String> producer = new KafkaProducer<>(props);

		return producer;
	}

	private static void produce(Producer<String, String> producer, String topic, int eventCount) {
		Random rnd = new Random();

		for (int ei = 0; ei < eventCount; ei++) {
			long runtime = System.currentTimeMillis();
			String ip = "192.168.2." + rnd.nextInt(255); // NON-NLS
			String msg = runtime + ",www.example.com," + ip + "," + (ei + 1);// NON-NLS
			ProducerRecord<String, String> data = new ProducerRecord<>(topic, ip, msg);
			producer.send(data);
			if (ei % 5 == 0) {
				try {
					Thread.sleep((long) (2000 * Math.random()));
				} catch (InterruptedException exc) {
				}
			}
		}

		producer.close();
	}

	private static Consumer<String, String> initConsumer() throws Exception {
		Properties props = new Properties();
		props.load(new FileReader(System.getProperty("consumer.config"))); // NON-NLS
		topicName = props.getProperty("test.app.topic.name", "tnt4j_streams_kafka_intercept_test_page_visits"); // NON-NLS
		props.remove("test.app.topic.name");

		Consumer<String, String> consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Collections.singletonList(topicName));

		return consumer;
	}

	private static void consume(Consumer<String, String> consumer) {
		boolean halt = false;
		while (!halt) {
			try {
				ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
				int ei = 0;
				for (ConsumerRecord<String, String> record : records) {
					Map<String, Object> data = new HashMap<>();
					data.put("partition", record.partition()); // NON-NLS
					data.put("offset", record.offset()); // NON-NLS
					data.put("value", record.value()); // NON-NLS
					LOGGER.log(OpLevel.INFO, "Consuming Kafka message: {1}", consumer.hashCode(), data); // NON-NLS

					try {
						Thread.sleep((long) (200 * Math.random()));
					} catch (InterruptedException exc) {
					}
				}
			} catch (WakeupException exc) {
				halt = true;
			}
		}

		consumer.close();
	}
}
