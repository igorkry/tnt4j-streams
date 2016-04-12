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

package com.jkool.tnt4j.streams.utils;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.junit.Test;

import com.nastel.jkool.tnt4j.core.OpType;

/**
 * @author akausinis
 * @version 1.0
 */
public class UtilsTest {

	private static final int FILE_WR_LINES = 100;
	private static final String TEST = "TEST";

	@Test
	public void testBase64Encode() {
		final byte[] resultDecode = Utils.base64Decode(TEST.getBytes());
		final byte[] resultEncode = Utils.base64Encode(resultDecode);
		assertArrayEquals(resultEncode, TEST.getBytes());
	}

	@Test
	public void testBase64Decode() {
		final byte[] resultEncode = Utils.base64Encode(TEST.getBytes());
		final byte[] resultDecode = Utils.base64Decode(resultEncode);
		assertArrayEquals(resultDecode, TEST.getBytes());
	}

	// @Test
	// public void testEncodeHex()
	// {
	// final char[] resultEncode = Utils.encodeHex(TEST.getBytes());
	// final byte[] resultDecode = Utils.decodeHex(resultEncode.toString());
	// assertArrayEquals(resultDecode, TEST.getBytes());
	// }

	@Test
	public void testComputeSignatureMessageTypeStringByteArrayStringStringStringStringString() {
		// TODO
		// fail("Not yet implemented");
	}

	@Test
	public void testComputeSignatureMessageDigestMessageTypeStringByteArrayStringStringStringStringString() {
		// TODO
		// fail("Not yet implemented");
	}

	@Test
	public void testMapOpType() {
		int opTypeCount = 20;
		Map<String, OpType> opTypes = new HashMap<String, OpType>(opTypeCount);

		for (int i = 0; i <= opTypeCount; i++) {
			final OpType opType = Utils.mapOpType(i);
			if (opType == OpType.STOP) {
				////////////////////////////
				opTypes.put("END", opType);
				///////////////////////////
			} else {
				opTypes.put(opType.name(), opType);
			}
		}

		final Set<Entry<String, OpType>> entrySet = opTypes.entrySet();
		for (Entry<String, OpType> entry : entrySet) {

			assertEquals(entry.getValue(), Utils.mapOpType(entry.getKey()));
		}
	}

	@Test
	public void testIsWildcardFileName() {
		final String N_WILDC = "c:/Users/Default.migrated/AppData/Local/Microsoft/Windows/INetCache/";
		final String WILDC = "c:/Windows/schemas/TSWorkSpace/*.*";
		final String WILDC2 = "c:/Windows/schemas/TSWorkSpace/*.*";
		final String WILDC3 = "c:/Windows/schemas/TSWorkSpa?e/*.*";
		final String EMPTY = "";

		assertFalse(Utils.isWildcardFileName(N_WILDC));
		assertTrue(Utils.isWildcardFileName(WILDC));
		assertTrue(Utils.isWildcardFileName(WILDC2));
		assertTrue(Utils.isWildcardFileName(WILDC3));
		assertFalse(Utils.isWildcardFileName(EMPTY));
	}

	@Test
	public void testCountLines() throws IOException {
		File tempFile = File.createTempFile("TEST", "TST");
		FileWriter wr = new FileWriter(tempFile);
		for (int i = 1; i <= FILE_WR_LINES; i++) {
			wr.write("TEST\n");
		}
		System.out.println(tempFile.getAbsolutePath());
		wr.flush();
		Utils.close(wr);
		final int lines = Utils.countLines(tempFile);
		assertEquals(FILE_WR_LINES, lines);

	}

	@Test
	public void testGetFirstNewer() throws IOException, InterruptedException {
		final int count = 5;
		Long date = null;
		List<File> files = new ArrayList<File>();
		for (int i = 0; i <= count; i++) {
			File tempFile = File.createTempFile("TEST", "TST");
			if (count / 2 >= i)
				date = (new Date()).getTime();
			files.add(tempFile);
			Thread.sleep(1);
		}
		File result = Utils.getFirstNewer(files.toArray(new File[files.size()]), null);
		assertEquals(files.get(0), result);

		// result = Utils.getFirstNewer(files.toArray(new File[files.size()]),
		// date);
		// assertEquals(files.get(count/2+1), result);

		for (File fileToRemove : files) {
			fileToRemove.delete();
		}

	}

	@Test
	public void testFromJsonToMap() {
		Map<String, String> testMap = new HashMap<String, String>() {
			{
				put("TEST", "TESTVAL");
				put("TEST2", "TESTVAL2");
				put("TEST3", "TESTVAL3");

			}
		};
		String testString = "{\"TEST2\"=\"TESTVAL2\", \"TEST3\"=\"TESTVAL3\", \"TEST\"=\"TESTVAL\"}";
		// Gson gson = new Gson();
		// final String json = gson.toJson(testMap);
		Map<String, ?> result = Utils.fromJsonToMap(testString, false);
		assertTrue(testMap.equals(result));
		result = Utils.fromJsonToMap(testString.getBytes(), false);
		assertTrue(testMap.equals(result));
		result = Utils.fromJsonToMap(toReader(testString), false);
		assertTrue(testMap.equals(result));
		result = Utils.fromJsonToMap(toInputStream(testString), false);
		assertTrue(testMap.equals(result));

	}

	public static StringReader toReader(String testString) {
		return new StringReader(testString);
	}

	public static ByteArrayInputStream toInputStream(String testString) {
		return new ByteArrayInputStream(testString.getBytes());
	}

	@Test
	public void testGetStringLine() throws IOException {
		String testString = "TEST \n TEST1 \n TEST2 \n TEST3 \n TEST4 \n TEST5 \n";
		String testStringLine = "TEST ";
		String result = Utils.getStringLine(testString);
		assertEquals(testStringLine, result);

		result = Utils.getStringLine(testString.getBytes());
		assertEquals(testStringLine, result);

		Reader rdr = toReader(testString);
		result = Utils.getStringLine(rdr);
		assertEquals(testStringLine, result);
		Utils.close(rdr);

		rdr = new BufferedReader(toReader(testString));
		result = Utils.getStringLine(rdr);
		assertEquals(testStringLine, result);
		Utils.close(rdr);

		InputStream is = toInputStream(testString);
		result = Utils.getStringLine(is);
		assertEquals(testStringLine, result);
		Utils.close(is);
	}

	@Test
	public void testGetTags() {
		String testStrig = "TAG1,TAG2,TAG3";
		String[] expected = { "TAG1", "TAG2", "TAG3" };
		String[] result = Utils.getTags(testStrig);
		assertArrayEquals(expected, result);

		result = Utils.getTags(expected);
		assertArrayEquals(expected, result);

		final List<String> list = Arrays.asList(expected);
		result = Utils.getTags(list);
		assertArrayEquals(expected, result);

		result = Utils.getTags(this);
		assertNull(result);
	}

	@Test
	public void testCleanActivityData() {

		// {\"sinkName\":\"TNT4JStreams\",\"chanelName\":\"memoryChannel\",\"headers\":{},\"body\":\"127.0.0.1
		// - - [26/Nov/2015:16:26:21 +0200] \\\"POST
		// /gvm_java/gvm/services/OperatorWebService HTTP/1.1\\\" 200 380\\r\"}
		String testStrig = "line\\r";
		String testStrig2 = "line\\n";
		String expected = "line";
		// assertEquals(expected, Utils.cleanActivityData(testStrig));
		// assertEquals(expected, Utils.cleanActivityData(testStrig2));
	}

}
