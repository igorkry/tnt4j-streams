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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.TopicPartition;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.InterceptionsReporter;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.jkool.StreamsInterceptionsReporter;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.tnt.TNTInterceptionsReporter;
import com.jkoolcloud.tnt4j.streams.utils.KafkaStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * TODO
 *
 * @version $Revision: 1 $
 */
public class InterceptionsManager {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(InterceptionsManager.class);

	private final AtomicInteger referencesCount = new AtomicInteger();
	private final Collection<InterceptionsReporter> reporters = new ArrayList<>();

	private static InterceptionsManager instance;

	public static String DEFAULT_INTERCEPTORS_PROP_FILE = "interceptors.properties";
	private String interceptorsPropFile = DEFAULT_INTERCEPTORS_PROP_FILE;

	private Properties interceptorProps = new Properties();

	private InterceptionsManager() {
	}

	private void loadProperties() {
		interceptorsPropFile = System.getProperty("interceptors.config", DEFAULT_INTERCEPTORS_PROP_FILE);

		try (FileInputStream fis = new FileInputStream(interceptorsPropFile)) {
			interceptorProps.load(fis);
		} catch (IOException exc) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"Failed loading interceptors configuration properties", exc);
		}
	}

	private void initialize() {
		loadProperties();

		addReporter(new TNTInterceptionsReporter());
		boolean traceMessages = Boolean.parseBoolean(interceptorProps.getProperty("trace.kafka.messages", "true"));
		if (traceMessages) {
			addReporter(new StreamsInterceptionsReporter());
		}
	}

	public static synchronized InterceptionsManager getInstance() {
		if (instance == null) {
			instance = new InterceptionsManager();
			instance.initialize();
		}

		return instance;
	}

	public void bindReference(Object ref) {
		int crc = referencesCount.incrementAndGet();

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"Binding interceptor reference: ref={0}, refsCount={1}", ref, crc);
	}

	public void unbindReference(Object ref) {
		int crc = referencesCount.decrementAndGet();

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"Unbinding interceptor reference: ref={0}, refsCount={1}", ref, crc);

		if (crc <= 0) {
			for (InterceptionsReporter rep : reporters) {
				rep.shutdown();
			}
		}
	}

	public void addReporter(InterceptionsReporter reporter) {
		reporters.add(reporter);
	}

	public ProducerRecord<Object, Object> send(ProducerRecord<Object, Object> producerRecord) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.send: producerRecord={0}", producerRecord);

		for (InterceptionsReporter rep : reporters) {
			rep.send(producerRecord);
		}

		return producerRecord;
	}

	public void acknowledge(RecordMetadata recordMetadata, Exception e, ClusterResource clusterResource) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.acknowledge: recordMetadata={0}, exception={1}", recordMetadata, e);

		for (InterceptionsReporter rep : reporters) {
			rep.acknowledge(recordMetadata, e, clusterResource);
		}
	}

	public ConsumerRecords<Object, Object> consume(ConsumerRecords<Object, Object> consumerRecords,
			ClusterResource clusterResource) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.consume: consumerRecords={0}", consumerRecords);

		for (InterceptionsReporter rep : reporters) {
			rep.consume(consumerRecords, clusterResource);
		}

		return consumerRecords;
	}

	public void commit(Map<TopicPartition, OffsetAndMetadata> map) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.commit: map={0}", map);

		for (InterceptionsReporter rep : reporters) {
			rep.commit(map);
		}
	}
}
