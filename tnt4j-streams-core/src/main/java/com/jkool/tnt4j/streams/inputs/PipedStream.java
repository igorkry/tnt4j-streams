package com.jkool.tnt4j.streams.inputs;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * <p>
 * Implements piped activity stream, where each piped RAW data line is assumed
 * to represent a single activity or event which should be recorded. This class
 * wraps the RAW {@code InputStream} or {@code Reader} with a
 * {@code BufferedReader}. Default RAW input stream is {@code System.in}.
 * <p>
 * This activity stream requires parsers that can support {@code String} data.
 * <p>
 *
 * @version $Revision: 1 $
 *
 * @see com.jkool.tnt4j.streams.parsers.ActivityParser#isDataClassSupported(Object)
 */
public class PipedStream extends TNTInputStream<String> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(PipedStream.class);

	/**
	 * Reader from which activity data is read
	 */
	protected Reader rawReader = null;

	/**
	 * BufferedReader that wraps {@link #rawReader}
	 */
	protected BufferedReader dataReader = null;

	/**
	 * Construct empty PipedStream. Default input stream is {@code System.in}.
	 *
	 * @param logger
	 *            logger used by activity stream
	 */
	protected PipedStream(EventSink logger) {
		super(logger);
		setReader(new InputStreamReader(System.in));
	}

	/**
	 * Construct empty PipedStream. Default input stream is {@code System.in}.
	 */
	public PipedStream() {
		this(System.in);
	}

	/**
	 * Constructs a new PipedStream to obtain activity data from the specified
	 * {@code InputStream}.
	 *
	 * @param stream
	 *            input stream to read data from
	 */
	public PipedStream(InputStream stream) {
		this(new InputStreamReader(stream));
	}

	/**
	 * Constructs a new PipedStream to obtain activity data from the specified
	 * {@code Reader}.
	 *
	 * @param reader
	 *            reader to read data from
	 */
	public PipedStream(Reader reader) {
		super(LOGGER);
		setReader(reader);
	}

	/**
	 * Sets reader from which activity data should be read.
	 *
	 * @param reader
	 *            reader to read data from
	 */
	public void setReader(Reader reader) {
		rawReader = reader;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize() throws Exception {
		super.initialize();

		if (rawReader == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"CharacterStream.no.stream.reader"));
		}

		dataReader = new BufferedReader(rawReader);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method returns a string containing the contents of the next line in
	 * the piped RAW input.
	 */
	@Override
	public String getNextItem() throws Exception {
		if (dataReader == null) {
			throw new IllegalStateException(StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_CORE,
					"PipedStream.raw.stream.not.opened"));
		}

		String line = dataReader.readLine();

		return line;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void cleanup() {
		// NOTE: we just pipe input and did not open System.in or other RAW
		// input source enforcing the rule that who opens it, closes it - leave
		// the closing to opener.

		// Utils.close(rawReader);
		// Utils.close(dataReader);

		rawReader = null;
		dataReader = null;

		super.cleanup();
	}
}
