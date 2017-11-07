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

package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.trace;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.TopicPartition;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.StreamsAgent;
import com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.InterceptionsReporter;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.fields.StreamFieldType;
import com.jkoolcloud.tnt4j.streams.utils.KafkaStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * Producer/Consumer interceptors intercepted messages reporter sending JKool Cloud events containing intercepted
 * message payload data, metadata and context data.
 * <p>
 * JKool Event types sent on consumer/producer interceptions:
 * <ul>
 * <li>send - 1 {@link com.jkoolcloud.tnt4j.core.OpType#SEND} type event.</li>
 * <li>acknowledge - 1 {@link com.jkoolcloud.tnt4j.core.OpType#EVENT} type event.</li>
 * <li>consume - n {@link com.jkoolcloud.tnt4j.core.OpType#RECEIVE} type events.</li>
 * <li>commit - n {@link com.jkoolcloud.tnt4j.core.OpType#EVENT} type events.</li>
 * </ul>
 *
 * @version $Revision: 1 $
 */
public class MsgTraceReporter implements InterceptionsReporter {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(MsgTraceReporter.class);

	private KafkaMsgTraceStream stream;

	/**
	 * Constructs a new MsgTraceReporter.
	 */
	public MsgTraceReporter() {
		stream = new KafkaMsgTraceStream();
		StreamsAgent.runFromAPI(stream);
	}

	@Override
	public void send(ProducerRecord<Object, Object> producerRecord) {
		try {
			ActivityInfo ai = new ActivityInfo();
			ai.setFieldValue(new ActivityField(StreamFieldType.EventType.name()), OpType.SEND);
			ai.setFieldValue(new ActivityField(StreamFieldType.EventName.name()), "Kafka_Producer_Send");
			ai.setFieldValue(new ActivityField("Partition"), producerRecord.partition());
			ai.setFieldValue(new ActivityField("Topic"), producerRecord.topic());
			ai.setFieldValue(new ActivityField("Key"), producerRecord.key());
			ai.setFieldValue(new ActivityField("Value"), producerRecord.value());
			ai.setFieldValue(new ActivityField(StreamFieldType.StartTime.name()), producerRecord.timestamp());
			ai.addCorrelator(producerRecord.topic());

			stream.getOutput().logItem(ai);
		} catch (Exception exc) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"send failed", exc);
		}
	}

	@Override
	public void acknowledge(RecordMetadata recordMetadata, Exception e, ClusterResource clusterResource) {
		try {
			ActivityInfo ai = new ActivityInfo();
			ai.setFieldValue(new ActivityField(StreamFieldType.EventType.name()), OpType.EVENT);
			ai.setFieldValue(new ActivityField(StreamFieldType.EventName.name()), "Kafka_Producer_Acknowledge");
			ai.setFieldValue(new ActivityField("Offset"), recordMetadata.offset());
			ai.setFieldValue(new ActivityField(StreamFieldType.StartTime.name()), recordMetadata.timestamp());
			ai.setFieldValue(new ActivityField("Checksum"), recordMetadata.checksum());
			ai.setFieldValue(new ActivityField("Topic"), recordMetadata.topic());
			ai.setFieldValue(new ActivityField("Partition"), recordMetadata.partition());
			if (e != null) {
				ai.setFieldValue(new ActivityField(StreamFieldType.Exception.name()),
						StringUtils.isEmpty(e.getMessage()) ? e.toString() : e.getMessage());
			}
			if (clusterResource != null) {
				ai.setFieldValue(new ActivityField("ClusterId"), clusterResource.clusterId());
			}

			int size = Math.max(recordMetadata.serializedKeySize(), 0)
					+ Math.max(recordMetadata.serializedValueSize(), 0);

			ai.setFieldValue(new ActivityField("Size"), size);
			ai.setFieldValue(new ActivityField(StreamFieldType.TrackingId.name()),
					calcSignature(recordMetadata.topic(), recordMetadata.partition(), recordMetadata.offset()));
			ai.addCorrelator(recordMetadata.topic());

			stream.getOutput().logItem(ai);
		} catch (Exception exc) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"acknowledge failed", exc);
		}
	}

	@Override
	public void consume(ConsumerRecords<Object, Object> consumerRecords, ClusterResource clusterResource) {
		try {
			ActivityInfo ai;

			for (ConsumerRecord<Object, Object> cr : consumerRecords) {
				ai = new ActivityInfo();
				ai.setFieldValue(new ActivityField(StreamFieldType.EventType.name()), OpType.RECEIVE);
				ai.setFieldValue(new ActivityField(StreamFieldType.EventName.name()), "Kafka_Consumer_Consume_Record");
				ai.setFieldValue(new ActivityField("Topic"), cr.topic());
				ai.setFieldValue(new ActivityField("Partition"), cr.partition());
				ai.setFieldValue(new ActivityField("Offset"), cr.offset());
				ai.setFieldValue(new ActivityField(StreamFieldType.StartTime.name()), cr.timestamp());
				ai.setFieldValue(new ActivityField("TimestampType"), cr.timestampType());
				ai.setFieldValue(new ActivityField("Key"), cr.key());
				ai.setFieldValue(new ActivityField("Value"), cr.value());

				int size = Math.max(cr.serializedKeySize(), 0) + Math.max(cr.serializedValueSize(), 0);
				long latency = System.currentTimeMillis() - cr.timestamp();

				ai.setFieldValue(new ActivityField("Size"), size);
				ai.setFieldValue(new ActivityField("Latency"), latency);

				if (clusterResource != null) {
					ai.setFieldValue(new ActivityField("ClusterId"), clusterResource.clusterId());
				}

				ai.setFieldValue(new ActivityField(StreamFieldType.TrackingId.name()),
						calcSignature(cr.topic(), cr.partition(), cr.offset()));
				ai.addCorrelator(cr.topic(), String.valueOf(cr.offset()));

				stream.getOutput().logItem(ai);
			}
		} catch (Exception exc) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"consume failed", exc);
		}
	}

	@Override
	public void commit(Map<TopicPartition, OffsetAndMetadata> map) {
		try {
			ActivityInfo ai;

			for (Map.Entry<TopicPartition, OffsetAndMetadata> me : map.entrySet()) {
				ai = new ActivityInfo();
				ai.setFieldValue(new ActivityField(StreamFieldType.EventType.name()), OpType.EVENT);
				ai.setFieldValue(new ActivityField(StreamFieldType.EventName.name()), "Kafka_Consumer_Commit_Entry");
				ai.setFieldValue(new ActivityField("Partition"), me.getKey().partition());
				ai.setFieldValue(new ActivityField("Topic"), me.getKey().topic());
				ai.setFieldValue(new ActivityField("Offset"), me.getValue().offset());
				ai.setFieldValue(new ActivityField("Metadata"), me.getValue().metadata());
				// ai.setFieldValue(new ActivityField(StreamFieldType.TrackingId.name()),
				// calcSignature(me.getKey().topic(), me.getKey().partition(), me.getValue().offset()));
				ai.addCorrelator(me.getKey().topic(), String.valueOf(me.getValue().offset()));

				stream.getOutput().logItem(ai);
			}
		} catch (Exception exc) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getBundle(KafkaStreamConstants.RESOURCE_BUNDLE_NAME),
					"commit failed", exc);
		}
	}

	@Override
	public void shutdown() {
		if (stream != null) {
			stream.markEnded();
		}
	}

	private static final MessageDigest MSG_DIGEST = Utils.getMD5Digester();

	private static String calcSignature(String topic, int partition, long offset) {
		synchronized (MSG_DIGEST) {
			MSG_DIGEST.reset();
			if (topic != null) {
				MSG_DIGEST.update(topic.getBytes());
			}
			MSG_DIGEST.update(ByteBuffer.allocate(4).putInt(partition).array());
			MSG_DIGEST.update(ByteBuffer.allocate(8).putLong(offset).array());

			return Utils.base64EncodeStr(MSG_DIGEST.digest());
		}
	}
}
