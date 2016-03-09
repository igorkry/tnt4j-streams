/*
 * Copyright 2014-2016 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jkool.tnt4j.streams.configure.jaxb;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * This object contains factory methods for each Java content interface and Java
 * element interface generated in the com.jkool.tnt4j.streams.configure.jaxb
 * package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the
 * Java representation for XML content. The Java representation of XML content
 * can consist of schema derived interfaces and classes representing the binding
 * of schema type definitions, element declarations and model groups. Factory
 * methods for each of these are provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

	/**
	 * Create a new ObjectFactory that can be used to create new instances of
	 * schema derived classes for package:
	 * com.jkool.tnt4j.streams.configure.jaxb
	 * 
	 */
	public ObjectFactory() {
	}

	/**
	 * Create an instance of {@link TntDataSource }
	 * 
	 */
	public TntDataSource createTntDataSource() {
		return new TntDataSource();
	}

	/**
	 * Create an instance of {@link TntDataSource.Stream }
	 * 
	 */
	public TntDataSource.Stream createTntDataSourceStream() {
		return new TntDataSource.Stream();
	}

	/**
	 * Create an instance of {@link TntDataSource.Parser }
	 * 
	 */
	public TntDataSource.Parser createTntDataSourceParser() {
		return new TntDataSource.Parser();
	}

	/**
	 * Create an instance of {@link TntDataSource.Parser.Field }
	 * 
	 */
	public TntDataSource.Parser.Field createTntDataSourceParserField() {
		return new TntDataSource.Parser.Field();
	}

	/**
	 * Create an instance of {@link FieldMap }
	 * 
	 */
	public FieldMap createFieldMap() {
		return new FieldMap();
	}

	/**
	 * Create an instance of {@link ParserProperty }
	 * 
	 */
	public ParserProperty createParserProperty() {
		return new ParserProperty();
	}

	/**
	 * Create an instance of {@link ParserRef }
	 * 
	 */
	public ParserRef createParserRef() {
		return new ParserRef();
	}

	/**
	 * Create an instance of {@link Property }
	 * 
	 */
	public Property createProperty() {
		return new Property();
	}

	/**
	 * Create an instance of {@link StreamProperty }
	 * 
	 */
	public StreamProperty createStreamProperty() {
		return new StreamProperty();
	}

	/**
	 * Create an instance of {@link TntDataSource.Stream.Tnt4JProperties }
	 * 
	 */
	public TntDataSource.Stream.Tnt4JProperties createTntDataSourceStreamTnt4JProperties() {
		return new TntDataSource.Stream.Tnt4JProperties();
	}

	/**
	 * Create an instance of {@link TntDataSource.Parser.Field.FieldLocator }
	 * 
	 */
	public TntDataSource.Parser.Field.FieldLocator createTntDataSourceParserFieldFieldLocator() {
		return new TntDataSource.Parser.Field.FieldLocator();
	}

}
