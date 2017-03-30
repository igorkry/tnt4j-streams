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

package com.jkoolcloud.tnt4j.streams.fields;

import java.net.InetAddress;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.jkoolcloud.tnt4j.core.*;
import com.jkoolcloud.tnt4j.format.JSONFormatter;
import com.jkoolcloud.tnt4j.sink.DefaultEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;
import com.jkoolcloud.tnt4j.source.SourceType;
import com.jkoolcloud.tnt4j.streams.utils.StreamsResources;
import com.jkoolcloud.tnt4j.streams.utils.TimestampFormatter;
import com.jkoolcloud.tnt4j.streams.utils.Utils;
import com.jkoolcloud.tnt4j.tracker.TimeTracker;
import com.jkoolcloud.tnt4j.tracker.Tracker;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;
import com.jkoolcloud.tnt4j.uuid.UUIDFactory;

/**
 * This class represents an {@link Trackable} entity (e.g. activity/event/snapshot) to record to JKool Cloud.
 *
 * @version $Revision: 3 $
 */
public class ActivityInfo {
	private static final EventSink LOGGER = DefaultEventSinkFactory.defaultEventSink(ActivityInfo.class);

	private static final Map<String, String> HOST_CACHE = new ConcurrentHashMap<>();
	private static final String LOCAL_SERVER_NAME_KEY = "LOCAL_SERVER_NAME_KEY"; // NON-NLS
	private static final String LOCAL_SERVER_IP_KEY = "LOCAL_SERVER_IP_KEY"; // NON-NLS

	private String serverName = null;
	private String serverIp = null;
	private String applName = null;
	private String userName = null;

	private String resourceName = null;

	private String eventName = null;
	private OpType eventType = null;
	private ActivityStatus eventStatus = null;
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

	private boolean filteredOut = false;

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
	 * the JKool Cloud.
	 *
	 * @param field
	 *            field to apply
	 * @param value
	 *            value to apply for this field, which could be an array of objects if value for field consists of
	 *            multiple locations
	 *
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
			if (locators.size() > 1 && locators.size() != values.length) {
				throw new ParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityInfo.failed.parsing", field), 0);
			}

			ActivityFieldLocator locator;
			Object fValue;
			List<Object> fvList = new ArrayList<>(locators.size());
			for (int v = 0; v < values.length; v++) {
				locator = locators.size() == 1 ? locators.get(0) : locators.get(v);
				fValue = formatValue(field, locator, values[v]);
				if (fValue == null && locator.isOptional()) {
					continue;
				}
				fvList.add(fValue);
			}

			values = fvList.toArray();

