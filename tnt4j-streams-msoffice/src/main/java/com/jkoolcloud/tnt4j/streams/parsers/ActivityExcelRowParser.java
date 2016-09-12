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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocatorType;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.MsOfficeStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * <p>
 * Implements activity data parser that assumes each activity data item is an MS Excel
 * {@link org.apache.poi.ss.usermodel.Workbook} {@link Row} data structure, where each field is represented by a row
 * column reference (i.e B, C, AB) and the name is used to map each field onto its corresponding activity field.
 */
public class ActivityExcelRowParser extends GenericActivityParser<Row> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityExcelRowParser.class);

	/**
	 * Constructs a new ExcelRowParser.
	 */
	public ActivityExcelRowParser() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link org.apache.poi.ss.usermodel.Row}</li>
	 * </ul>
	 */
	@Override
	public boolean isDataClassSupported(Object data) {
		return Row.class.isInstance(data);
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		// for (Map.Entry<String, String> prop : props) {
		// String name = prop.getKey();
		// String value = prop.getValue();
		//
		// // no any additional properties are required yet.
		// }
	}

	@Override
	public ActivityInfo parse(TNTInputStream<?, ?> stream, Object data) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}

		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.parsing"), data);

		Row row = (Row) data;

		return parsePreparedItem(stream, row.toString(), row);
	}

	/**
	 * Gets field value from raw data location and formats it according locator definition.
	 *
	 * @param stream
	 *            parent stream
	 * @param locator
	 *            activity field locator
	 * @param row
	 *            MS Excel document row representing activity object data fields
	 *
	 * @return value formatted based on locator definition or {@code null} if locator is not defined
	 *
	 * @throws ParseException
	 *             if error applying locator format properties to specified value
	 *
	 * @see ActivityFieldLocator#formatValue(Object)
	 */
	@Override
	protected Object getLocatorValue(TNTInputStream<?, ?> stream, ActivityFieldLocator locator, Row row)
			throws ParseException {
		Object val = null;
		if (locator != null) {
			String locStr = locator.getLocator();
			if (!StringUtils.isEmpty(locStr)) {
				if (locator.getBuiltInType() == ActivityFieldLocatorType.StreamProp) {
					val = stream.getProperty(locStr);
				} else {
					int cellIndex = CellReference.convertColStringToIndex(locStr);
					Cell cell = row.getCell(cellIndex);
					boolean cellFound = false;
					if (cell != null) {
						val = cell.toString();
						cellFound = true;
					}

					if (!cellFound) {
						LOGGER.log(OpLevel.WARNING,
								StreamsResources.getString(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME,
										"AbstractExcelStream.cell.not.found"),
								locStr, row.getSheet().getWorkbook().getMissingCellPolicy());
					}
				}
			}
			val = locator.formatValue(val);
		}
		return val;
	}
}
