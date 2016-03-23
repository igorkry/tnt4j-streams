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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.*;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import com.jkool.tnt4j.streams.configure.StreamProperties;
import com.jkool.tnt4j.streams.utils.StreamsConstants;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a Http requests transmitted activity stream, where each request
 * body is assumed to represent:
 * <ul>
 * <li>a single activity event sent as form data (parameters keys/values set)
 * </li>
 * <li>a byte array as request payload data(i.e. log file contents)</li>
 * </ul>
 * Running this stream Http server is started on configuration defined port.
 * <p>
 * This activity stream requires parsers that can support {@link Map} data. On
 * message reception message data is packed into {@link Map} filling these
 * entries:
 * <ul>
 * <li>ActivityData - raw activity data as {@code byte[]} retrieved from http
 * request.</li>
 * <li>ActivityTransport - activity transport definition: 'Http'.</li>
 * </ul>
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>Port - port number to run Http server. (Optional - default 8080 used if
 * not defined)</li>
 * <li>UseSSL - flag identifying to use SSL. (Optional)</li>
 * <li>Keystore - keystore path. (Optional)</li>
 * <li>KeystorePass - keystore password. (Optional)</li>
 * <li>KeyPass - key password. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1$
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 * @see com.jkool.tnt4j.streams.parsers.ActivityMapParser
 */
