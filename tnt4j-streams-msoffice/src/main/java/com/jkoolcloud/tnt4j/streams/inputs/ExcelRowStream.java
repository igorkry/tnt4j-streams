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
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.IntRange;

/**
 * Implements a MS Excel {@link org.apache.poi.ss.usermodel.Workbook} stored activity stream, where each workbook sheet
 * {@link Row} is assumed to represent a single activity or event which should be recorded.
 * <p>
 * This activity stream requires parsers that can support {@link Row} data.
 * <p>
 * This activity stream supports the following properties (in addition to those supported by
 * {@link AbstractExcelStream}):
 * <ul>
 * <li>RangeToStream - defines streamed data rows index range. Default value - {@code 1:}. (Optional)</li>
 * </ul>
 * 
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class ExcelRowStream extends AbstractExcelStream<Row> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ExcelRowStream.class);

	private String rangeValue = "1:"; // NON-NLS
	private IntRange rowRange = null;

	private int totalRows = 0;

	private Sheet currSheet;
	private Iterator<Row> rowIterator;

	/**
	 * Constructs a new ExcelRowStream. Requires configuration settings to set input stream source.
	 */
	public ExcelRowStream() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
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
			if (StreamProperties.PROP_RANGE_TO_STREAM.equalsIgnoreCase(name)) {
				rangeValue = value;
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_RANGE_TO_STREAM.equalsIgnoreCase(name)) {
			return rangeValue;
		}

		return super.getProperty(name);
	}

	@Override
	public int getTotalActivities() {
		return totalRows;
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		rowRange = IntRange.getRange(rangeValue);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns a excel sheet {@link Row} containing the contents of the next raw activity data item.
	 * <p>
	 * If row index is not within user defined property
	 * {@value com.jkoolcloud.tnt4j.streams.configure.StreamProperties#PROP_RANGE_TO_STREAM} range, such rows are
	 * skipped.
	 */
	@Override
	public Row getNextItem() throws Exception {
		while (true) {
			if (currSheet == null || !rowIterator.hasNext()) {
				activityPosition = 0;
				currSheet = getNextNameMatchingSheet(false);

				if (currSheet == null) {
					return null;
				} else {
					rowIterator = currSheet.rowIterator();
					totalRows += currSheet.getPhysicalNumberOfRows();
				}
			}

			if (!rowIterator.hasNext()) {
				continue;
			}

			activityPosition++;
			if (!rowRange.inRange(activityPosition)) {
				// skip row if it is not in range
				skipFilteredActivities();
				rowIterator.next();

				continue;
			}

			Row row = rowIterator.next();

			if (row != null) {
				addStreamedBytesCount(getRowBytesCount(row));
			}

			return row;
		}
	}
}
