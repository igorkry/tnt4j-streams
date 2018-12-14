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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.Test;
import org.w3c.dom.Element;

import com.ibm.mq.MQMessage;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.MQRFH;
import com.ibm.mq.headers.MQRFH2;
import com.ibm.mq.headers.pcf.MQCFBS;
import com.ibm.mq.headers.pcf.MQCFGR;
import com.ibm.mq.headers.pcf.PCFConstants;
import com.ibm.mq.headers.pcf.PCFMessage;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.StreamsScriptingUtils;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WmqUtils;

/**
 * @author akausinis
 * @version 1.0
 */
public class MyTests {

	@Test
	public void testConstantLookup() throws Exception {
		String cs = "MQCACF_RESOLVED_LOCAL_Q_NAME"; // NON-NLS
		int ic = 3195;

		String lcs = PCFConstants.lookupParameter(ic);
		assertEquals(cs, lcs);

		int lic = PCFConstants.getIntValue(cs);
		assertEquals(ic, lic);
	}

	@Test
	public void testJavaScriptImports() throws Exception {
		String script = "$EventType == OpType.SEND";

		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("javascript"); // NON-NLS
		factory.put("$EventType", OpType.CALL); // NON-NLS

		Object value = engine.eval(StreamsScriptingUtils.addDefaultJSScriptImports(script));

		assertNotNull(value);

		script = "$EventType == MQConstants.MQRC_NO_MSG_AVAILABLE";

		factory.put("$EventType", MQConstants.MQRC_NO_MSG_AVAILABLE); // NON-NLS

		value = engine.eval(StreamsScriptingUtils.addDefaultJSScriptImports(script));

		assertNotNull(value);
	}

	@Test
	public void testRFH2Parsing() throws Exception {
		Path msgFilePath = Paths.get("../tnt4j-streams-core/samples/XML-from-bin-data/RFH2.dump");
		DataInputStream rfhInput = new DataInputStream(new ByteArrayInputStream(Files.readAllBytes(msgFilePath)));

		// MQMessage mqMsg = new MQMessage();
		// mqMsg.write(Files.readAllBytes(msgFilePath));
		MQRFH rfh = new MQRFH();
		rfh.read(rfhInput);
		rfhInput.close();

		parseMQRFH2(rfh);
	}

	private static void parseMQRFH2(MQRFH rfh) throws Exception {

		rfh.getFormat();
	}

	@Test
	public void testRFH2ParsingMsg() throws Exception {
		Path msgFilePath = Paths.get("../tnt4j-streams-core/samples/XML-from-bin-data/RFH2.msg");
		byte[] dataBytes = Files.readAllBytes(msgFilePath);
		DataInputStream rfhInput = new DataInputStream(new ByteArrayInputStream(dataBytes));

		com.ibm.msg.client.wmq.compat.base.internal.MQMessage mqMsg = new com.ibm.msg.client.wmq.compat.base.internal.MQMessage();
		mqMsg.write(dataBytes);
		rfhInput.close();

		// parseMQRFH2(mqMsg);
		mqMsg.getDataLength();
	}

