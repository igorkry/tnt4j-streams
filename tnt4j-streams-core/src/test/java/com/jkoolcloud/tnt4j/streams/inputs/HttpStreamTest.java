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

import static com.jkoolcloud.tnt4j.streams.TestUtils.testPropertyList;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
public class HttpStreamTest {

	private static final Integer TEST_PORT = 50643;

	private static File samplesDir;
	private static HttpStream htStream;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		initSamplesDir();
		initHttpStream();
	}

	private static void initSamplesDir() throws Exception {
		samplesDir = new File("./samples/");
		if (!samplesDir.isDirectory()) {
			samplesDir = new File("./tnt4j-streams-core/samples/");
			if (!samplesDir.isDirectory()) {
				fail("Samples root directory doesn't exist");
			}
		}
	}

	private static void initHttpStream() throws Exception {
		htStream = new HttpStream();
		Map<String, String> props = new HashMap<>(2);
		props.put(StreamProperties.PROP_HALT_ON_PARSER, String.valueOf(false));
		props.put(StreamProperties.PROP_PORT, String.valueOf(TEST_PORT));
		htStream.setProperties(props.entrySet());
		StreamThread thread = new StreamThread(htStream);
		thread.start();
	}

	@AfterClass
	public static void tearDown() throws InterruptedException {
		htStream.cleanup();
	}

	@Test
	public void httpFilePostTest() throws Exception {
		HttpClientBuilder builder = HttpClientBuilder.create();
		HttpClient client = builder.build();

		URI url = makeURI();
		HttpPost post = new HttpPost(url);

		File file = new File(samplesDir, "/http-file/log.txt");
		EntityBuilder entityBuilder = EntityBuilder.create();
		entityBuilder.setFile(file);
		entityBuilder.setContentType(ContentType.TEXT_PLAIN);

		MultipartEntityBuilder builder2 = MultipartEntityBuilder.create();
		builder2.addBinaryBody("file", file, ContentType.APPLICATION_OCTET_STREAM, "file.ext"); // NON-NLS
		HttpEntity multipart = builder2.build();

		post.setEntity(multipart);

		final HttpResponse returned = client.execute(post);
		assertNotNull(returned);

	}

	@Test
	public void httpFormPostTest() throws Exception {

		FileReader fileReader = new FileReader(new File(samplesDir, "/http-form/form-data.json"));
		Map<String, ?> jsonMap = Utils.fromJsonToMap(fileReader, false);
		Utils.close(fileReader);

		assertNotNull("Could not load form data from JSON", jsonMap);
		assertFalse("Loaded form data is empty", jsonMap.isEmpty());
		Form form = Form.form();

		for (Map.Entry<String, ?> e : jsonMap.entrySet()) {
			form.add(e.getKey(), String.valueOf(e.getValue()));
		}

		try {
			Thread.sleep(100);
			Request.Get(makeURI()).execute().returnContent();
		} catch (HttpResponseException ex) {

		}
		HttpResponse resp = Request.Post(makeURI()).version(HttpVersion.HTTP_1_1).bodyForm(form.build()).execute()
				.returnResponse();
		assertEquals(200, resp.getStatusLine().getStatusCode());
	}

	private URI makeURI() throws URISyntaxException {
		URIBuilder uriBuilder = new URIBuilder("http://localhost"); // NON-NLS
		uriBuilder.setHost("localhost"); // NON-NLS
		uriBuilder.setPort(TEST_PORT);
		URI url = uriBuilder.build();
		return url;
	}

	@Test
	public void httpHtmlGetTest() throws Exception {
		HttpResponse response = Request.Get(makeURI()).execute().returnResponse();
		assertNotNull(response);
	}

	@Test
	public void propertiesTest() throws Exception {
		Map<String, String> props = new HashMap<>(4);
		props.put(StreamProperties.PROP_PORT, String.valueOf(TEST_PORT));
		props.put(StreamProperties.PROP_KEYSTORE, "TEST"); // NON-NLS
		props.put(StreamProperties.PROP_KEYSTORE_PASS, "TEST"); // NON-NLS
		props.put(StreamProperties.PROP_KEY_PASS, "TEST"); // NON-NLS
		htStream.setProperties(props.entrySet());
		testPropertyList(htStream, props.entrySet());
	}

}
