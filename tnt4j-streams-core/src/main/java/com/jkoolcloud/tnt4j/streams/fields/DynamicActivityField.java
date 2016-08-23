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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.jkoolcloud.tnt4j.streams.parsers.GenericActivityParser;

/**
 * Class for activities field witch name will be resolved based on streamed data.
 *
 * @version $Revision: 1 $
 */
public class DynamicActivityField extends ActivityField {

	private static final String NAME_PATTERN_TOKEN = Pattern.quote("{}"); // NON-NLS

	private List<ActivityFieldLocator> nameLocators = null;

	private boolean synthetic = false;
	private int index;

	/**
	 * Constructs a new dynamic activity field entry.
	 *
	 * @param fieldTypeName
	 *            name of activity field type
	 * @throws IllegalArgumentException
	 *             if field name is {@code null} or empty
	 */
	public DynamicActivityField(String fieldTypeName) {
		super(fieldTypeName);
	}

	/**
	 * Constructs a new dynamic activity field entry.
	 *
	 * @param aField
	 */
	public DynamicActivityField(ActivityField aField) {
		this(aField.getFieldTypeName());
		addLocators(aField.getLocators());
		setValueTypeLocator(aField.getValueTypeLocator());
	}

	/**
	 *
	 * @param aField
	 * @param index
	 */
	public DynamicActivityField(ActivityField aField, int index) {
		this(aField);

		this.index = index;
		this.synthetic = true;
	}

	private void addLocators(List<ActivityFieldLocator> locators) {
		if (locators != null) {
			for (ActivityFieldLocator locator : locators) {
				addLocator(locator);
			}
		}
	}

	/**
	 * Get locators for activityFields, to resolve activityField name
	 *
	 * @return the nameLocators Locator for resolving activityField name
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
	 * Returns if field was dynamically userDefined by parser.
	 *
	 * @return {@code true} if field is userDefined by {@link GenericActivityParser}, {@code false} - if defined by user
	 *         mapping
	 */
	public boolean isSynthetic() {
		return synthetic;
	}

	/**
	 * Returns index of field mapping value.
	 *
	 * @return index of field mapping value
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Constructs a field name based on template
	 */
	public void appendName(String namePart, int index) {
		setFieldTypeName(getFieldTypeName().replaceFirst(NAME_PATTERN_TOKEN, namePart));
		setFieldTypeName(getFieldTypeName().replaceAll("#", String.valueOf(index)));
	}

}
