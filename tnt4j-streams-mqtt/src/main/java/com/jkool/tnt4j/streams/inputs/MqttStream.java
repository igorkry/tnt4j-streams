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

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.utils.MqttStreamConstants;
import com.jkool.tnt4j.streams.utils.StreamsConstants;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a MQTT topics transmitted activity stream, where each message body
 * is assumed to represent a single activity or event which should be recorded.
 * Topic to listen is defined using "Topic" property in stream configuration.
 * <p>
 * This activity stream requires parsers that can support {@code Map} data. On
 * message reception message data is packed into {@code Map} filling these
 * entries:
 * <ul>
 * <li>ActivityTopic - topic name message with activity data was received.</li>
 * <li>ActivityData - raw activity data as {@code byte[]} retrieved from
 * message.</li>
 * <li>ActivityTransport - activity transport definition: 'Mqtt'.</li>
 * </ul>
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>ServerURI - Mqtt server URI. (Required)</li>
 * <li>Topic - topic name to listen. (Required)</li>
 * <li>UserName - authentication user name. (Optional)</li>
 * <li>Password - user password. (Optional)</li>
 * <li>UseSSL - flag identifying to use SSL. (Optional)</li>
 * <li>Keystore - keystore path. (Optional)</li>
 * <li>KeystorePass - keystore password. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see com.jkool.tnt4j.streams.parsers.ActivityMapParser
 * @see org.eclipse.paho.client.mqttv3.MqttClient
 * @see org.eclipse.paho.client.mqttv3.MqttCallback
 */
