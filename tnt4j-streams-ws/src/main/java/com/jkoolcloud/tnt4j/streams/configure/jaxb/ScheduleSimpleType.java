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

import java.math.BigInteger;

import javax.annotation.Generated;
import javax.xml.bind.annotation.*;

/**
 * Defines simple scheduler parameters.
 * 
 * 
 * <p>
 * Java class for ScheduleSimpleType complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ScheduleSimpleType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="interval" use="required" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" />
 *       &lt;attribute name="units" type="{}UnitsTypes" default="Milliseconds" />
 *       &lt;attribute name="repeatCount" type="{http://www.w3.org/2001/XMLSchema}integer" default="1" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ScheduleSimpleType")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-04-12T03:47:04+03:00", comments = "JAXB RI v2.2.4-2")
public class ScheduleSimpleType {

	@XmlAttribute(name = "interval", required = true)
	@XmlSchemaType(name = "nonNegativeInteger")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-04-12T03:47:04+03:00", comments = "JAXB RI v2.2.4-2")
	protected BigInteger interval;
	@XmlAttribute(name = "units")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-04-12T03:47:04+03:00", comments = "JAXB RI v2.2.4-2")
	protected UnitsTypes units;
	@XmlAttribute(name = "repeatCount")
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-04-12T03:47:04+03:00", comments = "JAXB RI v2.2.4-2")
	protected BigInteger repeatCount;

	public ScheduleSimpleType() {

	}

	public ScheduleSimpleType(int interval, UnitsTypes units, Integer repeatCount) {
		this.interval = BigInteger.valueOf(interval);
		this.units = units;
		this.repeatCount = repeatCount == null ? null : BigInteger.valueOf(repeatCount);
	}

	/**
	 * Gets the value of the interval property.
	 * 
	 * @return possible object is {@link BigInteger }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-04-12T03:47:04+03:00", comments = "JAXB RI v2.2.4-2")
	public BigInteger getInterval() {
		return interval;
	}

	/**
	 * Sets the value of the interval property.
	 * 
	 * @param value
	 *            allowed object is {@link BigInteger }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-04-12T03:47:04+03:00", comments = "JAXB RI v2.2.4-2")
	public void setInterval(BigInteger value) {
		this.interval = value;
	}

	/**
	 * Gets the value of the units property.
	 * 
	 * @return possible object is {@link UnitsTypes }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-04-12T03:47:04+03:00", comments = "JAXB RI v2.2.4-2")
	public UnitsTypes getUnits() {
		if (units == null) {
			return UnitsTypes.MILLISECONDS;
		} else {
			return units;
		}
	}

	/**
	 * Sets the value of the units property.
	 * 
	 * @param value
	 *            allowed object is {@link UnitsTypes }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-04-12T03:47:04+03:00", comments = "JAXB RI v2.2.4-2")
	public void setUnits(UnitsTypes value) {
		this.units = value;
	}

	/**
	 * Gets the value of the repeatCount property.
	 * 
	 * @return possible object is {@link BigInteger }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-04-12T03:47:04+03:00", comments = "JAXB RI v2.2.4-2")
	public BigInteger getRepeatCount() {
		if (repeatCount == null) {
			return new BigInteger("1");
		} else {
			return repeatCount;
		}
	}

	/**
	 * Sets the value of the repeatCount property.
	 * 
	 * @param value
	 *            allowed object is {@link BigInteger }
	 * 
	 */
	@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2016-04-12T03:47:04+03:00", comments = "JAXB RI v2.2.4-2")
	public void setRepeatCount(BigInteger value) {
		this.repeatCount = value;
	}

}
