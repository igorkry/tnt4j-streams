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

package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.collections4.MapUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.TopicPartition;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleSerializers;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.jkoolcloud.tnt4j.TrackingLogger;
import com.jkoolcloud.tnt4j.config.DefaultConfigFactory;
import com.jkoolcloud.tnt4j.config.TrackerConfig;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.source.SourceType;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.InterceptionsManager;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.InterceptionsReporter;
import com.jkoolcloud.tnt4j.streams.utils.KafkaStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.tracker.Tracker;

/**
 * Producer/Consumer interceptors intercepted data metrics aggregator and reporter sending collected metrics data as
 * JSON over configured TNT4J {@link Tracker}.
 * <p>
 * Reported metrics:
 * <ul>
 * <li>Kafka consumer JMX metrics</li>
 * <li>Kafka producer JMX metrics</li>
 * <li>JVM JMX metrics</li>
 * <li>This reporter accumulated additional Kafka metrics</li>
 * </ul>
 * <p>
 * Metrics data is reported in scheduled intervals and on interceptions shutdown.
 *
 * @version $Revision: 1 $
 */
public class MetricsReporter implements InterceptionsReporter {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(MetricsReporter.class);

	/**
	 * Default metrics reporting period - {@value} seconds.
	 */
	public static final int DEFAULT_REPORTING_PERIOD_SEC = 30;
	private static final String TOPIC_UNRELATED = "Topic unrelated"; // NON-NLS

	private java.util.Timer metricsReportingTimer;

	private Tracker tracker;

	private Map<String, TopicMetrics> topicsMetrics = new HashMap<>();

	private static class TopicMetrics {
		/**
		 * The topic name.
		 */
		String topic;
		/**
		 * The partition index.
		 */
		Integer partition;
		/**
		 * The collected metrics registry.
		 */
		MetricRegistry mRegistry = new MetricRegistry();
		/**
		 * Last producer sent message timestamp.
		 */
		long lastSend;

		/**
		 * Constructs a new TopicMetrics.
		 *
		 * @param topic
		 *            the topic name
		 * @param partition
		 *            the partition index
		 */
		TopicMetrics(String topic, Integer partition) {
			this.topic = topic;
			this.partition = partition;
		}
	}

	private static class ConsumerTopicMetrics extends TopicMetrics {
		private final Meter consumeM = mRegistry.meter("consumeMeter"); // NON-NLS
		private final Counter consumeC = mRegistry.counter("consumeIterationsCounter"); // NON-NLS
		private final Counter consumeMessagesC = mRegistry.counter("consumeMessageCounter"); // NON-NLS
		private final Counter commitC = mRegistry.counter("commitCounter"); // NON-NLS
		private final Histogram keySize = mRegistry.histogram("keySize"); // NON-NLS
		private final Histogram valueSize = mRegistry.histogram("valueSize"); // NON-NLS
		private final com.codahale.metrics.Timer latency = mRegistry.timer("messageLatency");// NON-NLS

		/**
		 * Constructs a new ConsumerTopicMetrics.
		 *
		 * @param topic
		 *            the topic name
		 * @param partition
		 *            the partition index
		 */
		ConsumerTopicMetrics(String topic, Integer partition) {
			super(topic, partition);
		}
	}

	private static class ProducerTopicMetrics extends TopicMetrics {
		private final Histogram jitter = mRegistry.histogram("messageProducerJitter"); // NON-NLS
		private final Meter sendM = mRegistry.meter("sendMeter"); // NON-NLS
		private final Meter ackM = mRegistry.meter("ackMeter"); // NON-NLS
		private final Counter sendC = mRegistry.counter("sendCounter"); // NON-NLS
		private final Counter ackC = mRegistry.counter("ackCounter"); // NON-NLS
		private final Meter errorMeter = mRegistry.meter("errorCounter"); // NON-NLS

		/**
		 * Constructs a new ProducerTopicMetrics.
		 *
		 * @param topic
		 *            the topic name
		 * @param partition
		 *            the partition index
		 */
		ProducerTopicMetrics(String topic, Integer partition) {
			super(topic, partition);
		}
	}

	/**
	 * Constructs a new MetricsReporter.
	 */
	public MetricsReporter() {
		this(DEFAULT_REPORTING_PERIOD_SEC);
	}

	/**
	 * Constructs a new MetricsReporter.
	 *
	 * @param reportingPeriod
	 *            the metrics reporting period in seconds
	 */
	public MetricsReporter(int reportingPeriod) {
		tracker = initTracker();
		TimerTask mrt = new TimerTask() {
			@Override
			public void run() {
				reportMetrics(topicsMetrics);
			}
		};
		long period = TimeUnit.SECONDS.toMillis(reportingPeriod);
		metricsReportingTimer = new java.util.Timer();
		metricsReportingTimer.scheduleAtFixedRate(mrt, period, period);
	}

	@Override
	public void send(ProducerRecord<Object, Object> producerRecord) {
		String topic = producerRecord.topic();
		ProducerTopicMetrics topicMetrics = getProducerTopicMetrics(topic, producerRecord.partition());
		long now = System.currentTimeMillis();
		long jitter = now - topicMetrics.lastSend;
		topicMetrics.jitter.update(jitter);
		topicMetrics.lastSend = now;
		topicMetrics.sendM.mark();
		topicMetrics.sendC.inc();
	}

