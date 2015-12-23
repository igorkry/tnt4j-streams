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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.http.*;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.ssl.SSLContexts;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * TODO
 *
 * @version $Revision: 1$
 */
public class HttpStream extends AbstractBufferedStream<BufferedHttpEntity> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(CharacterStream.class);

	private static final int DEFAULT_HTTP_PORT = 8080;
	private static final int DEFAULT_HTTPS_PORT = 8443;

	private static final int SOCKET_TIMEOUT = 15000;
	private static final boolean TCP_NO_DELAY = true;

	private Integer serverPort = null;
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
		if (StreamsConfig.PROP_PORT.equalsIgnoreCase(name)) {
			return serverPort;
		}
		if (StreamsConfig.PROP_KEYSTORE.equalsIgnoreCase(name)) {
			return keystore;
		}
		if (StreamsConfig.PROP_KEYSTORE_PASS.equalsIgnoreCase(name)) {
			return keystorePass;
		}
		if (StreamsConfig.PROP_KEY_PASS.equalsIgnoreCase(name)) {
			return keyPass;
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
			if (StreamsConfig.PROP_PORT.equalsIgnoreCase(name)) {
				serverPort = Integer.valueOf(value);
			} else if (StreamsConfig.PROP_KEYSTORE.equalsIgnoreCase(name)) {
				keystore = value;
			} else if (StreamsConfig.PROP_KEYSTORE_PASS.equalsIgnoreCase(name)) {
				keystorePass = value;
			} else if (StreamsConfig.PROP_KEY_PASS.equalsIgnoreCase(name)) {
				keyPass = value;
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize() throws Throwable {
		super.initialize();

		Thread th = new Thread(new Runnable() {
			@Override
			public void run() {
				try {

					int port = DEFAULT_HTTP_PORT;
					if (serverPort != null) {
						port = serverPort;
					}

					SSLContext sslcontext = null;
					if (port == DEFAULT_HTTPS_PORT) {
						// Initialize SSL context
						URL url = new URL(keystore);
						sslcontext = SSLContexts.custom()
								.loadKeyMaterial(url, keystorePass.toCharArray(), keystorePass.toCharArray()).build();
					}

					SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(SOCKET_TIMEOUT)
							.setTcpNoDelay(TCP_NO_DELAY).build();
					final HttpServer server = ServerBootstrap.bootstrap().setListenerPort(port)
							.setServerInfo("TNT4-Streams-HttpStream").setSocketConfig(socketConfig)
							.setSslContext(sslcontext).setExceptionLogger(new HttpStreamExceptionLogger())
							.registerHandler("*", new HttpStreamRequestHandler()).create();

					server.start();
					server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

					Runtime.getRuntime().addShutdownHook(new Thread() {
						@Override
						public void run() {
							server.shutdown(5, TimeUnit.SECONDS);
						}
					});
				} catch (Exception exc) {
					LOGGER.log(OpLevel.ERROR, "TODO", exc);
				}
			}
		});

		th.start();
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

	private static class HttpStreamExceptionLogger implements ExceptionLogger {
		@Override
		public void log(final Exception ex) {
			if (ex instanceof SocketTimeoutException) {
				LOGGER.log(OpLevel.ERROR, "Connection timed out");
			} else if (ex instanceof ConnectionClosedException) {
				LOGGER.log(OpLevel.ERROR, ex.getMessage());
			} else {
				LOGGER.log(OpLevel.ERROR, "HTTP server exception", ex);
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
			if (port == DEFAULT_HTTPS_PORT) {
				// Initialize SSL context
				URL url = new URL(keystore);
				sslcontext = SSLContexts.custom()
						.loadKeyMaterial(url, keystorePass.toCharArray(), keyPass.toCharArray()).build();
			}

			SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(SOCKET_TIMEOUT).setTcpNoDelay(TCP_NO_DELAY)
					.build();
			server = ServerBootstrap.bootstrap().setListenerPort(port).setServerInfo("TNT4-Streams-HttpStream")
					.setSocketConfig(socketConfig).setSslContext(sslcontext)
					.setExceptionLogger(new HttpStreamExceptionLogger()).registerHandler("*", this).create();

			server.start();
			server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					server.shutdown(5, TimeUnit.SECONDS);
				}
			});
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		void close() throws Exception {
			server.shutdown(5, TimeUnit.SECONDS);

			super.close();
		}

		public void handle(final HttpRequest request, final HttpResponse response, final HttpContext context)
				throws HttpException, IOException {

			if (!(request instanceof HttpEntityEnclosingRequest)) {
				response.setStatusCode(HttpStatus.SC_NO_CONTENT);
				StringEntity entity = new StringEntity(
						"<html><body><h1>No activity content found!..</h1></body></html>",
						ContentType.create("text/html", "UTF-8"));
				response.setEntity(entity);
				LOGGER.log(OpLevel.DEBUG, "No activity content found!..");
			} else {
				HttpEntity reqEntity = ((HttpEntityEnclosingRequest) request).getEntity();
				boolean added = false;
				if (reqEntity != null) {
					reqEntity = new BufferedHttpEntity(reqEntity);
					addInputToBuffer((BufferedHttpEntity) reqEntity);
					added = true;
				}
				if (added) {
					response.setStatusCode(HttpStatus.SC_OK);
					StringEntity entity = new StringEntity("<html><body><h1>OK</h1></body></html>",
							ContentType.create("text/html", "UTF-8"));
					response.setEntity(entity);
					LOGGER.log(OpLevel.DEBUG, "OK");
				} else {
					response.setStatusCode(HttpStatus.SC_INSUFFICIENT_STORAGE);
					StringEntity entity = new StringEntity(
							"<html><body><h1>Activities buffer size limit is reached and activity entry is skipped!..</h1></body></html>",
							ContentType.create("text/html", "UTF-8"));
					response.setEntity(entity);
					LOGGER.log(OpLevel.WARNING,
							"Activities buffer size limit is reached and activity entry is skipped!..");
				}
			}
		}
	}
}
