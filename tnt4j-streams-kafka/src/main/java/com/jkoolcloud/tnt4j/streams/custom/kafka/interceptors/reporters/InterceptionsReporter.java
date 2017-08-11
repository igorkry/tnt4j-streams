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

package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.ClusterResource;
import org.apache.kafka.common.TopicPartition;

/**
 * TODO
 *
 * @version $Revision: 1 $
 */
public interface InterceptionsReporter {
	void send(ProducerRecord<Object, Object> producerRecord);

	void acknowledge(RecordMetadata recordMetadata, Exception e, ClusterResource clusterResource);

	void consume(ConsumerRecords<Object, Object> consumerRecords, ClusterResource clusterResource);

	void commit(Map<TopicPartition, OffsetAndMetadata> map);

	void shutdown();
}
