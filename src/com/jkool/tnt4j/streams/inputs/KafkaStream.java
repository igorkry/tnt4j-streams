/*
 * Copyright (c) 2015 jKool, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * jKool, LLC. ("Confidential Information").  You shall not disclose
 * such Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with jKool, LLC.
 *
 * JKOOL MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. JKOOL SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 * CopyrightVersion 1.0
 *
 */

package com.jkool.tnt4j.streams.inputs;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.Whitelist;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.utils.StreamsConstants;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a Kafka topics transmitted activity stream, where each message
 * body is assumed to represent a single activity or event which should be
 * recorded. Topic to listen is defined using "Topic" property in stream
 * configuration.
 * </p>
 * <p>
 * This activity stream requires parsers that can support {@code Map} data. On
 * message reception message data is packed into {@code Map} filling these
 * entries:
 * <ul>
 * <li>ActivityTopic - topic name message with activity data was received.</li>
 * <li>ActivityData - raw activity data as {@code byte[]} retrieved from
 * message.</li>
 * <li>ActivityTransport - activity transport definition: 'Kafka'.</li>
 * </ul>
 * </p>
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>Topic - regex of topic name to listen. (Required)</li>
 * <li>List of properties used by Kafka API. i.e zookeeper.connect, group.id.
 * See {@link kafka.consumer.ConsumerConfig} for more details on Kafka consumer
 * properties.</li>
 * </ul>
 *
 * @version $Revision: 2 $
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see com.jkool.tnt4j.streams.parsers.ActivityMapParser
 * @see kafka.consumer.ConsumerConfig
 */
public class KafkaStream extends TNTInputStream<Map<String, ?>> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(KafkaStream.class);

	private final AtomicBoolean closed = new AtomicBoolean(false);

	private ConsumerConnector consumer;
	private ConsumerConfig kafkaProperties;
	private String topicNameRegex;

	private Iterator<MessageAndMetadata<byte[], byte[]>> messageBuffer;
	private int partition = 0;

	/**
	 * Constructs a new KafkaStream.
	 */
	public KafkaStream() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Throwable {
		if (props == null) {
			return;
		}
		Properties properties = new Properties();
		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamsConfig.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
				topicNameRegex = value;
			} else {
				properties.put(name, value);
			}
		}
		kafkaProperties = new ConsumerConfig(properties);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getProperty(String name) {
		if (StreamsConfig.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
			return topicNameRegex;
		}

		Object prop = super.getProperty(name);
		if (prop == null) {
			prop = kafkaProperties.props().getProperty(name);
		}

		return prop;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize() throws Throwable {
		super.initialize();
		if (StringUtils.isEmpty(topicNameRegex)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted("TNTInputStream.property.undefined",
					StreamsConfig.PROP_TOPIC_NAME));
		}

		consumer = Consumer.createJavaConsumerConnector(kafkaProperties);
		LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("KafkaStream.ready.to.receive.messages"));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns a string from next record received over Kafka
	 * consumer.
	 * </p>
	 */
	@Override
	public Map<String, ?> getNextItem() throws Throwable {
		while (!closed.get()) {
			if (messageBuffer == null || !messageBuffer.hasNext()) {
				LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("KafkaStream.empty.messages.buffer"));
				final List<kafka.consumer.KafkaStream<byte[], byte[]>> streams = consumer
						.createMessageStreamsByFilter(new Whitelist(topicNameRegex));
				if (CollectionUtils.isNotEmpty(streams)) {
					kafka.consumer.KafkaStream<byte[], byte[]> stream = streams.get(0);
					messageBuffer = stream.iterator();
					LOGGER.log(OpLevel.DEBUG,
							StreamsResources.getStringFormatted("KafkaStream.retrieved.new.messages", stream.size()));
				} else {
					LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("KafkaStream.retrieved.no.new.messages"));
				}
			}

			if (messageBuffer != null && messageBuffer.hasNext()) {
				MessageAndMetadata<byte[], byte[]> msg = messageBuffer.next();
				byte[] msgPayload = msg.message();
				String msgData = Utils.getString(msgPayload);

				LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("KafkaStream.next.message", msgData));

				Map<String, Object> msgDataMap = new HashMap<String, Object>();
				msgDataMap.put(StreamsConstants.TOPIC_KEY, msg.topic());
				msgDataMap.put(StreamsConstants.ACTIVITY_DATA_KEY, msgPayload);
				msgDataMap.put(StreamsConstants.TRANSPORT_KEY, StreamsConstants.TRANSPORT_KAFKA);

				return msgDataMap;
			}
		}
		LOGGER.log(OpLevel.ERROR, StreamsResources.getString("KafkaStream.failed.consumer"));
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanup() {
		closed.set(true);
		consumer.shutdown();

		super.cleanup();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getActivityPosition() {
		return partition;
	}

}
