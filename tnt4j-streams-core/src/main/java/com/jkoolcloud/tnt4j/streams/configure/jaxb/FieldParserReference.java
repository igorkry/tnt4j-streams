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

package com.jkoolcloud.tnt4j.streams.configure.jaxb;

import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Associates field with stacked parser reference.
 * 
 * 
 * <p>
 * Java class for FieldParserReference complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FieldParserReference">
 *   &lt;complexContent>
 *     &lt;extension base="{}Reference">
 *       &lt;attribute name="aggregation" type="{}AggregationTypes" default="Merge" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FieldParserReference")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-24T11:46:52+03:00", comments = "JAXB RI v2.2.4-2")
public class FieldParserReference extends Reference {

	@XmlAttribute(name = "aggregation")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-24T11:46:52+03:00", comments = "JAXB RI v2.2.4-2")
	protected AggregationTypes aggregation;

	public FieldParserReference() {
		super();
	}

	public FieldParserReference(String name, AggregationTypes aType) {
		super(name);
		this.aggregation = aType;
	}

	/**
	 * Gets the value of the aggregation property.
	 * 
	 * @return possible object is {@link AggregationTypes }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-24T11:46:52+03:00", comments = "JAXB RI v2.2.4-2")
	public AggregationTypes getAggregation() {
		if (aggregation == null) {
			return AggregationTypes.MERGE;
		} else {
			return aggregation;
		}
	}

	/**
	 * Sets the value of the aggregation property.
	 * 
	 * @param value
	 *            allowed object is {@link AggregationTypes }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2017-04-24T11:46:52+03:00", comments = "JAXB RI v2.2.4-2")
	public void setAggregation(AggregationTypes value) {
		this.aggregation = value;
	}

}
