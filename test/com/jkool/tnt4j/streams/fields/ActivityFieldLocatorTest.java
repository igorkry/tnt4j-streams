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

package com.jkool.tnt4j.streams.fields;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import org.junit.Test;

import com.jkool.tnt4j.streams.utils.StreamTimestamp;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityFieldLocatorTest {
	ActivityFieldLocator locator;

	@Test
	public void testActivityFieldLocatorStringString() {
		locator = new ActivityFieldLocator("TestType", "1");
		assertNotNull(locator);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testActivityFieldLocatorStringStringThrowOnNegative() {
		locator = new ActivityFieldLocator("TestType", "-1");
	}

	@Test
	public void testActivityFieldLocatorActivityFieldLocatorTypeString() {
		for (ActivityFieldLocatorType type : ActivityFieldLocatorType.values()) {
			locator = new ActivityFieldLocator(type, "1");
			assertNotNull(locator);
			assertEquals(type, locator.getBuiltInType());
			assertEquals(locator.getLocator(), "1");
		}
	}

	@Test
	public void testRadix() {
		locator = new ActivityFieldLocator(1);
		locator.setRadix(1);
		assertEquals(1, locator.getRadix());
	}

	@Test
	public void testDataType() {
		locator = new ActivityFieldLocator(1);
		for (ActivityFieldDataType testType : ActivityFieldDataType.values()) {
			locator.setDataType(testType);
			assertEquals("Failed on " + testType.name(), testType, locator.getDataType());
		}
	}

	@Test
	public void testUnits() {
		locator = new ActivityFieldLocator(1);
		locator.setUnits("TEST");
		assertNull(locator.getBuiltInUnits());
		for (ActivityFieldUnitsType unit : ActivityFieldUnitsType.values()) {
			locator.setFormat(unit.name(), null); // XXX
			locator.setUnits(unit.name());
			assertEquals("Failed on " + unit.name(), unit, locator.getBuiltInUnits());
			locator.toDebugString();
		}
	}

	@Test
	public void testFormatNumericValue() throws ParseException {
		locator = new ActivityFieldLocator(1);
		try {
			locator.setFormat(null, Locale.FRANCE.toString());
			assertNull(locator.formatNumericValue(""));
			assertEquals(1.0, locator.formatNumericValue("1.0"));
			assertEquals(1000.0, locator.formatNumericValue("1.000,0"));
		} catch (Exception e) {
		}
	}

	@Test
	public void testFormatDateValue() throws ParseException {
		locator = new ActivityFieldLocator(1);
		StreamTimestamp ts = new StreamTimestamp(new Date());
		assertEquals(ts, locator.formatDateValue(ts));
		locator.setDataType(ActivityFieldDataType.DateTime);
		locator.setUnits(ActivityFieldUnitsType.Milliseconds.name());
		locator.formatDateValue(new Date());
	}

	@Test
	public void testformatValue() throws ParseException {
		locator = new ActivityFieldLocator("TEST");
		assertEquals("TEST", locator.formatValue("DENY"));

		locator = new ActivityFieldLocator("TEST", "1");
		assertNull(locator.formatValue(null));

		locator.setDataType(ActivityFieldDataType.Number);
		assertTrue(locator.formatValue("1") instanceof Number);

		locator.setDataType(ActivityFieldDataType.String);
		assertTrue(locator.formatValue("1") instanceof String);

		locator.setDataType(ActivityFieldDataType.Binary);
		locator.setFormat(ActivityFieldFormatType.hexBinary.name(), null);
		// assertTrue(locator.formatValue("1") instanceof Byte[]); //XXX

		locator.setDataType(ActivityFieldDataType.Binary);
		locator.setFormat(ActivityFieldFormatType.base64Binary.name(), null);
		// assertTrue(locator.formatValue("1") instanceof String); //XXX
	}

	@Test
	public void testGetMappedValue() {
		locator = new ActivityFieldLocator("TEST", "1");
		assertEquals("TEST", locator.getMappedValue("TEST"));
		locator.addValueMap("TEST", "VALUE");
		assertEquals("VALUE", locator.getMappedValue("TEST"));
	}

}
