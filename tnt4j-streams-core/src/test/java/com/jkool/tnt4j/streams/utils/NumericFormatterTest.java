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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.text.ParseException;

import org.junit.Test;

/**
 * @author akausinis
 * @version 1.0
 */
public class NumericFormatterTest {

	@Test
	public void testParseObject() throws ParseException {
		// Test Integers
		assertEquals(2.0, NumericFormatter.parse(null, 1, 2));
		assertEquals(1.0, NumericFormatter.parse(null, 1, null));
		assertNull(NumericFormatter.parse(null, null, null));

	}

	@Test
	public void testParseStaticHex() throws ParseException {
		assertEquals(342.0, NumericFormatter.parse(null, "0xAB", 2));
		assertEquals(171.0, NumericFormatter.parse(null, 0XAB, null));
		assertNull(NumericFormatter.parse(null, null, null));
	}

	@Test
	public void testParseObjectHex() throws ParseException {
		NumericFormatter formatter = new NumericFormatter();
		assertEquals(342.0, formatter.parse("0xAB", 2));
		assertEquals(171.0, formatter.parse(0XAB));
		assertNull(formatter.parse(null, null));
	}

	@Test
	public void testParseStringObjectNumber() throws ParseException {
		NumericFormatter.parse(null, 10, (Number) 1);
	}

	@Test
	public void testRadix() {
		NumericFormatter formatter = new NumericFormatter(5);
		assertEquals(5, formatter.getRadix());
		formatter.setRadix(6);
		assertEquals(6, formatter.getRadix());
	}

	@Test
	public void testPattern() {
		NumericFormatter formatter = new NumericFormatter();
		formatter.setPattern("#", null);
		assertEquals("#", formatter.getPattern());
	}

}