			if (field.isEnumeration() && values.length > 1) {
				throw new ParseException(StreamsResources.getStringFormatted(StreamsResources.RESOURCE_BUNDLE_NAME,
						"ActivityInfo.multiple.enum.values", field), 0);
			}
		}

		Object fieldValue = Utils.simplifyValue(values);

		if (fieldValue == null) {
			LOGGER.log(OpLevel.TRACE,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.field.value.null"),
					field);
			return;
		}
		LOGGER.log(OpLevel.TRACE,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.applying.field.value"),
				field, fieldValue);

		fieldValue = transform(field, fieldValue);
		fieldValue = filterFieldValue(field, fieldValue);
		if (!field.isTransparent()) {
			setFieldValue(field, fieldValue);
		}
	}

	/**
	 * Transforms the value for the field using defined field transformations.
	 * <p>
	 * Note that field value there is combination of all field locators resolved values. Transformations defined for
	 * particular locator is already performed by parser while resolving locator value.
	 *
	 * @param field
	 *            field whose value is to be transformed
	 * @param fieldValue
	 *            field data value to transform
	 * @return transformed field value
	 */
	protected Object transform(ActivityField field, Object fieldValue) {
		try {
			fieldValue = field.transformValue(fieldValue);
		} catch (Exception exc) {
			LOGGER.log(OpLevel.WARNING,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME,
							"ActivityInfo.transformation.failed"),
					field.getFieldTypeName(), Utils.toString(fieldValue), exc);
		}

		return fieldValue;
	}

	/**
	 * Applies filed defined filtering rules and marks this activity as filtered out or sets field value to
	 * {@code null}, if field is set as "optional" using attribute {@code required=false}.
	 * 
	 * @param field
	 *            field instance to use filter definition
	 * @param fieldValue
	 *            value to apply filters
	 * @return value after filtering applied: {@code null} if value gets filtered out and field is optional, or same as
	 *         passed over parameters - otherwise
	 *
	 * @see com.jkoolcloud.tnt4j.streams.fields.ActivityField#filterValue(ActivityInfo, Object)
	 */
	protected Object filterFieldValue(ActivityField field, Object fieldValue) {
		try {
			return field.filterValue(this, fieldValue);
		} catch (Exception exc) {
			LOGGER.log(OpLevel.WARNING,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.filtering.failed"),
					field.getFieldTypeName(), Utils.toString(fieldValue), exc);
			return fieldValue;
		}
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
		if (isValueEmpty(fieldValue)) {
			return;
		}

		StreamFieldType fieldType = field.getFieldType();
		if (fieldType != null) {
			switch (fieldType) {
			case Message:
				message = substitute(message, fieldValue);
				break;
			case EventName:
				eventName = substitute(eventName, getStringValue(fieldValue, field));
				break;
			case EventType:
				eventType = substitute(eventType, Utils.mapOpType(fieldValue));
				break;
			case EventStatus:
				ActivityStatus as = fieldValue instanceof ActivityStatus ? (ActivityStatus) fieldValue
						: ActivityStatus.valueOf(fieldValue);
				eventStatus = substitute(eventStatus, as);
			case ApplName:
				applName = substitute(applName, getStringValue(fieldValue, field));
				break;
			case Correlator:
				addCorrelator(Utils.getTags(fieldValue));
				break;
			case ElapsedTime:
				elapsedTime = substitute(elapsedTime, getNumberValue(fieldValue, Long.class));
				break;
			case EndTime:
				endTime = substitute(endTime, getTimestampValue(fieldValue, field));
				break;
			case Exception:
				exception = substitute(exception, getStringValue(fieldValue, field));
				break;
			case Location:
				location = substitute(location, getStringValue(fieldValue, field));
				break;
			case ReasonCode:
				reasonCode = substitute(reasonCode, getNumberValue(fieldValue, Integer.class));
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
				OpLevel sev = fieldValue instanceof OpLevel ? (OpLevel) fieldValue : OpLevel.valueOf(fieldValue);
				severity = substitute(severity, sev);
				break;
			case TrackingId:
				trackingId = substitute(trackingId, getStringValue(fieldValue, field));
				break;
			case StartTime:
				startTime = substitute(startTime, getTimestampValue(fieldValue, field));
				break;
			case CompCode:
				OpCompCode cc = fieldValue instanceof OpCompCode ? (OpCompCode) fieldValue
						: OpCompCode.valueOf(fieldValue);
				compCode = substitute(compCode, cc);
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
				msgLength = substitute(msgLength, getNumberValue(fieldValue, Integer.class));
				break;
			case MsgMimeType:
				msgMimeType = substitute(msgMimeType, getStringValue(fieldValue, field));
				break;
			case ProcessId:
				processId = substitute(processId, getNumberValue(fieldValue, Integer.class));
				break;
			case ThreadId:
				threadId = substitute(threadId, getNumberValue(fieldValue, Integer.class));
				break;
			case Category:
				category = substitute(category, getStringValue(fieldValue, field));
				break;
			case ParentId:
				parentId = substitute(parentId, getStringValue(fieldValue, field));
				break;
			default:
				throw new IllegalArgumentException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.unrecognized.field", field));
			}

			LOGGER.log(OpLevel.TRACE,
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.set.field"), field,
					fieldValue);
		} else {
			addCustomActivityProperty(field, fieldValue);
		}
	}

	private static boolean isValueEmpty(Object fieldValue) {
		if (fieldValue != null) {
			Object[] va = fieldValue instanceof Object[] ? (Object[]) fieldValue : new Object[] { fieldValue };

			for (Object ve : va) {
				if (ve != null) {
					return false;
				}
			}
		}

		return true;
	}

	private void addCustomActivityProperty(ActivityField field, Object fieldValue) throws ParseException {
		if (fieldValue instanceof Trackable) {
			addActivityProperty(field.getFieldTypeName(), fieldValue);
		} else if (fieldValue instanceof Map) {
			addPropertiesMap((Map<?, ?>) fieldValue, "");
		} else {
			addActivityProperty(field.getFieldTypeName(), getPropertyValue(fieldValue, field), field.getValueType());
		}
	}

	private void addPropertiesMap(Map<?, ?> pMap, String propPrefix) {
		String ppStr = StringUtils.isEmpty(propPrefix) ? "" : propPrefix + '.';

		for (Map.Entry<?, ?> pme : pMap.entrySet()) {
			if (pme.getValue() instanceof Map) {
				addPropertiesMap((Map<?, ?>) pme.getValue(), ppStr + pme.getKey());
			} else {
				addActivityProperty(ppStr + String.valueOf(pme.getKey()), pme.getValue());
			}
		}
	}

	private static Object getPropertyValue(Object fieldValue, ActivityField field) throws ParseException {
		ActivityFieldLocator fmLocator = field.getMasterLocator();

		if (fmLocator != null) {
			switch (fmLocator.getDataType()) {
			case Number:
				return getNumberValue(fieldValue);
			case DateTime:
			case Timestamp:
				return getTimestampValue(fieldValue, field);
			case Binary:
			default:
				return getStringValue(fieldValue, field);
			}
		}

		return getStringValue(fieldValue, field);
	}

	private static UsecTimestamp getTimestampValue(Object fieldValue, ActivityField field) throws ParseException {
		ActivityFieldLocator fmLocator = field.getMasterLocator();

		return fieldValue instanceof UsecTimestamp ? (UsecTimestamp) fieldValue
				: TimestampFormatter.parse(fmLocator == null ? null : fmLocator.getFormat(),
						getStringValue(fieldValue, field), fmLocator == null ? null : fmLocator.getTimeZone(),
						fmLocator == null ? null : fmLocator.getLocale());
	}

	private static String substitute(String value, String newValue) {
		return StringUtils.isEmpty(newValue) ? value : newValue;
	}

	private static <T> T substitute(T value, T newValue) {
		return newValue == null ? value : newValue;
	}

	private static String getStringValue(Object value, ActivityField field) {
		if (value instanceof Object[]) {
			Object[] vArray = (Object[]) value;
			StringBuilder sb = new StringBuilder();
			for (int v = 0; v < vArray.length; v++) {
				if (v > 0) {
					sb.append(field.getSeparator());
				}

				if (vArray[v] instanceof UsecTimestamp) {
					ActivityFieldLocator locator = field.getLocators().size() == 1 ? field.getLocators().get(0)
							: v >= 0 && v < field.getLocators().size() ? field.getLocators().get(v) : null; // TODO

					String format = locator == null ? null : locator.getFormat();
					if (StringUtils.isNotEmpty(format)) {
						sb.append(((UsecTimestamp) vArray[v]).toString(format));
					}
				} else {
					sb.append(vArray[v] == null ? "" : Utils.toString(vArray[v])); // NON-NLS
				}
			}

			return sb.toString();
		} else if (value instanceof byte[]) {
			Utils.encodeHex((byte[]) value);
		} else if (value != null && value.getClass().isArray()) {
			return ArrayUtils.toString(value);
		}

		return Utils.toString(value);
	}

	/**
	 * Adds activity item property to item properties map. Properties from map are transferred as tracking event
	 * properties when {@link #buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker)} is invoked. Same as invoking
	 * {@link #addActivityProperty(String, Object, String)} setting value type to {@code null}.
	 *
	 * @param propName
	 *            activity item property key
	 * @param propValue
	 *            activity item property value
	 * @return previous property value replaced by {@code propValue} or {@code null} if there was no such activity
	 *         property set
	 *
	 * @see Map#put(Object, Object)
	 * @see #buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker)
	 * @see #addActivityProperty(String, Object, String)
	 */
	public Object addActivityProperty(String propName, Object propValue) {
		return addActivityProperty(propName, propValue, null);
	}

	/**
	 * Adds activity item property to item properties map. Properties from map are transferred as tracking event
	 * properties when {@link #buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker)} is invoked.
	 *
	 * @param propName
	 *            activity item property key
	 * @param propValue
	 *            activity item property value
	 * @param valueType
	 *            activity item property value type from {@link com.jkoolcloud.tnt4j.core.ValueTypes} set
	 * @return previous property value replaced by {@code propValue} or {@code null} if there was no such activity
	 *         property set
	 *
	 * @see Map#put(Object, Object)
	 * @see #buildTrackable(com.jkoolcloud.tnt4j.tracker.Tracker)
	 * @see com.jkoolcloud.tnt4j.core.ValueTypes
	 */
	public Object addActivityProperty(String propName, Object propValue, String valueType) {
		if (activityProperties == null) {
			activityProperties = new HashMap<>();
		}

		Property p = new Property(propName, wrapPropertyValue(propValue),
				StringUtils.isEmpty(valueType) ? getDefaultValueType(propValue) : valueType);
		Property prevValue = activityProperties.put(propName, p);

		LOGGER.log(OpLevel.TRACE,
				StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.set.property"),
				propName, Utils.toString(p.getValue()), p.getValueType(), Utils.toString(prevValue));

		return prevValue;
	}

	private static String getDefaultValueType(Object propValue) {
		if (propValue instanceof UsecTimestamp) {
			return ValueTypes.VALUE_TYPE_TIMESTAMP;
		}

		return ValueTypes.VALUE_TYPE_NONE;
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
				collection = new ArrayList<>();
			}

			for (String str : strings) {
				if (StringUtils.isNotEmpty(str)) {
					collection.add(str.trim());
				}
			}
		}

		return collection;
	}

	/**
	 * Makes fully qualified name of activity source. Name is made from stream parsed data attributes.
	 *
	 * @param resolveOverDNS
	 *            flag indicating whether to use DNS to resolve server names and IP addresses
	 *
	 * @return fully qualified name of this activity source, or {@code null} if no source defining attributes where
	 *         parsed from stream.
	 */
	public String getSourceFQN(boolean resolveOverDNS) {
		resolveServer(resolveOverDNS);

		StringBuilder fqnB = new StringBuilder();

		addSourceValue(fqnB, SourceType.APPL, applName);
		addSourceValue(fqnB, SourceType.USER, userName);
		addSourceValue(fqnB, SourceType.SERVER, serverName);
		addSourceValue(fqnB, SourceType.NETADDR, serverIp);
		addSourceValue(fqnB, SourceType.GEOADDR, location);

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
	 * Creates the appropriate data package {@link com.jkoolcloud.tnt4j.tracker.TrackingActivity},
	 * {@link com.jkoolcloud.tnt4j.tracker.TrackingEvent} or {@link com.jkoolcloud.tnt4j.core.PropertySnapshot} using
	 * the specified tracker for this activity data entity to be sent to JKool Cloud.
	 *
	 * @param tracker
	 *            {@link com.jkoolcloud.tnt4j.tracker.Tracker} instance to be used to build
	 *            {@link com.jkoolcloud.tnt4j.core.Trackable} activity data package
	 *
	 * @return trackable instance made from this activity entity data
	 * @throws java.lang.IllegalArgumentException
	 *             if {@code tracker} is null
	 * @see com.jkoolcloud.tnt4j.streams.outputs.JKCloudActivityOutput#logItem(ActivityInfo)
	 */
	public Trackable buildTrackable(Tracker tracker) {
		if (tracker == null) {
			throw new IllegalArgumentException(
					StreamsResources.getString(StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.tracker.null"));
		}

		resolveServer(false);
		determineTimes();

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
			} else {
				String strData = Utils.toString(message);
				event.setMessage(strData, (Object[]) null);
			}

			if (msgLength != null) {
				event.setSize(msgLength);
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
		event.getOperation().setType(eventType == null ? OpType.EVENT : eventType);
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
		if (eventStatus != null) {
			addActivityProperty(JSONFormatter.JSON_STATUS_FIELD, eventStatus);
		}
		event.start(startTime);
		event.stop(endTime, elapsedTime);

		if (activityProperties != null) {
			for (Map.Entry<String, Property> ape : activityProperties.entrySet()) {
				if (ape.getValue().getValue() instanceof Snapshot) {
					event.getOperation().addSnapshot((Snapshot) ape.getValue().getValue());
				} else {
					event.getOperation().addProperty(ape.getValue());
				}
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
				strData = Utils.base64EncodeStr(binData);
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
		activity.setStatus(StringUtils.isNotEmpty(exception) ? ActivityStatus.EXCEPTION
				: eventStatus == null ? ActivityStatus.END : eventStatus);
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
				if (ape.getValue().getValue() instanceof Trackable) {
					activity.add((Trackable) ape.getValue().getValue());
				} else {
					activity.addProperty(ape.getValue());
				}
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
				strData = Utils.base64EncodeStr(binData);
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
		if (eventStatus != null) {
			addActivityProperty(JSONFormatter.JSON_STATUS_FIELD, eventStatus);
		}

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
			serverIp = " "; // prevents streams API from resolving it to the local IP address
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

	// private static Integer getIntValue(Object value) {
	// return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(Utils.toString(value));
	// }
	//
	// private static Long getLongValue(Object value) {
	// return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(Utils.toString(value));
	// }

	private static Number getNumberValue(Object value) {
		return value instanceof Number ? (Number) value : NumberUtils.createNumber(Utils.toString(value));
	}

	private static <T extends Number> T getNumberValue(Object value, Class<T> clazz) {
		Number num = value instanceof Number ? (Number) value : NumberUtils.createNumber(Utils.toString(value));

		return Utils.castNumber(num, clazz);
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
		if (eventStatus == null) {
			eventStatus = otherAi.eventStatus;
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
				correlator = new ArrayList<>();
			}

			correlator.addAll(otherAi.correlator);
		}

		if (StringUtils.isEmpty(trackingId)) {
			trackingId = otherAi.trackingId;
		}
		if (otherAi.tag != null) {
			if (tag == null) {
				tag = new ArrayList<>();
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

		filteredOut |= otherAi.filteredOut;

		if (otherAi.activityProperties != null) {
			if (activityProperties == null) {
				activityProperties = new HashMap<>();
			}

			activityProperties.putAll(otherAi.activityProperties);
		}

		if (CollectionUtils.isNotEmpty(otherAi.children)) {
			if (children == null) {
				children = new ArrayList<>();
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
	 * Gets event status.
	 *
	 * @return the event status
	 */
	public ActivityStatus getEventStatus() {
		return eventStatus;
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
	 * Returns activity filtered out flag value.
	 *
	 * @return activity filtered out flag value
	 */
	public boolean isFilteredOut() {
		return filteredOut;
	}

	/**
	 * Sets activity filtered out flag value.
	 *
	 * @param filteredOut
	 *            {@code true} if activity is filtered out, {@code false} otherwise
	 */
	public void setFiltered(boolean filteredOut) {
		this.filteredOut = filteredOut;
	}

	/**
	 * Adds child activity info data package.
	 *
	 * @param ai
	 *            activity info object containing child data
	 */
	public void addChild(ActivityInfo ai) {
		if (children == null) {
			children = new ArrayList<>();
		}

		children.add(ai);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ActivityInfo{"); // NON-NLS
		sb.append("serverName='").append(serverName).append('\''); // NON-NLS
		sb.append(", serverIp='").append(serverIp).append('\''); // NON-NLS
		sb.append(", applName='").append(applName).append('\''); // NON-NLS
		sb.append(", userName='").append(userName).append('\''); // NON-NLS
		sb.append(", resourceName='").append(resourceName).append('\''); // NON-NLS
		sb.append(", eventName='").append(eventName).append('\''); // NON-NLS
		sb.append(", eventType=").append(eventType); // NON-NLS
		sb.append(", eventStatus=").append(eventStatus); // NON-NLS
		sb.append(", startTime=").append(startTime); // NON-NLS
		sb.append(", endTime=").append(endTime); // NON-NLS
		sb.append(", elapsedTime=").append(elapsedTime); // NON-NLS
		sb.append(", compCode=").append(compCode); // NON-NLS
		sb.append(", reasonCode=").append(reasonCode); // NON-NLS
		sb.append(", exception='").append(exception).append('\''); // NON-NLS
		sb.append(", severity=").append(severity); // NON-NLS
		sb.append(", location='").append(location).append('\''); // NON-NLS
		sb.append(", correlator=").append(correlator); // NON-NLS
		sb.append(", trackingId='").append(trackingId).append('\''); // NON-NLS
		sb.append(", parentId='").append(parentId).append('\''); // NON-NLS
		sb.append(", tag=").append(tag); // NON-NLS
		sb.append(", message=").append(message); // NON-NLS
		sb.append(", msgCharSet='").append(msgCharSet).append('\''); // NON-NLS
		sb.append(", msgEncoding='").append(msgEncoding).append('\''); // NON-NLS
		sb.append(", msgLength=").append(msgLength); // NON-NLS
		sb.append(", msgMimeType='").append(msgMimeType).append('\''); // NON-NLS
		sb.append(", processId=").append(processId); // NON-NLS
		sb.append(", threadId=").append(threadId); // NON-NLS
		sb.append(", category='").append(category).append('\''); // NON-NLS
		sb.append(", filteredOut=").append(filteredOut); // NON-NLS
		sb.append(", activityProperties=").append(activityProperties == null ? "NONE" : activityProperties.size());// NON-NLS
		sb.append(", children=").append(children == null ? "NONE" : children.size()); // NON-NLS
		sb.append('}');
		return sb.toString();
	}

	/**
	 * Returns activity field value.
	 * 
	 * @param fieldName
	 *            field name value to get
	 * @return field contained value
	 */
	public Object getFieldValue(String fieldName) {
		try {
			StreamFieldType sft = StreamFieldType.valueOfIgnoreCase(fieldName);
			switch (sft) {
			case ApplName:
				return applName;
			case Category:
				return category;
			case CompCode:
				return compCode;
			case Correlator:
				return correlator;
			case ElapsedTime:
				return elapsedTime;
			case EndTime:
				return endTime;
			case EventName:
				return eventName;
			case EventStatus:
				return eventStatus;
			case EventType:
				return eventType;
			case Exception:
				return exception;
			case Location:
				return location;
			case Message:
				return message;
			case MsgCharSet:
				return msgCharSet;
			case MsgEncoding:
				return msgEncoding;
			case MsgLength:
				return msgLength;
			case MsgMimeType:
				return msgMimeType;
			case ParentId:
				return parentId;
			case ProcessId:
				return processId;
			case ReasonCode:
				return reasonCode;
			case ResourceName:
				return resourceName;
			case ServerIp:
				return serverIp;
			case ServerName:
				return serverName;
			case Severity:
				return severity;
			case StartTime:
				return startTime;
			case Tag:
				return tag;
			case ThreadId:
				return threadId;
			case TrackingId:
				return trackingId;
			case UserName:
				return userName;
			default:
				throw new IllegalArgumentException(StreamsResources.getStringFormatted(
						StreamsResources.RESOURCE_BUNDLE_NAME, "ActivityInfo.unrecognized.field", fieldName));
			}
		} catch (IllegalArgumentException exc) {
			Property p = activityProperties.get(fieldName);

			return p == null ? null : p.getValue();
		}
	}
}
