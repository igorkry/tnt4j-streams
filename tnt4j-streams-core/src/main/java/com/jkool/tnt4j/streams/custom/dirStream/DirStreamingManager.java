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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;

import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.jkool.tnt4j.streams.utils.Utils;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * @author akausinis
 * @version 1.0
 *
 *          TODO
 */
public class DirStreamingManager {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(DirStreamingManager.class);

	private static final int CORE_TREAD_POOL_SIZE = 10;
	private static final int MAX_TREAD_POOL_SIZE = 100;
	private static final long KEEP_ALIVE_TIME = 2 * 60L;
	private static final long DEFAULT_EXECUTOR_REJECTED_TASK_TIMEOUT = 5 * 60L;
	private static final long DEFAULT_EXECUTORS_TERMINATION_TIMEOUT = 20;

	private long offerTimeout = DEFAULT_EXECUTOR_REJECTED_TASK_TIMEOUT;
	private long executorsTerminationTimeout = DEFAULT_EXECUTORS_TERMINATION_TIMEOUT;

	private String dirPath;
	private String fileWildcardName;

	private ThreadPoolExecutor executorService;
	private DirWatchdog dirWatchdog;

	private String tnt4jCfgFilePath;

	private List<StreamingJobListener> streamingJobsListeners;

	protected DirStreamingManager() {
	}

	public DirStreamingManager(String dirPath) {
		this(dirPath, null);
	}

	public DirStreamingManager(String dirPath, String fileWildcardName) {
		this.dirPath = dirPath;
		this.fileWildcardName = fileWildcardName;

		initialize();
	}

