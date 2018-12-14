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

import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;

import com.ibm.mq.constants.MQConstants;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.streams.utils.WmqUtils;

/**
 * TODO
 * 
 * @version 1.0
 */
public class WmqDataTypeResolver extends StreamDataTypeResolver {

	@Override
	public Integer getIntValue(String s) throws NumberFormatException {
		try {
			return WmqUtils.getParamId(s);
		} catch (NoSuchElementException exc) {
			throw new NumberFormatException("No such numeric MQ constant: " + Utils.quote(s)); // TODO
		}
	}

	@Override
	public String getStrValue(Object obj) {
		String mqConstName = MQConstants.lookup(obj, "MQ.*");

		return StringUtils.isEmpty(mqConstName) ? super.getStrValue(obj) : mqConstName;
	}

	@Override
	public String getStrValue(Number n) {
		String mqConstName = MQConstants.lookup(n.intValue(), "MQ.*");

		return StringUtils.isEmpty(mqConstName) ? super.getStrValue(n) : mqConstName;
	}
}
