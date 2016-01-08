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

import static com.jkool.tnt4j.streams.inputs.InputPropertiesTestUtils.testInputPropertySetAndGet;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;

import com.ibm.mq.MQDestination;
import com.ibm.mq.MQException;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import com.jkool.tnt4j.streams.configure.StreamsConfig;

/**
 * @author akausinis
 * @version 1.0
 */
public class WmqStreamTest {
	WmqStream wmqStream = new WmqStream();

	@Test
	public void propertiesSetTest() throws Throwable {
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_QMGR_NAME, "TEST");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_QUEUE_NAME, "TEST");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_TOPIC_NAME, "TEST");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_SUB_NAME, "TEST");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_TOPIC_STRING, "TEST");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_QMGR_NAME, "TEST");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_HOST, "localhost");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_PORT, 8080);
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_CHANNEL_NAME, "TEST");
		testInputPropertySetAndGet(wmqStream, StreamsConfig.PROP_STRIP_HEADERS, false);
	}

	@Test
	public void testInitialize() throws Throwable {
		propertiesSetTest();
		wmqStream.initialize();
		assertNotNull(wmqStream.gmo);
	}

	@Test(expected = MQException.class)
	public void testConnects() throws Throwable {
		propertiesSetTest();
		wmqStream.connectToQmgr();
	}

	@Test()
	public void isConnectedToQmgr() throws Throwable {
		final MQQueueManager mqqManager = mock(MQQueueManager.class);
		final MQException mqe = mock(MQException.class);
		wmqStream.qmgr = mqqManager;

		when(mqqManager.isConnected()).thenReturn(true);
		when(mqe.getCompCode()).thenReturn(MQConstants.MQCC_FAILED);

		when(mqe.getReason()).thenReturn(MQConstants.MQRC_CONNECTION_BROKEN, MQConstants.MQRC_CONNECTION_ERROR,
				MQConstants.MQRC_Q_MGR_NOT_ACTIVE, MQConstants.MQRC_Q_MGR_NOT_AVAILABLE,
				MQConstants.MQRC_Q_MGR_QUIESCING, MQConstants.MQRC_Q_MGR_STOPPING,
				MQConstants.MQRC_CONNECTION_QUIESCING, MQConstants.MQRC_CONNECTION_STOPPING);
		for (int i = 1; i <= 8; i++) {
			assertFalse(wmqStream.isConnectedToQmgr(mqe));
		}

		when(mqe.getReason()).thenReturn(MQConstants.MQRC_AIR_ERROR);
		assertTrue(wmqStream.isConnectedToQmgr(mqe));
	}

	@Test
	public void testcloseQmgrConnection() throws MQException {
		final MQQueueManager mqqManager = mock(MQQueueManager.class);
		final MQDestination mqDestination = mock(MQDestination.class);

		wmqStream.dest = mqDestination;
		wmqStream.qmgr = mqqManager;

		wmqStream.cleanup();

		verify(mqqManager).disconnect();
		verify(mqDestination).close();
	}

	@Test
	public void testcloseQmgrConnectionException() throws MQException {
		final MQQueueManager mqqManager = mock(MQQueueManager.class);
		final MQDestination mqDestination = mock(MQDestination.class);

		doThrow(new MQException(1, 1, this)).when(mqqManager).disconnect();
		doThrow(new MQException(1, 1, this)).when(mqDestination).close();

		wmqStream.dest = mqDestination;
		wmqStream.qmgr = mqqManager;

		wmqStream.cleanup();

		// Verify doesn't throw
	}
}
