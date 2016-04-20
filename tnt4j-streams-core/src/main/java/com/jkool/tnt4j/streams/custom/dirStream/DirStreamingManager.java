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
 * This class implements directory files streaming manager. Manager monitors
 * directory (and subdirectories) for stream configuration files (i.e.
 * tnt-data-source*.xml) and invokes streaming actions on stream configuration
 * files changes. Stream configuration file name must also contain job
 * identifier (@link UUID) set by job producer (i.e.
 * tnt-data-source_123e4567-e89b-12d3-a456-426655440000.xml) to make
 * events/actions correlations between all participating pats: job producer,
 * TNT4J-Streams API and JKoolCloud. Without identifier, stream job
 * configurations won't be processed.
 * <p>
 * Because streaming actions are invoked on stream configuration file changes,
 * it is recommended to upload (make to be available for steaming) actual
 * activities RAW data files before stream configuration file gets available in
 * monitored directory.
 * <p>
 * Streaming jobs are processed using {@link ThreadPoolExecutor} with 10 core
 * threads and max 100 threads by default.
 * <p>
 * Directory monitoring is performed using {@link DirWatchdog}. Manager handles
 * those watchdog invoked file notifications:
 * <ul>
 * <li>Create - creates new streaming job and enqueues it to executor service.
 * </li>
 * <li>Change - creates new streaming job if such job is not available in
 * executor queue.</li>
 * <li>Delete - removes streaming job from executor queue.</li>
 * </ul>
 *
 * <p>
 * Sample:
 * 
 * <pre>
 * String dirPath = "./../temp/";
 * String fwn = "tnt-data-source*.xml";
 * 
 * final DirStreamingManager dm = new DirStreamingManager(dirPath, fwn);
 * dm.setTnt4jCfgFilePath("./../config/tnt4j.properties");
 * dm.addStreamingJobListener(new StreamingJobLogger());
 * 
 * Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
 * 	&#64;Override
 * 	public void run() {
 * 		System.out.println("JVM exiting!...");
 * 		synchronized (dm) {
 * 			dm.notify();
 * 		}
 * 		dm.stop();
 * 	}
 * }));
 * 
 * dm.start();
 * synchronized (dm) {
 * 	dm.wait();
 * }
 * </pre>
 *
 * @version $Revision: 1 $
 *
 * @see ThreadPoolExecutor
 * @see DirWatchdog
 * @see DefaultStreamingJob
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

	/**
	 * Constructs an empty DirStreamingManager.
	 */
	protected DirStreamingManager() {
	}

	/**
	 * Constructs a new DirStreamingManager. Defines watched directory path.
	 *
	 * @param dirPath
	 *            watched directory path
	 */
	public DirStreamingManager(String dirPath) {
		this(dirPath, null);
	}

	/**
	 * Constructs a new DirStreamingManager. Defines watched directory path and
	 * monitored files wildcard name pattern.
	 *
	 * @param dirPath
	 *            watched directory path
	 * @param fileWildcardName
	 *            monitored files wildcard name pattern
	 */
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
						LOGGER.log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
								"TNTInputStream.tasks.buffer.limit"), offerTimeout);
						notifyStreamingJobRejected(r);
					}
				} catch (InterruptedException exc) {
					LOGGER.log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
							"DirStreamingManager.job.offer.interrupted"), ((StreamingJob) r).getJobId(), exc);
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

		LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
				"DirStreamingManager.dir.monitoring.started"), dirPath, fileWildcardName);
	}

	/**
	 * Starts directory files streaming manager.
	 */
	public void start() {
		try {
			dirWatchdog.start();
		} catch (Exception exc) {
			LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"DirStreamingManager.could.not.start.watchdog"), exc);
			stop();
		}
	}

	/**
	 * Stops directory streaming manager.
	 */
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

	/**
	 * Cleans up directory files streaming manager referenced objects after
	 * manager has stopped. Stops directory watchdog and shuts down executors.
	 */
	protected void cleanup() {
		try {
			dirWatchdog.stop();
		} catch (Exception exc) {
			LOGGER.log(OpLevel.WARNING, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"DirStreamingManager.could.not.stop.watchdog"), exc);
		}

		shutdownExecutors();
	}

	/**
	 * Handles streaming job configuration file creation notification.
	 *
	 * @param jobCfgFile
	 *            streaming job configuration file
	 */
	protected void handleJobConfigCreate(File jobCfgFile) {
		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "DirStreamingManager.job.created"),
				jobCfgFile);

		UUID jobId = Utils.findUUID(jobCfgFile.getName());

		if (jobId == null) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"DirStreamingManager.job.id.not.found"), jobCfgFile.getName());
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

	/**
	 * Handles streaming job configuration file change notification.
	 *
	 * @param jobCfgFile
	 *            streaming job configuration file
	 */
	protected void handleJobConfigChange(File jobCfgFile) {
		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "DirStreamingManager.job.changed"),
				jobCfgFile);

		UUID jobId = Utils.findUUID(jobCfgFile.getName());

		if (jobId == null) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"DirStreamingManager.job.id.not.found"), jobCfgFile.getName());
			return;
		}

		synchronized (executorService) {
			// TODO: check job state - if already processing halt and restart???
			if (!executorService.getQueue().contains(jobId)) {
				handleJobConfigCreate(jobCfgFile);
			}
		}
	}

	/**
	 * Handles streaming job configuration file removal notification.
	 *
	 * @param jobCfgFile
	 *            streaming job configuration file
	 */
	protected void handleJobConfigRemoval(File jobCfgFile) {
		LOGGER.log(OpLevel.DEBUG,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE, "DirStreamingManager.job.deleted"),
				jobCfgFile);

		UUID jobId = Utils.findUUID(jobCfgFile.getName());

		if (jobId == null) {
			LOGGER.log(OpLevel.DEBUG, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"DirStreamingManager.job.id.not.found"), jobCfgFile.getName());
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

	/**
	 * Sets TNT4J configuration file (tnt4j.properties) path.
	 *
	 * @param tnt4jCfgFilePath
	 *            TNT4J configuration file path string
	 */
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
	 * Notifies that stream executor service has rejected offered files
	 * streaming job to queue.
	 *
	 * @param job
	 *            executor rejected files streaming job
	 */
	protected void notifyStreamingJobRejected(Runnable job) {
		if (streamingJobsListeners != null) {
			for (StreamingJobListener l : streamingJobsListeners) {
				l.onStatusChange((StreamingJob) job, StreamingJobStatus.REJECT);
			}
		}
	}

	/**
	 * Notifies that stream executor service has been shot down and unprocessed
	 * files streaming job has been dropped off the queue.
	 *
	 * @param job
	 *            executor dropped off files streaming job
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
