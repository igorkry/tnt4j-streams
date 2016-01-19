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

import java.io.File;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Ignore;
import org.junit.Test;

import com.jitlogic.zorka.common.tracedata.Symbol;
import com.jitlogic.zorka.common.tracedata.TraceRecord;
import com.jitlogic.zorka.common.util.ZorkaUtil;
import com.jitlogic.zorka.common.zico.ZicoClientConnector;
import com.jitlogic.zorka.common.zico.ZicoDataLoader;
import com.jkool.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
@Ignore
public class ZorkaConnectorTest {

	// TODO move this thing to stress tests

	private AtomicLong records = new AtomicLong(0), bytes = new AtomicLong(0);
	private AtomicInteger errors = new AtomicInteger(0), passes = new AtomicInteger(0);

	int submissions = 0;

	private Executor executor = Executors.newFixedThreadPool(12);

	private void load(File dir, String file, String hostname) {
		System.out.println("Starting: " + new File(dir, file));
		long t1 = System.nanoTime();
		try {
			ZicoDataLoader loader = new ZicoDataLoader("127.0.0.1", 8640, hostname, "");
			loader.load(new File(dir, file).getPath());

			records.addAndGet(loader.getRecords());
			bytes.addAndGet(loader.getBytes());

			long t = (System.nanoTime() - t1) / 1000000;
			long recsps = 1000L * loader.getRecords() / t;
			long bytesps = 1000L * loader.getBytes() / t;

			System.out.println("File " + dir + "/" + file + " finished: t=" + t + " records=" + loader.getRecords()
					+ " (" + recsps + " recs/s)" + " bytes=" + loader.getBytes() + "(" + bytesps + " bytes/s).");

		} catch (Exception e) {
			errors.incrementAndGet();
		}
		passes.incrementAndGet();
	}

	@Test
	public void testLoadDataFile() throws Exception {
		ZicoDataLoader loader = new ZicoDataLoader("127.0.0.1", 8640, System.getProperty("load.host", "test"), "");
		loader.load(System.getProperty("load.file", "./test/trace.ztr"));
		Thread.sleep(10000000);
	}

	private Set<String> VERBOTEN = ZorkaUtil.set(".", "src/main");

	// @Test @Ignore
	public void testLoadMultipleDataFiles() throws Exception {
		File rootdir = new File("/tmp/traces");
		for (final String d : rootdir.list()) {
			final File dir = new File(rootdir, d);
			if (!VERBOTEN.contains(d) && dir.isDirectory()) {
				for (final String f : dir.list()) {
					if (f.matches("^trace.ztr.*")) {
						System.out.println("Submitting: " + new File(dir, f));
						submissions++;
						executor.execute(new Runnable() {
							@Override
							public void run() {
								load(dir, f, d + f);
							}
						});
					}
				}
			}
		}

		long t1 = System.nanoTime();
		while (submissions > passes.get()) {
			Thread.sleep(500);
		}
		long t = (System.nanoTime() - t1) / 1000000;
		long recsps = 1000L * records.get() / t;
		long bytesps = 1000L * bytes.get() / t;

		System.out.println("Overall execution time: " + t + "ms");
		System.out.println("Overall Records processed: " + records.get() + "(" + recsps + " recs/s)");
		System.out.println("Overall Bytes processed: " + bytes.get() + "(" + bytesps + " bytes/s");
	}

	@Test(timeout = 1000)
	public void testSendSimpleSymbolMessage() throws Exception {

		ZicoClientConnector conn = new ZicoClientConnector("127.0.0.1", 8640);
		conn.connect();

		conn.hello("test", "aaa");
		final TraceRecord traceRecord = new TraceRecord();
		conn.submit(new Symbol(1, "test"));
		Thread.sleep(10000000);
		Utils.close(conn);
	}

}
