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

package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

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
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics.MetricsReporter;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.trace.MsgTraceReporter;
import com.jkoolcloud.tnt4j.streams.utils.KafkaStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.utils.Utils;

/**
 * TNT4J-Streams Kafka interceptions manager. It loads interceptions configuration from {@code "interceptors.config"}
 * referenced properties file (default name is {@code "interceptors.properties"} and initiates interceptions reporters
 * to be used.
 * <p>
 * There are those types of reporters:
 * <ul>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics.MetricsReporter} - to aggregate
 * and report Kafka metrics collected over interceptions and JMX to dedicated Kafka topic.</li>
 * <li>{@link com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.trace.MsgTraceReporter} - to trace
 * intercepted messages and send message related events to JKool Cloud.</li>
 * </ul>
 * <p>
 * Interceptions configuration properties:
 * <ul>
 * <li>{@code metrics.report.period} - metrics reporting period in seconds. Default value - {@code 30} sec.</li>
 * <li>{@code trace.kafka.messages} - flag indicating whether to trace intercepted messages. Default value -
 * {@code true}.</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics.MetricsReporter
 * @see com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.trace.MsgTraceReporter
 */
public class InterceptionsManager {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(InterceptionsManager.class);

	private static final String DEFAULT_INTERCEPTORS_PROP_FILE = "interceptors.properties"; // NON-NLS

	private final Set<TNTKafkaInterceptor> references = new HashSet<>(5);
	private final Collection<InterceptionsReporter> reporters = new ArrayList<>(2);

	private static InterceptionsManager instance;

	private String interceptorsPropFile = DEFAULT_INTERCEPTORS_PROP_FILE;
	private Properties interceptorProps = new Properties();

	private InterceptionsManager() {
	}

	private void loadProperties() {
		interceptorsPropFile = System.getProperty("interceptors.config", DEFAULT_INTERCEPTORS_PROP_FILE); // NON-NLS

		try (FileInputStream fis = new FileInputStream(interceptorsPropFile)) {
			interceptorProps.load(fis);
		} catch (IOException exc) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"InterceptionsManager.cfg.load.failed", interceptorProps, exc);
		}
	}

	private void initialize() {
		loadProperties();

		int reportingPeriod = Utils.getInt("metrics.report.period", interceptorProps,
				MetricsReporter.DEFAULT_REPORTING_PERIOD_SEC);
		addReporter(new MetricsReporter(reportingPeriod));
		boolean traceMessages = Utils.getBoolean("trace.kafka.messages", interceptorProps, true); // NON-NLS
		if (traceMessages) {
			addReporter(new MsgTraceReporter());
		}
	}

	/**
	 * Gets instance of interceptions manager.
	 *
	 * @return instance of interceptions manager
	 */
	public static synchronized InterceptionsManager getInstance() {
		if (instance == null) {
			instance = new InterceptionsManager();
			instance.initialize();
		}

		return instance;
	}

	/**
	 * Binds interceptor reference.
	 *
	 * @param ref
	 *            interceptor reference to bind
	 */
	public void bindReference(TNTKafkaInterceptor ref) {
		references.add(ref);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.bind.reference", ref, references.size());
	}

	/**
	 * Unbinds interceptor reference.
	 *
	 * @param ref
	 *            interceptor reference to unbind
	 */
	public void unbindReference(TNTKafkaInterceptor ref) {
		references.remove(ref);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.unbind.reference", ref, references.size());

		if (references.isEmpty()) {
			for (InterceptionsReporter rep : reporters) {
				rep.shutdown();
			}
		}
	}

	/**
	 * Checks if Kafka producer is intercepted.
	 *
	 * @return {@code true} if interceptors references has any instance if
	 *         {@link com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor} class, {@code false}
	 *         - otherwise
	 */
	public boolean isProducerIntercepted() {
		return isClientIntercepted(TNTKafkaPInterceptor.class);
	}

	/**
	 * Checks if Kafka consumer is intercepted.
	 *
	 * @return {@code true} if interceptors references has any instance if
	 *         {@link com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor} class, {@code false}
	 *         - otherwise
	 */
	public boolean isConsumerIntercepted() {
		return isClientIntercepted(TNTKafkaCInterceptor.class);
	}

	private boolean isClientIntercepted(Class<? extends TNTKafkaInterceptor> refClass) {
		for (TNTKafkaInterceptor ref : references) {
			if (refClass.isInstance(ref)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Adds interceptions reporter to reporters list.
	 *
	 * @param reporter
	 *            reporter instance to add
	 */
	public void addReporter(InterceptionsReporter reporter) {
		reporters.add(reporter);
	}

	/**
	 * Notifies reporters that Kafka producer has sent message.
	 *
	 * @param producerRecord
	 *            producer record to be sent
	 *
	 * @return producer record to be sent
	 */
	public ProducerRecord<Object, Object> send(ProducerRecord<Object, Object> producerRecord) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.send", producerRecord);

		for (InterceptionsReporter rep : reporters) {
			rep.send(producerRecord);
		}

		return producerRecord;
	}

	/**
	 * Notifies reporters that Kafka producer has acknowledge sent message.
	 *
	 * @param recordMetadata
	 *            sent message record metadata
	 * @param e
	 *            occurred exception
	 * @param clusterResource
	 *            cluster resource metadata records where sent to
	 */
	public void acknowledge(RecordMetadata recordMetadata, Exception e, ClusterResource clusterResource) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.acknowledge", recordMetadata, e);

		for (InterceptionsReporter rep : reporters) {
			rep.acknowledge(recordMetadata, e, clusterResource);
		}
	}

	/**
	 * Notifies reporters that Kafka consumer has consumed messages.
	 *
	 * @param consumerRecords
	 *            consumed records collection
	 * @param clusterResource
	 *            cluster resource metadata records where consumed from
	 *
	 * @return consumed records collection
	 */
	public ConsumerRecords<Object, Object> consume(ConsumerRecords<Object, Object> consumerRecords,
			ClusterResource clusterResource) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.consume", consumerRecords);

		for (InterceptionsReporter rep : reporters) {
			rep.consume(consumerRecords, clusterResource);
		}

		return consumerRecords;
	}

	/**
	 * Notifies reporters that Kafka consumer has committed messages.
	 *
	 * @param map
	 *            committed records topics and messages map
	 */
	public void commit(Map<TopicPartition, OffsetAndMetadata> map) {
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
				"InterceptionsManager.commit", map);

		for (InterceptionsReporter rep : reporters) {
			rep.commit(map);
		}
	}

	public Map<String, ?> getInterceptorsConfig() {
		Map<String, Object> getConfigResponse = new HashMap<>();
		for (TNTKafkaInterceptor ref : references) {
			getConfigResponse.putAll(ref.getConfig());
		}

		return getConfigResponse;
	}
}
