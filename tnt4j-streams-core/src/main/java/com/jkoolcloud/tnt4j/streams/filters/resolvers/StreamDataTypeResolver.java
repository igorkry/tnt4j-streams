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

package com.jkoolcloud.tnt4j.streams.filters.resolvers;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 * @created 2017-04-06 17:41
 */
public class StreamDataTypeResolver {

	public Integer getIntValue(String s) throws NumberFormatException {
		return Integer.parseInt(s);
	}

	public Long getLongValue(String s) throws NumberFormatException {
		return Long.parseLong(s);
	}

	public Short getShortValue(String s) throws NumberFormatException {
		return Short.parseShort(s);
	}

	public Byte getByteValue(String s) throws NumberFormatException {
		return Byte.parseByte(s);
	}

	public Double getDoubleValue(String s) throws NumberFormatException {
		return Double.parseDouble(s);
	}

	public Float getFloatValue(String s) throws NumberFormatException {
		return Float.parseFloat(s);
	}

	public BigInteger getBigIntegerValue(String s) throws NumberFormatException {
		return new BigInteger(s);
	}

	public BigDecimal getBigDecimalValue(String s) throws NumberFormatException {
		return new BigDecimal(s);
	}

	public Boolean getBooleanValue(String s) throws IllegalArgumentException {
		return Utils.toBoolean(s);
	}

	public String getStrValue(Object obj) {
		return Utils.toString(obj);
	}

	public String getStrValue(Number n) {
		return String.valueOf(n);
	}
}
