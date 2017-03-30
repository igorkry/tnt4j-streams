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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.lang.IllegalStateException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.JMSStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Implements a JMS message transported activity stream, where each JMS message payload data carried data is assumed to
 * represent a single activity or event which should be recorded.
 * <p>
 * This activity stream requires parsers that can support JMS {@link Message} data.
 * <p>
 * This activity stream supports the following properties (in addition to those supported by
 * {@link AbstractBufferedStream}):
 * <ul>
 * <li>ServerURI - JMS server URL. (Required)</li>
 * <li>Queue - queue destination name. (Required - just one of 'Queue' or 'Topic')</li>
 * <li>Topic - topic destination name. (Required - just one of 'Queue' or 'Topic')</li>
 * <li>JNDIFactory - JNDI context factory name. (Required)</li>
 * <li>JMSConnFactory - JMS connection factory name. (Required)</li>
 * </ul>
 *
 * @version $Revision: 1$
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class JMSStream extends AbstractBufferedStream<Message> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(JMSStream.class);

	// Stream properties
	private String serverURL = null;
	private String queueName = null;
	private String topicName = null;
	private String jndiFactory = null;
	private String jmsConnFactory = null;

	private JMSDataReceiver jmsDataReceiver;

	/**
	 * Constructs an empty JMSStream. Requires configuration settings to set input stream source.
	 */
	public JMSStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_SERVER_URI.equalsIgnoreCase(name)) {
			return serverURL;
		}
		if (StreamProperties.PROP_QUEUE_NAME.equalsIgnoreCase(name)) {
			return queueName;
		}
		if (StreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
			return topicName;
		}
		if (StreamProperties.PROP_JNDI_FACTORY.equalsIgnoreCase(name)) {
			return jndiFactory;
		}
		if (JMSStreamConstants.PROP_JMS_CONN_FACTORY.equalsIgnoreCase(name)) {
			return jmsConnFactory;
		}

		return super.getProperty(name);
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamProperties.PROP_SERVER_URI.equalsIgnoreCase(name)) {
				serverURL = value;
			} else if (StreamProperties.PROP_QUEUE_NAME.equalsIgnoreCase(name)) {
				if (StringUtils.isNotEmpty(topicName)) {
					throw new IllegalStateException(StreamsResources.getStringFormatted(
							StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.cannot.set.both",
							StreamProperties.PROP_QUEUE_NAME, StreamProperties.PROP_TOPIC_NAME));
				}
				queueName = value;
			} else if (StreamProperties.PROP_TOPIC_NAME.equalsIgnoreCase(name)) {
				if (StringUtils.isNotEmpty(queueName)) {
					throw new IllegalStateException(StreamsResources.getStringFormatted(
							StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.cannot.set.both",
							StreamProperties.PROP_QUEUE_NAME, StreamProperties.PROP_TOPIC_NAME));
				}
				topicName = value;
			} else if (StreamProperties.PROP_JNDI_FACTORY.equalsIgnoreCase(name)) {
				jndiFactory = value;
			} else if (JMSStreamConstants.PROP_JMS_CONN_FACTORY.equalsIgnoreCase(name)) {
				jmsConnFactory = value;
			}
		}
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		if (StringUtils.isEmpty(serverURL)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", StreamProperties.PROP_SERVER_URI));
		}

		if (StringUtils.isEmpty(queueName) && StringUtils.isEmpty(topicName)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined.one.of", StreamProperties.PROP_QUEUE_NAME,
					StreamProperties.PROP_TOPIC_NAME));
		}

		jmsDataReceiver = new JMSDataReceiver();
		Hashtable<String, String> env = new Hashtable<>(2);
		env.put(Context.INITIAL_CONTEXT_FACTORY, jndiFactory);
		env.put(Context.PROVIDER_URL, serverURL);

		Context ic = new InitialContext(env);

		jmsDataReceiver.initialize(ic, StringUtils.isEmpty(queueName) ? topicName : queueName, jmsConnFactory);
	}

	@Override
	protected void start() throws Exception {
		super.start();

		jmsDataReceiver.start();

		logger().log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "TNTInputStream.stream.start"),
				getClass().getSimpleName(), getName());
	}

	@Override
	protected void cleanup() {
		if (jmsDataReceiver != null) {
			jmsDataReceiver.shutdown();
		}

		super.cleanup();
	}

	@Override
	protected boolean isInputEnded() {
		return jmsDataReceiver.isInputEnded();
	}

	@Override
	protected long getActivityItemByteSize(Message itemMsg) {
		try {
			if (itemMsg instanceof BytesMessage) {
				return ((BytesMessage) itemMsg).getBodyLength();
			} else if (itemMsg instanceof MapMessage) {

			} else if (itemMsg instanceof ObjectMessage) {

			} else if (itemMsg instanceof StreamMessage) {

			} else if (itemMsg instanceof TextMessage) {
				String text = ((TextMessage) itemMsg).getText();
				return text == null ? 0 : text.length();
			}
		} catch (JMSException exc) {
		}

		return 0; // TODO
	}

	private class JMSDataReceiver extends InputProcessor implements MessageListener {

		private ConnectionFactory jmsConFactory;
		private Connection jmsCon;
		private Session jmsSession;
		private MessageConsumer jmsReceiver;
		private Destination destination;

		private JMSDataReceiver() {
			super("JMSStream.JMSDataReceiver"); // NON-NLS
		}

		/**
		 * Input data receiver initialization - JMS client configuration.
		 *
		 * @param params
		 *            initialization parameters array
		 *
		 * @throws Exception
		 *             if fails to initialize data receiver and configure JMS client
		 */
		@Override
		protected void initialize(Object... params) throws Exception {
			Context ctx = (Context) params[0];
			String destinationName = (String) params[1];
			String jmsConnFactoryName = (String) params[2];

			jmsConFactory = (ConnectionFactory) ctx.lookup(jmsConnFactoryName);
			jmsCon = jmsConFactory.createConnection();
			jmsSession = jmsCon.createSession(false, Session.AUTO_ACKNOWLEDGE);
			destination = (Destination) ctx.lookup(destinationName);
			jmsReceiver = jmsSession.createConsumer(destination);
			jmsReceiver.setMessageListener(this);
		}

		/**
		 * Starts JMS client to receive incoming data. Shuts down this data receiver if exception occurs.
		 */
		@Override
		public void run() {
			if (jmsCon != null) {
				try {
					jmsCon.start();
				} catch (JMSException exc) {
					logger().log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"AbstractBufferedStream.input.start.failed"), exc);
					shutdown();
				}
			}
		}

		/**
		 * Closes JMS client objects.
		 *
		 * @throws JMSException
		 *             if JMS fails to close objects due to internal error
		 */
		@Override
		void closeInternals() throws JMSException {
			jmsReceiver.close();
			jmsSession.close();
			jmsCon.close();
		}

		/**
		 * Adds received JMS message to input buffer destination.
		 *
		 * @param msg
		 *            received JMS message
		 */
		@Override
		public void onMessage(Message msg) {
			if (msg == null) {
				return;
			}

			addInputToBuffer(msg);
		}
	}
}
