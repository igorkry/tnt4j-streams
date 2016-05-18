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

import javax.xml.bind.annotation.*;

/**
 * Persistable configuration of currently streaming file and line for the
 * purpose of continuation of stream after progress failed, uses file CRC to
 * point the file, instead of commonly used name, because the rolling file log
 * streams file name is changing. TODO
 *
 * @version $Revision: 1 $
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "tnt-file-access-state")
@XmlType(name = "FileAccessState")
public class FileAccessState {
	/**
	 * CRC value of file header last read.
	 */
	@XmlElement(required = true)
	public Long currentFileCrc;
	/**
	 * Line number of file last read.
	 */
	@XmlElement(required = true)
	public Integer lineNumberRead;
	/**
	 * CRC value of line last read.
	 */
	@XmlElement(required = true)
	public Long lineNumberReadCrc;

	/**
	 * Constructs a new ActivityJsonParser.
	 */
	public FileAccessState() {
	}
}