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

import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.TopicPartition;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.gson.Gson;
import com.jkoolcloud.tnt4j.TrackingLogger;
import com.jkoolcloud.tnt4j.config.DefaultConfigFactory;
import com.jkoolcloud.tnt4j.config.TrackerConfig;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.source.SourceType;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.InterceptionsReporter;
import com.jkoolcloud.tnt4j.tracker.Tracker;

/**
 * TODO
 *
 * @version $Revision: 1 $
 */
public class TNTInterceptionsReporter implements InterceptionsReporter {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(TNTInterceptionsReporter.class);

	private MetricRegistry mRegistry = new MetricRegistry();
	private java.util.Timer metricsReportingTimer;

	private Tracker tracker;

	private final Meter sendM = mRegistry.meter("sendMeter");
	private final Meter ackM = mRegistry.meter("ackMeter");
	private final Meter consumeM = mRegistry.meter("consumeMeter");

	private final Counter sendC = mRegistry.counter("sendCounter");
	private final Counter ackC = mRegistry.counter("ackCounter");
	private final Counter consumeC = mRegistry.counter("consumeCounter");
	private final Counter commitC = mRegistry.counter("commitCounter");

	public TNTInterceptionsReporter() {
		tracker = initTracker();
		TimerTask mrt = new TimerTask() {
			@Override
			public void run() {
				reportMetrics(mRegistry);
			}
		};
		long period = TimeUnit.SECONDS.toMillis(30);
		metricsReportingTimer = new java.util.Timer();
		metricsReportingTimer.scheduleAtFixedRate(mrt, period, period);
	}

	@Override
	public void send(ProducerRecord<Object, Object> producerRecord) {
		sendM.mark();
		sendC.inc();
	}

	@Override
	public void acknowledge(RecordMetadata recordMetadata, Exception e, ClusterResource clusterResource) {
		ackM.mark();
		ackC.inc();
	}

	@Override
	public void consume(ConsumerRecords<Object, Object> consumerRecords, ClusterResource clusterResource) {
		consumeM.mark();
		consumeC.inc();
	}

	@Override
	public void commit(Map<TopicPartition, OffsetAndMetadata> map) {
		commitC.inc();
	}

	@Override
	public void shutdown() {
		metricsReportingTimer.cancel();
		reportMetrics(mRegistry);

		try {
			tracker.close();
		} catch (Exception exc) {
			// TODO
		}
	}

	private void reportMetrics(MetricRegistry mRegistry) {
		Gson gson = new Gson();
		String mJSON = gson.toJson(mRegistry.getMetrics());

		tracker.log(OpLevel.INFO, mJSON);
	}

	private Tracker initTracker() {
		TrackerConfig trackerConfig = DefaultConfigFactory.getInstance().getConfig(
				"com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.tnt", SourceType.APPL, (String) null);
		Tracker tracker = TrackingLogger.getInstance(trackerConfig.build());

		return tracker;
	}
}
