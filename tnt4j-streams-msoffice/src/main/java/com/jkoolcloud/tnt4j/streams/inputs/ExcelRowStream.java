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

package com.jkoolcloud.tnt4j.streams.inputs;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.utils.MsOfficeStreamConstants;

/**
 * <p>
 * Implements a MS Excel {@link org.apache.poi.ss.usermodel.Workbook} stored
 * activity stream, where each workbook sheet {@link Row} is assumed to
 * represent a single activity or event which should be recorded.
 * <p>
 * This activity stream requires parsers that can support {@link Row} data.
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>FirstRowAsHeader - flag {@code true}/{@code false} indicating whether
 * first row in sheet is used to define table columns titles. If {@code true}
 * then first sheet row is skipped from streaming. Default value - {@code false}
 * . (Optional)</li>
 * </ul>
 * 
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class ExcelRowStream extends AbstractExcelStream<Row> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ExcelRowStream.class);

	private boolean firstRowAsHeader = false;

	private Sheet currSheet;
	private Iterator<Row> rowIterator;

	/**
	 * Constructs a new ExcelRowStream. Requires configuration settings to set
	 * input stream source.
	 */
	public ExcelRowStream() {
		super(LOGGER);
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}
		super.setProperties(props);

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (MsOfficeStreamConstants.PROP_FIRST_ROW_HEADER.equalsIgnoreCase(name)) {
				firstRowAsHeader = Boolean.parseBoolean(value);
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (MsOfficeStreamConstants.PROP_FIRST_ROW_HEADER.equalsIgnoreCase(name)) {
			return firstRowAsHeader;
		}

		return super.getProperty(name);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns a excel sheet {@link Row} containing the contents of
	 * the next raw activity data item.
	 */
	@Override
	public Row getNextItem() throws Exception {
		if (currSheet == null || !rowIterator.hasNext()) {
			currSheet = getNextNameMatchingSheet();

			if (currSheet == null) {
				return null;
			} else {
				rowIterator = currSheet.rowIterator();

				if (firstRowAsHeader) {
					// skip header row from parsing
					rowIterator.next();
				}
			}
		}

		if (!rowIterator.hasNext()) {
			return getNextItem();
		}

		return rowIterator.next();
	}
}
