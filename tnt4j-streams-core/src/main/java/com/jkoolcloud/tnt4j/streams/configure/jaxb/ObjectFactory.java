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

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each Java content interface and Java element interface generated in the
 * com.jkoolcloud.tnt4j.streams.configure.jaxb package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation for XML content.
 * The Java representation of XML content can consist of schema derived interfaces and classes representing the binding
 * of schema type definitions, element declarations and model groups. Factory methods for each of these are provided in
 * this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

	private final static QName _FieldLocatorTypeFieldTransform_QNAME = new QName("", "field-transform");
	private final static QName _FieldLocatorTypeFieldMap_QNAME = new QName("", "field-map");
	private final static QName _FieldLocatorTypeFilter_QNAME = new QName("", "filter");

	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package:
	 * com.jkoolcloud.tnt4j.streams.configure.jaxb
	 * 
	 */
	public ObjectFactory() {
	}

	/**
	 * Create an instance of {@link FilterType }
	 * 
	 */
	public FilterType createFilterType() {
		return new FilterType();
	}

	/**
	 * Create an instance of {@link StreamType }
	 * 
	 */
	public StreamType createStreamType() {
		return new StreamType();
	}

	/**
	 * Create an instance of {@link ParserType }
	 * 
	 */
	public ParserType createParserType() {
		return new ParserType();
	}

	/**
	 * Create an instance of {@link TntDataSource }
	 * 
	 */
	public TntDataSource createTntDataSource() {
		return new TntDataSource();
	}

	/**
	 * Create an instance of {@link JavaObjectType }
	 * 
	 */
	public JavaObjectType createJavaObjectType() {
		return new JavaObjectType();
	}

	/**
	 * Create an instance of {@link ParserProperty }
	 * 
	 */
	public ParserProperty createParserProperty() {
		return new ParserProperty();
	}

	/**
	 * Create an instance of {@link ParamType }
	 * 
	 */
	public ParamType createParamType() {
		return new ParamType();
	}

	/**
	 * Create an instance of {@link Property }
	 * 
	 */
	public Property createProperty() {
		return new Property();
	}

	/**
	 * Create an instance of {@link FieldLocatorType }
	 * 
	 */
	public FieldLocatorType createFieldLocatorType() {
		return new FieldLocatorType();
	}

	/**
	 * Create an instance of {@link FieldMap }
	 * 
	 */
	public FieldMap createFieldMap() {
		return new FieldMap();
	}

	/**
	 * Create an instance of {@link FieldTransform }
	 * 
	 */
	public FieldTransform createFieldTransform() {
		return new FieldTransform();
	}

	/**
	 * Create an instance of {@link FieldParserReference }
	 * 
	 */
	public FieldParserReference createFieldParserReference() {
		return new FieldParserReference();
	}

	/**
	 * Create an instance of {@link Reference }
	 * 
	 */
	public Reference createReference() {
		return new Reference();
	}

	/**
	 * Create an instance of {@link StreamProperty }
	 * 
	 */
	public StreamProperty createStreamProperty() {
		return new StreamProperty();
	}

	/**
	 * Create an instance of {@link FilterType.Value }
	 * 
	 */
	public FilterType.Value createFilterTypeValue() {
		return new FilterType.Value();
	}

	/**
	 * Create an instance of {@link FilterType.Expression }
	 * 
	 */
	public FilterType.Expression createFilterTypeExpression() {
		return new FilterType.Expression();
	}

	/**
	 * Create an instance of {@link StreamType.Tnt4JProperties }
	 * 
	 */
	public StreamType.Tnt4JProperties createStreamTypeTnt4JProperties() {
		return new StreamType.Tnt4JProperties();
	}

	/**
	 * Create an instance of {@link ParserType.Field }
	 * 
	 */
	public ParserType.Field createParserTypeField() {
		return new ParserType.Field();
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link FieldTransform }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "field-transform", scope = FieldLocatorType.class)
	public JAXBElement<FieldTransform> createFieldLocatorTypeFieldTransform(FieldTransform value) {
		return new JAXBElement<FieldTransform>(_FieldLocatorTypeFieldTransform_QNAME, FieldTransform.class,
				FieldLocatorType.class, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link FieldMap }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "field-map", scope = FieldLocatorType.class)
	public JAXBElement<FieldMap> createFieldLocatorTypeFieldMap(FieldMap value) {
		return new JAXBElement<FieldMap>(_FieldLocatorTypeFieldMap_QNAME, FieldMap.class, FieldLocatorType.class,
				value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link FilterType }{@code >}}
	 * 
	 */
	@XmlElementDecl(namespace = "", name = "filter", scope = FieldLocatorType.class)
	public JAXBElement<FilterType> createFieldLocatorTypeFilter(FilterType value) {
		return new JAXBElement<FilterType>(_FieldLocatorTypeFilter_QNAME, FilterType.class, FieldLocatorType.class,
				value);
	}

}