public class HttpStream extends AbstractBufferedStream<Map<String, ?>> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(HttpStream.class);

	private static final String HTML_MSG_PATTERN = "<html><body><h1>{0}</h1></body></html>"; // NON-NLS

	private static final int DEFAULT_HTTP_PORT = 8080;
	private static final int DEFAULT_HTTPS_PORT = 8443;

	private static final int SOCKET_TIMEOUT = 15000;
	private static final boolean TCP_NO_DELAY = true;

	private Integer serverPort = null;
	private boolean useSSL = false;
	private String keystore = null;
	private String keystorePass = null;
	private String keyPass = null;

	private HttpStreamRequestHandler requestHandler;

	/**
	 * Construct empty HttpStream. Requires configuration settings to set input
	 * stream source.
	 */
	public HttpStream() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
			return serverPort;
		}
		if (StreamProperties.PROP_USE_SSL.equalsIgnoreCase(name)) {
			return useSSL;
		}
		if (StreamProperties.PROP_KEYSTORE.equalsIgnoreCase(name)) {
			return keystore;
		}
		if (StreamProperties.PROP_KEYSTORE_PASS.equalsIgnoreCase(name)) {
			return keystorePass;
		}
		if (StreamProperties.PROP_KEY_PASS.equalsIgnoreCase(name)) {
			return keyPass;
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
			if (StreamProperties.PROP_PORT.equalsIgnoreCase(name)) {
				serverPort = Integer.valueOf(value);
			} else if (StreamProperties.PROP_USE_SSL.equalsIgnoreCase(name)) {
				useSSL = Boolean.parseBoolean(value);
			} else if (StreamProperties.PROP_KEYSTORE.equalsIgnoreCase(name)) {
				keystore = value;
			} else if (StreamProperties.PROP_KEYSTORE_PASS.equalsIgnoreCase(name)) {
				keystorePass = value;
			} else if (StreamProperties.PROP_KEY_PASS.equalsIgnoreCase(name)) {
				keyPass = value;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize() throws Exception {
		super.initialize();

		requestHandler = new HttpStreamRequestHandler();
		requestHandler.initialize();

		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "HttpStream.stream.ready"));

		requestHandler.start();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanup() {
		requestHandler.shutdown();

		super.cleanup();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isInputEnded() {
		return requestHandler.isInputEnded();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected long getActivityItemByteSize(Map<String, ?> itemMap) {
		byte[] payload = (byte[]) itemMap.get(StreamsConstants.ACTIVITY_DATA_KEY);

		return payload == null ? 0 : payload.length;
	}

	private static class HttpStreamExceptionLogger implements ExceptionLogger {
		@Override
		public void log(final Exception ex) {
			if (ex instanceof SocketTimeoutException) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
						"HttpStream.connection.timed.out"));
			} else if (ex instanceof ConnectionClosedException) {
				LOGGER.log(OpLevel.ERROR, String.valueOf(ex.getLocalizedMessage()));
				// } else if (ex instanceof SocketException) {
				// LOGGER.log(OpLevel.ERROR, ex.getMessage());
			} else {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
						"HttpStream.http.server.exception"), ex);
			}
		}
	}

	private class HttpStreamRequestHandler extends InputProcessor implements HttpRequestHandler {

		private HttpServer server;

		/**
		 * Instantiates a new Http stream request handler.
		 */
		public HttpStreamRequestHandler() {
			super("HttpStream.HttpStreamRequestHandler"); // NON-NLS
		}

		/**
		 * Initialize.
		 *
		 * @throws Exception
		 *             the exception
		 */
		void initialize() throws Exception {
			int port = DEFAULT_HTTP_PORT;
			if (serverPort != null) {
				port = serverPort;
			}

			SSLContext sslcontext = null;
			if (useSSL) {
				// Initialize SSL context
				URL url = new URL(keystore);
				sslcontext = SSLContexts.custom()
						.loadKeyMaterial(url, (keystorePass == null ? "" : keystorePass).toCharArray(),
								(keyPass == null ? "" : keyPass).toCharArray())
						.build();
			}

			SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(SOCKET_TIMEOUT).setTcpNoDelay(TCP_NO_DELAY)
					.build();
			server = ServerBootstrap.bootstrap().setListenerPort(port).setServerInfo("TNT4J-Streams-HttpStream") // NON-NLS
					.setSocketConfig(socketConfig).setSslContext(sslcontext)
					.setExceptionLogger(new HttpStreamExceptionLogger()).registerHandler("*", this).create();
		}

		@Override
		public void run() {
			if (server != null) {
				try {
					server.start();
				} catch (IOException exc) {
					LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
							"HttpStream.could.not.receive.data"), exc);
					shutdown();
				}

				// try {
				// server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
				// } catch (InterruptedException exc) {
				//
				// }

				Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
					@Override
					public void run() {
						if (server != null) {
							server.shutdown(5, TimeUnit.SECONDS);
							// server.stop();
						}
					}
				}));
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		void close() throws Exception {
			if (server != null) {
				// server.shutdown(5, TimeUnit.SECONDS);
				server.stop();
				server = null;
			}

			super.close();
		}

		public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context)
				throws HttpException, IOException {

			if (!(request instanceof HttpEntityEnclosingRequest)) {
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
				String msg = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
						"HttpStream.bad.http.request");
				response.setEntity(createHtmlStringEntity(msg));
				LOGGER.log(OpLevel.DEBUG, msg);
			} else {
				HttpEntity reqEntity = ((HttpEntityEnclosingRequest) request).getEntity();
				boolean activityAvailable = false;
				boolean added = false;

				if (reqEntity != null) {
					Map<String, Object> reqMap = new HashMap<String, Object>();
					if (URLEncodedUtils.isEncoded(reqEntity)) {
						List<NameValuePair> reqParams = URLEncodedUtils.parse(reqEntity);
						if (reqParams != null) {
							for (NameValuePair param : reqParams) {
								reqMap.put(param.getName(), param.getValue());
							}
						}
					} else {
						byte[] bytes = EntityUtils.toByteArray(reqEntity);
						if (ArrayUtils.isNotEmpty(bytes)) {
							reqMap.put(StreamsConstants.ACTIVITY_DATA_KEY, bytes);
						}
					}

					if (!reqMap.isEmpty()) {
						activityAvailable = true;
						reqMap.put(StreamsConstants.TRANSPORT_KEY, StreamsConstants.TRANSPORT_HTTP);
						added = addInputToBuffer(reqMap);
					}
				}

				if (!activityAvailable) {
					response.setStatusCode(HttpStatus.SC_NO_CONTENT);
					String msg = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
							"HttpStream.no.activity");
					response.setEntity(createHtmlStringEntity(msg));
					LOGGER.log(OpLevel.DEBUG, msg);
				} else if (!added) {
					response.setStatusCode(HttpStatus.SC_INSUFFICIENT_STORAGE);
					String msg = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
							"HttpStream.activities.buffer.size.limit");
					response.setEntity(createHtmlStringEntity(msg));
					LOGGER.log(OpLevel.WARNING, msg);
				} else {
					response.setStatusCode(HttpStatus.SC_OK);
					String msg = StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "HttpStream.ok");
					response.setEntity(createHtmlStringEntity(msg));
					LOGGER.log(OpLevel.DEBUG, msg);
				}

			}
		}

		private StringEntity createHtmlStringEntity(String msg) {
			StringEntity entity = new StringEntity(MessageFormat.format(HTML_MSG_PATTERN, msg),
					ContentType.create("text/html", "UTF-8")); // NON-NLS

			return entity;
		}
	}
}