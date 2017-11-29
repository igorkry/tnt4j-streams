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

package com.jkoolcloud.tnt4j.streams.parsers;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.poi.ss.usermodel.*;

/**
 * Base class for abstract activity data parser that assumes each activity data item can MS Excel
 * {@link org.apache.poi.ss.usermodel.Workbook} contained data structure (e.g.,
 * {@link org.apache.poi.ss.usermodel.Sheet} or {@link org.apache.poi.ss.usermodel.Row}), where each field is
 * represented by a {@link org.apache.poi.ss.usermodel.Cell} and the sheet name and cell identifier (row number and
 * column letter) is used to map cell(s) contained data into its corresponding activity field.
 * <p>
 * This activity parser supports configuration properties from {@link GenericActivityParser} (and higher hierarchy
 * parsers).
 *
 * @version $Revision: 1 $
 */
public abstract class AbstractExcelParser<T> extends GenericActivityParser<T> {

	private FormulaEvaluator evaluator;
	protected final ReentrantLock evaluationLock = new ReentrantLock();

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		super.setProperties(props);

		// if (CollectionUtils.isNotEmpty(props)) {
		// for (Map.Entry<String, String> prop : props) {
		// String name = prop.getKey();
		// String value = prop.getValue();
		//
		// // no any additional properties are required yet.
		// if (false) {
		// logger().log(OpLevel.DEBUG,
		// StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityParser.setting"),
		// name, value);
		// }
		// }
		// }
	}

	/**
	 * Evaluates and returns cell contained value.
	 *
	 * @param cell
	 *            cell instance to evaluate value
	 * @return evaluated cell value
	 */
	protected Object getCellValue(Cell cell) {
		switch (cell.getCellTypeEnum()) {
		case BOOLEAN:
			return cell.getBooleanCellValue();
		case NUMERIC:
			return DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue() : cell.getNumericCellValue();
		case FORMULA:
			return evaluateCellFormula(cell);
		case STRING:
			return cell.getRichStringCellValue().toString();
		case BLANK:
			return null;
		default:
			return cell.toString();
		}
	}

	private Object evaluateCellFormula(Cell cell) {
		CellValue cellValue;
		evaluationLock.lock();
		try {
			if (evaluator == null) {
				Workbook workbook = cell.getSheet().getWorkbook();
				evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			}

			cellValue = evaluator.evaluate(cell);
		} finally {
			evaluationLock.unlock();
		}

		return getCellValue(cell, cellValue);
	}

	private static Object getCellValue(Cell cell, CellValue cellValue) {
		if (cellValue == null) {
			return cell.toString();
		}

		switch (cellValue.getCellTypeEnum()) {
		case BOOLEAN:
			return cellValue.getBooleanValue();
		case NUMERIC:
			return DateUtil.isCellDateFormatted(cell) ? DateUtil.getJavaDate(cellValue.getNumberValue())
					: cellValue.getNumberValue();
		case STRING:
			return cellValue.getStringValue();
		default:
			return cellValue.formatAsString();
		}
	}
}
