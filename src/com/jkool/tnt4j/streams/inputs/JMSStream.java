/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jkool.tnt4j.streams.inputs;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a JMS message transported activity stream, where each JMS message
 * payload data carried data is assumed to represent a single activity or event
 * which should be recorded.
 *
 * </p>
 * <p>
 * This activity stream requires parsers that can support JMS {@code Message}
 * data.
 * </p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>ServerURI - JMS server URL</li>
 * <li>Queue - queue name</li>
 * <li>JNDIFactory - JNDI factory name</li>
 * <li>JMSFactory - JMS factory name</li>
 * </ul>
 *
 * @version $Revision: 1$
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class JMSStream extends AbstractBufferedStream<Message> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(JMSStream.class);

	// Stream properties
	private String serverURL = null;
	private String queueName = null;
	private String jndiFactory = null;
	private String jmsFactory = null;

	private JMSDataReceiver jmsDataReceiver;

	/**
	 * Construct empty JMSStream. Requires configuration settings to set input
	 * stream source.
	 */
	public JMSStream() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getProperty(String name) {
		if (StreamsConfig.PROP_SERVER_URI.equalsIgnoreCase(name)) {
			return serverURL;
		}
		if (StreamsConfig.PROP_QUEUE_NAME.equalsIgnoreCase(name)) {
			return queueName;
		}
		if (StreamsConfig.PROP_JNDI_FACTORY.equalsIgnoreCase(name)) {
			return jndiFactory;
		}
		if (StreamsConfig.PROP_JMS_FACTORY.equalsIgnoreCase(name)) {
			return jmsFactory;
		}

		return super.getProperty(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Throwable {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamsConfig.PROP_SERVER_URI.equalsIgnoreCase(name)) {
				serverURL = value;
			} else if (StreamsConfig.PROP_QUEUE_NAME.equalsIgnoreCase(name)) {
				queueName = value;
			} else if (StreamsConfig.PROP_JNDI_FACTORY.equalsIgnoreCase(name)) {
				jndiFactory = value;
			} else if (StreamsConfig.PROP_JMS_FACTORY.equalsIgnoreCase(name)) {
				jmsFactory = value;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize() throws Throwable {
		super.initialize();

		jmsDataReceiver = new JMSDataReceiver();
		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, jndiFactory);
		env.put(Context.PROVIDER_URL, serverURL);

		Context ic = new InitialContext(env);

		jmsDataReceiver.initialize(ic, queueName, jmsFactory);

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getString("JMSStream.stream.ready"));

		jmsDataReceiver.start();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanup() {
		jmsDataReceiver.shutdown();

		super.cleanup();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isInputEnded() {
		return jmsDataReceiver.isInputEnded();
	}

	private class JMSDataReceiver extends InputProcessor implements MessageListener {

		private QueueConnectionFactory qConFactory;
		private QueueConnection qCon;
		private QueueSession qSession;
		private QueueReceiver qReceiver;
		private Queue queue;

		private JMSDataReceiver() {

			super("JMSStream.JMSDataReceiver"); // NON-NLS
		}

		private void initialize(Context ctx, String queueName, String jmsFactory) throws NamingException, JMSException {
			qConFactory = (QueueConnectionFactory) ctx.lookup(jmsFactory);
			qCon = qConFactory.createQueueConnection();
			qSession = qCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			queue = (Queue) ctx.lookup(queueName);
			qReceiver = qSession.createReceiver(queue);
			qReceiver.setMessageListener(this);
			qCon.start();
		}

		/**
		 * Adds received JMS message to input buffer queue.
		 *
		 * @param msg
		 *            received JMS message
		 *
		 * @see javax.jms.MessageListener#onMessage(Message)
		 */
		@Override
		public void onMessage(Message msg) {
			if (msg == null) {
				return;
			}

			addInputToBuffer(msg);
		}

		/**
		 * Closes JMS objects.
		 *
		 * @throws JMSException
		 *             if JMS fails to close objects due to internal error
		 */
		void close() throws Exception {
			qReceiver.close();
			qSession.close();
			qCon.close();

			super.close();
		}
	}
}
