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
import java.net.Socket;
import java.util.*;

import com.jitlogic.zico.core.TraceRecordStore;
import com.jitlogic.zico.core.ZicoService;
import com.jitlogic.zorka.common.tracedata.HelloRequest;
import com.jitlogic.zorka.common.tracedata.Symbol;
import com.jitlogic.zorka.common.tracedata.SymbolRegistry;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.zico.ZicoDataProcessor;
import com.jitlogic.zorka.common.zico.ZicoDataProcessorFactory;
import com.jitlogic.zorka.common.zico.ZicoException;
import com.jitlogic.zorka.common.zico.ZicoPacket;
import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * The type Zorka connector.
 */
public class ZorkaConnector extends AbstractBufferedStream<Object>implements ZicoDataProcessor {
	private static final int CONNECTION_TIMEOUT = 10 * 1000;

	private static final int MAX_THREADS = 5;

	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ZorkaConnector.class);

	// Object may be Symbol or TraceRecord

	private TraceRecordStore traceDataStore;
	private TraceRecordStore traceIndexStore;

	// for persisiting symbols inf ile System use PersistentSymbolRegistry
	private SymbolRegistry symbolRegistry;
	private Integer socketPort = 8640;

	private static final String HOSTNAME = "localhost";

	/**
	 * Instantiates a new Zorka connector.
	 *
	 * @param logger
	 *            the logger
	 */
	protected ZorkaConnector(EventSink logger) {
		super(logger);
	}

	/**
	 * Instantiates a new Zorka connector.
	 */
	public ZorkaConnector() {
		super(LOGGER);
	}

	@Override
	public Object getProperty(String name) {
		if (StreamsConfig.PROP_PORT.equalsIgnoreCase(name)) {
			return socketPort;
		}
		return null;
	}

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
				socketPort = Integer.valueOf(value);
			}
		}

	}

	@Override
	protected void initialize() throws Throwable {
		super.initialize();
		symbolRegistry = new SymbolRegistry();
		// traceDataStore = new TraceRecordStore()
		// traceIn
		ZicoDataProcessorFactory zdf = new ZicoDataProcessorFactory() {

			@Override
			public ZicoDataProcessor get(Socket socket, HelloRequest hello) throws IOException {
				if (hello == null) {
					LOGGER.log(OpLevel.ERROR, "Received null HELLO packet.");
					throw new ZicoException(ZicoPacket.ZICO_BAD_REQUEST, "Null Hello packet.");
				}
				if (hello.getHostname() == null) {
					LOGGER.log(OpLevel.ERROR, "Received HELLO packet with null hostname.");
					throw new ZicoException(ZicoPacket.ZICO_BAD_REQUEST, "Null hostname.");
				}

				if ("BAD".equals(hello.getAuth())) {
					throw new ZicoException(ZicoPacket.ZICO_AUTH_ERROR, "Login failed.");
				}
				return ZorkaConnector.this;
			}
		};

		ZicoService zicoService = new ZicoService(zdf, "127.0.0.1", 8640, MAX_THREADS, CONNECTION_TIMEOUT);
		zicoService.start();
	}

	public void process(Object obj) throws IOException {
		if (obj instanceof Symbol) {
			Symbol symbol = (Symbol) obj;
			symbolRegistry.put(symbol.getId(), symbol.getName());
		}
		if (obj instanceof TraceRecord) {
			TraceRecord rec = (TraceRecord) obj;

			if (traceDataStore == null || traceIndexStore == null) {
				LOGGER.log(OpLevel.CRITICAL, "Configuration error");
			}

			processTraceRecursive(rec, rec.getChildren());
			addInputToBuffer(rec.getAttrs());
		}

	}

	private void processTraceRecursive(TraceRecord rec, List<TraceRecord> childrens) {
		if (childrens == null)
			return;

		for (TraceRecord children : childrens) {
			if (children.getAttrs() != null) {
				LOGGER.log(OpLevel.DEBUG, "Decorating child");
			}
			processTraceRecursive(rec, children.getChildren());
		}

	}

	public void commit() {
		LOGGER.log(OpLevel.DEBUG, "committing event");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isInputEnded() {
		return false; // TODO
	}

	/**
	 * Gets symbol registry.
	 *
	 * @return the symbol registry
	 */
	public SymbolRegistry getSymbolRegistry() {
		return symbolRegistry;
	}

	/**
	 * Gets trace data store.
	 *
	 * @return the trace data store
	 */
	public TraceRecordStore getTraceDataStore() {
		return traceDataStore;
	}

	/**
	 * Sets trace data store.
	 *
	 * @param traceDataStore
	 *            the trace data store
	 */
	public void setTraceDataStore(TraceRecordStore traceDataStore) {
		this.traceDataStore = traceDataStore;
	}

	/**
	 * Gets trace index store.
	 *
	 * @return the trace index store
	 */
	public TraceRecordStore getTraceIndexStore() {
		return traceIndexStore;
	}

	/**
	 * Sets trace index store.
	 *
	 * @param traceIndexStore
	 *            the trace index store
	 */
	public void setTraceIndexStore(TraceRecordStore traceIndexStore) {
		this.traceIndexStore = traceIndexStore;
	}

	/**
	 * Contains all boolean.
	 *
	 * @param c
	 *            the c
	 * @param a
	 *            the a
	 * @return the boolean
	 */
	public static boolean containsAll(Set<Integer> c, int[] a) {
		for (int i : a) {
			if (!c.contains(i)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * To int array int [ ].
	 *
	 * @param is
	 *            the is
	 * @return the int [ ]
	 */
	public static int[] toIntArray(Set<Integer> is) {
		int[] rslt = new int[is.size()];
		Iterator<Integer> iter = is.iterator();
		for (int i = 0; i < rslt.length; i++) {
			Integer x = iter.next();
			if (x != null) {
				rslt[i] = x;
			}
		}
		return rslt;
	}
}