	private ConsumerTopicMetrics getConsumerTopicMetrics(String topic, Integer partition) {
		if (topic == null) {
			topic = TOPIC_UNRELATED;
		}
		String key = getMetricsKey(topic, partition, ConsumerTopicMetrics.class);
		ConsumerTopicMetrics topicMetrics = (ConsumerTopicMetrics) topicsMetrics.get(key);
		if (topicMetrics == null) {
			topicMetrics = new ConsumerTopicMetrics(topic, partition);
			topicsMetrics.put(key, topicMetrics);
		}

		return topicMetrics;
	}

	private ProducerTopicMetrics getProducerTopicMetrics(String topic, Integer partition) {
		if (topic == null) {
			topic = TOPIC_UNRELATED;
		}
		String key = getMetricsKey(topic, partition, ProducerTopicMetrics.class);
		ProducerTopicMetrics topicMetrics = (ProducerTopicMetrics) topicsMetrics.get(key);
		if (topicMetrics == null) {
			topicMetrics = new ProducerTopicMetrics(topic, partition);
			topicsMetrics.put(key, topicMetrics);
		}

		return topicMetrics;
	}

	private static String getMetricsKey(String topic, Integer partition, Class<? extends TopicMetrics> metricsClass) {
		return topic + ":" + partition + ":" + metricsClass.getSimpleName(); // NON-NLS
	}

	@Override
	public void acknowledge(RecordMetadata recordMetadata, Exception e, ClusterResource clusterResource) {
		ProducerTopicMetrics topicMetrics = getProducerTopicMetrics(recordMetadata.topic(), recordMetadata.partition());
		if (e != null) {
			topicMetrics.errorMeter.mark();
		}
		topicMetrics.ackM.mark();
		topicMetrics.ackC.inc();
	}

	@Override
	public void consume(ConsumerRecords<Object, Object> consumerRecords, ClusterResource clusterResource) {
		for (ConsumerRecord<?, ?> record : consumerRecords) {
			ConsumerTopicMetrics topicMetrics = getConsumerTopicMetrics(record.topic(), record.partition());
			long duration = System.currentTimeMillis() - record.timestamp();
			topicMetrics.latency.update(duration, TimeUnit.MILLISECONDS);
			topicMetrics.keySize.update(record.serializedKeySize());
			topicMetrics.valueSize.update(record.serializedValueSize());
			topicMetrics.consumeMessagesC.inc();
			topicMetrics.consumeM.mark();
			topicMetrics.consumeC.inc();
		}

	}

	@Override
	public void commit(Map<TopicPartition, OffsetAndMetadata> tpomMap) {
		for (Map.Entry<TopicPartition, OffsetAndMetadata> tpom : tpomMap.entrySet()) {
			TopicPartition partition = tpom.getKey();
			ConsumerTopicMetrics topicMetrics = getConsumerTopicMetrics(partition.topic(), partition.partition());
			topicMetrics.commitC.inc();
		}
	}

	@Override
	public void shutdown() {
		metricsReportingTimer.cancel();
		reportMetrics(topicsMetrics);

		Utils.close(tracker);
	}