public class MqttStream extends AbstractBufferedStream<Map<String, ?>> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(MqttStream.class);

	private static final String SSL_PROTOCOL = "SSL"; // NON-NLS
	private static final String KEYSTORE_TYPE = KeyStore.getDefaultType();

	// Stream properties
	private String serverURI = null;
	private String userName = null;
	private String password = null;
	private String topic = null;
	private boolean useSSL = false;
	private String keystore = null;
	private String keystorePass = null;

	private MqttDataReceiver mqttDataReceiver;

	/**
	 * Construct empty MqttStream. Requires configuration settings to set input
	 * stream source.
	 */
	public MqttStream() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getProperty(String name) {
		if (StreamsConfig.PROP_SERVER_URI.equalsIgnoreCase(name)) {
			return serverURI;
		}
		if (StreamsConfig.PROP_USERNAME.equalsIgnoreCase(name)) {
			return userName;
		}
		if (StreamsConfig.PROP_PASSWORD.equalsIgnoreCase(name)) {
			return password;
		}
		if (StreamsConfig.PROP_TOPIC_STRING.equalsIgnoreCase(name)) {
			return topic;
		}
		if (StreamsConfig.PROP_USE_SSL.equalsIgnoreCase(name)) {
			return useSSL;
		}
		if (StreamsConfig.PROP_KEYSTORE.equalsIgnoreCase(name)) {
			return keystore;
		}
		if (StreamsConfig.PROP_KEYSTORE_PASS.equalsIgnoreCase(name)) {
			return keystorePass;
		}

		return super.getProperty(name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamsConfig.PROP_SERVER_URI.equalsIgnoreCase(name)) {
				serverURI = value;
			} else if (StreamsConfig.PROP_USERNAME.equalsIgnoreCase(name)) {
				userName = value;
			} else if (StreamsConfig.PROP_PASSWORD.equalsIgnoreCase(name)) {
				password = value;
			} else if (StreamsConfig.PROP_TOPIC_STRING.equalsIgnoreCase(name)) {
				topic = value;
			} else if (StreamsConfig.PROP_USE_SSL.equalsIgnoreCase(name)) {
				useSSL = Boolean.parseBoolean(value);
			} else if (StreamsConfig.PROP_KEYSTORE.equalsIgnoreCase(name)) {
				keystore = value;
			} else if (StreamsConfig.PROP_KEYSTORE_PASS.equalsIgnoreCase(name)) {
				keystorePass = value;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize() throws Exception {
		super.initialize();
		if (StringUtils.isEmpty(serverURI)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"TNTInputStream.property.undefined", StreamsConfig.PROP_SERVER_URI));
		}
		if (StringUtils.isEmpty(topic)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
					"TNTInputStream.property.undefined", StreamsConfig.PROP_TOPIC_STRING));
		}

		mqttDataReceiver = new MqttDataReceiver();
		mqttDataReceiver.initialize();

		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(MqttStreamConstants.RESOURCE_BUNDLE_MQTT, "MqttStream.stream.ready"));

		mqttDataReceiver.start();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanup() {
		try {
			mqttDataReceiver.close();
		} catch (MqttException exc) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getString(MqttStreamConstants.RESOURCE_BUNDLE_MQTT,
					"MqttStream.error.closing.receiver"), exc);
		}

		super.cleanup();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isInputEnded() {
		return mqttDataReceiver.isInputEnded();
	}

	/**
	 * Mqtt messages receiver thread. It implements {@code MqttCallback}
	 * interface and initiates Mqtt client to receive and handle Mqtt messages
	 * data.
	 */
	private class MqttDataReceiver extends InputProcessor implements MqttCallback {

		private MqttClient client;

		private MqttDataReceiver() {
			super("MqttStream.MqttDataReceiver"); // NON-NLS
		}

		private void initialize() throws MqttException, GeneralSecurityException, IOException {
			client = new MqttClient(serverURI, MqttClient.generateClientId(), new MemoryPersistence());
			client.setCallback(this);

			MqttConnectOptions options = new MqttConnectOptions();
			if (StringUtils.isNotEmpty(userName)) {
				options.setUserName(userName);
				options.setPassword((password == null ? "" : password).toCharArray());
			}

			if (useSSL) {
				SSLContext sslContext = SSLContext.getInstance(SSL_PROTOCOL);

				KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
				FileInputStream fis = null;
				try {
					keyStore.load(new FileInputStream(keystore), keystorePass.toCharArray());
				} finally {
					Utils.close(fis);
				}

				TrustManagerFactory trustManagerFactory = TrustManagerFactory
						.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				trustManagerFactory.init(keyStore);
				sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

				options.setSocketFactory(sslContext.getSocketFactory());
			}

			client.connect(options);
			client.subscribe(topic);
		}

		/**
		 * Closes Mqtt objects.
		 *
		 * @throws MqttException
		 *             if Mqtt fails to disconnect client due to internal error
		 */
		void close() throws MqttException {
			halt();
		}

		private void closeConnection() throws MqttException {
			client.unsubscribe(topic);
			client.disconnect();
			// client.close();
		}

		@Override
		public void connectionLost(Throwable cause) {
			LOGGER.log(OpLevel.ERROR,
					StreamsResources.getString(MqttStreamConstants.RESOURCE_BUNDLE_MQTT, "MqttStream.connection.lost"),
					cause);

			try {
				closeConnection();
			} catch (MqttException exc) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getString(MqttStreamConstants.RESOURCE_BUNDLE_MQTT,
						"MqttStream.error.closing.receiver"), exc);
			}

			try {
				initialize();
			} catch (Exception exc) {
				LOGGER.log(OpLevel.WARNING, StreamsResources.getString(MqttStreamConstants.RESOURCE_BUNDLE_MQTT,
						"MqttStream.error.reconnecting.receiver"), exc);
			}
		}

		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			if (message == null) {
				return;
			}

			String msgData = Utils.getString(message.getPayload());

			LOGGER.log(OpLevel.DEBUG, StreamsResources.getStringFormatted(MqttStreamConstants.RESOURCE_BUNDLE_MQTT,
					"MqttStream.message.received", msgData));

			Map<String, Object> msgDataMap = new HashMap<String, Object>();

			if (ArrayUtils.isNotEmpty(message.getPayload())) {
				msgDataMap.put(StreamsConstants.TOPIC_KEY, topic);
				msgDataMap.put(StreamsConstants.ACTIVITY_DATA_KEY, message.getPayload());
				msgDataMap.put(StreamsConstants.TRANSPORT_KEY, MqttStreamConstants.TRANSPORT_MQTT);
			}

			if (!msgDataMap.isEmpty()) {
				addInputToBuffer(msgDataMap);
			}
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {
		}
	}

}
