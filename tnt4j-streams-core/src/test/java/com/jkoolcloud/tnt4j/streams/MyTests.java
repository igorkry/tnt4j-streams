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

import static org.junit.Assert.*;

import java.io.*;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.*;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.xpath.*;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.HexDump;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.matcher.StringMatcher;
import org.apache.commons.text.matcher.StringMatcherFactory;
import org.junit.Ignore;
import org.junit.Test;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jkoolcloud.tnt4j.core.*;
import com.jkoolcloud.tnt4j.format.JSONFormatter;
import com.jkoolcloud.tnt4j.streams.configure.jaxb.Field;
import com.jkoolcloud.tnt4j.streams.configure.jaxb.Parser;
import com.jkoolcloud.tnt4j.streams.configure.jaxb.Stream;
import com.jkoolcloud.tnt4j.streams.configure.jaxb.TntDataSource;
import com.jkoolcloud.tnt4j.streams.custom.dirStream.DirStreamingManager;
import com.jkoolcloud.tnt4j.streams.custom.dirStream.DirWatchdog;
import com.jkoolcloud.tnt4j.streams.custom.dirStream.StreamingJobLogger;
import com.jkoolcloud.tnt4j.streams.fields.*;
import com.jkoolcloud.tnt4j.streams.filters.GroovyExpressionFilter;
import com.jkoolcloud.tnt4j.streams.filters.HandleType;
import com.jkoolcloud.tnt4j.streams.filters.XPathExpressionFilter;
import com.jkoolcloud.tnt4j.streams.preparsers.AbstractPreParser;
import com.jkoolcloud.tnt4j.streams.preparsers.ActivityDataPreParser;
import com.jkoolcloud.tnt4j.streams.transform.FuncGetFileName;
import com.jkoolcloud.tnt4j.streams.utils.*;
import com.jkoolcloud.tnt4j.tracker.TimeTracker;
import com.jkoolcloud.tnt4j.utils.SizeOf;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import net.minidev.json.JSONValue;

@Ignore
public class MyTests {

	@Test
	public void testNumberFormat() throws Exception {
		DecimalFormat df = new DecimalFormat("##0.000", DecimalFormatSymbols.getInstance(Locale.US)); // NON-NLS
		String strNumberValue = "0.375"; // NON-NLS

		Number num = df.parse(strNumberValue);

		assertEquals("Parsed number does not match", 0.375, num.doubleValue(), Utils.DEFAULT_EPSILON); // NON-NLS
	}

	@Test
	public void testJsonParsing() throws Exception {
		String json =
				// "{\"k1\":\"v1\",\"k2\":\"v2\"}";

				"{msg1: {date:'2012-01-01 02:00:01',severity:\"ERROR\", msg:\"Foo failed\"}," // NON-NLS
						+ "msg2: {date:'2012-01-01 02:04:02', severity:\"INFO\", msg:\"Bar was successful\"}," // NON-NLS
						+ "msg3: {date:'2012-01-01 02:10:12', severity:\"DEBUG\", msg:\"Baz was notified\"}}"; // NON-NLS

		Map<String, ?> map = Utils.fromJsonToMap(json, true);

		assertEquals("Messages count differs from expected", 3, map.size()); // NON-NLS

		for (Map.Entry<String, ?> e : map.entrySet()) {
			Object ev = e.getValue();

			if (ev instanceof Map<?, ?>) {
				assertEquals("Message attributes count differs from expected", 3, ((Map<?, ?>) ev).size()); // NON-NLS
			} else if (ev instanceof String) {

			}
		}
	}

	@Test
	public void testLogStashJsonParsing() throws Exception {
		// String json = "{\"message\":\"127.0.0.1 - - [17/Nov/2015:15:05:11
		// +0200] \\\"POST /gvm_java/gvm/services/OperatorWebService
		// HTTP/1.1\\\" 200
		// 124647\\r\",\"@version\":\"1\",\"@timestamp\":\"2015-11-17T13:05:15.157Z\",\"host\":\"vezimas\",\"path\":\"g:/workspace/.metadata/.plugins/org.eclipse.wst.server.core/tmp0/logs/localhost_access_log.2015-11-17.txt\",\"tags\":[\"Normal
		// server\"]}"; //NON-NLS
		String json = "{\"sinkName\":\"TNT4JStreams\",\"chanelName\":\"memoryChannel\",\"headers\":{},\"body\":\"127.0.0.1 - - [26/Nov/2015:16:26:21 +0200] \\\"POST /gvm_java/gvm/services/OperatorWebService HTTP/1.1\\\" 200 380\r\"}"; // NON-NLS

		Map<String, ?> map = Utils.fromJsonToMap(json, true);

		assertEquals("Messages count differs from expected", 4, map.size()); // NON-NLS
	}

	@Test
	public void pipeTest() throws Exception {
		System.setIn(new FileInputStream("./samples/piping-stream/orders.log")); // NON-NLS
		StreamsAgent.main("-p:./samples/piping-stream/parsers.xml"); // NON-NLS
	}

	@Test
	public void dirWatchdogTest() throws Exception {
		String dirPath = "./../temp/"; // NON-NLS

		DirWatchdog dw = new DirWatchdog(dirPath, DirWatchdog.getDefaultFilter("job_config_*.xml")); // NON-NLS
		dw.addObserverListener(new FileAlterationListenerAdaptor() {
			@Override
			public void onFileCreate(File file) {
				System.out.println("File created: " + file.getAbsolutePath()); // NON-NLS
			} // NON-NLS

			@Override
			public void onFileChange(File file) {
				System.out.println("File changed: " + file.getAbsolutePath()); // NON-NLS
			} // NON-NLS

			@Override
			public void onFileDelete(File file) {
				System.out.println("File deleted: " + file.getAbsolutePath()); // NON-NLS
			} // NON-NLS
		});

		dw.start();

		Thread.sleep(TimeUnit.MINUTES.toMillis(5));

		dw.stop();
	}

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
	public void streamConfigReadJAXBTest() throws Exception {
		JAXBContext jc = JAXBContext.newInstance("com.jkoolcloud.tnt4j.streams.configure.jaxb"); // NON-NLS
		Unmarshaller u = jc.createUnmarshaller();
		TntDataSource sConfig = (TntDataSource) u
				.unmarshal(new File("./samples/dirStream/tnt-data-source_123e4567-e89b-12d3-a456-426655440000.xml")); // NON-NLS

		assertEquals("Stream count does not match", 1, sConfig.getStream().size()); // NON-NLS
		assertEquals("Parsers count does not match", 1, sConfig.getParser().size()); // NON-NLS

		assertEquals("Unexpected stream name", "FileStream", sConfig.getStream().get(0).getName()); // NON-NLS
	}