	private static void parseMQRFH2(MQMessage mqMessage) throws Exception {
		mqMessage.seek(0); // Set read position back to the beginning of Message Data part

		String rfhStrucID = mqMessage.readStringOfCharLength(4); // MQC.MQRFH_STRUC_ID
		int rfhVersion = mqMessage.readInt(); // MQRFH_VERSION_1 or MQRFH_VERSION_2
		int rfhStrucLength = mqMessage.readInt(); // MQRFH_STRUC_LENGTH_FIXED or MQRFH_STRUC_LENGTH_FIXED_2
		int rfhEncoding = mqMessage.readInt(); // MQC.MQENC_NATIVE
		int rfhCodedCharSetId = mqMessage.readInt(); // MQC.MQCCSI_DEFAULT
		String rfhFormat = mqMessage.readStringOfCharLength(8); // MQFMT_RF_HEADER_2
		int rfhFlags = mqMessage.readInt(); // MQC.MQRFH_NO_FLAGS
		int rfhNameValueCCSID = mqMessage.readInt(); // 1208 - UTF-8

		// int unknownRFHFieldContainingNoValue = mqMessage.readInt(); // unknown field containing hex value 00000000

		// Validate Header
		// if (!mqMessage.format.equals(MQConstants.MQFMT_RF_HEADER_2)) {
		// throw new Exception("Invalid MQRFH2 format. MQFormat[" + mqMessage.format + "] should be["
		// + MQConstants.MQFMT_RF_HEADER_2 + "]");
		// }

		// The coded character set identifier of character data in the application
		// message data. The behavior of the readString, readLine, and writeString
		// methods is altered accordingly.
		mqMessage.characterSet = rfhNameValueCCSID;

		int hdrOffset = mqMessage.getDataOffset();

		//
		// Now parse through all the pairs of data (length, xml data)
		// Part 1: <mcd></mcd>
		// Part 2: <jms></jms>
		// Part 3: <usr></usr> - Contains USER Set properties
		// ...etc ...
		// Last Part: Trailing Message Data
		//
		int dataLen;
		String xml;
		Map<String, Element> rfhXmlSection = new HashMap<>();
		while (hdrOffset < rfhStrucLength) {
			dataLen = mqMessage.readInt(); // Get the length of the XML Data part
			xml = mqMessage.readStringOfCharLength(dataLen); // Get the XML data
			hdrOffset += dataLen + 4;
			//
			// Try to parse XML element, and place the element in the HashMap store
			try {
				// Document doc = XMLUtils.getXmlFromString(xml);
				// Element e = doc.getRootElement();
				// String xmlName = e.getName();
				// rfhXmlSection.put(xmlName, e);
			} catch (Exception ex) {
				// log.error("Failed to parse XML into JDOM Element. XML String[" + xml + "]", ex);
				throw ex;
			}
		}
		mqMessage.encoding = rfhEncoding;
		mqMessage.characterSet = rfhCodedCharSetId;

		//
		// Read Trailing Message Data
		int pos = mqMessage.getDataOffset(); // Current Position
		dataLen = mqMessage.getDataLength(); // Number of bytes remaining
		byte[] rfhDataBytes = new byte[dataLen];
		String rfhDataString = mqMessage.readStringOfCharLength(dataLen);
		mqMessage.seek(pos); // Set read position back to the beginning of Data part
		mqMessage.readFully(rfhDataBytes);
	}

	@Test
	public void howardStreamTest() throws Exception {
		StreamsConfigLoader cfg = new StreamsConfigLoader("../tnt4j-streams-core/samples/swift/tnt-data-source5.xml");
		TNTInputStream<?, ?> stream = cfg.getStream("WmqActivityTraceStreamQM_A");

		byte[] data = Files.readAllBytes(Paths.get("../tnt4j-streams-core/samples/swift/msg.bin"));

		MQMessage msg = new MQMessage();
		MQCFGR traceParam = new MQCFGR();
		traceParam.setParameter(MQConstants.MQCMD_ACTIVITY_TRACE);
		traceParam.write(msg, traceParam.encoding(), 1); // number in grp
		MQCFBS bs = new MQCFBS(MQConstants.MQBACF_MESSAGE_DATA, data);
		bs.write(msg);

		Method m = msg.getClass().getDeclaredMethod("performProcessingBeforePut", int.class);
		m.setAccessible(true);
		m.invoke(msg, 0);

		m = msg.getClass().getDeclaredMethod("performProcessingAfterPut");
		m.setAccessible(true);
		m.invoke(msg);

		m = msg.getClass().getDeclaredMethod("performProcessingBeforeGet");
		m.setAccessible(true);
		m.invoke(msg);

		m = msg.getClass().getDeclaredMethod("performProcessingAfterGet");
		m.setAccessible(true);
		m.invoke(msg);

		PCFMessage pcfMsg = new PCFMessage(msg);

		cfg.getParser("TraceEventsParser").parse(stream, pcfMsg);
	}