	private void initialize() {
		executorService = new ThreadPoolExecutor(CORE_TREAD_POOL_SIZE, MAX_TREAD_POOL_SIZE, KEEP_ALIVE_TIME,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(MAX_TREAD_POOL_SIZE * 2),
				new TNTInputStream.StreamsThreadFactory("DirStreamingManagerExecutorThread-")); // NON-NLS

		executorService.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				try {
					boolean added = executor.getQueue().offer(r, offerTimeout, TimeUnit.SECONDS);
					if (!added) {
						LOGGER.log(OpLevel.WARNING,
								StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_CORE,
										"TNTInputStream.tasks.buffer.limit", offerTimeout)); // TODO
						notifyStreamingJobRejected(r);
					}
				} catch (InterruptedException exc) {
					LOGGER.log(OpLevel.WARNING, "Streaming job adding to executor queue was interrupted", exc);
				}
			}
		});

		dirWatchdog = new DirWatchdog(dirPath, DirWatchdog.getDefaultFilter(fileWildcardName));
		dirWatchdog.addObserverListener(new FileAlterationListenerAdaptor() {
			@Override
			public void onFileCreate(File file) {
				handleJobConfigCreate(file);
			}

			@Override
			public void onFileChange(File file) {
				handleJobConfigChange(file);
			}

			@Override
			public void onFileDelete(File file) {
				handleJobConfigRemoval(file);
			}
		});

		LOGGER.log(OpLevel.DEBUG, "Directory ''{0}'' monitoring for files ''{1}'' has started...", dirPath,
				fileWildcardName);
	}

	public void start() {
		try {
			dirWatchdog.start();
		} catch (Exception exc) {
			LOGGER.log(OpLevel.ERROR, "Could not start directory watchdog", exc);
			cleanup();
		}
	}

	public void stop() {
		cleanup();
	}

	private synchronized void shutdownExecutors() {
		if (executorService == null || executorService.isShutdown()) {
			return;
		}

		executorService.shutdown();
		try {
			executorService.awaitTermination(executorsTerminationTimeout, TimeUnit.SECONDS);
		} catch (InterruptedException exc) {
		} finally {
			List<Runnable> droppedJobs = executorService.shutdownNow();

			if (CollectionUtils.isNotEmpty(droppedJobs)) {
				for (Runnable job : droppedJobs) {
					notifyStreamingJobDropOff(job);
				}
			}
		}
	}

	protected void cleanup() {
		try {
			dirWatchdog.stop();
		} catch (Exception exc) {
			LOGGER.log(OpLevel.WARNING, "Could not stop directory watchdog correctly", exc);
		}

		shutdownExecutors();
	}

	protected void handleJobConfigCreate(File jobCfgFile) {
		LOGGER.log(OpLevel.DEBUG, "Job config created {0}", jobCfgFile);

		UUID jobId = Utils.findUUID(jobCfgFile.getName());

		if (jobId == null) {
			return;
		}

		DefaultStreamingJob sJob = new DefaultStreamingJob(jobId, jobCfgFile);
		sJob.setTnt4jCfgFilePath(tnt4jCfgFilePath);

		if (CollectionUtils.isNotEmpty(streamingJobsListeners)) {
			for (StreamingJobListener sjl : streamingJobsListeners) {
				sJob.addStreamingJobListener(sjl);
			}
		}

		executorService.submit(sJob);
	}

	protected void handleJobConfigChange(File jobCfgFile) {
		LOGGER.log(OpLevel.DEBUG, "Job config changed {0}", jobCfgFile);
		// TODO: restart, add new job?

		UUID jobId = Utils.findUUID(jobCfgFile.getName());

		if (jobId == null) {
			return;
		}

		synchronized (executorService) {
			if (!executorService.getQueue().contains(jobId)) {
				handleJobConfigCreate(jobCfgFile);
			}
		}
	}

	protected void handleJobConfigRemoval(File jobCfgFile) {
		LOGGER.log(OpLevel.DEBUG, "Job config deleted {0}", jobCfgFile);

		UUID jobId = Utils.findUUID(jobCfgFile.getName());

		if (jobId == null) {
			return;
		}

		synchronized (executorService) {
			for (Runnable r : executorService.getQueue()) {
				DefaultStreamingJob sJob = (DefaultStreamingJob) r;

				if (sJob.equals(jobId)) {
					executorService.remove(sJob);
					break;
				}
			}
		}
	}

	// public void cancelJob(String jobId) { //TODO
	// if (jobId == null) {
	// return;
	// }
	//
	// synchronized (executorService) {
	// for (Runnable r : executorService.getQueue()) {
	// DefaultStreamingJob sJob = (DefaultStreamingJob) r;
	//
	// if (sJob.equals(jobId)) {
	// executorService.remove(sJob);
	// break;
	// }
	// }
	// }
	// }

	public String getTnt4jCfgFilePath() {
		return tnt4jCfgFilePath;
	}

	public void setTnt4jCfgFilePath(String tnt4jCfgFilePath) {
		this.tnt4jCfgFilePath = tnt4jCfgFilePath;
	}

	/**
	 * Adds defined {@code StreamingJobListener} to streaming jobs listeners
	 * list.
	 *
	 * @param l
	 *            the {@code StreamingJobListener} to be added
	 */
	public void addStreamingJobListener(StreamingJobListener l) {
		if (l == null) {
			return;
		}

		if (streamingJobsListeners == null) {
			streamingJobsListeners = new ArrayList<StreamingJobListener>();
		}

		streamingJobsListeners.add(l);
	}

	/**
	 * Removes defined {@code StreamingJobListener} from streaming jobs
	 * listeners list.
	 *
	 * @param l
	 *            the {@code StreamingJobListener} to be removed
	 */
	public void removeStreamingJobListener(StreamingJobListener l) {
		if (l != null && streamingJobsListeners != null) {
			streamingJobsListeners.remove(l);
		}
	}

	/**
	 * Notifies that stream executor service has rejected offered activity items
	 * streaming task to queue. TODO
	 *
	 * @param job
	 *            executor rejected streaming job
	 */
	protected void notifyStreamingJobRejected(Runnable job) {
		if (streamingJobsListeners != null) {
			for (StreamingJobListener l : streamingJobsListeners) {
				l.onStatusChange((StreamingJob) job, StreamingJobStatus.REJECT);
			}
		}
	}

	/**
	 * Notifies that stream executor service has been shot down and some of
	 * unprocessed activity items streaming tasks has been dropped of the queue.
	 * TODO
	 *
	 * @param jobs
	 *            executor dropped of streaming job
	 */
	protected void notifyStreamingJobDropOff(Runnable job) {
		if (streamingJobsListeners != null) {
			for (StreamingJobListener l : streamingJobsListeners) {
				l.onStatusChange((StreamingJob) job, StreamingJobStatus.DROP_OFF);
			}
		}
	}

	// /**
	// * Sample how to run directory streaming as standalone application.
	// *
	// * @param args
	// * command-line arguments. Supported arguments: dirPath
	// * fileWildcardName tnt4jCfgFilePath
	// *
	// * @throws Exception
	// * if an error occurs while running application
	// */
	// public static void main(String... args) throws Exception {
	// final DirStreamingManager dm = new DirStreamingManager(args[0], args[1]);
	// dm.setTnt4jCfgFilePath(args[2]);
	//
	// Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
	// @Override
	// public void run() {
	// System.out.println("JVM exiting!...");
	// synchronized (dm) {
	// dm.notify();
	// }
	// dm.stop();
	// }
	// }));
	//
	// dm.start();
	// synchronized (dm) {
	// dm.wait();
	// }
	// }
}
