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

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.Whitelist;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.parsers.ActivityParser;
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
 * This activity stream requires parsers that can support {@code String} data.
 * </p>
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>Topic - topic name to listen.</li>
 * <li>List of properties used by Kafka API. i.e zookeeper.connect, group.id
 * </li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see ActivityParser#isDataClassSupported(Object)
 * @see ConsumerConfig
 */
public class KafkaStream extends TNTInputStream {

	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(KafkaStream.class);

	private final AtomicBoolean closed = new AtomicBoolean(false);

	private ConsumerConnector consumer;
	private ConsumerConfig kafkaProperties;
	private String topicName;

	private Iterator<MessageAndMetadata<byte[], byte[]>> messageBuffer;
	private int partition = 0;

	/**
	 * Constructs an KafkaStream.
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
				topicName = value;
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
			return topicName;
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

		consumer = Consumer.createJavaConsumerConnector(kafkaProperties);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns a string from next record received over Kafka
	 * consumer.
	 * </p>
	 */
	@Override
	public Object getNextItem() throws Throwable {
		while (!closed.get()) {
			if (messageBuffer == null || !messageBuffer.hasNext()) {
				final List<kafka.consumer.KafkaStream<byte[], byte[]>> streams = consumer
						.createMessageStreamsByFilter(new Whitelist(topicName));
				messageBuffer = streams.get(0).iterator();
			}

			if (messageBuffer != null && messageBuffer.hasNext()) {
				MessageAndMetadata<byte[], byte[]> msg = messageBuffer.next();

				// TODO: pass byte[] to parser
				return Utils.cleanActivityData(new String(msg.message(), Charset.forName("UTF-8")));
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
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getActivityPosition() {
		return partition;
	}

}
