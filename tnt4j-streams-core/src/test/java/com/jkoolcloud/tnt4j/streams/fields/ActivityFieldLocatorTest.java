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

import static org.junit.Assert.*;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.jkoolcloud.tnt4j.core.UsecTimestamp;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
public class ActivityFieldLocatorTest {
	ActivityFieldLocator locator;

	@Test
	public void testActivityFieldLocatorStringString() {
		locator = new ActivityFieldLocator("TestType", "1"); // NON-NLS
		assertNotNull(locator);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testActivityFieldLocatorStringStringThrowOnNegative() {
		locator = new ActivityFieldLocator("TestType", "-1"); // NON-NLS
	} // NON-NLS

	@Test
	public void testActivityFieldLocatorActivityFieldLocatorTypeString() {
		for (ActivityFieldLocatorType type : ActivityFieldLocatorType.values()) {
			locator = new ActivityFieldLocator(type, "1");
			assertNotNull(locator);
			assertEquals(type, locator.getBuiltInType());
			assertEquals("1", locator.getLocator());
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
			assertEquals("Failed on " + testType.name(), testType, locator.getDataType()); // NON-NLS
		}
	}

	@Test
	public void testUnits() {
		locator = new ActivityFieldLocator(1);
		locator.setUnits("TEST"); // NON-NLS
		assertNull(locator.getBuiltInUnits());
		for (TimeUnit unit : TimeUnit.values()) {
			locator.setFormat(unit.name(), null); // TODO
			locator.setUnits(unit.name());
			assertEquals("Failed on " + unit.name(), unit, locator.getBuiltInUnits()); // NON-NLS
			Utils.getDebugString(locator);
		}
	}

	@Test
	public void testFormatNumericValue() throws ParseException {
		locator = new ActivityFieldLocator(1);
		try {
			locator.setFormat(null, Locale.FRANCE.toString());
			assertNull(locator.formatNumericValue(""));
			assertEquals(1.0, locator.formatNumericValue("1.0")); // NON-NLS
			assertEquals(1000.0, locator.formatNumericValue("1.000,0")); // NON-NLS
		} catch (Exception e) {
		}
	}

	@Test
	public void testFormatDateValue() throws ParseException {
		locator = new ActivityFieldLocator(1);
		UsecTimestamp ts = new UsecTimestamp(new Date());
		assertEquals(ts, locator.formatDateValue(ts));
		locator.setDataType(ActivityFieldDataType.DateTime);
		locator.setUnits(TimeUnit.MILLISECONDS.name());
		locator.formatDateValue(new Date());
	}

	@Test
	public void testformatValue() throws ParseException {
		locator = new ActivityFieldLocator("TEST"); // NON-NLS
		assertEquals("TEST", locator.formatValue("DENY")); // NON-NLS

		locator = new ActivityFieldLocator("TEST", "1"); // NON-NLS
		assertNull(locator.formatValue(null));

		locator.setDataType(ActivityFieldDataType.Number);
		assertTrue(locator.formatValue("1") instanceof Number);

		locator.setDataType(ActivityFieldDataType.String);
		assertTrue(locator.formatValue("1") instanceof String);

		locator.setDataType(ActivityFieldDataType.Binary);
		locator.setFormat(ActivityFieldFormatType.hexBinary.name(), null);
		// assertTrue(locator.formatValue("1") instanceof Byte[]); //TODO

		locator.setDataType(ActivityFieldDataType.Binary);
		locator.setFormat(ActivityFieldFormatType.base64Binary.name(), null);
		// assertTrue(locator.formatValue("1") instanceof String); //TODO
	}

	@Test
	public void testGetMappedValueByValue() {
		locator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "testLocator"); // NON-NLS
		assertEquals("Unexpected mapped value", "TEST", locator.getMappedValue("TEST")); // NON-NLS

		locator.addValueMap("TEST", "VALUE").addValueMap("200", "SUCCESS") // NON-NLS
				.addValueMap("301", "WARNING", ActivityFieldMappingType.Value).addValueMap("404", "ERROR"); // NON-NLS

		assertNotEquals("Mapped value should be different", "TEST", locator.getMappedValue("TEST")); // NON-NLS
		assertNotEquals("Mapped value should be different", "200", locator.getMappedValue(200));
		assertNotEquals("Mapped value should be different", "301", locator.getMappedValue("301")); // NON-NLS
		assertNotEquals("Mapped value should be different", "404", locator.getMappedValue("404")); // NON-NLS

		assertEquals("Unexpected mapped value", "VALUE", locator.getMappedValue("TEST")); // NON-NLS
		assertEquals("Unexpected mapped value", "SUCCESS", locator.getMappedValue("200")); // NON-NLS
		assertEquals("Unexpected mapped value", "WARNING", locator.getMappedValue("301")); // NON-NLS
		assertEquals("Unexpected mapped value", "ERROR", locator.getMappedValue("404")); // NON-NLS
	}

	@Test
	public void testGetMappedValueByCalc() {
		locator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "testLocator"); // NON-NLS
		locator.addValueMap("odd", "odd number", ActivityFieldMappingType.Calc) // NON-NLS
				.addValueMap("Even", "even number", ActivityFieldMappingType.Calc) // NON-NLS
				.addValueMap("EVEN", "other even number"); // NON-NLS

		assertNotEquals("Mapped value should be different", "0", locator.getMappedValue("0")); // NON-NLS
		assertNotEquals("Mapped value should be different", "1", locator.getMappedValue("1")); // NON-NLS
		assertNotEquals("Mapped value should be different", "3", locator.getMappedValue("3")); // NON-NLS

