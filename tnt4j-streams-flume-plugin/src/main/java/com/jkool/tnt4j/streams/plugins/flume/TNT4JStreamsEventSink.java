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

package com.jkool.tnt4j.streams.plugins.flume;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import org.apache.commons.lang.StringUtils;
import org.apache.flume.*;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;

import com.google.common.base.Throwables;
import com.google.gson.Gson;
import com.jkool.tnt4j.streams.StreamsAgent;
import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Extends Apache Flume {@code AbstractSink} to deliver output events to JKool
 * Cloud service.
 * </p>
 *
 * @version $Revision: 1 $
 */
public class TNT4JStreamsEventSink extends AbstractSink implements Configurable {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(TNT4JStreamsEventSink.class);

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_HOST = "hostname"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_PORT = "port"; // NON-NLS

	/**
	 * Constant for name of built-in {@value} property.
	 */
	public static final String PROP_STREAM_CONFIG = "streamConfig"; // NON-NLS

	private static final String DEFAULT_HOST = "localhost"; // NON-NLS
	private static final int DEFAULT_PORT = 9595;
	private static final String DEFAULT_CONFIG_FILE_NAME = StreamsConfig.DFLT_CFG_FILE_NAME;

	private String hostname;
	private String streamConfig;
	private int port;
	private Socket socket;
	private PrintWriter out;

	/**
	 *
	 * @return status of event delivery
	 *
	 * @throws EventDeliveryException
	 *             when event can't be delivered
	 */
	@Override
	public Status process() throws EventDeliveryException {
		Status result = Status.READY;
		Channel channel = getChannel();
		Transaction transaction = null;
		final Event event;

		try {

			transaction = channel.getTransaction();
			transaction.begin();
			event = channel.take();

			if (event != null) {
				StreamsEvent eventE = new StreamsEvent(event, getName(), channel.getName());
				Gson gson = new Gson();
				String jsonEvent = gson.toJson(eventE);

				if (out == null || socket == null || socket.isClosed()) {
					openSocket();
				}

				out.println(jsonEvent);

				LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("TNT4JStreamsEventSink.sending.json"),
						hostname, port, jsonEvent);
			}
			transaction.commit();
		} catch (Exception ex) {
			String errorMsg = StreamsResources.getString("TNT4JStreamsEventSink.failed.to.publish");
			LOGGER.log(OpLevel.ERROR, errorMsg, ex);
			result = Status.BACKOFF;
			if (transaction != null) {
				try {
					transaction.rollback();
				} catch (Exception exc) {
					LOGGER.log(OpLevel.ERROR,
							StreamsResources.getString("TNT4JStreamsEventSink.transaction.rollback.failed"), exc);
					throw Throwables.propagate(exc);
				}
			}
			throw new EventDeliveryException(errorMsg, ex);
		} finally {
			if (transaction != null) {
				transaction.close();
			}
		}

		return result;
	}

	private void openSocket() throws IOException {
		closeSocket();

		socket = new Socket(hostname, port);
		out = new PrintWriter(socket.getOutputStream(), true);
	}

	private void closeSocket() throws IOException {
		if (socket != null && !socket.isClosed()) {
			socket.close();
		}
	}

	/**
	 * Starts Apache Flume output events sink to JKool Cloud service.
	 */
	@Override
	public synchronized void start() {
		LOGGER.log(OpLevel.INFO, StreamsResources.getString("TNT4JStreamsEventSink.plugin.starting"));
		if (StringUtils.isEmpty(hostname) || hostname.equals(DEFAULT_HOST)) {
			LOGGER.log(OpLevel.INFO, StreamsResources.getString("TNT4JStreamsEventSink.streams.starting"));
			StreamsAgent.runFromAPI(streamConfig);
		}
		try {
			openSocket();
		} catch (Exception exc) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getString("TNT4JStreamsEventSink.failed.open.socket"), exc);
		}

		super.start();
	}

	/**
	 * Stops Apache Flume output events sinking.
	 */
	@Override
	public synchronized void stop() {
		LOGGER.log(OpLevel.INFO, StreamsResources.getString("TNT4JStreamsEventSink.plugin.stopping"));
		try {
			closeSocket();
		} catch (IOException exc) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getString("TNT4JStreamsEventSink.io.exception.occurred"), exc);
		}
		super.stop();
		LOGGER.log(OpLevel.INFO, StreamsResources.getString("TNT4JStreamsEventSink.plugin.stopped"));
	}

	/**
	 * Configures Apache Flume JKool Cloud event sink (plugin).
	 *
	 * @param context
	 *            Apache Flume configuration context
	 */
	public void configure(Context context) {
		hostname = context.getString(PROP_HOST);
		String portStr = context.getString(PROP_PORT);
		streamConfig = context.getString(PROP_STREAM_CONFIG);

		if (hostname == null) {
			LOGGER.log(OpLevel.WARNING,
					StreamsResources.getStringFormatted("TNT4JStreamsEventSink.no.hostname.configured"), DEFAULT_HOST);
			hostname = DEFAULT_HOST;
		}

		if (portStr == null) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getStringFormatted("TNT4JStreamsEventSink.no.port.configured"),
					DEFAULT_PORT);
			port = DEFAULT_PORT;
		} else {
			try {
				port = Integer.parseInt(portStr);
			} catch (NumberFormatException exc) {
				LOGGER.log(OpLevel.WARNING,
						StreamsResources.getStringFormatted("TNT4JStreamsEventSink.parse.port.error"), portStr);
				port = DEFAULT_PORT;
			}
		}

		if (streamConfig == null) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getStringFormatted("TNT4JStreamsEventSink.no.tnt4j.config"),
					DEFAULT_CONFIG_FILE_NAME);
			streamConfig = DEFAULT_CONFIG_FILE_NAME;
		}
	}
}