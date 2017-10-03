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

package com.jkoolcloud.tnt4j.streams.preparsers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
public class XMLFromBinDataPreParserTest {

	@Test
	public void testPreParseDefaultNS() throws Exception {
		byte[] fileBuffer = Files
				.readAllBytes(Paths.get("../tnt4j-streams-core/samples/XML-from-bin-data/default_ns.dump")); // NON-NLS

		String output = preParseBinData(fileBuffer);

		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<BLOB_BO:BLOB_BO value=\"asdf\"\n"
				+ "    xmlns:BLOB_BO=\"http://www.ibm.com/websphere/crossworlds/2002/BOSchema/BLOB_BO\"\n"
				+ "    xmlns:JText_META_DATA=\"http://www.ibm.com/websphere/crossworlds/2002/BOSchema/JText_META_DATA\"\n"
				+ "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
				+ "    <BLOB_BO:BLOB>3</BLOB_BO:BLOB>\n" + "    <BLOB_BO:MetaData>\n"
				+ "        <JText_META_DATA:JText_META_DATA>\n"
				+ "            <JText_META_DATA:FileWriteMode>o</JText_META_DATA:FileWriteMode>\n"
				+ "            <JText_META_DATA:OutFileName>D:\\MQTools\\test_nastel\\file\\SAP_ZCORDELVRY05_OTC_XXX_YYYY_20160317_134839222.txt</JText_META_DATA:OutFileName>\n"
				+ "        </JText_META_DATA:JText_META_DATA>\n" + "    </BLOB_BO:MetaData>\n" + "</BLOB_BO:BLOB_BO>\n"; // NON-NLS

		Assert.assertEquals(expected, output);
	}

	@Test
	public void testPreParse() throws Exception {
		// Source data
		byte[] fileBuffer = Files.readAllBytes(Paths.get("../tnt4j-streams-core/samples/XML-from-bin-data/RFH2.dump")); // NON-NLS

		String output = preParseBinData(fileBuffer);

		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root>\n    <mcd>\n        <Msd>jms_text</Msd>\n    </mcd>\n    <jms/>\n    <usr>\n        <Root>ADP</Root>\n        <DataClass>HR</DataClass>\n        <Verb>GET</Verb>\n        <Noun>INDICATIVEASSOCIATE</Noun>\n        <DataVer>003:000</DataVer>\n        <SrcAppID>ACS</SrcAppID>\n        <CustID>0AACH8JB0DZ00013</CustID>\n        <ADPMsgCorrelationID>FE02F255-8872-1319-42C3-B84DFBBBFDF1</ADPMsgCorrelationID>\n        <SrcSysID>ACS</SrcSysID>\n        <SrcAppVer>001:000</SrcAppVer>\n        <ADPHdrVer>002:001</ADPHdrVer>\n        <PldEffTime>20090910T120813000Z</PldEffTime>\n        <PldFmt>XML</PldFmt>\n        <ADPSegCont>N</ADPSegCont>\n        <SrcCorrID>D47E6EE0-C01B-117B-8552-FFD7C1F0F7FF</SrcCorrID>\n        <RoutingOverride>REPLACE</RoutingOverride>\n        <RoutingOverrideQueue>ADP.ES.INFO.BB.PAYX</RoutingOverrideQueue>\n        <TestID>ACS_Get_IndicativeAssociate_3.0</TestID>\n    </usr>\n</root>\n"; // NON-NLS
		Assert.assertEquals(expected, output);
	}

	@Test
	public void testPreParseIncomplete() throws Exception {
		// Source data
		byte[] fileBuffer = Files
				.readAllBytes(Paths.get("../tnt4j-streams-core/samples/XML-from-bin-data/RFH2_incomplete.dump")); // NON-NLS

		String output = preParseBinData(fileBuffer);

		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root>\n    <mcd>\n        <Msd>jms_text</Msd>\n    </mcd>\n    <jms/>\n    <usr>\n        <Root>ADP</Root>\n        <DataClass>HR</DataClass>\n        <Verb>GET</Verb>\n        <Noun>INDICATIVEASSOCIATE</Noun>\n        <DataVer>003:000</DataVer>\n        <SrcAppID>ACS</SrcAppID>\n        <CustID>0AACH8JB0DZ00013</CustID>\n        <ADPMsgCorrelationID>FE02F255-8872-1319-42C3-B84DFBBBFDF1</ADPMsgCorrelationID>\n        <SrcSysID>ACS</SrcSysID>\n        <SrcAppVer>001:000</SrcAppVer>\n        <ADPHdrVer>002:001</ADPHdrVer>\n        <PldEffTime>20090910T120813000Z</PldEffTime>\n        <PldFmt>XML</PldFmt>\n        <ADPSegCont>N</ADPSegCont>\n        <SrcCorrID>D47E6EE0-C01B-117B-8552-FFD7C1F0F7FF</SrcCorrID>\n        <RoutingOverride>REPLACE</RoutingOverride>\n        <RoutingOverrideQueue>ADP.ES.INFO.BB.PAYX</RoutingOverrideQueue>\n    </usr>\n</root>\n"; // NON-NLS
		Assert.assertEquals(expected, output);
	}

	private String preParseBinData(byte[] binData) throws Exception {
		// init
		XMLFromBinDataPreParser parser = new XMLFromBinDataPreParser();

		// Source
		InputStream is = new ByteArrayInputStream(binData);

		// Parse
		Node document = parser.preParse(is);

		// Result
		DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
		DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS"); // NON-NLS
		LSOutput lsOutput = impl.createLSOutput();
		lsOutput.setEncoding(Utils.UTF8);
		Writer stringWriter = new StringWriter();
		lsOutput.setCharacterStream(stringWriter);
		LSSerializer lsSerializer = impl.createLSSerializer();

		lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE); // NON-NLS
		// lsSerializer.getDomConfig().setParameter("xml-declaration",
		// keepDeclaration); // Set this to true if the declaration is needed to
		// be outputted.

		lsSerializer.write(document, lsOutput);
		String output = stringWriter.toString();

		return output;
	}
}
