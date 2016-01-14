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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * @author akausinis
 * @version 1.0
 */
public class AbstractBufferedStreamTest {
	public boolean inputShouldEnd = false;
	EventSink es = mock(EventSink.class);
	AbstractBufferedStream<String> abs = new AbstractBuferedStreamTestStub(es);

	@Test
	public void getNextItemTest() throws Throwable {
		abs.initialize();
		abs.addInputToBuffer("TEST");
		assertEquals("TEST", abs.getNextItem());
	}

	@Test(expected = IllegalStateException.class)
	public void getNextItemFailOnNullTest() throws Throwable {
		abs.addInputToBuffer(null);
		assertEquals(null, abs.getNextItem());
	}

	@Test
	public void getNextItemNullOnEmptyTest() throws Throwable {
		abs.initialize();
		inputShouldEnd = true;
		assertNull(abs.getNextItem());
	}

	@Test(timeout = 3000)
	public void getNextItemExpectedToWaitTest() throws Throwable {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					abs.initialize();
					abs.addInputToBuffer("TEST");
					assertNotNull(abs.getNextItem());
					assertNull(abs.getNextItem());
				} catch (Throwable e) {
				}
			}
		});
		thread.start();
		Thread.sleep(500);
		assertEquals(Thread.State.WAITING, thread.getState());
		thread.interrupt();
	}

	private class AbstractBuferedStreamTestStub extends AbstractBufferedStream<String> {
		protected AbstractBuferedStreamTestStub(EventSink logger) {
			super(logger);
		}

		@Override
		protected boolean isInputEnded() {
			return inputShouldEnd;
		}
	}

	int overflowRecordCount = 1024 * 10 + 1;

	@Test(timeout = 5000)
	public void addInputToBufferOverflowTest() throws Throwable {
		abs.initialize();

		Thread thread = new Thread(new Runnable() {
			public void run() {
				for (; overflowRecordCount >= 0; overflowRecordCount--) {
					abs.addInputToBuffer("T");
					System.out.println(overflowRecordCount);

				}
			}
		});
		thread.start();
		Thread.sleep(500);
		if (overflowRecordCount >= 2) {
			Thread.sleep(30);
			assertEquals(Thread.State.WAITING, thread.getState());
		}
	}

}
