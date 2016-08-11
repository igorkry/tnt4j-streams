package com.jkoolcloud.tnt4j.streams.parsers;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.DocumentContext;
import com.jkoolcloud.tnt4j.streams.configure.ParserProperties;
import com.jkoolcloud.tnt4j.streams.fields.ActivityField;
import com.jkoolcloud.tnt4j.streams.fields.ActivityFieldLocator;
import com.jkoolcloud.tnt4j.streams.fields.ActivityInfo;
import com.jkoolcloud.tnt4j.streams.inputs.TNTInputStream;
import com.jkoolcloud.tnt4j.streams.outputs.JKCloudActivityOutput;
import com.jkoolcloud.tnt4j.uuid.JUGFactoryImpl;
import com.jkoolcloud.tnt4j.uuid.UUIDFactory;

public class ChopingActivityJSONParser extends ActivityJsonParser {

	private String splitField = null;
	private ActivityField splitField2;
	
	public ChopingActivityJSONParser() {

	}

	@Override
	public void setProperties(Collection<Map.Entry<String, String>> props) throws Exception {
		if (props == null) {
			return;
		}

		for (Map.Entry<String, String> prop : props) {
			String name = prop.getKey();
			String value = prop.getValue();

			if (ParserProperties.PROP_SPLIT_FIELD.equalsIgnoreCase(name)) {
				splitField = value;
			}
		}
	}
	
	@Override
	protected ActivityInfo parsePreparedItem(TNTInputStream<?, ?> stream, String dataStr, DocumentContext data)
			throws ParseException {
		
		if (splitField2 == null) {
			for (ActivityField field : fieldList) {
				if (field.getFieldTypeName().equalsIgnoreCase(this.splitField)) {
					splitField2 = field;
				}
			}
			fieldList.remove(splitField2);
		}
		if (splitField2 == null) {
			return super.parsePreparedItem(stream, dataStr, data);
		}
		
		ActivityInfo ai = new ActivityInfo();
		//applyFieldValue(stream, ai, field, dataStr);
		
		ai = super.parsePreparedItem(stream, dataStr, data);
		UUIDFactory uuidGenerator = new JUGFactoryImpl();
		final String parentId = uuidGenerator.newUUID();
		ai.setTrackingId(parentId);
		
		List<ActivityFieldLocator> locations = splitField2.getLocators();
		Object value = null;
		if (locations.size() == 1) {
			value = getLocatorValue(stream, locations.get(0), data);
		}	
		if (!value.getClass().isArray()) {
			throw new ParseException("Chopping parser field must resove array", 0);
		}
		Object[] values = (Object[]) value;
		
		
		for (Object resolvedValue: values) {
			for (ActivityParser parser: splitField2.getStackedParsers()) {
				final ActivityInfo parsedItem = parser.parse(stream, resolvedValue);
				parsedItem.setParentId(parentId);
				/// Workaround due to not able to send snapshot if parentID is not still recorded
				ai.addSnapshot(parsedItem, ((JKCloudActivityOutput) stream.getOutput()).getTracker(ai.getSourceFQN(), Thread.currentThread()), parsedItem.getEventName() );
				
				// Original implementation instead of workaround
/*				try {
					((JKCloudActivityOutput)stream.getOutput()).sendItem(parsedItem);
				} catch (Exception e) {
					e.printStackTrace();
				}*/
			}
		}
		
		
		return ai;
	}

}
