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

package com.jkool.tnt4j.streams.custom.dirStream;

import java.io.File;
import java.io.FileFilter;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang.StringUtils;

/**
 * @author akausinis
 * @version 1.0 TODO
 */
public class DirWatchdog {

	private static final long DEFAULT_WATCH_REFRESH_INTERVAL = 5 * 1000L;

	private String dirPath;
	private long interval;
	private FileFilter filter = null;

	private FileAlterationObserver observer = null;
	private FileAlterationMonitor monitor = null;

	public DirWatchdog(String dirPath) {
		this(dirPath, DEFAULT_WATCH_REFRESH_INTERVAL, null);
	}

	public DirWatchdog(String dirPath, long refreshInterval) {
		this(dirPath, refreshInterval, null);
	}

	public DirWatchdog(String dirPath, FileFilter filter) {
		this(dirPath, DEFAULT_WATCH_REFRESH_INTERVAL, filter);
	}

	public DirWatchdog(String dirPath, long refreshInterval, FileFilter filter) {
		if (StringUtils.isEmpty(dirPath)) {
			throw new IllegalArgumentException("Path of directory to watch must be non-empty");
		}

		this.dirPath = dirPath;
		this.interval = refreshInterval;
		this.filter = filter;

		initialize();
	}

	private void initialize() {
		observer = new FileAlterationObserver(new File(dirPath), filter);

		monitor = new FileAlterationMonitor(interval);
		monitor.addObserver(observer);
	}

	public void addObserverListener(FileAlterationListener l) {
		if (l != null) {
			observer.addListener(l);
		}
	}

	public void start() throws Exception {
		if (monitor != null) {
			monitor.start();
		}
	}

	public void stop() throws Exception {
		if (monitor != null) {
			monitor.stop();
		}
	}

	public static FileFilter getDefaultFilter(String fileWildcardName) {
		if (StringUtils.isEmpty(fileWildcardName)) {
			return null;
		}

		IOFileFilter directories = FileFilterUtils.and(FileFilterUtils.directoryFileFilter(), HiddenFileFilter.VISIBLE);

		IOFileFilter files = FileFilterUtils.and(FileFilterUtils.fileFileFilter(),
				new WildcardFileFilter(fileWildcardName, IOCase.INSENSITIVE));

		IOFileFilter filter = FileFilterUtils.or(directories, files);

		return filter;
	}
}
