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

package com.jkoolcloud.tnt4j.streams.custom.dirStream;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.configure.OutputProperties;
import com.jkoolcloud.tnt4j.streams.configure.StreamsConfigLoader;
import com.jkoolcloud.tnt4j.streams.inputs.InputStreamListener;
import com.jkoolcloud.tnt4j.streams.inputs.StreamStatus;
import com.jkoolcloud.tnt4j.streams.inputs.StreamThread;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;

/**
 * This class implements a default directory files streaming job. In general it defines stream configuration attributes
 * and initiates new stream thread when job gets invoked by executor service.
 *
 * @version $Revision: 1 $
 */
public class DefaultStreamingJob implements StreamingJob {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(DefaultStreamingJob.class);

	private File streamCfgFile;
	private UUID jobId;

	private String tnt4jCfgFilePath;

	private List<StreamingJobListener> jobListeners;

	/**
	 * Constructs a new DefaultStreamingJob.
	 *
	 * @param jobId
	 *            unique job identifier
	 * @param streamCfgFile
	 *            stream configuration file
	 */
	public DefaultStreamingJob(UUID jobId, File streamCfgFile) {
		this.jobId = jobId;
		this.streamCfgFile = streamCfgFile;
	}

	@Override
	public UUID getJobId() {
		return jobId;
	}

	/**
	 * Sets path string of TNT4J configuration file.
	 *
	 * @param tnt4jCfgFilePath
	 *            path of TNT4J configuration file
	 */
	public void setTnt4jCfgFilePath(String tnt4jCfgFilePath) {
		this.tnt4jCfgFilePath = tnt4jCfgFilePath;
	}

	/**
	 * Initializes and starts configuration defined {@link TNTInputStream}s when job gets invoked by executor service.
	 */
	@Override
	public void run() {
		// StreamsAgent.runFromAPI(streamCfgFile);

		// TODO: configuration from ZooKeeper

		try {
			StreamsConfigLoader cfg = new StreamsConfigLoader(streamCfgFile);
			Collection<TNTInputStream<?, ?>> streams = cfg.getStreams();

			if (CollectionUtils.isEmpty(streams)) {
				throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"StreamsAgent.no.activity.streams"));
			}

			ThreadGroup streamThreads = new ThreadGroup(DefaultStreamingJob.class.getName() + "Threads"); // NON-NLS
			StreamThread ft;

			DefaultStreamListener dsl = new DefaultStreamListener();

			for (TNTInputStream<?, ?> stream : streams) {
				stream.addStreamListener(dsl);

				stream.output().setProperty(OutputProperties.PROP_TNT4J_CONFIG_FILE, tnt4jCfgFilePath);

				ft = new StreamThread(streamThreads, stream,
						String.format("%s:%s", stream.getClass().getSimpleName(), stream.getName())); // NON-NLS
				ft.start();
			}
		} catch (Exception e) {
			LOGGER.log(OpLevel.ERROR, String.valueOf(e.getLocalizedMessage()), e);
		}
	}

	/**
	 * Returns text string representing streaming job.
	 *
	 * @return job string representation
	 */
	@Override
	public String toString() {
		return "DefaultStreamingJob{" + "jobId=" + jobId + '}'; // NON-NLS
	}

	/**
	 * Indicates whether some other streaming job is "equal to" this one.
	 *
	 * @param otherJob
	 *            the reference job object to compare
	 *
	 * @return {@code true} if this job is the same as the otherJob argument, {@code false} - otherwise
	 */
	@Override
	public boolean equals(Object otherJob) {
		if (this == otherJob) {
			return true;
		}
		if (otherJob == null) {
			return false;
		}

		if (otherJob instanceof String) {
			return jobId.toString().equals(otherJob.toString());
		} else if (otherJob instanceof UUID) {
			return jobId.equals(otherJob);
		} else if (otherJob instanceof DefaultStreamingJob) {
			return jobId.equals(((DefaultStreamingJob) otherJob).jobId);
		}

		return super.equals(otherJob);
	}

	/**
	 * Returns a hash code value for the streaming job.
	 *
	 * @return job hash code value
	 */
	@Override
	public int hashCode() {
		return jobId.hashCode();
	}

	private void cleanup() {
		if (jobListeners != null) {
			jobListeners.clear();
		}
	}

	/**
	 * Adds defined {@code StreamingJobListener} to streaming jobs listeners list.
	 *
	 * @param l
	 *            the {@code StreamingJobListener} to be added
	 */
	public void addStreamingJobListener(StreamingJobListener l) {
		if (l == null) {
			return;
		}

		if (jobListeners == null) {
			jobListeners = new ArrayList<>();
		}

		jobListeners.add(l);
	}

	/**
	 * Removes defined {@code StreamingJobListener} from streaming jobs listeners list.
	 *
	 * @param l
	 *            the {@code StreamingJobListener} to be removed
	 */
	public void removeStreamingJobListener(StreamingJobListener l) {
		if (l != null && jobListeners != null) {
			jobListeners.remove(l);
		}
	}

	private class DefaultStreamListener implements InputStreamListener {

		@Override
		public void onProgressUpdate(TNTInputStream<?, ?> stream, int current, int total) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onProgressUpdate(DefaultStreamingJob.this, current, total);
				}
			}
		}

		@Override
		public void onSuccess(TNTInputStream<?, ?> stream) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onSuccess(DefaultStreamingJob.this);
				}
			}
		}

		@Override
		public void onFailure(TNTInputStream<?, ?> stream, String msg, Throwable exc, String code) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onFailure(DefaultStreamingJob.this, msg, exc, code);
				}
			}
		}

		@Override
		public void onStatusChange(TNTInputStream<?, ?> stream, StreamStatus status) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onStatusChange(DefaultStreamingJob.this, status);
				}
			}
		}

		@Override
		public void onFinish(TNTInputStream<?, ?> stream, TNTInputStream.StreamStats stats) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onFinish(DefaultStreamingJob.this, stats);
				}
			}

			cleanup();
		}

		@Override
		public void onStreamEvent(TNTInputStream<?, ?> stream, OpLevel level, String message, Object source) {
			if (jobListeners != null) {
				for (StreamingJobListener l : jobListeners) {
					l.onStreamEvent(DefaultStreamingJob.this, level, message, source);
				}
			}
		}
	}
}