		assertEquals("Unexpected mapped value", "even number", locator.getMappedValue("-2")); // NON-NLS
		assertEquals("Unexpected mapped value", "odd number", locator.getMappedValue("-1")); // NON-NLS
		assertEquals("Unexpected mapped value", "even number", locator.getMappedValue("0")); // NON-NLS
		assertEquals("Unexpected mapped value", "odd number", locator.getMappedValue("1")); // NON-NLS
		assertEquals("Unexpected mapped value", "odd number", locator.getMappedValue("1.9999")); // NON-NLS
		assertEquals("Unexpected mapped value", "even number", // NON-NLS
				locator.getMappedValue("1.9999999999999999999999999999999")); // NON-NLS
		assertEquals("Unexpected mapped value", "even number", locator.getMappedValue("2")); // NON-NLS
		assertEquals("Unexpected mapped value", "odd number", locator.getMappedValue("2.4")); // NON-NLS
		assertEquals("Unexpected mapped value", "odd number", locator.getMappedValue("2.5")); // NON-NLS
		assertEquals("Unexpected mapped value", "odd number", locator.getMappedValue("2.6")); // NON-NLS
		assertEquals("Unexpected mapped value", "odd number", locator.getMappedValue("3")); // NON-NLS
	}

	@Test
	public void testGetMappedValueByRange() {
		locator = new ActivityFieldLocator(ActivityFieldLocatorType.Label, "testLocator"); // NON-NLS
		locator.addValueMap(":-101", "UNKNOWN_FATAL", ActivityFieldMappingType.Range) // NON-NLS
				.addValueMap("-100:-50", "UNKNOWN_LN", ActivityFieldMappingType.Range) // NON-NLS
				.addValueMap("-49:99", "UNKNOWN_L", ActivityFieldMappingType.Range) // NON-NLS
				.addValueMap("100:206", "SUCCESS", ActivityFieldMappingType.Range) // NON-NLS
				.addValueMap("207", "CRITICAL", ActivityFieldMappingType.Range) // NON-NLS
				.addValueMap("208.:220.1223124", "ELECTRO", ActivityFieldMappingType.Range) // NON-NLS
				.addValueMap("300:308", "WARNING", ActivityFieldMappingType.Range) // NON-NLS
				.addValueMap("400:417", "ERROR", ActivityFieldMappingType.Range) // NON-NLS
				.addValueMap("500:511", "ERROR", ActivityFieldMappingType.Range) // NON-NLS
				.addValueMap("512:", "UNKNOWN_U", ActivityFieldMappingType.Range); // NON-NLS

		assertEquals("Unexpected mapped value", "UNKNOWN_FATAL", locator.getMappedValue(-200));
		assertEquals("Unexpected mapped value", "UNKNOWN_FATAL", locator.getMappedValue(-101));
		assertEquals("Unexpected mapped value", "UNKNOWN_LN", locator.getMappedValue("-100")); // NON-NLS
		assertEquals("Unexpected mapped value", "UNKNOWN_LN", locator.getMappedValue("-50")); // NON-NLS
		assertEquals("Unexpected mapped value", "UNKNOWN_L", locator.getMappedValue(-49));
		assertEquals("Unexpected mapped value", "UNKNOWN_L", locator.getMappedValue("0")); // NON-NLS
		assertEquals("Unexpected mapped value", "UNKNOWN_L", locator.getMappedValue(99));
		assertEquals("Unexpected mapped value", "SUCCESS", locator.getMappedValue("100")); // NON-NLS
		assertEquals("Unexpected mapped value", "SUCCESS", locator.getMappedValue(102));
		assertEquals("Unexpected mapped value", "SUCCESS", locator.getMappedValue("206")); // NON-NLS
		assertEquals("Unexpected mapped value", "CRITICAL", locator.getMappedValue(207));
		assertEquals("Unexpected mapped value", "WARNING", locator.getMappedValue("300")); // NON-NLS
		assertEquals("Unexpected mapped value", "WARNING", locator.getMappedValue(303));
		assertEquals("Unexpected mapped value", "WARNING", locator.getMappedValue("308")); // NON-NLS
		assertEquals("Unexpected mapped value", 309, locator.getMappedValue(309));
		assertEquals("Unexpected mapped value", "ERROR", locator.getMappedValue("400")); // NON-NLS
		assertEquals("Unexpected mapped value", "ERROR", locator.getMappedValue(404));
		assertEquals("Unexpected mapped value", "ERROR", locator.getMappedValue("417")); // NON-NLS
		assertEquals("Unexpected mapped value", "418", locator.getMappedValue("418")); // NON-NLS
		assertEquals("Unexpected mapped value", "ERROR", locator.getMappedValue(500));
		assertEquals("Unexpected mapped value", "ERROR", locator.getMappedValue(505));
		assertEquals("Unexpected mapped value", "ERROR", locator.getMappedValue("511")); // NON-NLS
		assertEquals("Unexpected mapped value", "UNKNOWN_U", locator.getMappedValue(512));
		assertEquals("Unexpected mapped value", "UNKNOWN_U", locator.getMappedValue("666")); // NON-NLS
		assertEquals("Unexpected mapped value", "UNKNOWN_U", locator.getMappedValue(99999));

		assertEquals("Unexpected mapped value", "SUCCESS", locator.getMappedValue("200")); // NON-NLS
		assertEquals("Unexpected mapped value", "SUCCESS", locator.getMappedValue(200));
		assertEquals("Unexpected mapped value", "SUCCESS", locator.getMappedValue(200.0));
		assertEquals("Unexpected mapped value", "SUCCESS", locator.getMappedValue(200.0d));
		assertEquals("Unexpected mapped value", "SUCCESS", locator.getMappedValue("200.0")); // NON-NLS
	}
}
