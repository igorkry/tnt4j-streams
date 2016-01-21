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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
public class HttpStreamTest {
	@Test
	public void HttpFilePostTest() throws Throwable {
		// initializeTest();
		HttpClientBuilder builder = HttpClientBuilder.create();
		HttpClient client = builder.build();

		URI url = new URI("http://localhost:8080/");
		HttpPost post = new HttpPost(url);

		File file = new File("./samples/http-file/log.txt");
		EntityBuilder entityBuilder = EntityBuilder.create();
		entityBuilder.setFile(file);
		entityBuilder.setContentType(ContentType.TEXT_PLAIN);
		final HttpEntity entity = entityBuilder.build();

		// MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		// builder.addBinaryBody("file", new File("test.txt"),
		// ContentType.APPLICATION_OCTET_STREAM, "file.ext");
		// HttpEntity multipart = builder.build();

		post.setEntity(entity);

		final HttpResponse returned = client.execute(post);
		assertNotNull(returned);

		// tearDownTest();
	}

	@Test
	public void HttpFormPostTest() throws Throwable {
		// initializeTest();
		HttpClientBuilder builder = HttpClientBuilder.create();
		HttpClient client = builder.build();

		URI url = new URI("Http://localhost:8080/");
		HttpPost post = new HttpPost(url);

		FileReader fileReader = new FileReader(new File("./samples/http-form/form-data.json"));
		Map<String, ?> jsonMap = Utils.fromJsonToMap(fileReader);
		Utils.close(fileReader);

		assertNotNull("Could not load form data from JSON", jsonMap);
		assertFalse("Loaded form data is empty", jsonMap.isEmpty());

		List<NameValuePair> params = new ArrayList<NameValuePair>();

		for (Map.Entry<String, ?> e : jsonMap.entrySet()) {
			params.add(new BasicNameValuePair(e.getKey(), String.valueOf(e.getValue())));
		}

		HttpEntity entity = new UrlEncodedFormEntity(params);

		post.setEntity(entity);

		final HttpResponse returned = client.execute(post);
		assertNotNull(returned);

		// tearDownTest();
	}

	@Test
	public void HttpHtmlGetTest() throws Throwable {
		// initializeTest();
		HttpClientBuilder builder = HttpClientBuilder.create();
		HttpClient client = builder.build();
		URI url = new URI("Http://localhost:8080/");
		HttpGet get = new HttpGet(url);
		final HttpResponse response = client.execute(get);
		assertNotNull(response);
		// assertEquals("<html><body><h1>No activity content
		// found!..</h1></body></html>", response.getEntity().toString());

		// tearDownTest();
	}

	private static HttpStream htStream;

	@BeforeClass
	public static void initializeTest() throws Throwable {
		htStream = new HttpStream();
		htStream.initialize();
	}

	@AfterClass
	public static void tearDownTest() throws InterruptedException {
		htStream.cleanup();
	}

	@Test
	public void propertiesTest() throws Throwable {
		InputPropertiesTestUtils.testInputPropertySetAndGet(htStream, StreamsConfig.PROP_PORT, 8080);
		InputPropertiesTestUtils.testInputPropertySetAndGet(htStream, StreamsConfig.PROP_KEYSTORE, "TEST");
		InputPropertiesTestUtils.testInputPropertySetAndGet(htStream, StreamsConfig.PROP_KEYSTORE_PASS, "TEST");
		InputPropertiesTestUtils.testInputPropertySetAndGet(htStream, StreamsConfig.PROP_KEY_PASS, "TEST");

	}

}
