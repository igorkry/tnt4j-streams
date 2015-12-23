package com.jkool.tnt4j.streams.parsers;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.entity.BufferedHttpEntity;

import com.jkool.tnt4j.streams.configure.StreamsConfig;
import com.jkool.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkool.tnt4j.streams.fields.ActivityInfo;
import com.jkool.tnt4j.streams.inputs.TNTInputStream;
import com.jkool.tnt4j.streams.utils.StreamsResources;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.sink.DefaultEventSinkFactory;
import com.nastel.jkool.tnt4j.sink.EventSink;

/**
 * The type Activity http request parser.
 */
public class ActivityHTTPRequestParser extends GenericActivityParser<BufferedHttpEntity> {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityHTTPRequestParser.class);

	private boolean readLines = true;

	private BufferedReader lineBuffer;

	/**
	 * Constructs a new ActivityHTTPRequestParser.
	 */
	public ActivityHTTPRequestParser() {
		super(LOGGER);
	}

	@Override
	public void setProperties(Collection<Entry<String, String>> props) throws Throwable {
		if (props == null) {
			return;
		}

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();
			if (StreamsConfig.PROP_READ_LINES.equals(name)) {
				readLines = Boolean.parseBoolean(value);
				LOGGER.log(OpLevel.DEBUG,
						StreamsResources.getStringFormatted("ActivityParser.setting", name, readLines));
			}
		}
	}

	@Override
	public boolean isDataClassSupported(Object data) {
		return HttpEntity.class.isInstance(data);
	}

	@Override
	public ActivityInfo parse(TNTInputStream stream, Object data) throws IllegalStateException, ParseException {
		if (data == null) {
			return null;
		}
		logger.log(OpLevel.DEBUG, StreamsResources.getStringFormatted("ActivityParser.parsing", data));

		BufferedHttpEntity entity = null;
		try {
			entity = new BufferedHttpEntity((HttpEntity) data);
		} catch (IOException exc) {
			ParseException pe = new ParseException("TODO", 0);
			pe.initCause(exc);

			throw pe;
		}

		return parsePreparedItem(stream, null, entity);
	}

	/**
	 * Gets field value from raw data location and formats it according locator
	 * definition.
	 *
	 * @param stream
	 *            parent stream
	 * @param locator
	 *            activity field locator
	 * @param entity
	 *            activity object HTTP entity
	 *
	 * @return value formatted based on locator definition or {@code null} if
	 *         locator is not defined
	 *
	 * @throws ParseException
	 *             if error applying locator format properties to specified
	 *             value
	 *
	 * @see ActivityFieldLocator#formatValue(Object)
	 */
	protected Object getLocatorValue(TNTInputStream stream, ActivityFieldLocator locator, BufferedHttpEntity entity)
			throws ParseException {
		return null; // TODO
	}
}
