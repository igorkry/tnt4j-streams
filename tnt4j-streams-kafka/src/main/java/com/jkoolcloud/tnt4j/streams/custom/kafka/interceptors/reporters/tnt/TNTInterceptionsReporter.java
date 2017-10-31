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

package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.tnt;

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
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.InterceptionsReporter;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.tracker.Tracker;

/**
 * TODO
 *
 * @version $Revision: 1 $
 */
public class TNTInterceptionsReporter implements InterceptionsReporter {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(TNTInterceptionsReporter.class);
	private static final String TOPIC_UNRELATED = "Topic unrelated"; // NON-NLS

	private java.util.Timer metricsReportingTimer;

	private Tracker tracker;

	private Map<String, TopicMetrics> topicsMetrics = new HashMap<>();

	private static class TopicMetrics {
		String topic;
		Integer partition;
		MetricRegistry mRegistry = new MetricRegistry();

		long lastSend;

		public TopicMetrics(String topic, Integer partition) {
			this.topic = topic;
			this.partition = partition;
		}
	}

	private static class ConsumerTopicMetrics extends TopicMetrics {
		static final String SUFFIX = "_consumer"; // NON-NLS

		private final Meter consumeM = mRegistry.meter("consumeMeter"); // NON-NLS
		private final Counter consumeC = mRegistry.counter("consumeIterationsCounter"); // NON-NLS
		private final Counter consumeMessagesC = mRegistry.counter("consumeMessageCounter"); // NON-NLS
		private final Counter commitC = mRegistry.counter("commitCounter"); // NON-NLS
		private final Histogram keySize = mRegistry.histogram("keySize"); // NON-NLS
		private final Histogram valueSize = mRegistry.histogram("valueSize"); // NON-NLS
		private final com.codahale.metrics.Timer latency = mRegistry.timer("messageLatency");// NON-NLS

		public ConsumerTopicMetrics(String topic, Integer partition) {
			super(topic, partition);
		}
	}

	private static class ProducerTopicMetrics extends TopicMetrics {
		static final String SUFFIX = "_producer"; // NON-NLS

		private final Histogram jitter = mRegistry.histogram("messageProducerJitter"); // NON-NLS
		private final Meter sendM = mRegistry.meter("sendMeter"); // NON-NLS
		private final Meter ackM = mRegistry.meter("ackMeter"); // NON-NLS
		private final Counter sendC = mRegistry.counter("sendCounter"); // NON-NLS
		private final Counter ackC = mRegistry.counter("ackCounter"); // NON-NLS
		private final Meter errorMeter = mRegistry.meter("errorCounter"); // NON-NLS

		public ProducerTopicMetrics(String topic, Integer partition) {
			super(topic, partition);
		}
	}

	public TNTInterceptionsReporter() {
		tracker = initTracker();
		TimerTask mrt = new TimerTask() {
			@Override
			public void run() {
				reportMetrics(topicsMetrics);
			}
		};
		long period = TimeUnit.SECONDS.toMillis(30);
		metricsReportingTimer = new java.util.Timer();
		metricsReportingTimer.scheduleAtFixedRate(mrt, period, period);
	}

	@Override
	public void send(ProducerRecord<Object, Object> producerRecord) {
		String topic = producerRecord.topic();
		// TODO producerRecord.partition()
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
			// TODO record.partition()
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
			Map<String, Object> metricsJMX = collectKafkaClientsMerticsJMX();
		} catch (Exception exc) {
			LOGGER.log(OpLevel.WARNING, "failed to collect Kafka clients metrics over JMX", exc);
		}

		try {
			for (Map.Entry<String, TopicMetrics> metrics : mRegistry.entrySet()) {
				String msg = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(metrics.getValue());
				tracker.log(OpLevel.INFO, msg);
			}
		} catch (JsonProcessingException e) {
			LOGGER.log(OpLevel.ERROR, "failed to report metrics", e);
		}
	}

	private static Tracker initTracker() {
		TrackerConfig trackerConfig = DefaultConfigFactory.getInstance().getConfig(
				"com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.tnt", SourceType.APPL, (String) null); // NON-NLS
		Tracker tracker = TrackingLogger.getInstance(trackerConfig.build());

		return tracker;
	}

	private static Map<String, Object> collectKafkaClientsMerticsJMX() {
		Map<String, Object> attrsMap = new HashMap<>();

		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		try {
			collectMetricsJMX("kafka.producer:", "kafka.producer:*", mBeanServer, attrsMap); // NON-NLS
		} catch (Exception exc) {
			LOGGER.log(OpLevel.WARNING, "failed to collect KafkaProducer metrics over JMX", exc);
		}
		try {
			collectMetricsJMX("kafka.consumer:", "kafka.consumer:*", mBeanServer, attrsMap); // NON-NLS
		} catch (Exception exc) {
			LOGGER.log(OpLevel.WARNING, "failed to collect KafkaConsumer metrics over JMX", exc);
		}

		return attrsMap;
	}

	private static void collectMetricsJMX(String prefix, String objNameStr, MBeanServer mBeanServer,
			Map<String, Object> attrsMap) throws Exception {
		ObjectName oName = new ObjectName(objNameStr);
		Set<ObjectName> metricsBeans = mBeanServer.queryNames(oName, null);

		for (ObjectName mBeanName : metricsBeans) {
			MBeanInfo metricsBean = mBeanServer.getMBeanInfo(mBeanName);
			MBeanAttributeInfo[] pMetricsAttrs = metricsBean.getAttributes();
			for (MBeanAttributeInfo pMetricsAttr : pMetricsAttrs) {
				attrsMap.put(prefix + pMetricsAttr.getName(),
						mBeanServer.getAttribute(mBeanName, pMetricsAttr.getName()));
			}
		}
	}

	private static class MetricsRegistrySerializer extends StdSerializer<TopicMetrics> {

		protected MetricsRegistrySerializer() {
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
		public static final Version VERSION = new Version(1, 2, 0, "", "com.jkoolcloud.tnt4j.streams", // NON-NLS
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
