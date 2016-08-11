package com.jkoolcloud.tnt4j.streams.fields;

import java.util.ArrayList;
import java.util.List;

import com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser;

/**
 *  Class for activities field witch name will be resolved based on data received. 
 * 
 * @author work
 *
 */

public class DynamicNameActivityField extends ActivityField {

	public int index;
	public int dept;
	private List<ActivityFieldLocator> nameLocators = null;
	private boolean created = false;
	
	/**
	 * Inherited constructor
	 * 
	 * @param fieldTypeName 
	 * 					Name template for resolving name 
	 */
	
	public DynamicNameActivityField(String fieldTypeName) {
		super(fieldTypeName);
	}
	
	/**
	 * Inherited constructor
	 * 
	 * @param fieldTypeName
	 * 					Name template for resolving name
	 * @param dataType
	 * 					Type of field data type
	 */

	public DynamicNameActivityField(String fieldTypeName, ActivityFieldDataType dataType) {
		super(fieldTypeName, dataType);
	}
	
	
	public DynamicNameActivityField(String fieldTypeName, List<ActivityFieldLocator> locators, int index, int depth) {
		super(fieldTypeName);
		this.dept = depth;
		this.index = index;
		addLocators(locators);
		setCreated(true);
	}
	
	/**
	 * Adding a multiple locators
	 * 
	 * @param locators
	 * 					Locator list;
	 */

	public void addLocators(List<ActivityFieldLocator> locators) {
		for (ActivityFieldLocator locator : locators) {
			addLocator(locator);
		}
		
	}
	
	/**
	 *	Get locators for activityFields, to resolve activityField name  
	 * 
	 * @return the nameLocators
	 * 					Locator for resolving activityField name
	 */
	
	public List<ActivityFieldLocator> getNameLocators() {
		return nameLocators;
	}
	
	
	/**
	 * Adds activity field name locator.
	 *
	 * @param locator
	 *            the locator to add
	 * @return instance of this activity field
	 */
	public ActivityField addNameLocator(ActivityFieldLocator locator) {
		if (nameLocators == null) {
			nameLocators = new ArrayList<ActivityFieldLocator>();
		}
		nameLocators.add(locator);

		return this;
	}
	
	/**
	 *  Sets field type name
	 * 
	 */
	public void setFieldTypeName(String fieldTypeName ) {
		this.fieldTypeName = fieldTypeName;
	}

	/**
	 * @return the created
	 * 			Return's true if field is created by {@link GenericActivityParser} 
	 * 				and false if specified by user
	 */
	
	public boolean isCreated() {
		return created;
	}

	/**
	 * @param created 
	 * 			to set Flag created
	 */
	
	public void setCreated(boolean created) {
		this.created = created;
	}
	
	/**
	 * Constructs a field name based on template
	 * 
	 */
	
	public void appendName(String namePart, int index) {
		setFieldTypeName(getFieldTypeName().replace("{" + index +"}", namePart));
	}

}