	@Test
	public void emptyMD5() {
		MessageDigest MSG_DIGEST = Utils.getMD5Digester();
		MSG_DIGEST.reset();

		byte[] dig = MSG_DIGEST.digest();
		String b64 = Utils.base64EncodeStr(dig);

		assertEquals(b64, "xxxxx");
	}

	@Test
	public void testJMSRFH2() throws Exception {
		Map<String, Object> rfh2Map = new HashMap<>(2);

		byte[] msgBytes = Files.readAllBytes(Paths.get("./samples/_dev/TEST_MSGS/bnym_jmsbyte_crash.bin"));
		ByteArrayInputStream bis = new ByteArrayInputStream(msgBytes);
		DataInputStream dis = new DataInputStream(bis);

		MQRFH2 rfh2 = new MQRFH2();
		rfh2.read(dis);
		Utils.close(dis);

		StringBuilder sb = new StringBuilder("<rfh2Root>" + "\n");
		String[] fStrings = rfh2.getFolderStrings();
		for (String fStr : fStrings) {
			sb.append(fStr).append("\n");
		}
		sb.append("</rfh2Root>\n");

		rfh2Map.put("HEADERS", sb.toString());

		bis.mark(0);

		try (ObjectInputStream ois = new ObjectInputStream(bis)) {
			Object msgData = ois.readObject();
			rfh2Map.put("DATA", msgData);
		} catch (StreamCorruptedException exc) {
			bis.reset();

			byte[] msgDataBytes = new byte[bis.available()];
			bis.read(msgDataBytes);
		}

		Utils.close(bis);

		rfh2Map.toString();
	}

	@Test
	public void testEBCDIC() throws Exception {
		byte[] ebcdicBytes = Files.readAllBytes(Paths.get("./samples/ebcdic/ebcdic data.msg"));

		String asciiStr = new String(ebcdicBytes, Charset.forName("IBM500"));

		assertTrue(asciiStr.startsWith("This is"));

		asciiStr = new String(ebcdicBytes, "Cp037");

		assertTrue(asciiStr.startsWith("This is"));

		asciiStr = WmqUtils.getString(ebcdicBytes, 37);

		assertTrue(asciiStr.startsWith("This is"));

		asciiStr = WmqUtils.getString(ebcdicBytes, 1208);

		assertFalse(asciiStr.startsWith("This is"));

		String ccsidName = WmqUtils.getCharsetName(37);

		assertFalse(ccsidName == null);
	}

	@Test
	public void testEquality() {
		int valS = Integer.parseInt("037"); // 37 in dec

		int val = 037; // octal value, 31 in dec

		assertFalse(valS == val);
		assertTrue(valS == 37);
		assertTrue(val == 31);
	}

	@Test
	public void testMQIStructMatch() {
		Pattern STRUCT_ATTR_PATTERN = Pattern.compile("MQBACF_(\\w{4,5})_STRUCT"); // NON-NLS

		boolean matches = STRUCT_ATTR_PATTERN.matcher("MQBACF_MQGMO_STRUCT").matches();
		assertTrue(matches);
		matches = STRUCT_ATTR_PATTERN.matcher("MQBACF_MQMD_STRUCT").matches();
		assertTrue(matches);

		matches = STRUCT_ATTR_PATTERN.matcher("MQBACF_MQM_STRUCT").matches();
		assertFalse(matches);

		matches = STRUCT_ATTR_PATTERN.matcher("MQBACF_MQMRTG_STRUCT").matches();
		assertFalse(matches);

		matches = STRUCT_ATTR_PATTERN.matcher("MQCFC_PUT_TIME").matches();
		assertFalse(matches);
	}

	@Test
	public void testBase64() {
		byte[] bytes = Utils.base64Decode("NTY0NTQ2MTU2MTkxOTgrMTQ5ODE5KwAA");

		assertNotEquals(bytes.length, 22);
		assertEquals(bytes.length, 24);

		String bs = Utils.base64EncodeStr(bytes);

		assertEquals(bs.length(), 32);

		bs = Utils.base64EncodeStr(Arrays.copyOfRange(bytes, 0, 22));

		assertEquals(bs.length(), 32);
	}
}
