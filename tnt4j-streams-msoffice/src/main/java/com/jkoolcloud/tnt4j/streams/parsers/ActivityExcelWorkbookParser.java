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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.text.ParseException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.utils.MsOfficeStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * Implements activity data parser that assumes each activity data item is an MS Excel
 * {@link org.apache.poi.ss.usermodel.Workbook} data structure, where each field is represented by a workbook cell
 * reference (e.g., "Sheet1!A1", "Sheet2!H12" or "AA1" where part before "!" is sheet name and remaining is: letters
 * identifies column, number identifies row. When sheet name is missing, workbook active sheet is used!) and the name is
 * used to map each field into its corresponding activity field.
 * <p>
 * This activity parser supports configuration properties from {@link AbstractExcelParser} (and higher hierarchy
 * parsers).
 *
 * @version $Revision: 2 $
 */
public class ActivityExcelWorkbookParser extends AbstractExcelParser<Workbook> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityExcelWorkbookParser.class);

	/**
	 * Constructs a new ExcelWorkbookParser.
	 */
	public ActivityExcelWorkbookParser() {
		super();
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}

	/**
	 * Returns whether this parser supports the given format of the activity data. This is used by activity streams to
	 * determine if the parser can parse the data in the format that the stream has it.
	 * <p>
	 * This parser supports the following class types (and all classes extending/implementing any of these):
	 * <ul>
	 * <li>{@link org.apache.poi.ss.usermodel.Workbook}</li>
	 * </ul>
	 *
	 * @param data
	 *            data object whose class is to be verified
	 * @return {@code true} if this parser can process data in the specified format, {@code false} - otherwise
	 */
	@Override
	protected boolean isDataClassSupportedByParser(Object data) {
		return Workbook.class.isInstance(data);
	}

	/**
	 * Gets field raw data value resolved by locator.
	 *
	 * @param locator
	 *            activity field locator
	 * @param cData
	 *            activity context data package having MS Excel document workbook as activity data object
	 * @param formattingNeeded
	 *            flag to set if value formatting is not needed
	 * @return raw value resolved by locator, or {@code null} if value is not resolved
	 *
	 * @throws ParseException
	 *             if exception occurs while resolving raw data value
	 */
	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) throws ParseException {
		Object val = null;
		String locStr = locator.getLocator();
		Workbook workbook = cData.getData();

		if (StringUtils.isNotEmpty(locStr)) {
			CellReference ref = new CellReference(locStr);
			if (ref.getRow() < 0 || ref.getCol() < 0) {
				throw new ParseException(
						StreamsResources.getStringFormatted(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME,
								"ActivityExcelRowParser.unresolved.cell.reference", locStr),
						0);
			}
			String sheetName = ref.getSheetName();
			Sheet sheet = sheetName != null ? workbook.getSheet(sheetName)
					: workbook.getSheetAt(workbook.getActiveSheetIndex());
			Row row = sheet.getRow(ref.getRow());
			if (row != null) {
				Cell cell = row.getCell(ref.getCol());
				if (cell != null) {
					val = getCellValue(cell);
				} else {
					val = row;
				}
			}

			logger().log(OpLevel.TRACE, StreamsResources.getBundle(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME),
					"ActivityExcelRowParser.resolved.cell.value", locStr, workbook.getMissingCellPolicy(),
					toString(val));
		}

		return val;
	}

	/**
	 * Returns type of RAW activity data entries.
	 *
	 * @return type of RAW activity data entries - EXCEL WORKBOOK
	 */
	@Override
	protected String getActivityDataType() {
		return "EXCEL WORKBOOK"; // NON-NLS
	}
}
