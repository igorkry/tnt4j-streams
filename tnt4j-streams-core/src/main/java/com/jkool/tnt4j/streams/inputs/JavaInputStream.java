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

package com.jkool.tnt4j.streams.inputs;

import java.io.*;

import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements a Java {@link InputStream} or {@link Reader} carried activity
 * stream, where each line of input is assumed to represent a single activity or
 * event which should be recorded. {@link InputStream} or {@link Reader} is
 * defined over configuration element "reference".
 * <p>
 * This activity stream requires parsers that can support {@link String} data.
 *
 * @version $Revision: 1 $
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class JavaInputStream extends TNTInputStream<String> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(JavaInputStream.class);

	private LineNumberReader inputReader;

	private int lineNumber = 0;

	/**
	 * Constructs a new JavaInputStream.
	 */
	public JavaInputStream() {
		super(LOGGER);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Adds reference to {@link InputStream} to read lines.
	 */
	@Override
	public void addReference(Object refObject) throws IllegalStateException {
		if (refObject instanceof InputStream) {
			InputStream is = (InputStream) refObject;
			inputReader = new LineNumberReader(new BufferedReader(new InputStreamReader(is)));
		} else if (refObject instanceof Reader) {
			Reader reader = (Reader) refObject;
			inputReader = new LineNumberReader(new BufferedReader(reader));
		}

		super.addReference(refObject);
	}

	@Override
	protected void initialize() throws Exception {
		super.initialize();

		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "JavaInputStream.stream.ready"));
	}

	@Override
	public String getNextItem() throws Exception {
		if (inputReader == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"JavaInputStream.input.not.opened"));
		}

		String line = Utils.getNonEmptyLine(inputReader);
		lineNumber = inputReader.getLineNumber();

		if (line != null) {
			addStreamedBytesCount(line.getBytes().length);
		}

		return line;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns line number of the input stream last read.
	 */
	@Override
	public int getActivityPosition() {
		return lineNumber;
	}

	@Override
	protected void cleanup() {
		Utils.close(inputReader);
		inputReader = null;

		super.cleanup();
	}
}
