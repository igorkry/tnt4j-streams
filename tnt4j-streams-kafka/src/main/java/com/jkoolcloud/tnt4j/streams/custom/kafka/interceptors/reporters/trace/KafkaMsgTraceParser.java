package com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.reporters.trace;

import java.text.ParseException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser;

public class KafkaMsgTraceParser extends GenericActivityParser<ActivityInfo> {

	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(KafkaMsgTraceParser.class);

	@Override
	protected Object resolveLocatorValue(ActivityFieldLocator locator, ActivityContext cData,
			AtomicBoolean formattingNeeded) throws ParseException {
		if (locator == null || cData.getData() == null) {
			return null;
		}
		return cData.getData().getFieldValue(locator.getLocator());
	}

	@Override
	protected void postParse(ActivityContext cData) throws ParseException {
		super.postParse(cData);
		cData.getActivity().merge(cData.getData());
	}

	@Override
	protected EventSink logger() {
		return LOGGER;
	}
}
