package com.jkoolcloud.tnt4j.streams.outputs;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;

public abstract class AbstractTNTStreamOutput<T> implements TNTStreamOutput<T> {

	private String name;
	private TNTInputStream<?, ?> stream;

	private boolean closed = false;

	protected AbstractTNTStreamOutput() {
	}

	protected AbstractTNTStreamOutput(String name) {
		this.name = name;
	}

	/**
	 * Returns logger used by this stream output handler.
	 *
	 * @return parser logger
	 */
	protected abstract EventSink logger();

	@Override
	public void setStream(TNTInputStream<?, ?> inputStream) {
		this.stream = inputStream;
	}

	@Override
	public TNTInputStream<?, ?> getStream() {
		return stream;
	}

	/**
	 * Returns output name value.
	 *
	 * @return output name value
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets output name value.
	 *
	 * @param name
	 *            output name value
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) {
		if (CollectionUtils.isNotEmpty(props)) {
			for (Map.Entry<String, String> prop : props) {
				setProperty(prop.getKey(), prop.getValue());
			}
		}
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	protected void setClosed(boolean closed) {
		this.closed = closed;
	}

	@Override
	public void cleanup() {
		closed = true;

	}

	/**
	 * Returns empty statistics map.
	 *
	 * @return empty map
	 */
	@Override
	public Map<String, Object> getStats() {
		return Collections.emptyMap();
	}
}