	@Test
	public void streamConfigWriteJAXBTest() throws Exception {
		Parser p = new Parser("SampleTestParserName", // NON-NLS
				"com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser", "Tag1, Tag2, Tag3"); // NON-NLS
		p.addProperty("FieldDelim", "|"); // NON-NLS

		p.addField(new Field("StartTime", "1", "dd MMM yyyy HH:mm:ss", "en-US")); // NON-NLS
		p.addField(new Field("ServerIp", "2")); // NON-NLS
		p.addField(new Field("ApplName", null, "orders")); // NON-NLS

		Field f = new Field("EventType", "5"); // NON-NLS
		f.addFieldMap("Order Placed", "START"); // NON-NLS
		f.addFieldMap("Order Received", "RECEIVE"); // NON-NLS
		f.addFieldMap("Order Processing", "OPEN"); // NON-NLS
		f.addFieldMap("Order Processed", "SEND"); // NON-NLS
		f.addFieldMap("Order Shipped", "END"); // NON-NLS

		p.addField(f);

		Stream s = new Stream("SampleTestParserName", "com.jkoolcloud.tnt4j.streams.inputs.FileLineStream"); // NON-NLS
		s.addProperty("HaltIfNoParser", "true"); // NON-NLS
		s.addProperty("FileName", "orders.log"); // NON-NLS

		s.addParserRef("SampleTestParserName"); // NON-NLS

		TntDataSource sConfig = new TntDataSource();
		sConfig.addParser(p);
		sConfig.addStream(s);

		JAXBContext jc = JAXBContext.newInstance("com.jkoolcloud.tnt4j.streams.configure.jaxb"); // NON-NLS
		Marshaller m = jc.createMarshaller();
		m.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION,
				"https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd"); // NON-NLS
		m.marshal(sConfig, new File("./../temp/tnt-data-source-output.xml")); // NON-NLS
	}

	@Test
	public void testNameUUID() {
		File f = new File("./../temp/job_config_123e4567-e89b-12d3-a456-426655440000__.xml"); // NON-NLS

		UUID uuid = Utils.findUUID(f.getName());

		assertEquals("Unexpected UUID", "123e4567-e89b-12d3-a456-426655440000", uuid.toString()); // NON-NLS
	}

	@Test
	public void testFileSearch() {
		String namePattern = "./../temp/*.xml"; // NON-NLS
		File f = new File(namePattern);
		File dir = f.getAbsoluteFile().getParentFile();
		File[] activityFiles = dir.listFiles((FilenameFilter) new WildcardFileFilter(f.getName()));
		if (activityFiles != null) {
			Arrays.sort(activityFiles, new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					/*
					 * try { BasicFileAttributes bfa1 = Files.readAttributes(o1.toPath(), BasicFileAttributes.class);
					 * BasicFileAttributes bfa2 = Files.readAttributes(o2.toPath(), BasicFileAttributes.class);
					 *
					 * // NOTE: we want files to be sorted from oldest return
					 * bfa1.creationTime().compareTo(bfa2.creationTime()) * (-1); } catch (IOException exc) { return 0;
					 * }
					 */

					long f1ct = o1.lastModified();
					long f2ct = o2.lastModified();
					// NOTE: we want files to be sorted from newest->oldest
					// (DESCENDING)
					return f1ct < f2ct ? 1 : (f1ct == f2ct ? 0 : -1);
				}
			});
		}

		int l = activityFiles.length;
	}

	@Test
	public void testJSONPath() {
		String json = "{\"message\":\"127.0.0.1 - - [17/Nov/2015:15:05:11 +0200] \\\"POST /gvm_java/gvm/services/OperatorWebService HTTP/1.1\\\" 200 124647\\r\",\"@version\":\"1\",\"@timestamp\":\"2015-11-17T13:05:15.157Z\",\"host\":\"vezimas\",\"path\":\"g:/workspace/.metadata/.plugins/org.eclipse.wst.server.core/tmp0/logs/localhost_access_log.2015-11-17.txt\",\"tags\":[\"Normal server\"]}"; // NON-NLS

		DocumentContext jsonDoc = JsonPath.parse(json);
		Object val = jsonDoc.read("$.tags"); // NON-NLS

		assertNotNull(val);
	}

	@Test
	public void testLocale() {
		String locale = "lt-LT"; // NON-NLS
		String l = locale.replace("-", "_"); // NON-NLS
		Locale lll = LocaleUtils.toLocale(l);

		System.currentTimeMillis();
	}

	@Test
	public void testProcessBuilder() throws Exception {
		// String command = "C:\\SysUtils\\curl\\curl -i
		// https://api.github.com/users/octocat/orgs";
		String command = "C:\\SysUtils\\curl\\curl -izz"; // NON-NLS

		StringTokenizer st = new StringTokenizer(command);
		String[] cmdarray = new String[st.countTokens()];
		for (int i = 0; st.hasMoreTokens(); i++) {
			cmdarray[i] = st.nextToken();
		}

		ProcessBuilder pb = new ProcessBuilder(cmdarray);
		// pb.redirectErrorStream(true);
		Process p = pb.start();
		// Process p = Runtime.getRuntime().exec("C:\\SysUtils\\curl\\curl -i
		// https://api.github.com/users/octocat/orgs");

		String result = readInput(p.getInputStream());
		System.out.print("IN:\n" + result); // NON-NLS

		result = readInput(p.getErrorStream());
		System.out.print("ERR:\n" + result); // NON-NLS
	}

	private String readInput(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder builder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
			builder.append(Utils.NEW_LINE);
		}
		return builder.toString();
	}

	@Test
	public void testStartTime() throws Exception {
		TimestampFormatter tsf = new TimestampFormatter(TimeUnit.MILLISECONDS);
		UsecTimestamp t = tsf.parseAny(1460015371411L);

		System.currentTimeMillis();
	}

	@Test
	public void testSizeOf() throws Exception {
		String s00 = null;
		String s0 = ""; // NON-NLS
		String s1 = "1"; // NON-NLS
		String s10 = "1234567890"; // NON-NLS
		String s20 = "12345678901234567890"; // NON-NLS

		long dSize00 = SizeOf.deepSizeOf(s00);
		long dSize0 = SizeOf.deepSizeOf(s0);
		long dSize1 = SizeOf.deepSizeOf(s1);
		long dSize10 = SizeOf.deepSizeOf(s10);
		long dSize20 = SizeOf.deepSizeOf(s20);

		long size10 = SizeOf.sizeOf(s10);

		long chSize = SizeOf.sizeOf('a');

		assertEquals("Wrong String size", 24, size10);
		assertEquals("Wrong char size", 16, chSize);
	}

	@Test
	public void testSplit() {
		String s = "xxx.yyyy.yyyy"; // NON-NLS
		String d = "."; // NON-NLS

		String[] ss = s.split(d);

		assertEquals("XXX", 1, ss.length); // NON-NLS
	}

	@Test
	public void testSplit2() {
		String str = "=xxxxxxx"; // NON-NLS

		String[] v = str.split("="); // NON-NLS

		assertEquals("YYY", 2, v.length); // NON-NLS
	}

	@Test
	public void testSplit3() {
		String str = "12|52"; // NON-NLS

		String[] v = str.split("\\|"); // NON-NLS

		assertEquals("YYY", 2, v.length); // NON-NLS

		str = "1253";

		v = str.split("\\|"); // NON-NLS

		assertEquals("YYY", 1, v.length); // NON-NLS
	}

	@Test
	public void testWSDataPackage() throws Exception {
		String soapRequestData = "SOAPAction:http://tempuri.org/IWeatherInformation/GetCurrentWeatherInformationByStationID\n" // NON-NLS
				+ "                        <tem:GetCurrentWeatherInformationByStationID xmlns:tem=\"http://tempuri.org/\">\n" // NON-NLS
				+ "                            <tem:AccessCode>aeb652b7-f6f5-49e6-9bdb-e2b737ebd507</tem:AccessCode>\n" // NON-NLS
				+ "                            <tem:StationID>1909</tem:StationID>\n" // NON-NLS
				+ "                        </tem:GetCurrentWeatherInformationByStationID>"; // NON-NLS

		Map<String, String> headers = new HashMap<>();
		// separate SOAP message header values from request body XML
		BufferedReader br = new BufferedReader(new StringReader(soapRequestData));
		StringBuilder sb = new StringBuilder();
		try {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().startsWith("<")) { // NON-NLS
					sb.append(line + "\n"); // NON-NLS
				} else {
					int bi = line.indexOf(':'); // NON-NLS
					if (bi >= 0) {
						String hKey = line.substring(0, bi).trim();
						String hValue = line.substring(bi + 1).trim();
						headers.put(hKey, hValue);
					} else {
						sb.append(line + "\n"); // NON-NLS
					}
				}
			}
		} finally {
			Utils.close(br);
		}

		soapRequestData = sb.toString();

		assertEquals("TTTTT", 1, headers.size()); // NON-NLS
	}

	@Test
	public void testJSON() {
		String json = "{\"name\": \"PeakUsage\\max\", \"type\": \"long\", \"value\": 1073741824}"; // NON-NLS
		String jsonE = StringEscapeUtils.escapeJson(json);

		boolean valid = JSONValue.isValidJson(json);
		boolean validE = JSONValue.isValidJson(jsonE);

		assertTrue(valid);
	}

	@Test
	public void testArrayCheck() {
		Object array = new String[] { "a", "b" }; // NON-NLS

		assertTrue(array instanceof Object[]);
		assertTrue(array.getClass().isArray());

		array = new String[] {};

		assertTrue(array instanceof Object[]);
		assertTrue(array.getClass().isArray());

		array = null;

		assertFalse(array instanceof Object[]);
		// assertFalse(array.getClass().isArray());

		array = new int[] { 1, 2, 3 };

		assertFalse(array instanceof Object[]);
		assertTrue(array instanceof int[]);
		assertTrue(array.getClass().isArray());

		array = new Integer[] { 1, 2, 3 };
		assertTrue(array instanceof Object[]);
		assertTrue(array.getClass().isArray());

		array = 8;
		assertFalse(array instanceof Object[]);
		assertFalse(array.getClass().isArray());

		array = new String[] { "x", "r" }; // NON-NLS
		assertFalse(array instanceof String);

		array = new int[] { 1, 2, 3 };
		assertFalse(array.getClass().isPrimitive());
	}

	@Test
	public void testRanges() throws Exception {
		String rangeStr = "-100:"; // NON-NLS
		Range<?> r = IntRange.getRange(rangeStr);
		assertEquals(-100, r.getFrom());
		assertEquals(Integer.MAX_VALUE, r.getTo());
		rangeStr = ":23536"; // NON-NLS
		r = IntRange.getRange(rangeStr);
		assertEquals(-Integer.MAX_VALUE, r.getFrom());
		assertEquals(23536, r.getTo());
		r = IntRange.getRange(rangeStr, true);
		assertEquals(0, r.getFrom());
		assertEquals(23536, r.getTo());
		rangeStr = "2:23536"; // NON-NLS
		r = IntRange.getRange(rangeStr);
		assertEquals(2, r.getFrom());
		assertEquals(23536, r.getTo());
		rangeStr = "-3.5:"; // NON-NLS
		r = DoubleRange.getRange(rangeStr);
		assertEquals(-3.5, r.getFrom());
		assertEquals(Double.MAX_VALUE, r.getTo());
		rangeStr = ":2353.6"; // NON-NLS
		r = DoubleRange.getRange(rangeStr);
		assertEquals(-Double.MAX_VALUE, r.getFrom());
		assertEquals(2353.6, r.getTo());
		r = DoubleRange.getRange(rangeStr, true);
		assertEquals(0.0, r.getFrom());
		assertEquals(2353.6, r.getTo());
		rangeStr = "-2.5:2353.6"; // NON-NLS
		r = DoubleRange.getRange(rangeStr);
		assertEquals(-2.5, r.getFrom());
		assertEquals(2353.6, r.getTo());
		rangeStr = "-2.5:-1.6"; // NON-NLS
		r = DoubleRange.getRange(rangeStr);
		assertEquals(-2.5, r.getFrom());
		assertEquals(-1.6, r.getTo());
	}

	@Test
	public void testStreamDataTypes() {
		StreamFieldType t = StreamFieldType.ApplName;
		assertFalse(Utils.isCollectionType(t.getDataType()));
		t = StreamFieldType.ElapsedTime;
		assertFalse(Utils.isCollectionType(t.getDataType()));
		t = StreamFieldType.Correlator;
		assertTrue(Utils.isCollectionType(t.getDataType()));
		t = StreamFieldType.Tag;
		assertTrue(Utils.isCollectionType(t.getDataType()));
		assertTrue(Utils.isCollectionType(HashSet.class));
		assertTrue(Utils.isCollectionType(ArrayList.class));
	}

	@Test
	public void testInputStack() throws Exception {
		String str = "1234567890\n2345678901\n3456789012\n4567890123"; // NON-NLS

		ByteArrayInputStream bis = new ByteArrayInputStream(str.getBytes());
		BufferedReader br = new BufferedReader(new InputStreamReader(bis));

		byte[] lineBytes = new byte[11];
		bis.read(lineBytes);
		String tStr = new String(lineBytes);
		assertEquals("1234567890", tStr.substring(0, tStr.length() - 1)); // NON-NLS
		tStr = br.readLine();
		assertEquals("2345678901", tStr); // NON-NLS
	}

	@Test
	public void testDeepToString() {
		Object[] array = new Object[] { 25, new Object[] { 52, true, null }, false, "pnu", "byte array test".getBytes(), // NON-NLS
				new Object[] { "deeep", new Object[] { "even deeper", new Object[] {}, null, true, 1524.147 }, 15, // NON-NLS
						"char array test".toCharArray() } }; // NON-NLS

		String str = String.valueOf(array);
		assertNotNull(str);

		str = Arrays.toString(array);
		assertNotNull(str);

		str = Utils.toStringDeep(array);
		assertNotNull(str);

		str = Arrays.deepToString(array);
		assertNotNull(str);

		str = ArrayUtils.toString(array);
		assertNotNull(str);
	}

	@Test
	public void testHEXString() throws Exception {
		String str = "makaronai buvo GILIAI neskanus!"; // NON-NLS
		byte[] b = str.getBytes();

		String hexStr = Hex.encodeHexString(b);
		assertNotNull(hexStr);

		hexStr = DatatypeConverter.printHexBinary(b);
		assertNotNull(hexStr);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		HexDump.dump(b, 0, bos, 0);
		hexStr = bos.toString(Utils.UTF8);
		bos.close();
		assertNotNull(hexStr);
	}

	@Test
	public void testGroovy() throws Exception {
		// String path = "C:\\program files\\skype\\skype.exe"; //NON-NLS
		byte[] path = new byte[] { 32, 38, 42 };

		Binding binding = new Binding();
		binding.setVariable("$fieldValue", path); // NON-NLS
		GroovyShell shell = new GroovyShell(binding);

		// String groovyScript = "fieldValue.toUpperCase()"; //NON-NLS
		// String groovyScript = "String file = $fieldValue.substring($fieldValue.lastIndexOf(\"/\") + //NON-NLS
		// 1).substring($fieldValue.lastIndexOf(\"\\\\\") + 1); return file.toUpperCase();"; //NON-NLS
		String groovyScript = "new String($fieldValue, \"UTF-8\")"; // NON-NLS

		Object value = shell.evaluate(groovyScript, "FieldTransformScript"); // NON-NLS

		assertNotNull(value);

		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("groovy"); // NON-NLS
		factory.put("$fieldValue", path); // NON-NLS

		value = engine.eval(groovyScript);

		assertNotNull(value);
	}

	@Test
	public void testGroovyWithJavaAPI() throws Exception {
		// int gmo = 8272;
		int gmo = 10240;

		Binding binding = new Binding();
		binding.setVariable("$fieldValue", gmo); // NON-NLS
		GroovyShell shell = new GroovyShell(binding);

		// String groovyScript = "com.jkoolcloud.tnt4j.streams.utils.Utils.matchAny ($fieldValue, 16 | 32 | 2048)";
		// //NON-NLS
		String groovyScript = "($fieldValue & (16 | 32 | 2048)) != 0"; // NON-NLS

		Object value = shell.evaluate(groovyScript, "MaskMatchScript"); // NON-NLS

		assertNotNull(value);

		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("groovy"); // NON-NLS
		factory.put("$fieldValue", gmo); // NON-NLS

		value = engine.eval(groovyScript);

		assertNotNull(value);
	}

	@Test
	public void testGroovyImports() throws Exception {
		String groovyScript = "$EventType == OpType.SEND && $OperationTime != null && new Date($OperationTime / 1000).before(Date.parse())";

		// inject context and properties
		Binding binding = new Binding();
		binding.setVariable("$EventType", OpType.CALL);
		binding.setVariable("$OperationTime", System.currentTimeMillis() - 352225);

		// parse the recipe text file
		GroovyShell gs = new GroovyShell(binding, StreamsScriptingUtils.getDefaultGroovyCompilerConfig());

		Object shellValue = gs.evaluate(groovyScript);

		assertNotNull(shellValue);

		CompiledScript script = StreamsScriptingUtils.compileGroovyScript(groovyScript);

		Bindings bindings = new SimpleBindings();
		bindings.put("$EventType", OpType.CALL);
		bindings.put("$OperationTime", System.currentTimeMillis() - 352225);

		Object seValue = script.eval(bindings);

		assertNotNull(seValue);

		assertEquals(shellValue, seValue);
	}

	@Test
	public void testJavaScript() throws Exception {
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("javascript"); // NON-NLS
		// factory.put("$fieldValue", "xxxxxxxxxxxxxxxxxx"); //NON-NLS
		factory.put("$fieldValue", new byte[] { 32, 38, 42 }); // NON-NLS

		String tString = new String((byte[]) factory.get("$fieldValue"), StandardCharsets.UTF_8); // NON-NLS

		// Object value = engine.eval("$fieldValue.toUpperCase()");

		String script = "function bin2string(array){\n" + "\tvar result = \"\";\n" // NON-NLS
				+ "\tfor(var i = 0; i < array.length; ++i){\n" + "\t\tresult+= (String.fromCharCode(array[i]));\n" // NON-NLS
				+ "\t}\n" + "\treturn result;\n" + "} bin2string($fieldValue);"; // NON-NLS

		Object value = engine.eval(script);

		// assertNotNull(value);
		assertEquals(tString, value);
	}

	@Test
	public void testJavaScriptImports() throws Exception {
		String script = "$EventType == OpType.SEND";

		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("javascript"); // NON-NLS
		factory.put("$EventType", OpType.CALL); // NON-NLS

		Object value = engine.eval(StreamsScriptingUtils.addDefaultJSScriptImports(script));

		assertNotNull(value);
	}

	@Test
	public void testXPath() throws Exception {
		XPath xPath = XPathFactory.newInstance().newXPath();

		Object value = xPath.evaluate("round(5.5213)", (Object) null); // NON-NLS

		assertNotNull(value);

		String path = "opt/log/cccc.log"; // NON-NLS

		NamespaceMap nsm = new NamespaceMap();
		nsm.setPrefixUriMapping("ts", "http://github.com/Nastel/tnt4j-streams"); // NON-NLS

		xPath.setNamespaceContext(nsm);
		xPath.setXPathFunctionResolver(new MyFunctionResolver());
		xPath.setXPathVariableResolver(new MyVariableResolver(path));

		value = xPath.evaluate("ts:getFileName(concat($fieldValue, \"459\"))", (Object) null); // NON-NLS

		assertNotNull(value);

		// xPath.setNamespaceContext(new ExtensionNamespaceContext());
		// xPath.setXPathFunctionResolver(new XPathFunctionResolverImpl());
		//
		// value = xPath.evaluate("math:random()", (Object) null);
		//
		// assertNotNull(value);
	}

	private static class MyFunctionResolver implements XPathFunctionResolver {
		@Override
		public XPathFunction resolveFunction(QName fname, int arity) {
			if (fname == null) {
				throw new NullPointerException("The function name cannot be null."); // NON-NLS
			}

			if (fname.equals(new QName("http://github.com/Nastel/tnt4j-streams", FuncGetFileName.FUNCTION_NAME))) { // NON-NLS
				return new FuncGetFileName();
			} else {
				return null;
			}
		}
	}

	private static class MyVariableResolver implements XPathVariableResolver {
		private Object fieldValue;

		public MyVariableResolver(Object fieldValue) {
			this.fieldValue = fieldValue;
		}

		@Override
		public Object resolveVariable(QName variableName) {
			if (variableName == null) {
				throw new NullPointerException("The variable name cannot be null."); // NON-NLS
			}

			if (variableName.equals(new QName("fieldValue"))) { // NON-NLS
				return fieldValue;
			} else {
				return null;
			}
		}
	}

	@Test
	public void testFile() throws Exception {
		String fileD = "D:/tmp/Slabs.xml"; // NON-NLS
		File file = new File(fileD);

		assertNotNull(file);
		assertTrue(file.exists());

		URI fUri = file.toURI();

		String fileDU = "file:///" + fileD; // NON-NLS
		file = new File(fileDU);

		assertNotNull(file);
		assertFalse(file.exists());

		fUri = new URI(fileDU);
		file = new File(fUri);

		assertNotNull(file);
		assertTrue(file.exists());

		Path p = Paths.get(fileD);
		file = p.toFile();

		assertNotNull(file);
		assertTrue(file.exists());

		p = Paths.get(fUri);
		file = p.toFile();

		assertNotNull(file);
		assertTrue(file.exists());
	}

	@Test
	public void testMsgId() throws Exception {
		byte[] msgId = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		String msgIdStr = Utils.base64EncodeStr(msgId);
		assertEquals("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", msgIdStr); // NON-NLS

		msgIdStr = Utils.encodeHex(msgId);
		assertEquals("000000000000000000000000000000000000000000000000", msgIdStr); // NON-NLS

		msgIdStr = Utils.toHexString(msgId);
		assertEquals("0x000000000000000000000000000000000000000000000000", msgIdStr); // NON-NLS

		msgIdStr = Utils.toHexDump(msgId);
		assertEquals("\r\n" + "00000000 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ................" + "\r\n" // NON-NLS
				+ "00000010 00 00 00 00 00 00 00 00                         ........\r\n", msgIdStr); // NON-NLS
	}

	@Test
	public void testExprStringVariables() {
		Set<String> vars = new HashSet<>();
		String exprStr = "${EventType} == OpType.SEND && ${OperationTime} != null && new Date(${OperationTime}.getTimeMillis()).before(Date.parse())"; // NON-NLS

		Utils.resolveExpressionVariables(vars, exprStr);

		assertTrue(!vars.isEmpty());
	}

	@Test
	public void testXPathActivityFilter() throws Exception {
		// String exprString = "equals(${EventType}, OpType.SEND) && round(${OperationTime})"; //NON-NLS
		String exprString = "round(${OperationTime})"; // NON-NLS

		ActivityField etf = new ActivityField("EventType"); // NON-NLS
		ActivityField otf = new ActivityField("OperationTime", ActivityFieldDataType.Number); // NON-NLS

		ActivityInfo ai = new ActivityInfo();
		ai.applyField(etf, "SEND"); // NON-NLS
		ai.applyField(otf, System.currentTimeMillis());

		XPathExpressionFilter filter = new XPathExpressionFilter(exprString);
		filter.doFilter(null, ai);

		assertFalse(ai.isFilteredOut());
	}

	@Test
	public void testGroovyExpressionFilter() throws Exception {
		// String exprString = "$fieldValue.startsWith(\"{\"ResourceStatistics\")"; // NON-NLS
		String exprString = "$fieldValue.contains('ResourceStatistics')"; // NON-NLS
		String value = "{\"ResourceStatistics\":{\"ResourceType\":[{\"name\":\"JVM\",\"resourceIdentifier\""; // NON-NLS

		GroovyExpressionFilter filter = new GroovyExpressionFilter(HandleType.EXCLUDE.name(), exprString);
		boolean filteredOut = filter.doFilter(value, null);

		assertTrue(filteredOut);
	}

	@Test
	public void textMsgFormat() {
		MessageFormat mf = new MessageFormat("{0}={1}@{2}");
		mf.format(new Object[] { "file", "c://temp.txt" });
	}

	@Test
	public void testReplace() {
		String res = StringUtils.replaceChars("asd@vsd$ffff", "@$", "__");
		res.length();
	}

	@Test
	public void testSplit4() {
		// String[] res = StringUtils.split("asd@vsd$ffff", "@$");
		String[] res = StringUtils.split("asd@vsd@ffff", "@");
		int len = res.length;
	}

	@Test
	public void testDelimiter() {
		String dataStr = ": :33A: TEST11_TID1234;\n" + ":24B: TEST11_TRANTYPE;\n" + ":35J: TEST11_$123.45;\n"
				+ ":57J: /ABIC/TEST11_LOYDGB22\n" + "/NAME/Lloyds_Treasury\r" + "/GBSC/301557;\n\r"
				+ ":36: TEST11_SENDING_PARTY;";
		StringMatcher fieldDelim = StringMatcherFactory.INSTANCE.charSetMatcher("\n\r");

		org.apache.commons.text.StringTokenizer tk = new org.apache.commons.text.StringTokenizer(dataStr, fieldDelim);
		tk.setIgnoreEmptyTokens(false);
		String[] fields = tk.getTokenArray();

		assertNotNull(fields);
	}

	@Test
	public void testDelimiter2() {
		String dataStr = ":33A:BankOfParis\n" + ":25:234.00DLR";
		StringMatcher fieldDelim = StringMatcherFactory.INSTANCE.charSetMatcher("\n");
		String valueDelim = ":";
		String regex = "";

		org.apache.commons.text.StringTokenizer tk = new org.apache.commons.text.StringTokenizer(dataStr, fieldDelim);
		tk.setIgnoreEmptyTokens(false);
		String[] fields = tk.getTokenArray();

		assertNotNull(fields);

		Map<String, String> nameValues = new HashMap<>(fields.length);
		for (String field : fields) {
			if (field != null) {
				String[] nv = field.split(Pattern.quote(valueDelim));
				if (ArrayUtils.isNotEmpty(nv)) {
					nameValues.put(nv[0], nv.length > 1 ? nv[1].trim() : "");
				}
			}
		}

		assertNotNull(nameValues);
	}

	@Test
	public void testDataType() throws Exception {
		byte[] data = Files.readAllBytes(Paths.get("./samples/swift/msg.bin"));

		ActivityFieldLocator afl = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "XXXXXX",
				ActivityFieldDataType.String);
		afl.setFormat("string", null);
		Object val = afl.formatValue(data);

		assertNotNull(val);
	}

	@Test
	public void testUsecTimestamp() {
		UsecTimestamp ts = new UsecTimestamp();
		ts.setTimeUsec(-1507940941563610L);

		assertEquals(ts.getTimeUsec(), -1507940941563610L);

		ts.add(0, -350);
		ts.subtract(0, -350);

		assertEquals(ts.getTimeUsec(), -1507940941563610L);
	}

	@Test
	public void testTimeTracker() throws Exception {
		String res = "XXXXXX";
		TimeTracker ACTIVITY_TIME_TRACKER = TimeTracker.newTracker(1000, TimeUnit.HOURS.toMillis(8));

		long t11 = TimeTracker.hitAndGet();
		long diff11 = TimestampFormatter.convert(t11, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);
		assertTrue(diff11 >= 0);
		assertTrue(diff11 < 5);

		Thread.sleep(500);
		long t12 = TimeTracker.hitAndGet();
		long diff12 = TimestampFormatter.convert(t12, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);

		assertTrue(diff12 > 490);
		assertTrue(diff12 < 510);

		long t21 = ACTIVITY_TIME_TRACKER.hitAndGet(res);
		long diff21 = TimestampFormatter.convert(t21, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);
		assertTrue(diff21 >= 0);
		assertTrue(diff21 < 10);

		Thread.sleep(500);
		long t22 = TimeTracker.hitAndGet();
		long diff22 = TimestampFormatter.convert(t22, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);
		assertTrue(diff22 > 490);
		assertTrue(diff22 < 510);

		Thread.sleep(500);
		long t23 = ACTIVITY_TIME_TRACKER.hitAndGet(res);
		long diff23 = TimestampFormatter.convert(t23, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);
		assertTrue(diff23 > 990);
		assertTrue(diff23 < 1010);
	}

	@Test
	public void testTimeConvert() {
		long ms = TimestampFormatter.convert(200000, TimeUnit.NANOSECONDS, TimeUnit.MICROSECONDS);
		assertEquals(ms, 200);
	}

	@Test
	public void testWhitspaceChars() {
		assertTrue(Character.isWhitespace(' '));
		assertTrue(Character.isWhitespace('\n'));
		assertTrue(Character.isWhitespace('\r'));

		assertTrue(Character.isWhitespace('\t'));
	}

	@Test
	public void testFormatter() {
		Property dpip = new Property("positiveInfinityDouble", Double.POSITIVE_INFINITY);
		Property dnip = new Property("negativeInfinityDouble", Double.NEGATIVE_INFINITY);
		Property dnanp = new Property("NaNDouble", Double.NaN);
		Property dn = new Property("Double", 8.0d);

		Property fpip = new Property("positiveInfinityFloat", Float.POSITIVE_INFINITY);
		Property fnip = new Property("negativeInfinityFloat", Float.NEGATIVE_INFINITY);
		Property fnanp = new Property("NaNFloat", Float.NaN);
		Property fn = new Property("Float", 8.0f);

		Property bp = new Property("BoolProperty", true);
		Property np = new Property("NullProperty", null);

		JSONFormatter jf = new JSONFormatter();

		Map<String, Object> cfg = new HashMap<>(1);
		cfg.put("SpecNumbersHandling", JSONFormatter.SpecNumbersHandling.SUPPRESS.name());
		jf.setConfiguration(cfg);

		String jStr = jf.format(dpip);
		assertEquals("", jStr);
		jStr = jf.format(dnip);
		assertEquals("", jStr);
		jStr = jf.format(dnanp);
		assertEquals("", jStr);
		jStr = jf.format(dn);
		assertTrue(jStr.contains("8.0}"));

		jStr = jf.format(fpip);
		assertEquals("", jStr);
		jStr = jf.format(fnip);
		assertEquals("", jStr);
		jStr = jf.format(fnanp);
		assertEquals("", jStr);
		jStr = jf.format(fn);
		assertTrue(jStr.contains("8.0}"));

		jStr = jf.format(bp);
		assertTrue(jStr.contains("true}"));
		jStr = jf.format(np);
		assertTrue(jStr.contains("null}"));

		cfg.put("SpecNumbersHandling", JSONFormatter.SpecNumbersHandling.ENQUOTE.name());
		jf.setConfiguration(cfg);

		jStr = jf.format(dpip);
		assertTrue(jStr.contains("\"Infinity\"}"));
		jStr = jf.format(dnip);
		assertTrue(jStr.contains("\"-Infinity\"}"));
		jStr = jf.format(dnanp);
		assertTrue(jStr.contains("\"NaN\"}"));
		jStr = jf.format(dn);
		assertTrue(jStr.contains("8.0}"));

		jStr = jf.format(fpip);
		assertTrue(jStr.contains("\"Infinity\"}"));
		jStr = jf.format(fnip);
		assertTrue(jStr.contains("\"-Infinity\"}"));
		jStr = jf.format(fnanp);
		assertTrue(jStr.contains("\"NaN\"}"));
		jStr = jf.format(fn);
		assertTrue(jStr.contains("8.0}"));

		jStr = jf.format(bp);
		assertTrue(jStr.contains("true}"));
		jStr = jf.format(np);
		assertTrue(jStr.contains("null}"));

		cfg.put("SpecNumbersHandling", JSONFormatter.SpecNumbersHandling.MAINTAIN.name());
		jf.setConfiguration(cfg);

		jStr = jf.format(dpip);
		assertTrue(jStr.contains("Infinity}"));
		jStr = jf.format(dnip);
		assertTrue(jStr.contains("-Infinity}"));
		jStr = jf.format(dnanp);
		assertTrue(jStr.contains("NaN}"));
		jStr = jf.format(dn);
		assertTrue(jStr.contains("8.0}"));

		jStr = jf.format(fpip);
		assertTrue(jStr.contains("Infinity}"));
		jStr = jf.format(fnip);
		assertTrue(jStr.contains("-Infinity}"));
		jStr = jf.format(fnanp);
		assertTrue(jStr.contains("NaN}"));
		jStr = jf.format(fn);
		assertTrue(jStr.contains("8.0}"));

		jStr = jf.format(bp);
		assertTrue(jStr.contains("true}"));
		jStr = jf.format(np);
		assertTrue(jStr.contains("null}"));

		Snapshot s = new PropertySnapshot("XXXXX");
		s.add("P1", "V1");
		s.add(dpip);
		s.add("P2", 555);
		s.add(dnip);

		cfg.put("SpecNumbersHandling", JSONFormatter.SpecNumbersHandling.ENQUOTE.name());
		jf.setConfiguration(cfg);
		jStr = jf.format(s);

		assertTrue(!jStr.contains(",,"));
		assertTrue(!jStr.contains(",]"));
	}

	@Test
	public void testSubstrig() {
		String str = "1245.55";

		String s = StringUtils.substringBefore(str, ".");
		assertEquals("1245", s);

		s = StringUtils.substringAfter(str, ".");
		assertEquals("55", s);

		str = "14526";

		s = StringUtils.substringBefore(str, ".");
		assertEquals("14526", s);

		s = StringUtils.substringAfter(str, ".");
		assertEquals("", s);
	}

	@Test
	public void testCreateNumber() throws Exception {
		Number num = NumberUtils.createNumber("0xAB");
		assertEquals(171, num);

		num = NumberUtils.createNumber("25");
		assertEquals(25, num);

		num = NumberUtils.createNumber("123456.789");
		assertEquals(123456.789, num);

		num = NumberUtils.createNumber("00001256");
		assertEquals(1256, num);

		// num = NumberUtils.createNumber("123,456.789");
		// assertEquals(123456.789, num);

		// num = NumberUtils.createNumber("-5896456,7898658");
		// assertEquals(-5896456.7898658, num);
	}

	@Test
	public void testException() {
		IOException ioe = new IOException("Failed to connect to uri=http://data.jkoolcloud.com:6580");
		ioe.fillInStackTrace();
		Exception ce = new UnknownHostException("data.jkoolcloud.com");
		ce.fillInStackTrace();
		ioe.initCause(ce);

		String exStr = Utils.getExceptionDetail(ioe);
		assertNotNull(exStr);

		exStr = ExceptionUtils.getMessage(ioe);
		assertNotNull(exStr);

		exStr = ExceptionUtils.getRootCauseMessage(ioe);
		assertNotNull(exStr);
	}

	@Test(expected = ClassCastException.class)
	public void testParametrizedInstance() throws Exception {
		ActivityDataPreParser<?, ?> preParser = new AbstractPreParser<String, String>() {
			@Override
			public String preParse(String data) throws Exception {
				return null;
			}

			@Override
			public boolean isDataClassSupported(Object data) {
				return true;
			}
		};

		List<AbstractPreParser<Object, Object>> ppList = new ArrayList<>();
		ppList.add((AbstractPreParser<Object, Object>) preParser);

		assertTrue(ppList.size() == 1);

		ppList.get(0).preParse(1245);
	}

	@Test
	public void testExpFix() {
		String expString = "regexp:$fieldvalue == 52";

		String fve = Matcher.quoteReplacement(StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR);
		String expStrFixed = expString.replaceAll("(?i)" + fve, fve);

		assertEquals("regexp:$fieldValue == 52", expStrFixed);

		expString = "regexp:$FIELDValue == 52";

		expStrFixed = expString.replaceAll("(?i)" + fve, fve);

		assertEquals("regexp:$fieldValue == 52", expStrFixed);
	}

	@Test
	public void testExpSyntax() {
		String expString = "regexp:$fieldvalue == 52";

		int c1 = StringUtils.countMatches(expString, StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR);
		int c2 = StringUtils.countMatches(expString.toLowerCase(),
				StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR.toLowerCase());

		assertNotEquals(c1, c2);

		expString = "regexp:$fieldvalue == 52 || $fieldValue == 53";

		c1 = StringUtils.countMatches(expString, StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR);
		c2 = StringUtils.countMatches(expString.toLowerCase(),
				StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR.toLowerCase());

		assertNotEquals(c1, c2);

		expString = "regexp:$Fieldvalue == 52 || $fieLDValue == 53";

		c1 = StringUtils.countMatches(expString, StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR);
		c2 = StringUtils.countMatches(expString.toLowerCase(),
				StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR.toLowerCase());

		assertNotEquals(c1, c2);

		expString = "regexp:$fieldValue == 52";

		c1 = StringUtils.countMatches(expString, StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR);
		c2 = StringUtils.countMatches(expString.toLowerCase(),
				StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR.toLowerCase());

		assertEquals(c1, c2);

		expString = "regexp:$fieldValue == 52 || $fieldValue == 53";

		c1 = StringUtils.countMatches(expString, StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR);
		c2 = StringUtils.countMatches(expString.toLowerCase(),
				StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR.toLowerCase());

		assertEquals(c1, c2);

		expString = "regexp:${fieldValue} == 52";

		c1 = StringUtils.countMatches(expString, StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR);
		c2 = StringUtils.countMatches(expString.toLowerCase(),
				StreamsScriptingUtils.FIELD_VALUE_VARIABLE_EXPR.toLowerCase());

		assertEquals(c1, c2);
	}

	@Test
	public void testExcSyntaxCheck() {
		String[] expStr = new String[] { "$fieldValue - 20", // 00
				"$fieldValue + \"xxxx\"", // 01
				"$fieldvalue - 20", // 02
				"$fieldValuee - 20", // 03
				"$ieldValue	- 20", // 04
				"$fieldValue.length", // 05
				"xpath:$fieldValue - 20", // 06
				"xpath:$fieldvalue - 20", // 07
				"return ${XXX} - \n\r $fieldValue - \r 50;", // 08
				"return ${XXX} - \n\r $ieldValue - \r 50;", // 09
				"return ${SOMETHING}", // 10
				"return 53 +47;", // 11
				"return $fieldValue++;", // 12
				"return ++$fieldValue;", // 13
				"return $fieldVALue++;", // 14
				"return $field1Value++;", // 15
				"return $fieldValue1++;", // 16
				"${PutTime} == null ? null : ${HighresTime}.difference({PutTime})", // 17
				"das == null ? null : \"XXXXX\".substring(3)", // 18
				"StringUtils.isNotEmpty(${ResolvedObjectString})\n" + "                    ? ${ResolvedObjectString}\n"
						+ "                    : StringUtils.isNotEmpty({TargetName})\n"
						+ "                        ? ${TargetName}\n" + "                        : ${QMgrName}", // 19
				"StringUtils.isNotEmpty(${ResolvedObjectString})\n" + "                    ? ${ResolvedObjectString}\n"
						+ "                    : StringUtils.isNotEmpty(${TargetName})\n"
						+ "                        ? ${TargetName}\n" + "                        : ${QMgrName}", // 20
		};

		assertTrue(StreamsScriptingUtils.isScriptExpressionValid(expStr[0]));
		assertTrue(StreamsScriptingUtils.isScriptExpressionValid(expStr[1]));
		assertFalse(StreamsScriptingUtils.isScriptExpressionValid(expStr[2]));
		assertFalse(StreamsScriptingUtils.isScriptExpressionValid(expStr[3]));
		assertFalse(StreamsScriptingUtils.isScriptExpressionValid(expStr[4]));
		assertTrue(StreamsScriptingUtils.isScriptExpressionValid(expStr[5]));
		assertTrue(StreamsScriptingUtils.isScriptExpressionValid(expStr[6]));
		assertFalse(StreamsScriptingUtils.isScriptExpressionValid(expStr[7]));
		assertTrue(StreamsScriptingUtils.isScriptExpressionValid(expStr[8]));
		assertFalse(StreamsScriptingUtils.isScriptExpressionValid(expStr[9]));
		assertTrue(StreamsScriptingUtils.isScriptExpressionValid(expStr[10]));
		assertTrue(StreamsScriptingUtils.isScriptExpressionValid(expStr[11]));
		assertTrue(StreamsScriptingUtils.isScriptExpressionValid(expStr[12]));
		assertTrue(StreamsScriptingUtils.isScriptExpressionValid(expStr[13]));
		assertFalse(StreamsScriptingUtils.isScriptExpressionValid(expStr[14]));
		assertFalse(StreamsScriptingUtils.isScriptExpressionValid(expStr[15]));
		assertFalse(StreamsScriptingUtils.isScriptExpressionValid(expStr[16]));
		assertFalse(StreamsScriptingUtils.isScriptExpressionValid(expStr[17]));
		assertTrue(StreamsScriptingUtils.isScriptExpressionValid(expStr[18]));
		assertFalse(StreamsScriptingUtils.isScriptExpressionValid(expStr[19]));
		assertTrue(StreamsScriptingUtils.isScriptExpressionValid(expStr[20]));
	}

	@Test
	public void testUsecTimestampToString() {
		long now = System.currentTimeMillis();
		UsecTimestamp utm = new UsecTimestamp(now, 0);

		String ts = utm.toString();

		ts = utm.toString(TimeZone.getTimeZone("GMT"));

		Date d = new Date(now);

		ts = d.toString();

		ts = d.toGMTString();

		assertTrue(ts != null);
	}

	@Test
	public void testURL() throws Exception {
		URI uri = URI.create("asdsdd.ggggg://myusername:mypassword@somehost:2222/tmp");

		String user = uri.getUserInfo();

		uri = new URI(null, "admin", "localhost", 222, "/xxx/ttt/qqq", null, null);

		user = uri.getUserInfo();

		uri = URI.create("zip:file:///./tnt4j-streams-fs/samples/zip-stream/sample.zip!/*");

		String path = uri.getPath();
	}

	@Test
	public void testRELock() {
		ReentrantLock lock = new ReentrantLock();

		lock.lock();
		try {
			assertTrue(lock.getHoldCount() == 1);
			lock.lock();
			try {
				assertTrue(lock.getHoldCount() == 2);
			} finally {
				lock.unlock();
				assertTrue(lock.getHoldCount() == 1);
			}
		} finally {
			lock.unlock();
			assertTrue(lock.getHoldCount() == 0);
		}
	}

	@Test
	public void testEnc() throws Exception {
		byte[] bytes = new byte[] { 0x33, (byte) 0xEE };

		String uft8Str = new String(bytes, StandardCharsets.UTF_8);
		String asciiStr = new String(bytes, StandardCharsets.US_ASCII);
		String ebcdicStr = new String(bytes, "IBM500");

		assertTrue(uft8Str.equals(asciiStr));
	}

	@Test
	public void testTimeFormatter() throws Exception {
		String dts = "20180927 20421607";

		TimestampFormatter tsf = new TimestampFormatter("yyyyMMdd HHmmssSS", null, null);
		UsecTimestamp ts = tsf.parse(dts);

		dts = "20180927 204216";
		ts = tsf.parse(dts);
	}

	@Test
	public void testNumberCast() {
		Long lValue = 20L;

		assertTrue(lValue.getClass().isAssignableFrom(Long.class));
		assertTrue(Long.class.isAssignableFrom(lValue.getClass()));

		assertFalse(lValue.getClass().isAssignableFrom(Number.class));
		assertTrue(Number.class.isAssignableFrom(lValue.getClass()));

		assertFalse(lValue.getClass().isAssignableFrom(Integer.class));
		assertFalse(Integer.class.isAssignableFrom(lValue.getClass()));
	}

}