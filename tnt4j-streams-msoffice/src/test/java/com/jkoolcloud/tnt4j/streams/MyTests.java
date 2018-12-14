/*
 * Copyright 2014-2018 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams;

import static org.junit.Assert.assertEquals;

import org.apache.poi.ss.util.CellReference;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.custom.dirStream.DirStreamingManager;
import com.jkoolcloud.tnt4j.streams.custom.dirStream.StreamingJobLogger;

/**
 * @author akausinis
 * @version 1.0
 */
public class MyTests {
	@Test
	public void dirStreamingTest() throws Exception {
		String dirPath = "./../temp/"; // NON-NLS
		String fwn = "tnt-data-source*.xml"; // NON-NLS

		System.setProperty("log4j.configuration", "file:./../config/log4j.properties"); // NON-NLS

		final DirStreamingManager dm = new DirStreamingManager(dirPath, fwn);
		dm.setTnt4jCfgFilePath("./../config/tnt4j.properties"); // NON-NLS
		dm.addStreamingJobListener(new StreamingJobLogger());

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("JVM exiting!..."); // NON-NLS
				synchronized (dm) {
					dm.notify();
				}
				dm.stop();
			}
		}));

		dm.start();
		synchronized (dm) {
			dm.wait();
		}
	}

	@Test
	public void cellReferenceTest() {
		CellReference cr = new CellReference("A6"); // NON-NLS
		String sn = cr.getSheetName();
		int row = cr.getRow();
		int col = cr.getCol();

		assertEquals(null, sn);
		assertEquals(5, row);
		assertEquals(0, col);

		cr = new CellReference("Sheet1!A6"); // NON-NLS

		sn = cr.getSheetName();
		row = cr.getRow();
		col = cr.getCol();

		assertEquals("Sheet1", sn); // NON-NLS
		assertEquals(5, row);
		assertEquals(0, col);

		cr = new CellReference("C"); // NON-NLS

		sn = cr.getSheetName();
		row = cr.getRow();
		col = cr.getCol();

		assertEquals(null, sn);
		assertEquals(-1, row);
		assertEquals(2, col);

		cr = new CellReference("6"); // NON-NLS

		sn = cr.getSheetName();
		row = cr.getRow();
		col = cr.getCol();

		assertEquals(null, sn);
		assertEquals(5, row);
		assertEquals(-1, col);
	}
}
