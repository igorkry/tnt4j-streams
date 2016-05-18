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

package com.jkool.tnt4j.streams.configure.state;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.LineNumberReader;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Test;

import com.jkool.tnt4j.streams.utils.Utils;

/**
 * @author akausinis
 * @version 1.0
 */
public class FileStreamStateHandlerTest {

	@Test
	public void findStreamingFile() throws Exception {
		FileStreamStateHandler rwd = new FileStreamStateHandler();

		File testFilesDir = new File("./samples/multiple-logs/");
		File[] testFiles = testFilesDir.listFiles((FilenameFilter) new WildcardFileFilter("orders*"));
		FileAccessState newFAS = new FileAccessState();
		FileAccessState fas = rwd.loadState(testFilesDir.getAbsolutePath(), null);
		int count = 0;
		File fileToSearchFor = null;
		int lineLastRead = 0;
		for (File testFile : testFiles) {
			count++;
			FileReader in;
			LineNumberReader reader;

			Long fileCRC = rwd.getFileCrc(testFile);

			if (fileCRC.equals(fas.currentFileCrc)) {
				System.out.println("File found " + testFile.getName());
			}

			if (count == 2) {
				newFAS.currentFileCrc = fileCRC;
				fileToSearchFor = testFile;
			}

			in = new FileReader(testFile);
			reader = new LineNumberReader(in);
			reader.setLineNumber(0);
			String line = reader.readLine();
			int count2 = 0;
			while (line != null) {
				count2++;
				Checksum crcLine = new CRC32();
				final byte[] bytes4Line = line.getBytes();
				crcLine.update(bytes4Line, 0, bytes4Line.length);
				final long lineCRC = crcLine.getValue();
				final int lineNumber = reader.getLineNumber();
				System.out.println("for " + lineNumber + " line CRC is " + lineCRC);
				if (count2 == 3) {
					newFAS.lineNumberReadCrc = lineCRC;
					newFAS.lineNumberRead = lineNumber;
					lineLastRead = lineNumber;
				}
				if (lineCRC == fas.lineNumberReadCrc) {
					System.out.println("Found line: " + lineNumber);
				}
				line = reader.readLine();
			}
			AbstractFileStreamStateHandler.writeState(newFAS, testFilesDir, null);
			Utils.close(reader);

		}
		final File findLastProcessed = rwd.findStreamingFile(newFAS, testFiles);
		assertEquals(fileToSearchFor, findLastProcessed);
		final int lineLastReadRecorded = rwd.checkLine(findLastProcessed, newFAS);
		assertEquals(lineLastRead, lineLastReadRecorded);
	}

}
