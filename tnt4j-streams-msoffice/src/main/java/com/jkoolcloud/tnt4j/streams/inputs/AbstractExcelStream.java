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

import java.io.FileInputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.StreamProperties;
import com.jkoolcloud.tnt4j.streams.utils.MsOfficeStreamConstants;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.Utils;

/**
 * <p>
 * Base class for MS Excel workbook stored activity stream, where each workbook
 * sheet or row is assumed to represent a single activity or event which should
 * be recorded.
 * <p>
 * This activity stream supports the following properties:
 * <ul>
 * <li>FileName - the system-dependent file name of MS Excel document.
 * (Required)</li>
 * <li>SheetsToProcess - defines workbook sheets name filter mask (wildcard or
 * RegEx) to process only sheets which names matches this mask. Default value -
 * ''. (Optional)</li>
 * </ul>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkoolcloud.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public abstract class AbstractExcelStream<T> extends TNTParseableInputStream<T> {
	/**
	 * Stream attribute defining file name.
	 */
	private String fileName = null;

	private String sheetName = null;
	private Pattern sheetNameMatcher = null;

	private Workbook workbook;
	private Iterator<Sheet> sheetIterator;

	/**
	 * Constructs a new AbstractExcelStream.
	 *
	 * @param logger
	 *            logger used by activity stream
	 */
	protected AbstractExcelStream(EventSink logger) {
		super(logger);
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
			if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
				fileName = value;
			} else if (MsOfficeStreamConstants.PROP_SHEETS.equalsIgnoreCase(name)) {
				sheetName = value;

				if (StringUtils.isNotEmpty(sheetName)) {
					sheetNameMatcher = Pattern.compile(Utils.wildcardToRegex2(sheetName));
				}
			}
		}
	}

	@Override
	public Object getProperty(String name) {
		if (StreamProperties.PROP_FILENAME.equalsIgnoreCase(name)) {
			return fileName;
		}
		if (MsOfficeStreamConstants.PROP_SHEETS.equalsIgnoreCase(name)) {
			return sheetName;
		}

		return super.getProperty(name);
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		if (StringUtils.isEmpty(fileName)) {
			throw new IllegalStateException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
					"TNTInputStream.property.undefined", StreamProperties.PROP_FILENAME));
		}

		workbook = new XSSFWorkbook(new FileInputStream(fileName));
		sheetIterator = workbook.sheetIterator();

		logger.log(OpLevel.DEBUG, StreamsResources.getString(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME,
				"AbstractExcelStream.stream.initialized"), getName());
	}

	@Override
	protected void cleanup() {
		Utils.close(workbook);

		super.cleanup();
	}

	/**
	 * Returns {@link Workbook} next {@link Sheet} which name matches
	 * configuration defined (property 'SheetsToProcess') sheets name filtering
	 * mask. If no more sheets matching name filter mask is available in
	 * workbook, then {@code null} is returned.
	 * 
	 * @return next workbook sheet matching name filter mask, or {@code null} if
	 *         no more sheets matching name mask available in this workbook.
	 */
	protected Sheet getNextNameMatchingSheet() {
		if (sheetIterator == null || !sheetIterator.hasNext()) {
			logger.log(OpLevel.DEBUG, StreamsResources.getString(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME,
					"AbstractExcelStream.no.more.sheets"));

			return null;
		}

		Sheet sheet = sheetIterator.next();
		boolean match = sheetNameMatcher == null || sheetNameMatcher.matcher(sheet.getSheetName()).matches();

		if (!match) {
			return getNextNameMatchingSheet();
		}

		logger.log(OpLevel.DEBUG, StreamsResources.getString(MsOfficeStreamConstants.RESOURCE_BUNDLE_NAME,
				"AbstractExcelStream.sheet.to.process"), sheet.getSheetName());

		return sheet;
	}

}