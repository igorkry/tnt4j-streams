/*
 * Copyright 2014-2016 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.streams.samples.custom;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.jkoolcloud.tnt4j.streams.StreamsAgent;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.fields.StreamFieldType;
import com.jkoolcloud.tnt4j.streams.inputs.FileLineStream;
import com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser;

/**
 * Sample integration of TNT4J-Streams into an application using plain Java API without configuration XML file.
 *
 * This sample applies same configuration as in 'single-log' sample.
 *
 * @version $Revision: 1 $
 */
public class SampleStreamingApp {

	/**
	 * Main entry point for running as a standalone application.
	 *
	 * @param args
	 *            program command-line arguments. None is required
	 * @throws Exception
	 *             if any exception occurs while running application
	 */
	public static void main(String... args) throws Exception {
		Collection<Map.Entry<String, String>> props = new ArrayList<Map.Entry<String, String>>();
		props.add(new AbstractMap.SimpleEntry<String, String>(ParserProperties.PROP_FLD_DELIM, "|"));

		ActivityTokenParser atp = new ActivityTokenParser();
		atp.setName("TokenParser");
		atp.setProperties(props);

		ActivityField f = new ActivityField(StreamFieldType.StartTime.name());
		ActivityFieldLocator afl = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "1");
		f.addLocator(afl).setFormat("dd MMM yyyy HH:mm:ss").setLocale("en-US");
		atp.addField(f);

		f = new ActivityField(StreamFieldType.ServerIp.name());
		afl = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "2");
		f.addLocator(afl);
		atp.addField(f);

		f = new ActivityField(StreamFieldType.ApplName.name());
		afl = new ActivityFieldLocator("orders");
		f.addLocator(afl);
		atp.addField(f);

		f = new ActivityField(StreamFieldType.Correlator.name());
		afl = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "3");
		f.addLocator(afl);
		atp.addField(f);

		f = new ActivityField(StreamFieldType.UserName.name());
		afl = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "4");
		f.addLocator(afl);
		atp.addField(f);

		f = new ActivityField(StreamFieldType.EventName.name());
		afl = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "5");
		f.addLocator(afl);
		atp.addField(f);

		f = new ActivityField(StreamFieldType.EventType.name());
		afl = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "5");
		afl.addValueMap("Order Placed", "START").addValueMap("Order Received", "RECEIVE")
				.addValueMap("Order Processing", "OPEN").addValueMap("Order Processed", "SEND")
				.addValueMap("Order Shipped", "END");
		f.addLocator(afl);
		atp.addField(f);

		f = new ActivityField("MsgValue");
		afl = new ActivityFieldLocator(ActivityFieldLocatorType.Index, "8");
		f.addLocator(afl);
		atp.addField(f);

		props = new ArrayList<Map.Entry<String, String>>();
		props.add(new AbstractMap.SimpleEntry<String, String>(StreamProperties.PROP_FILENAME, "orders.log"));

		FileLineStream fls = new FileLineStream();
		fls.setName("FileStream");
		fls.setProperties(props);
		fls.addParser(atp);
		// if (fls.getOutput() == null) {
		// fls.setDefaultStreamOutput();
		// }

		StreamsAgent.runFromAPI(fls);
	}
}
