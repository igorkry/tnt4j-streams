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

package com.jkoolcloud.tnt4j.streams.fields;

import static org.mockito.Mockito.*;

import java.io.FileReader;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.StreamsAgent;
import com.jkoolcloud.tnt4j.streams.inputs.InputStreamListener;
import com.jkoolcloud.tnt4j.streams.inputs.StreamStatus;
import com.jkoolcloud.tnt4j.streams.inputs.StreamTasksListener;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityCacheTest {

	private static final String PROGRESS_FILE = "./samples/cached-values/eventFirst.json"; // NON-NLS
	private static final String START_FILE = "./samples/cached-values/eventSecond.json"; // NON-NLS
	private static final String END_FILE = "./samples/cached-values/eventThird.json"; // NON-NLS

	private static final Integer TEST_PORT = 9595;

	private static final String cfgFile = "./samples/cached-values/tnt-data-source.xml"; // NON-NLS

	@Test
	public void runStreams() throws Exception {
		InputStreamListener streamListener = mock(InputStreamListener.class);
		StreamTasksListener streamTasksListener = mock(StreamTasksListener.class);
		StreamsAgent.runFromAPI(cfgFile, streamListener, streamTasksListener);

		Thread.sleep(3000);

		HttpClientBuilder builder = HttpClientBuilder.create();
		HttpClient client = builder.build();

		Thread.sleep(500);

		sendRequest(client, PROGRESS_FILE);
		sendRequest(client, START_FILE);

		Thread.sleep(50000);

		verify(streamListener, times(2)).onStatusChange(any(TNTInputStream.class), (StreamStatus) any());
	}

	private static void sendRequest(HttpClient client, String file) throws Exception {
		URI url = makeURI();
		HttpPost post = new HttpPost(url);

		post.setEntity(EntityBuilder.create().setText(getFileContents(file)).build());
		final HttpResponse returned = client.execute(post);
	}

	private static URI makeURI() {
		try {
			URIBuilder uriBuilder = new URIBuilder("http://localhost"); // NON-NLS
			uriBuilder.setHost("localhost"); // NON-NLS
			uriBuilder.setPort(TEST_PORT);
			URI url = uriBuilder.build();
			return url;
		} catch (Exception e) {
			return null;
		}
	}

	private static String getFileContents(String file) throws Exception {
		FileReader fr = new FileReader(file);
		return IOUtils.toString(fr);
	}

}
