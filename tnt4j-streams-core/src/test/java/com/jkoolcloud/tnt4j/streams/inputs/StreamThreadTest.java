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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class StreamThreadTest {

	private TNTInputStream<?, ?> streamMock;

	@Test
	public void testStreamThread() {
		streamMock = mock(TNTInputStream.class);
		StreamThread thread = new StreamThread(streamMock);
		verify(streamMock).setOwnerThread(thread);
		assertEquals(streamMock, thread.getTarget());
	}

	@Test
	public void testStreamThreadWithName() {
		streamMock = mock(TNTInputStream.class);
		StreamThread thread = new StreamThread(streamMock, "NAME"); // NON-NLS
		verify(streamMock).setOwnerThread(thread);
		assertEquals(streamMock, thread.getTarget());
	}

	@Test
	public void testStreamThreadWithGroupAndName() {
		ThreadGroup tgGroup = new ThreadGroup("TG"); // NON-NLS
		streamMock = mock(TNTInputStream.class);
		StreamThread thread = new StreamThread(tgGroup, streamMock, "NAME"); // NON-NLS
		verify(streamMock).setOwnerThread(thread);
		assertEquals(streamMock, thread.getTarget());
	}

	@Test
	public void testStreamThreadWithGroup() {
		ThreadGroup tgGroup = new ThreadGroup("TG"); // NON-NLS
		streamMock = mock(TNTInputStream.class);
		StreamThread thread = new StreamThread(tgGroup, streamMock);
		verify(streamMock).setOwnerThread(thread);
		assertEquals(streamMock, thread.getTarget());
	}
}
