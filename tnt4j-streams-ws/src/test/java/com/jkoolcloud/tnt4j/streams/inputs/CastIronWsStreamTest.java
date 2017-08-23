package com.jkoolcloud.tnt4j.streams.inputs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mockito.Mockito;

import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.utils.StreamsCache;

public class CastIronWsStreamTest {
	@Test
	public void testPreProcess() {
		CastIronWsStream stream = new CastIronWsStream();
		String req = "                     <![CDATA[\n" + "\t\t\t\t\t\t\t  SOAPAction:https://192.168.3.3/ws/lognotif\n"
				+ "\t\t\t\t\t\t\t   <log:searchLogs xmlns:log=\"http://www.approuter.com/schemas/2008/1/lognotif\">\n"
				+ "\t\t\t\t\t\t\t\t  <log:logComponent>all</log:logComponent>\n"
				+ "\t\t\t\t\t\t\t\t  <log:logLevel>all</log:logLevel>\n"
				+ "\t\t\t\t\t\t\t\t  <log:maxDaysOld>-1</log:maxDaysOld>\n"
				+ "\t\t\t\t\t\t\t\t  <log:status>all</log:status>\n" + "\t\t\t\t\t\t\t\t  <log:page>0</log:page>\n"
				+ "\t\t\t\t\t\t\t\t  <log:pageSize>${XDifference}</log:pageSize>\n" + "\t\t\t\t\t\t\t   </log:sea";

		StreamsCache.addEntry("XDifference", "XDifference", "17", null);
		ActivityInfo ai = Mockito.mock(ActivityInfo.class);
		Mockito.when(ai.getFieldValue("XDifference")).thenReturn("10");
		StreamsCache.cacheValues(ai, "Test");

		String result = stream.preProcess(req);

		assertEquals(result, "                     <![CDATA[\n"
				+ "\t\t\t\t\t\t\t  SOAPAction:https://192.168.3.3/ws/lognotif\n"
				+ "\t\t\t\t\t\t\t   <log:searchLogs xmlns:log=\"http://www.approuter.com/schemas/2008/1/lognotif\">\n"
				+ "\t\t\t\t\t\t\t\t  <log:logComponent>all</log:logComponent>\n"
				+ "\t\t\t\t\t\t\t\t  <log:logLevel>all</log:logLevel>\n"
				+ "\t\t\t\t\t\t\t\t  <log:maxDaysOld>-1</log:maxDaysOld>\n"
				+ "\t\t\t\t\t\t\t\t  <log:status>all</log:status>\n" + "\t\t\t\t\t\t\t\t  <log:page>0</log:page>\n"
				+ "\t\t\t\t\t\t\t\t  <log:pageSize>17</log:pageSize>\n" + "\t\t\t\t\t\t\t   </log:sea");

	}

}
