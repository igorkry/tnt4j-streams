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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/**
 * @author akausinis
 * @version 1.0
 */
public class TestFileList extends ArrayList<File> {

	public static final int TEST_FILE_LIST_SIZE = 5;
	private static final long serialVersionUID = 1L;

	public TestFileList() throws IOException, InterruptedException {
		super();
		Long date = null;
		final int count = TEST_FILE_LIST_SIZE;
		for (int i = 0; i <= count; i++) {
			File tempFile = File.createTempFile("TEST", "TST");
			if (count / 2 >= i)
				date = (new Date()).getTime();
			this.add(tempFile);
			Thread.sleep(1);
		}

	}

	public void cleanup() {
		final Iterator<File> iterator = this.iterator();
		while (iterator.hasNext()) {
			iterator.next().deleteOnExit();
		}
	}
}