	private void reportMetrics(Map<String, TopicMetrics> mRegistry) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.MILLISECONDS, false));
		mapper.registerModule(new MetricsRegistryModule());

		try {
			if (InterceptionsManager.getInstance().isProducerIntercepted()) {
				Map<String, Object> metricsJMX = collectKafkaProducerMetricsJMX();
				if (MapUtils.isNotEmpty(metricsJMX)) {
					String msg = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(metricsJMX);
					tracker.log(OpLevel.INFO, msg);
				}
			}
			if (InterceptionsManager.getInstance().isConsumerIntercepted()) {
				Map<String, Object> metricsJMX = collectKafkaConsumerMetricsJMX();
				if (MapUtils.isNotEmpty(metricsJMX)) {
					String msg = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(metricsJMX);
					tracker.log(OpLevel.INFO, msg);
				}
			}

			Map<String, Object> metricsJMX = collectJVMMetricsJMX();
			if (MapUtils.isNotEmpty(metricsJMX)) {
				String msg = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(metricsJMX);
				tracker.log(OpLevel.INFO, msg);
			}
		} catch (Exception exc) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MetricsReporter.clients.jmx.fail", exc);
		}

		try {
			for (Map.Entry<String, TopicMetrics> metrics : mRegistry.entrySet()) {
				String msg = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(metrics.getValue());
				tracker.log(OpLevel.INFO, msg);
			}
		} catch (JsonProcessingException exc) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MetricsReporter.report.metrics.fail", exc);
		}
	}

	private static Tracker initTracker() {
		TrackerConfig trackerConfig = DefaultConfigFactory.getInstance().getConfig(
				"com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.metrics", SourceType.APPL, // NON-NLS
				(String) null);
		Tracker tracker = TrackingLogger.getInstance(trackerConfig.build());

		return tracker;
	}

	/**
	 * Collects Kafka consumer domain {@code 'kafka.consumer'} JMX attributes values.
	 *
	 * @return map of Kafka consumer JMX attributes values
	 *
	 * @see #collectMetricsJMX(String, String, javax.management.MBeanServer, java.util.Map)
	 */
	private static Map<String, Object> collectKafkaConsumerMetricsJMX() {
		Map<String, Object> attrsMap = new HashMap<>(20);

		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		try {
			collectMetricsJMX("kafka.consumer:", "kafka.consumer:*", mBeanServer, attrsMap); // NON-NLS
		} catch (Exception exc) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MetricsReporter.consumer.jmx.fail", exc);
		}

		return attrsMap;
	}

	/**
	 * Collects Kafka producer domain {@code 'kafka.producer'} JMX attributes values.
	 *
	 * @return map of Kafka producer JMX attributes values
	 *
	 * @see #collectMetricsJMX(String, String, javax.management.MBeanServer, java.util.Map)
	 */
	public static Map<String, Object> collectKafkaProducerMetricsJMX() {
		Map<String, Object> attrsMap = new HashMap<>(20);

		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		try {
			collectMetricsJMX("kafka.producer:", "kafka.producer:*", mBeanServer, attrsMap); // NON-NLS
		} catch (Exception exc) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MetricsReporter.producer.jmx.fail", exc);
		}
		return attrsMap;
	}

	/**
	 * Collects JVM domain {@code 'java.lang'} JMX attributes values.
	 * 
	 * @return map of JVM JMX attributes values
	 *
	 * @see #collectMetricsJMX(String, String, javax.management.MBeanServer, java.util.Map)
	 */
	public static Map<String, Object> collectJVMMetricsJMX() {
		Map<String, Object> attrsMap = new HashMap<>();

		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		try {
			collectMetricsJMX("java.lang:", "java.lang:*", mBeanServer, attrsMap); // NON-NLS
		} catch (Exception exc) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"MetricsReporter.jvm.jmx.fail", exc);
		}
		return attrsMap;
	}

	/**
	 * Collects JMX attributes of MBeans defined by <tt>objNameStr</tt>.
	 *
	 * @param prefix
	 *            attribute key prefix
	 * @param objNameStr
	 *            MBeans object name pattern to query
	 * @param mBeanServer
	 *            MBean server instance to use
	 * @param attrsMap
	 *            attributes map to put collected values
	 * @throws Exception
	 *             if JMX attributes collecting fails
	 */
	public static void collectMetricsJMX(String prefix, String objNameStr, MBeanServer mBeanServer,
			Map<String, Object> attrsMap) throws Exception {
		ObjectName oName = new ObjectName(objNameStr);
		Set<ObjectName> metricsBeans = mBeanServer.queryNames(oName, null);

		for (ObjectName mBeanName : metricsBeans) {
			try {
				MBeanInfo metricsBean = mBeanServer.getMBeanInfo(mBeanName);
				MBeanAttributeInfo[] pMetricsAttrs = metricsBean.getAttributes();
				for (MBeanAttributeInfo pMetricsAttr : pMetricsAttrs) {
					try {
						attrsMap.put(prefix + pMetricsAttr.getName(),
								mBeanServer.getAttribute(mBeanName, pMetricsAttr.getName()));
					} catch (Exception exc) {
						LOGGER.log(OpLevel.WARNING,
								StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
								"MetricsReporter.bean.attr.fail", mBeanName, pMetricsAttr.getName(), exc);
					}
				}
			} catch (Exception exc) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
						"MetricsReporter.bean.info.fail", mBeanName, exc);
			}
		}
	}

	private static class MetricsRegistrySerializer extends StdSerializer<TopicMetrics> {

		/**
		 * Constructs a new MetricsRegistrySerializer.
		 */
		MetricsRegistrySerializer() {
			super(TopicMetrics.class);
		}

		@Override
		public void serialize(TopicMetrics topicMetrics, JsonGenerator json, SerializerProvider provider)
				throws IOException {
			json.writeStartObject();
			json.writeObjectField("Topic", topicMetrics.topic); // NON-NLS
			json.writeObjectField("Partition", topicMetrics.partition); // NON-NLS
			json.writeObjectField("Type", topicMetrics.getClass().getSimpleName()); // NON-NLS
			json.writeObjectField("Metrics", topicMetrics.mRegistry); // NON-NLS

			json.writeEndObject();
		}
	}

	private static class MetricsRegistryModule extends Module {
		private static final Version VERSION = new Version(1, 2, 0, "", "com.jkoolcloud.tnt4j.streams", // NON-NLS
				"tnt4j-streams-kafka"); // NON-NLS

		@Override
		public String getModuleName() {
			return "TopicMetrics"; // NON-NLS
		}

		@Override
		public Version version() {
			return VERSION;
		}

		@Override
		public void setupModule(SetupContext setupContext) {
			SimpleSerializers simpleSerializers = new SimpleSerializers();
			simpleSerializers.addSerializer(new MetricsRegistrySerializer());

			setupContext.addSerializers(simpleSerializers);
		}
	}
}
