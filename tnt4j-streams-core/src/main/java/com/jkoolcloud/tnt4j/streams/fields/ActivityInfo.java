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

package com.jkoolcloud.tnt4j.streams.fields;

import java.net.InetAddress;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.jkoolcloud.tnt4j.core.*;
import com.jkoolcloud.tnt4j.format.JSONFormatter;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.source.SourceType;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.StreamsThread;
import com.jkoolcloud.tnt4j.streams.utils.TimestampFormatter;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.tracker.TimeTracker;
import com.jkoolcloud.tnt4j.tracker.Tracker;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;
import com.jkoolcloud.tnt4j.uuid.UUIDFactory;

/**
 * This class represents an {@link Trackable} entity (e.g. activity/event/snapshot) to record to jKool Cloud Service.
 *
 * @version $Revision: 2 $
 */
public class ActivityInfo {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityInfo.class);

	private static final Map<String, String> HOST_CACHE = new ConcurrentHashMap<String, String>();
	private static final String LOCAL_SERVER_NAME_KEY = "LOCAL_SERVER_NAME_KEY"; // NON-NLS
	private static final String LOCAL_SERVER_IP_KEY = "LOCAL_SERVER_IP_KEY"; // NON-NLS

	private String serverName = null;
	private String serverIp = null;
	private String applName = null;
	private String userName = null;

	private String resourceName = null;

	private String eventName = null;
	private OpType eventType = null;
	private UsecTimestamp startTime = null;
	private UsecTimestamp endTime = null;
	private long elapsedTime = -1L;
	private OpCompCode compCode = null;
	private int reasonCode = 0;
	private String exception = null;
	private OpLevel severity = null;
	private String location = null;
	private Collection<String> correlator = null;

	private String trackingId = null;
	private String parentId = null;
	private Collection<String> tag = null;
	private Object message = null;
	private String msgCharSet = null;
	private String msgEncoding = null;
	private Integer msgLength = null;
	private String msgMimeType = null;

	private Integer processId = null;
	private Integer threadId = null;

	private String category = null;

	private boolean filtered = false;

	private static final TimeTracker ACTIVITY_TIME_TRACKER = TimeTracker.newTracker(1000, TimeUnit.HOURS.toMillis(8));

	private Map<String, Property> activityProperties;
	private List<ActivityInfo> children;

	/**
	 * Constructs a new ActivityInfo object.
	 */
	public ActivityInfo() {

	}

	/**
	 * Applies the given value(s) for the specified field to the appropriate internal data field for reporting field to
	 * the jKool Cloud Service.
	 *
	 * @param field
	 *            field to apply
	 * @param value
	 *            value to apply for this field, which could be an array of objects if value for field consists of
	 *            multiple locations
	 * @throws ParseException
	 *             if an error parsing the specified value based on the field definition (e.g. does not match defined
	 *             format, etc.)
	 */
	public void applyField(ActivityField field, Object value) throws ParseException {
		LOGGER.log(OpLevel.TRACE,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.applying.field"), field,
				value);
		Object[] values = Utils.makeArray(Utils.simplifyValue(value));

		List<ActivityFieldLocator> locators = field.getLocators();
		if (values != null && CollectionUtils.isNotEmpty(locators)) {
			if (locators.size() > 1 && field.isEnumeration()) {
				throw new ParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityInfo.multiple.locators", field), 0);
			}
			if (locators.size() > 1 && locators.size() != values.length) {
				throw new ParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityInfo.failed.parsing", field), 0);
			}
			ActivityFieldLocator locator;
			for (int v = 0; v < values.length; v++) {
				locator = locators.size() == 1 ? locators.get(0) : locators.get(v);
				values[v] = formatValue(field, locator, values[v]);
			}
		}

		Object fieldValue = Utils.simplifyValue(values);

		if (fieldValue == null) {
			LOGGER.log(OpLevel.TRACE,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.field.null"),
					field);
			return;
		}
		LOGGER.log(OpLevel.TRACE,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.applying.field.value"),
				field, fieldValue);

		setFieldValue(field, fieldValue);
	}

	/**
	 * Formats the value for the field based on the required internal data type of the field and the definition of the
	 * field.
	 *
	 * @param field
	 *            field whose value is to be formatted
	 * @param locator
	 *            locator information for value
	 * @param value
	 *            raw value of field
	 * @return formatted value of field in required internal data type
	 */
	protected Object formatValue(ActivityField field, ActivityFieldLocator locator, Object value) {
		if (value == null) {
			return null;
		}
		if (field.isEnumeration()) {
			if (value instanceof String) {
				String strValue = (String) value;
				value = StringUtils.containsOnly(strValue, "0123456789") ? Integer.valueOf(strValue) // NON-NLS
						: strValue.toUpperCase().trim();
			}
		}
		StreamFieldType fieldType = field.getFieldType();
		if (fieldType != null) {
			switch (fieldType) {
			case ElapsedTime:
				try {
					// Elapsed time needs to be converted to usec
					TimeUnit units = StringUtils.isEmpty(locator.getUnits()) ? TimeUnit.MICROSECONDS
							: TimeUnit.valueOf(locator.getUnits().toUpperCase());
					if (!(value instanceof Number)) {
						value = Long.valueOf(Utils.toString(value));
					}
					value = TimestampFormatter.convert((Number) value, units, TimeUnit.MICROSECONDS);
				} catch (Exception e) {
				}
				break;
			case ResourceName:
				value = getStringValue(value, field);
				break;
			case ServerIp:
				if (value instanceof InetAddress) {
					value = ((InetAddress) value).getHostAddress();
				}
				break;
			case ServerName:
				if (value instanceof InetAddress) {
					value = ((InetAddress) value).getHostName();
				}
				break;
			default:
				break;
			}
		}
		return value;
	}

	/**
	 * Sets field to specified value, handling any necessary conversions based on internal data type for field.
	 *
	 * @param field
	 *            field whose value is to be set
	 * @param fieldValue
	 *            formatted value based on locator definition for field
	 *
	 * @throws ParseException
	 *             if there are any errors with conversion to internal format
	 */
	private void setFieldValue(ActivityField field, Object fieldValue) throws ParseException {
		StreamFieldType fieldType = field.getFieldType();
		if (fieldType != null) {
			switch (fieldType) {
			case Message:
				message = substitute(message, fieldValue, Object.class);
				break;
			case EventName:
				eventName = substitute(eventName, getStringValue(fieldValue, field));
				break;
			case EventType:
				OpType ot = Utils.mapOpType(fieldValue);
				eventType = substitute(eventType, ot, OpType.class);
				break;
			case ApplName:
				applName = substitute(applName, getStringValue(fieldValue, field));
				break;
			case Correlator:
				addCorrelator(Utils.getTags(fieldValue));
				break;
			case ElapsedTime:
				elapsedTime = substitute(elapsedTime, getLongValue(fieldValue), Long.class);
				break;
			case EndTime:
				UsecTimestamp et = fieldValue instanceof UsecTimestamp ? (UsecTimestamp) fieldValue
						: TimestampFormatter.parse(field.getFormat(), fieldValue, null, field.getLocale());
				endTime = substitute(endTime, et, UsecTimestamp.class);
				break;
			case Exception:
				exception = substitute(exception, getStringValue(fieldValue, field));
				break;
			case Location:
				location = substitute(location, getStringValue(fieldValue, field));
				break;
			case ReasonCode:
				reasonCode = substitute(reasonCode, getIntValue(fieldValue), Integer.class);
				break;
			case ResourceName:
				resourceName = substitute(resourceName, getStringValue(fieldValue, field));
				break;
			case ServerIp:
				serverIp = substitute(serverIp, getStringValue(fieldValue, field));
				break;
			case ServerName:
				serverName = substitute(serverName, getStringValue(fieldValue, field));
				break;
			case Severity:
				OpLevel sev = fieldValue instanceof Number ? OpLevel.valueOf(((Number) fieldValue).intValue())
						: OpLevel.valueOf(fieldValue);
				severity = substitute(severity, sev, OpLevel.class);
				break;
			case TrackingId:
				trackingId = substitute(trackingId, getStringValue(fieldValue, field));
				break;
			case StartTime:
				UsecTimestamp st = fieldValue instanceof UsecTimestamp ? (UsecTimestamp) fieldValue
						: TimestampFormatter.parse(field.getFormat(), fieldValue, null, field.getLocale());
				startTime = substitute(startTime, st, UsecTimestamp.class);
				break;
			case CompCode:
				OpCompCode cc = fieldValue instanceof Number ? OpCompCode.valueOf(((Number) fieldValue).intValue())
						: OpCompCode.valueOf(fieldValue);
				compCode = substitute(compCode, cc, OpCompCode.class);
				break;
			case Tag:
				addTag(Utils.getTags(fieldValue));
				break;
			case UserName:
				userName = substitute(userName, getStringValue(fieldValue, field));
				break;
			case MsgCharSet:
				msgCharSet = substitute(msgCharSet, getStringValue(fieldValue, field));
				break;
			case MsgEncoding:
				msgEncoding = substitute(msgEncoding, getStringValue(fieldValue, field));
				break;
			case MsgLength:
				msgLength = substitute(msgLength, getIntValue(fieldValue), Integer.class);
				break;
			case MsgMimeType:
				msgMimeType = substitute(msgMimeType, getStringValue(fieldValue, field));
				break;
			case ProcessId:
				processId = substitute(processId, getIntValue(fieldValue), Integer.class);
				break;
			case ThreadId:
				threadId = substitute(threadId, getIntValue(fieldValue), Integer.class);
				break;
			case Category:
				category = substitute(category, getStringValue(fieldValue, field));
				break;
			case ParentId:
				parentId = substitute(parentId, getStringValue(fieldValue, field));
				break;
			default:
				throw new IllegalArgumentException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.unrecognized.activity", field));
			}

			LOGGER.log(OpLevel.TRACE,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.set.field"), field,
					fieldValue);
		} else {
			addActivityProperty(field.getFieldTypeName(),
					fieldValue instanceof Object[] ? getStringValue(fieldValue, field) : fieldValue,
					field.getValueType());
		}
	}

	private static String substitute(String value, String newValue) {
		return StringUtils.isEmpty(newValue) ? value : newValue;
	}

	private static <T> T substitute(T value, T newValue, Class<T> clazz) {
		return newValue == null ? value : newValue;
	}

	private static String getStringValue(Object value, ActivityField field) {
		if (value instanceof Object[]) {
			Object[] vArray = (Object[]) value;
			StringBuilder sb = new StringBuilder();
			ActivityFieldLocator locator;
			for (int v = 0; v < vArray.length; v++) {
				locator = field.getLocators().size() == 1 ? field.getLocators().get(0)
						: v >= 0 && v < field.getLocators().size() ? field.getLocators().get(v) : null;
				String format = locator == null ? null : locator.getFormat();

				if (v > 0) {
					sb.append(field.getSeparator());
				}

				if (vArray[v] instanceof UsecTimestamp && StringUtils.isNotEmpty(format)) {
					sb.append(((UsecTimestamp) vArray[v]).toString(format));
				} else {
					sb.append(Utils.toString(vArray[v]));
				}
			}

			return sb.toString();
		}

		return Utils.toString(value);
	}

	/**
	 * Adds activity item property to item properties map. Properties from map are transferred as tracking event
	 * properties when {@link #recordActivity(Tracker, long)} is invoked. Same as invoking
	 * {@link #addActivityProperty(String, Object, String)} setting value type to {@code null}.
	 *
	 * @param propName
	 *            activity item property key
	 * @param propValue
	 *            activity item property value
	 * @return previous property value replaced by {@code propValue} or {@code null} if there was no such activity
	 *         property set
	 * @see Map#put(Object, Object)
	 * @see #recordActivity(Tracker, long)
	 * @see #addActivityProperty(String, Object, String)
	 */
	public Object addActivityProperty(String propName, Object propValue) {
		return addActivityProperty(propName, propValue, null);
	}

	/**
	 * Adds activity item property to item properties map. Properties from map are transferred as tracking event
	 * properties when {@link #recordActivity(Tracker, long)} is invoked.
	 *
	 * @param propName
	 *            activity item property key
	 * @param propValue
	 *            activity item property value
	 * @param valueType
	 *            activity item property value type from {@link com.jkoolcloud.tnt4j.core.ValueTypes} set
	 * @return previous property value replaced by {@code propValue} or {@code null} if there was no such activity
	 *         property set
	 * @see Map#put(Object, Object)
	 * @see #recordActivity(Tracker, long)
	 * @see com.jkoolcloud.tnt4j.core.ValueTypes
	 */
	public Object addActivityProperty(String propName, Object propValue, String valueType) {
		if (activityProperties == null) {
			activityProperties = new HashMap<String, Property>();
		}

		Property p = new Property(propName, wrapPropertyValue(propValue),
				StringUtils.isEmpty(valueType) ? ValueTypes.VALUE_TYPE_NONE : valueType);
		Object prevValue = activityProperties.put(propName, p);

		LOGGER.log(OpLevel.TRACE,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.set.property"),
				propName, Utils.toString(p.getValue()), p.getValueType(), prevValue);

		return prevValue;
	}

	private static Object wrapPropertyValue(Object propValue) {
		if (propValue instanceof UsecTimestamp) {
			return ((UsecTimestamp) propValue).getTimeUsec();
		}

		return propValue;
	}

	/**
	 * Appends activity item tags collection with provided tag strings array contents.
	 *
	 * @param tags
	 *            tag strings array
	 */
	public void addTag(String... tags) {
		this.tag = addStrings(this.tag, tags);
	}

	/**
	 * Appends activity item correlators collection with provided correlator strings array contents.
	 *
	 * @param correlators
	 *            correlator strings array
	 */
	public void addCorrelator(String... correlators) {
		this.correlator = addStrings(this.correlator, correlators);
	}

	private static Collection<String> addStrings(Collection<String> collection, String... strings) {
		if (ArrayUtils.isNotEmpty(strings)) {
			if (collection == null) {
				collection = new ArrayList<String>();
			}

			for (String str : strings) {
				collection.add(str.trim());
			}
		}

		return collection;
	}

	/**
	 * Makes fully qualified name of activity source. Name is made from stream parsed data attributes.
	 *
	 * @return fully qualified name of this activity source, or {@code null} if no source defining attributes where
	 *         parsed from stream.
	 */
	public String getSourceFQN() {
		resolveServer(false);
		StringBuilder fqnB = new StringBuilder();

		addSourceValue(fqnB, SourceType.APPL, applName);
		addSourceValue(fqnB, SourceType.SERVER, serverName);
		addSourceValue(fqnB, SourceType.NETADDR, serverIp);

		String fqn = fqnB.toString();

		return StringUtils.isEmpty(fqn) ? null : fqn;
	}

	private static void addSourceValue(StringBuilder sb, SourceType type, String value) {
		if (StringUtils.isNotEmpty(value)) {
			if (sb.length() > 0) {
				sb.append('#'); // NON-NLS
			}
			sb.append(type).append('=').append(value); // NON-NLS
		}
	}

	/**
	 * Creates the appropriate data message to send to jKool Cloud Service and records the activity using the specified
	 * tracker.
	 *
	 * @param tracker
	 *            communication gateway to use to record activity
	 * @param retryPeriod
	 *            period in milliseconds between activity resubmission in case of failure
	 * @throws Exception
	 *             indicates an error building data message or sending data to jKool Cloud Service
	 */
	public void recordActivity(Tracker tracker, long retryPeriod) throws Exception {
		if (tracker == null) {
			LOGGER.log(OpLevel.WARNING,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.tracker.null"));
			return;
		}

		resolveServer(false);
		determineTimes();

		send(tracker, retryPeriod, buildTrackable(tracker));
	}

	private Trackable buildTrackable(Tracker tracker) {
		UUIDFactory uuidFactory = tracker.getConfiguration().getUUIDFactory();
		String trackId = StringUtils.isEmpty(trackingId) ? uuidFactory.newUUID() : trackingId;

		if (eventType == OpType.ACTIVITY) {
			return buildActivity(tracker, eventName, trackId);
		} else if (eventType == OpType.SNAPSHOT) {
			return buildSnapshot(tracker, eventName, trackId);
		} else {
			return buildEvent(tracker, eventName, trackId);
		}
	}

	private void send(Tracker tracker, long retryPeriod, Trackable trackable) throws Exception {
		StreamsThread thread = null;
		if (Thread.currentThread() instanceof StreamsThread) {
			thread = (StreamsThread) Thread.currentThread();
		}
		boolean retryAttempt = false;
		do {
			try {
				if (trackable instanceof TrackingActivity) {
					tracker.tnt((TrackingActivity) trackable);
				} else if (trackable instanceof Snapshot) {
					tracker.tnt((Snapshot) trackable);
				} else {
					tracker.tnt((TrackingEvent) trackable);
				}

				if (retryAttempt) {
					LOGGER.log(OpLevel.INFO, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ActivityInfo.retry.successful"));
				}
				return;
			} catch (Exception ioe) {
				LOGGER.log(OpLevel.ERROR, StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityInfo.recording.failed"), ioe);
				Utils.close(tracker);
				if (thread == null) {
					throw ioe;
				}
				retryAttempt = true;
				LOGGER.log(OpLevel.INFO,
						StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.will.retry"),
						TimeUnit.MILLISECONDS.toSeconds(retryPeriod));
				StreamsThread.sleep(retryPeriod);
			}
		} while (thread != null && !thread.isStopRunning());
	}

	/**
	 * Builds {@link TrackingEvent} for activity data recording.
	 *
	 * @param tracker
	 *            communication gateway to use to record activity
	 * @param trackName
	 *            name of tracking event
	 * @param trackId
	 *            identifier (signature) of tracking event
	 * @return tracking event instance
	 */
	protected TrackingEvent buildEvent(Tracker tracker, String trackName, String trackId) {
		TrackingEvent event = tracker.newEvent(severity == null ? OpLevel.INFO : severity, trackName, (String) null,
				(String) null, (Object[]) null);
		event.setTrackingId(trackId);
		event.setParentId(parentId);
		// event.setCorrelator(CollectionUtils.isEmpty(correlator) ? Collections.singletonList(trackId) : correlator);
		if (CollectionUtils.isNotEmpty(correlator)) {
			event.setCorrelator(correlator);
		}
		if (CollectionUtils.isNotEmpty(tag)) {
			event.setTag(tag);
		}
		if (message != null) {
			if (message instanceof byte[]) {
				byte[] binData = (byte[]) message;
				event.setMessage(binData, (Object[]) null);
				event.setSize(msgLength == null ? binData.length : msgLength);
			} else {
				String strData = Utils.toString(message);
				event.setMessage(strData, (Object[]) null);
				event.setSize(msgLength == null ? strData.length() : msgLength);
			}
		}
		if (StringUtils.isNotEmpty(msgMimeType)) {
			event.setMimeType(msgMimeType);
		}
		if (StringUtils.isNotEmpty(msgEncoding)) {
			event.setEncoding(msgEncoding);
		}
		if (StringUtils.isNotEmpty(msgCharSet)) {
			event.setCharset(msgCharSet);
		}

		event.getOperation().setCompCode(compCode == null ? OpCompCode.SUCCESS : compCode);
		event.getOperation().setReasonCode(reasonCode);
		event.getOperation().setType(eventType);
		event.getOperation().setException(exception);
		if (StringUtils.isNotEmpty(location)) {
			event.getOperation().setLocation(location);
		}
		event.getOperation().setResource(resourceName);
		event.getOperation().setUser(StringUtils.isEmpty(userName) ? tracker.getSource().getUser() : userName);
		event.getOperation().setTID(threadId == null ? Thread.currentThread().getId() : threadId);
		event.getOperation().setPID(processId == null ? Utils.getVMPID() : processId);
		// event.getOperation().setSeverity(severity == null ? OpLevel.INFO :
		// severity);
		event.start(startTime);
		event.stop(endTime, elapsedTime);

		if (activityProperties != null) {
			for (Map.Entry<String, Property> ape : activityProperties.entrySet()) {
				event.getOperation().addProperty(ape.getValue());
			}
		}

		if (CollectionUtils.isNotEmpty(children)) {
			for (ActivityInfo child : children) {
				addTrackableChild(event.getOperation(), buildChild(tracker, child, trackId));
			}
		}

		return event;
	}

	/**
	 * Builds {@link TrackingActivity} for activity data recording.
	 * 
	 * @param tracker
	 *            communication gateway to use to record activity
	 * @param trackName
	 *            name of tracking activity
	 * @param trackId
	 *            identifier (signature) of tracking activity
	 *
	 * @return tracking activity instance
	 */
	private TrackingActivity buildActivity(Tracker tracker, String trackName, String trackId) {
		TrackingActivity activity = tracker.newActivity(severity == null ? OpLevel.INFO : severity, trackName);
		activity.setTrackingId(trackId);
		activity.setParentId(parentId);
		// activity.setCorrelator(CollectionUtils.isEmpty(correlator) ? Collections.singletonList(trackId) :
		// correlator);
		if (CollectionUtils.isNotEmpty(correlator)) {
			activity.setCorrelator(correlator);
		}
		if (CollectionUtils.isNotEmpty(tag)) {
			addActivityProperty(JSONFormatter.JSON_MSG_TAG_FIELD, tag);
		}
		if (message != null) {
			String strData;
			if (message instanceof byte[]) {
				byte[] binData = (byte[]) message;
				strData = new String(Base64.encodeBase64(binData));
				msgEncoding = "base64"; // NON-NLS
				msgMimeType = "application/octet-stream"; // NON-NLS
			} else {
				strData = Utils.toString(message);
			}

			addActivityProperty(JSONFormatter.JSON_MSG_TEXT_FIELD, strData);
			addActivityProperty(JSONFormatter.JSON_MSG_SIZE_FIELD, msgLength == null ? strData.length() : msgLength);
		}
		if (StringUtils.isNotEmpty(msgMimeType)) {
			addActivityProperty(JSONFormatter.JSON_MSG_MIME_FIELD, msgMimeType);
		}
		if (StringUtils.isNotEmpty(msgEncoding)) {
			addActivityProperty(JSONFormatter.JSON_MSG_ENC_FIELD, msgEncoding);
		}
		if (StringUtils.isNotEmpty(msgCharSet)) {
			addActivityProperty(JSONFormatter.JSON_MSG_CHARSET_FIELD, msgCharSet);
		}

		activity.setCompCode(compCode == null ? OpCompCode.SUCCESS : compCode);
		activity.setReasonCode(reasonCode);
		activity.setType(eventType);
		activity.setStatus(StringUtils.isNotEmpty(exception) ? ActivityStatus.EXCEPTION : ActivityStatus.END);
		activity.setException(exception);
		if (StringUtils.isNotEmpty(location)) {
			activity.setLocation(location);
		}
		activity.setResource(resourceName);
		activity.setUser(StringUtils.isEmpty(userName) ? tracker.getSource().getUser() : userName);
		activity.setTID(threadId == null ? Thread.currentThread().getId() : threadId);
		activity.setPID(processId == null ? Utils.getVMPID() : processId);
		// activity.setSeverity(severity == null ? OpLevel.INFO : severity);
		activity.start(startTime);
		activity.stop(endTime, elapsedTime);

		if (activityProperties != null) {
			for (Map.Entry<String, Property> ape : activityProperties.entrySet()) {
				activity.addProperty(ape.getValue());
			}
		}

		if (CollectionUtils.isNotEmpty(children)) {
			for (ActivityInfo child : children) {
				addTrackableChild(activity, buildChild(tracker, child, trackId));
			}
		}

		return activity;
	}

	private static void addTrackableChild(Operation trackableOp, Trackable t) {
		if (t instanceof Snapshot) {
			trackableOp.addSnapshot((Snapshot) t);
		} else {
			LOGGER.log(OpLevel.WARNING,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.invalid.child"),
					t == null ? null : t.getClass());
		}
	}

	private static Trackable buildChild(Tracker tracker, ActivityInfo child, String parentId) {
		child.parentId = parentId;

		// child.resolveServer(false);
		child.determineTimes();

		return child.buildTrackable(tracker);
	}

	/**
	 * Builds {@link Snapshot} for activity data recording.
	 *
	 * @param tracker
	 *            communication gateway to use to record snapshot
	 * @param trackName
	 *            name of snapshot
	 * @param trackId
	 *            identifier (signature) of snapshot
	 *
	 * @return snapshot instance
	 */
	protected Snapshot buildSnapshot(Tracker tracker, String trackName, String trackId) {
		PropertySnapshot snapshot = category != null ? (PropertySnapshot) tracker.newSnapshot(category, trackName)
				: (PropertySnapshot) tracker.newSnapshot(trackName);
		snapshot.setTrackingId(trackId);
		snapshot.setParentId(parentId);
		snapshot.setSeverity(severity == null ? OpLevel.INFO : severity);
		// snapshot.setCorrelator(CollectionUtils.isEmpty(correlator) ? Collections.singletonList(trackId) :
		// correlator);
		if (CollectionUtils.isNotEmpty(correlator)) {
			snapshot.setCorrelator(correlator);
		}
		if (CollectionUtils.isNotEmpty(tag)) {
			snapshot.add(JSONFormatter.JSON_MSG_TAG_FIELD, tag);
		}
		if (message != null) {
			String strData;
			if (message instanceof byte[]) {
				byte[] binData = (byte[]) message;
				strData = new String(Base64.encodeBase64(binData));
				msgEncoding = "base64"; // NON-NLS
				msgMimeType = "application/octet-stream"; // NON-NLS
			} else {
				strData = Utils.toString(message);
			}

			addActivityProperty(JSONFormatter.JSON_MSG_TEXT_FIELD, strData);
			addActivityProperty(JSONFormatter.JSON_MSG_SIZE_FIELD, msgLength == null ? strData.length() : msgLength);
		}
		if (StringUtils.isNotEmpty(msgMimeType)) {
			snapshot.add(JSONFormatter.JSON_MSG_MIME_FIELD, msgMimeType);
		}
		if (StringUtils.isNotEmpty(msgEncoding)) {
			snapshot.add(JSONFormatter.JSON_MSG_ENC_FIELD, msgEncoding);
		}
		if (StringUtils.isNotEmpty(msgCharSet)) {
			snapshot.add(JSONFormatter.JSON_MSG_CHARSET_FIELD, msgCharSet);
		}

		snapshot.add(JSONFormatter.JSON_COMP_CODE_FIELD, compCode == null ? OpCompCode.SUCCESS : compCode);
		snapshot.add(JSONFormatter.JSON_REASON_CODE_FIELD, reasonCode);
		snapshot.add(JSONFormatter.JSON_TYPE_FIELD, eventType);
		snapshot.add(JSONFormatter.JSON_EXCEPTION_FIELD, exception);
		if (StringUtils.isNotEmpty(location)) {
			snapshot.add(JSONFormatter.JSON_LOCATION_FIELD, location);
		}
		snapshot.add(JSONFormatter.JSON_RESOURCE_FIELD, resourceName);
		snapshot.add(JSONFormatter.JSON_USER_FIELD,
				StringUtils.isEmpty(userName) ? tracker.getSource().getUser() : userName);
		snapshot.add(JSONFormatter.JSON_TID_FIELD, threadId == null ? Thread.currentThread().getId() : threadId);
		snapshot.add(JSONFormatter.JSON_PID_FIELD, processId == null ? Utils.getVMPID() : processId);
		snapshot.setTimeStamp(startTime == null ? (endTime == null ? UsecTimestamp.now() : endTime) : startTime);

		if (activityProperties != null) {
			for (Map.Entry<String, Property> ape : activityProperties.entrySet()) {
				snapshot.add(ape.getValue());
			}
		}

		return snapshot;
	}

	/**
	 * Resolves server name and/or IP Address based on values specified.
	 *
	 * @param resolveOverDNS
	 *            flag indicating whether to use DNS to resolve server names and IP addresses
	 */
	private void resolveServer(boolean resolveOverDNS) {
		if (StringUtils.isEmpty(serverName) && StringUtils.isEmpty(serverIp)) {
			serverName = HOST_CACHE.get(LOCAL_SERVER_NAME_KEY);
			serverIp = HOST_CACHE.get(LOCAL_SERVER_IP_KEY);

			if (serverName == null) {
				serverName = Utils.getLocalHostName();
				HOST_CACHE.put(LOCAL_SERVER_NAME_KEY, serverName);
			}
			if (serverIp == null) {
				serverIp = Utils.getLocalHostAddress();
				HOST_CACHE.put(LOCAL_SERVER_IP_KEY, serverIp);
			}
		} else if (StringUtils.isEmpty(serverName)) {
			if (resolveOverDNS) {
				try {
					serverName = HOST_CACHE.get(serverIp);
					if (StringUtils.isEmpty(serverName)) {
						serverName = Utils.resolveAddressToHostName(serverIp);
						if (StringUtils.isEmpty(serverName)) {
							// Add entry so we don't repeatedly attempt to look
							// up unresolvable IP Address
							HOST_CACHE.put(serverIp, "");
						} else {
							HOST_CACHE.put(serverIp, serverName);
							HOST_CACHE.put(serverName, serverIp);
						}
					}
				} catch (Exception e) {
					serverName = serverIp;
				}
			} else {
				serverName = serverIp;
			}
		} else if (StringUtils.isEmpty(serverIp)) {
			if (resolveOverDNS) {
				serverIp = HOST_CACHE.get(serverName);
				if (StringUtils.isEmpty(serverIp)) {
					serverIp = Utils.resolveHostNameToAddress(serverName);
					if (StringUtils.isEmpty(serverIp)) {
						// Add entry so we don't repeatedly attempt to look up
						// unresolvable host name
						HOST_CACHE.put(serverName, "");
					} else {
						HOST_CACHE.put(serverIp, serverName);
						HOST_CACHE.put(serverName, serverIp);
					}
				}
			}
		}

		if (StringUtils.isEmpty(serverIp)) {
			serverIp = " "; // prevents streams API from resolving it to the
			// local IP address
		}
	}

	/**
	 * Computes the unspecified operation times and/or elapsed time based on the specified ones.
	 */
	private void determineTimes() {
		if (elapsedTime < 0L) {
			long elapsedTimeNano = StringUtils.isEmpty(resourceName) ? TimeTracker.hitAndGet()
					: ACTIVITY_TIME_TRACKER.hitAndGet(resourceName);
			elapsedTime = TimestampFormatter.convert(elapsedTimeNano, TimeUnit.NANOSECONDS, TimeUnit.MICROSECONDS);
		}
		if (endTime == null) {
			if (startTime != null) {
				endTime = new UsecTimestamp(startTime);
				endTime.add(0L, elapsedTime);
			} else {
				endTime = new UsecTimestamp();
			}
		}
		if (startTime == null) {
			startTime = new UsecTimestamp(endTime);
			startTime.subtract(0L, elapsedTime);
		}
	}

	private static Integer getIntValue(Object value) {
		return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(Utils.toString(value));
	}

	private static Long getLongValue(Object value) {
		return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(Utils.toString(value));
	}

	/**
	 * Merges activity info data fields values. Values of fields are changed only if they currently hold default
	 * (initial) value.
	 *
	 * @param otherAi
	 *            activity info object to merge into this one
	 */
	public void merge(ActivityInfo otherAi) {
		if (StringUtils.isEmpty(serverName)) {
			serverName = otherAi.serverName;
		}
		if (StringUtils.isEmpty(serverIp)) {
			serverIp = otherAi.serverIp;
		}
		if (StringUtils.isEmpty(applName)) {
			applName = otherAi.applName;
		}
		if (StringUtils.isEmpty(userName)) {
			userName = otherAi.userName;
		}

		if (StringUtils.isEmpty(resourceName)) {
			resourceName = otherAi.resourceName;
		}

		if (StringUtils.isEmpty(eventName)) {
			eventName = otherAi.eventName;
		}
		if (eventType == null) {
			eventType = otherAi.eventType;
		}
		if (startTime == null) {
			startTime = otherAi.startTime;
		}
		if (endTime == null) {
			endTime = otherAi.endTime;
		}
		if (elapsedTime == -1L) {
			elapsedTime = otherAi.elapsedTime;
		}
		if (compCode == null) {
			compCode = otherAi.compCode;
		}
		if (reasonCode == 0) {
			reasonCode = otherAi.reasonCode;
		}
		if (StringUtils.isEmpty(exception)) {
			exception = otherAi.exception;
		}
		if (severity == null) {
			severity = otherAi.severity;
		}
		if (StringUtils.isEmpty(location)) {
			location = otherAi.location;
		}
		if (otherAi.correlator != null) {
			if (correlator == null) {
				correlator = new ArrayList<String>();
			}

			correlator.addAll(otherAi.correlator);
		}

		if (StringUtils.isEmpty(trackingId)) {
			trackingId = otherAi.trackingId;
		}
		if (otherAi.tag != null) {
			if (tag == null) {
				tag = new ArrayList<String>();
			}

			tag.addAll(otherAi.tag);
		}
		if (message == null) {
			message = otherAi.message;
		}
		if (StringUtils.isEmpty(msgCharSet)) {
			msgCharSet = otherAi.msgCharSet;
		}
		if (StringUtils.isEmpty(msgEncoding)) {
			msgEncoding = otherAi.msgEncoding;
		}
		if (msgLength == null) {
			msgLength = otherAi.msgLength;
		}
		if (StringUtils.isEmpty(msgMimeType)) {
			msgMimeType = otherAi.msgMimeType;
		}

		if (processId == null) {
			processId = otherAi.processId;
		}
		if (threadId == null) {
			threadId = otherAi.threadId;
		}

		if (StringUtils.isEmpty(category)) {
			category = otherAi.category;
		}

		if (StringUtils.isEmpty(parentId)) {
			parentId = otherAi.parentId;
		}

		filtered |= otherAi.filtered;

		if (otherAi.activityProperties != null) {
			if (activityProperties == null) {
				activityProperties = new HashMap<String, Property>();
			}

			activityProperties.putAll(otherAi.activityProperties);
		}

		if (CollectionUtils.isNotEmpty(otherAi.children)) {
			if (children == null) {
				children = new ArrayList<ActivityInfo>();
			}

			children.addAll(otherAi.children);
		}
	}

	/**
	 * Gets server name.
	 *
	 * @return the server name
	 */
	public String getServerName() {
		return serverName;
	}

	/**
	 * Gets server ip.
	 *
	 * @return the server ip
	 */
	public String getServerIp() {
		return serverIp;
	}

	/**
	 * Gets application name.
	 *
	 * @return the application name
	 */
	public String getApplName() {
		return applName;
	}

	/**
	 * Gets user name.
	 *
	 * @return the user name
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * Gets resource name.
	 *
	 * @return the resource name
	 */
	public String getResourceName() {
		return resourceName;
	}

	/**
	 * Gets event name.
	 *
	 * @return the event name
	 */
	public String getEventName() {
		return eventName;
	}

	/**
	 * Sets event name.
	 *
	 * @param eventName
	 *            the event name
	 */
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	/**
	 * Gets event type.
	 *
	 * @return the event type
	 */
	public OpType getEventType() {
		return eventType;
	}

	/**
	 * Gets start time.
	 *
	 * @return the start time
	 */
	public UsecTimestamp getStartTime() {
		return startTime;
	}

	/**
	 * Gets end time.
	 *
	 * @return the end time
	 */
	public UsecTimestamp getEndTime() {
		return endTime;
	}

	/**
	 * Gets elapsed time.
	 *
	 * @return the elapsed time
	 */
	public long getElapsedTime() {
		return elapsedTime;
	}

	/**
	 * Gets activity completion code.
	 *
	 * @return the activity completion code
	 */
	public OpCompCode getCompCode() {
		return compCode;
	}

	/**
	 * Gets reason code.
	 *
	 * @return the reason code
	 */
	public int getReasonCode() {
		return reasonCode;
	}

	/**
	 * Gets exception/error message.
	 *
	 * @return the exception/error message
	 */
	public String getException() {
		return exception;
	}

	/**
	 * Gets severity.
	 *
	 * @return the severity
	 */
	public OpLevel getSeverity() {
		return severity;
	}

	/**
	 * Gets location.
	 *
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * Gets tracking identifier.
	 *
	 * @return the tracking identifier
	 */
	public String getTrackingId() {
		return trackingId;
	}

	/**
	 * Gets activity tag strings collection.
	 *
	 * @return the activity tag strings collection
	 */
	public Collection<String> getTag() {
		return tag;
	}

	/**
	 * Gets activity correlator strings collection.
	 *
	 * @return the activity correlator string collection
	 */
	public Collection<String> getCorrelator() {
		return correlator;
	}

	/**
	 * Gets activity message data.
	 *
	 * @return the activity message data
	 */
	public Object getMessage() {
		return message;
	}

	/**
	 * Gets message char set.
	 *
	 * @return the message char set
	 */
	public String getMsgCharSet() {
		return msgCharSet;
	}

	/**
	 * Gets message encoding.
	 *
	 * @return the message encoding
	 */
	public String getMsgEncoding() {
		return msgEncoding;
	}

	/**
	 * Gets message length.
	 *
	 * @return the message length
	 */
	public int getMsgLength() {
		return msgLength;
	}

	/**
	 * Gets message MIME type.
	 *
	 * @return the message MIME type
	 */
	public String getMsgMimeType() {
		return msgMimeType;
	}

	/**
	 * Gets process identifier.
	 *
	 * @return the process identifier
	 */
	public Integer getProcessId() {
		return processId;
	}

	/**
	 * Gets thread identifier.
	 *
	 * @return the thread identifier
	 */
	public Integer getThreadId() {
		return threadId;
	}

	/**
	 * Gets activity category (i.e. snapshot category).
	 *
	 * @return the activity category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * Gets parent activity identifier.
	 *
	 * @return the parent activity identifier
	 */
	public String getParentId() {
		return parentId;
	}

	/**
	 * Returns activity filtering flag value.
	 *
	 * @return activity filtering flag value
	 */
	public boolean isFiltered() {
		return filtered;
	}

	/**
	 * Sets activity filtering flag value.
	 *
	 * @param filtered
	 *            {@code true} if activity is filtered out, {@code false} otherwise
	 */
	public void setFiltered(boolean filtered) {
		this.filtered = filtered;
	}

	/**
	 * Adds child activity info data package.
	 *
	 * @param ai
	 *            activity info object containing child data
	 */
	public void addChild(ActivityInfo ai) {
		if (children == null) {
			children = new ArrayList<ActivityInfo>();
		}

		children.add(ai);
	}
}
