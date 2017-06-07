# TNT4J-Streams
TNT4J Streams allows data streaming, parsing from various data sources into TNT4J event sinks.

Why TNT4J-Streams
======================================

* TNT4J-Streams can be run out of the box for a large set of data streaming without writing no additional code.
All You need is to define Your data format mapping to TNT4J event mapping in TNT4J-Streams configuration.

* Supports the following data sources:
    * File
    * Character stream over TCP/IP
    * HDFS
    * MQTT
    * HTTP
    * JMS
    * Apache Kafka (as Consumer and as Server/Consumer)
    * Apache Flume
    * Logstash
    * WMQ (IBM MQ)
    * OS pipes
    * Zipped files (HDFS also)
    * Standard Java InputStream/Reader
    * JAX-RS service (JSON/XML)
    * JAX-WS service
    * System command
    * MS Excel document

* Files (also HDFS) can be streamed:
    * as "whole at once" - when stream starts, it reads file contents line by line meaning single file line hols
    data of single activity event. After file reading completes - stream stops.
    * using file polling - when some application uses file to write data at runtime, stream waits for file changes.
    When file changes, changed (appended) lines are read by stream and interpreted as single line is single activity
    event. Stream stops only when application gets terminated or some critical runtime error occurs.

* Customized parser for Apache Access Logs.

* Customized IBM MQ Trace Events stream (and parser).

* It can be integrated with:
    * Logstash
    * Apache Flume
    * Angulartics
    * AJAX
    * Node.js
    * Collectd
    * Nagios
    * Fluentd

    just by applying configuration and without additional coding.

* Redirect streamed data from different TNT4J based producer APIs like `tnt4j-stream-*` - to be TNT4J based streams concentrator. 

Importing TNT4J-Streams project into IDE
======================================

## Eclipse
* Select File->Import...->Maven->Existing Maven Projects
* Click 'Next'
* In 'Root directory' field select path of directory where You have downloaded (checked out from git)
TNT4J-Streams project
* Click 'OK'
* Dialog fills in with project modules details
* Click 'Finish'

Running TNT4J-Streams
======================================

## Running TNT4J-Streams
* As standalone application
    * write streams configuration file. See ['Streams configuration'](#streams-configuration) chapter for more details
    * configure Your loggers
    * use `bin/tnt4j-streams.bat` or `bin/tnt4j-streams.sh` to run standalone application
* As API integrated into Your product
    * Write streams configuration file. See ['Streams configuration'](#streams-configuration) chapter for more details
    * use `StreamsAgent.runFromAPI(configFileName)` in Your code

## TNT4J Events field mappings

Mapping of streamed data to activity event fields are performed by parser. To map field value You have to define
`field` tag in parser configuration:
* `name` attribute defines activity event field name
* `locator` attribute defines location of data value from streamed data
* `format` attribute defines format of data value
* `value` attribute defines predefined (hardcoded) value of field
* `field-map` tag is used to perform manual mapping from streamed data value `source` to field value `target.`

sample:
```xml
    <parser name="TokenParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser">
        <.../>
        <field name="StartTime" locator="1" format="dd MMM yyyy HH:mm:ss" locale="en-US"/>
        <field name="ServerIp" locator="2"/>
        <field name="ApplName" value="orders"/>
        <field name="Correlator" locator="3"/>
        <field name="UserName" locator="4"/>
        <field name="EventName" locator="5"/>
        <field name="EventType" locator="5">
            <field-map source="Order Placed" target="START"/>
            <field-map source="Order Received" target="RECEIVE"/>
            <field-map source="Order Processing" target="OPEN"/>
            <field-map source="Order Processed" target="SEND"/>
            <field-map source="Order Shipped" target="END"/>
        </field>
        <field name="MsgValue" locator="8"/>
    </parser>
```

### Predefined fields set

```java
    /**
     * Name of application associated with the activity.
     */
    ApplName(String.class),

    /**
     * Host name of server to associate with activity.
     */
    ServerName(String.class),

    /**
     * IP Address of server to associate with activity.
     */
    ServerIp(String.class),

    /**
     * Name to assign to activity entry. Examples are operation, method, API call, event, etc.
     */
    EventName(String.class),

    /**
     * Type of activity - value must match values in {@link com.jkoolcloud.tnt4j.core.OpType} enumeration.
     */
    EventType(Enum.class),

    /**
     * Time action associated with activity started.
     */
    StartTime(UsecTimestamp.class),

    /**
     * Time action associated with activity ended.
     */
    EndTime(UsecTimestamp.class),

    /**
     * Elapsed time of the activity.
     */
    ElapsedTime(Long.class),

    /**
     * Identifier of process where activity event has occurred.
     */
    ProcessId(Integer.class),

    /**
     * Identifier of thread where activity event has occurred.
     */
    ThreadId(Integer.class),

    /**
     * Indicates completion status of the activity - value must match values in {@link com.jkoolcloud.tnt4j.core.OpCompCode} enumeration.
     */
    CompCode(Enum.class),

    /**
     * Numeric reason/error code associated with the activity.
     */
    ReasonCode(Integer.class),

    /**
     * Error/exception message associated with the activity.
     */
    Exception(String.class),

    /**
     * Indicates completion status of the activity - value can either be label from {@link com.jkoolcloud.tnt4j.core.OpLevel} enumeration 
     * or a numeric value.
     */
    Severity(Enum.class),

    /**
     * Location that activity occurred at.
     */
    Location(String.class),

    /**
     * Identifier used to correlate/relate activity entries to group them into logical entities.
     */
    Correlator(String[].class),

    /**
     * User-defined label to associate with the activity, generally for locating activity.
     */
    Tag(String[].class),

    /**
     * Name of user associated with the activity.
     */
    UserName(String.class),

    /**
     * Name of resource associated with the activity.
     */
    ResourceName(String.class),

    /**
     * User data to associate with the activity.
     */
    Message(String.class),

    /**
     * Identifier used to uniquely identify the data associated with this activity.
     */
    TrackingId(String.class),

    /**
     * Length of activity event message data.
     */
    MsgLength(Integer.class),

    /**
     * MIME type of activity event message data.
     */
    MsgMimeType(String.class),

    /**
     * Encoding of activity event message data.
     */
    MsgEncoding(String.class),

    /**
     * CharSet of activity event message data.
     */
    MsgCharSet(String.class),

    /**
     * Activity event category name.
     */
    Category(String.class),

    /**
     * Identifier used to uniquely identify parent activity associated with this activity.
     */
    ParentId(String.class);
```

**NOTE:** `EventType` field is mandatory and can't have value `null`. If this field value resolves to `null` then streams automatically sets 
value to `EVENT`. 

**NOTE:** Custom fields values can be found as activity event properties:

sample:
```xml
    <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
    <field name="Topic" locator="TopicName" locator-type="Label"/>
```

### Field locator types

```java
    /**
     * Indicates that raw data value is the value of a named property of the current stream.
     */
    StreamProp(String.class),

    /**
     * Indicates that raw data value is at a specified index location, offset, etc. This is a generic index/offset value whose 
     * interpretation is up to the specific parser applying the locator.
     */
   	Index(Integer.class),

    /**
     * Indicates that raw data value is the value of a particular key or label. Examples of this are XPath expressions for XML elements, 
     * and where each element of a raw activity data string is a name/value pair.
     */
    Label(String.class),

    /**
     * Indicates that raw data value is the value of a specific regular expression group, for parsers that interpret the raw activity data 
     * using a regular expression pattern defined as a sequence of groups.
     */
    REGroupNum(Integer.class),

    /**
     * Indicates that raw data value is the value of a specific regular expression match, for parsers that interpret the raw activity data 
     * using a regular expression pattern defined as a sequence of repeating match patterns.
     */
    REMatchNum(Integer.class),

    /**
     * Indicates that raw data value is the value of a specific regular expression group, for parsers that interpret the raw activity data 
     * using a regular expression pattern defined as a sequence of groups.
     */
    REGroupName(String.class),

    /**
     * Indicates that data value is the value from stream stored cache with specified cache entry key.
     */
    Cache(String.class),

    /**
     * Indicates that data value is the value from currently processed activity data entity with specified entity field name.
     */
    Activity(String.class);
```

**NOTE:** `Index` is default value and may be suppressed in field/locator definition:

this:
```xml
    <field name="UserName" locator="4"/>
```
is same as:
```xml
    <field name="UserName" locator="4" locator-type="Index"/>
```

### Stacked parsers

In stream parsers configuration You are allowed to use stacked parsers technique: it is when some field data parsed by
one parser can be forwarded to another parser to make more detailed parsing: envelope-message approach.

**NOTE:** activity event will contain all fields processed by all stacked parsers.

To define stacked parser You have to define `parser-ref` tad in parser `field` definition.

sample:
```xml
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <...>
    </parser>

    <parser name="SampleJMSParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJMSMessageParser">
        <...>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="AccessLogParserCommon"/>
        </field>
        <...>
    </parser>

    <stream name="SampleJMStream" class="com.jkoolcloud.tnt4j.streams.inputs.JMSStream">
        <...>
        <parser-ref name="SampleJMSParser"/>
    </stream>
</tnt-data-source>
```
See ['JMS text message'](#jms-text-message) sample for full configuration definition.

In this sample stream named `SampleJMStream` has primary parser reference `SampleJMSParser`. It parses data received
as JMS message (envelope). Field `MsgBody` carries JMS message payload data (message). In this sample consider we are
sending Apache Access log entry as JMS message payload. So to parse that Apache access log entry we use stacked parser
named `AccessLogParserCommon`.

After processing one JMS message TNT4J activity event will contain fields mapped by both `SampleJMSParser` and
`AccessLogParserCommon` in the end.

### Field value transformations

In streams configuration You can define field or locator resolved values transformations. In general transformations performs resolved 
activity value post-processing before sending it to JKool Cloud: e.g., extracts file name from resolved activity file path.

To pass resolved field/locator value to transformation script/expression use predefined variable placeholder `$fieldValue`.

#### Transformation definition

To define transformations stream configuration token `<field-transform>` shall be used. Attributes:
 * `name` - name of transformation (optional)
 * `lang` - transformation script/expression language. Can be one of `groovy`, `javascript` or `xpath`. Default value - `javascript`.
 * `beanRef` - transformation implementing bean reference
 
Token body is used to define transformation script/expression code.

Valid transformation configuration should define `beanRef`, or have script/expression code defined in token body data (`<![CDATA[]]>`).

##### TNT4J-Streams predefined custom XPath functions 

To use TNT4J-Streams predefined functions namespace `ts:` shall be used.

Streams predefined custom XPath functions to be used in transformation expressions:
* `ts:getFileName(filePath)` - implemented by transformation bean `com.jkoolcloud.tnt4j.streams.transform.FuncGetFileName`. Retrieves file 
name from provided file path.
* `ts:getObjectName(objectFQN, options)` - implemented by transformation bean `com.jkoolcloud.tnt4j.streams.transform.FuncGetObjectName`. 
Retrieves desired object name from provided fully qualified object name. Function supported options:
    * resolution options: `DEFAULT`, `BEFORE`, `AFTER`, `REPLACE`, `FULL`. (Optional)
    * search symbols. (Optional)
    * replacement symbols. (Optional)

You may also define your own customized XPath functions. To do this Your API has to:
* implement interface `javax.xml.xpath.XPathFunction`
* register function by invoking `com.jkoolcloud.tnt4j.streams.utils.StreamsXMLUtils.registerCustomFunction(functionName, function)`.

e.g.:
```java
public class YourTransform implements XPathFunction {
  @Override
   public Object evaluate(List args) {
    // retrieve expression code provided arguments and make transformation here. 
  }
}
...
StreamsXMLUtils.registerCustomFunction("yourTransformation", new YourTransform());
...
```
then You can use it from stream configuration:
```xml
<field name="InvoiceFileFromFunction" locator="7">
    <field-transform name="fileNameF" lang="xpath">
        ts:yourTransformation($fieldValue, "arg2", "arg3")
    </field-transform>
</field>
```

**NOTE:** those functions can be also used in XPath expressions of [ActivityXMLParser](#activity-xml-parser) (or [MessageActivityXMLParser](#message-activity-xml-parser)) 
field locators. For example: 
```xml
    <parser name="XMLParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <.../>
        <field name="MFT_SRC_FILE_NAME" locator="ts:getFileName(/transaction/transferSet/item/source/file)" locator-type="Label"/>
        <.../>
    </parser>
```

Use samples of `ts:getObjectName()` (consider XPath `/transaction/transferSet/item/source/queue` points to queue named `SomeQueueName@Agent1`):
```xml
    <parser name="XMLParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <.../>
        <!-- to get queue name without agent: 'SomeQueueName' -->
        <field name="MFT_SRC_QUEUE_NAME" locator="ts:getObjectName(/transaction/transferSet/item/source/queue)" locator-type="Label"/>
        <!-- to get queue name without agent (part before FQN delimiter): 'SomeQueueName'-->
        <field name="MFT_SRC_QUEUE_NAME" locator="ts:getObjectName(/transaction/transferSet/item/source/queue, 'BEFORE', '@')" locator-type="Label"/>
        <!-- to get agent name (part after FQN delimiter): 'Agent1'-->
        <field name="MFT_SRC_AGENT_NAME" locator="ts:getObjectName(/transaction/transferSet/item/source/queue, 'AFTER', '@')" locator-type="Label"/>
        <!-- to get queue fully qualified name with '@' replaced to '_': 'SomeQueueName_Agent1' -->
        <field name="MFT_SRC_QUEUE_FQN_REPLACED" locator="ts:getObjectName(/transaction/transferSet/item/source/queue, 'REPLACE', '@', '_')" locator-type="Label"/>
        <!-- to get queue fully qualified: 'SomeQueueName@Agent1'-->
        <field name="MFT_SRC_QUEUE_FQN" locator="ts:getObjectName(/transaction/transferSet/item/source/queue, 'FULL')" locator-type="Label"/>
        <.../>
    </parser>
```

#### Stream elements transformations 

* Field value transformation
```xml
<field name="Field" separator=",">
    <field-transform name="toUpper" lang="groovy">
        $fieldValue.toUpperCase()
    </field-transform>
    <field-locator locator="loc1" locator-type="Label"/>
    <field-locator locator="loc2" locator-type="Label"/>
    <...>
</field>
```
When transformation is defined for a field containing multiple locators, field value transformation is applied to all field locators 
aggregated field value. 

Sample above states that field value combined from locators `loc1` and `loc2` and separated by `,` should be upper cased. For example 
locator `loc1` resolves value `value1`, locator `loc2` resolves value `vaLue2`. Then field value before transformations is `value1,vaLue2` 
and field value after transformation is `VALUE1,VALUE2`.

* Field locator value transformation
```xml
<field name="Field" separator=",">
    <field-locator locator="loc1" locator-type="Label"/>
        <field-transform name="toUpper" lang="groovy">
            $fieldValue.toUpperCase()
        </field-transform>
    <field-locator locator="loc2" locator-type="Label"/>
        <field-transform name="toUpperConcat" lang="groovy">
            $fieldValue.toUpperCase() + "_transformed"
        </field-transform>
    <...>
</field>
```

Sample above states that locator `loc1` resolved value should be upper cased and locator `loc2` resolved value should be upper cased and 
concatenated with string `_transformed`. Then those transformed values should be aggregated to field value separated by `,` symbol. For 
example locator `loc1` resolved value `value1`, then after transformation value is changed to `VALUE1`. Locator `loc2` resolved value 
`value2`, then after transformation value is changed to `VALIUE2_transformed`. Field aggregates those locators values to 
`VALUE1,VALUE2_transformed`.

**NOTE:** 
* transformations are applied after field/locator value formatting.
* locator defined transformations are applied before field value transformations.
* it is possible to define multiple transformations for same stream element (field/locator). In that case transformations are applied 
sequentially where input of applied transformation is output of previous transformation. 
* it is allowed to combine field and locators transformations within same stream field scope setting transformations for both field and 
locators independently.
* when "short form" of field-locator configuration is used, transformation is bound to locator (because field-locator relation is 1:1 and 
resolved value is same for locator and for field). Example:
```xml
<field name="Correlator" locator="3">
    <field-transform name="concat">
        "corel_" + $fieldValue
    </field-transform>
</field>
```

#### Transformation definition samples

* Groovy script/expression
```xml
<field name="ApplName" value="orders">
    <field-transform name="toUpper" lang="groovy">
        $fieldValue.toUpperCase().center(30)
    </field-transform>
</field>
```
This sample upper case resolved value and makes "centred" (padded and tailed with space symbols) 30 symbols length string.

* JavaScript script/expression
```xml
<field name="UserName" locator="4">
    <field-transform name="toUpper" lang="javascript">
        $fieldValue.toUpperCase()
    </field-transform>
</field>
```
This sample upper case resolved value.

Or (using default attribute `lang` value) 
```xml
<field name="Correlator" locator="3">
    <field-transform name="concat">
        "corel_" + $fieldValue
    </field-transform>
</field>
```
This sample concatenates string `corel_` and resolved value.

* XPath expression
```xml
<field name="InvoiceFileFromFunction" locator="7">
    <field-transform name="fileNameF" lang="xpath">
        concat(ts:getFileName($fieldValue), "_cust")
    </field-transform>
</field>
```
This sample retrieves file name from resolved file path (contained in `$fieldValue`) and concatenates it with string `_cust`.

* Transformation bean

To make custom Java Bean based transformations Your API should implement interface `com.jkoolcloud.tnt4j.streams.transform.ValueTransformation<V, T>`.

```xml
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="tnt-data-source.xsd">
    <...>
    <java-object name="getFileNameTransform" class="com.jkoolcloud.tnt4j.streams.transform.FuncGetFileName"/>
    <...>
    <field name="InvoiceFileFromBean">
        <field-locator locator="7">
            <field-transform beanRef="getFileNameTransform"/>
        </field-locator>
    </field>
    <...>
```
This sample shows how to invoke Java Bean defined transformation. Class `com.jkoolcloud.tnt4j.streams.transform.FuncGetFileName` implements 
custom XPath function to get file name from provided file path. Field/locator value mapping to function arguments is performed automatically 
and there is no need to define additional mapping.

### Stream elements filtering
 
TODO
 
### Resolved activity entities aggregation
 
TODO 

##### Use of dynamic locators

TODO

### Caching of streamed data field values

`TNT4J-Streams` provides temporary storage i.e., cache for a resolved activity fields values. It is useful when there are some related 
activities streamed and particular JKool prepared activity entity requires data values form previously streamed activities.

Sample streamed values caching configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="EventParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ReadLines" value="false"/>

        <field name="EventType" value="EVENT"/>

        <field name="EventName" locator="$.event" locator-type="Label"/>
        <field name="LastEvent" locator="CachedEventName" locator-type="Cache"/>

        <field name="Correlator" locator="$.transaction" locator-type="Label"/>
        <field name="Transaction" locator="$.transaction" locator-type="Label"/>

        <field name="SecretValue" locator="$.secret" locator-type="Label"/>
        <field name="LastSecret" locator="SecretCache" locator-type="Cache"/>

        <field name="Message" locator="$.message" locator-type="Label"/>
        <field name="Resource" locator="$.resource" locator-type="Label"/>
    </parser>

    <stream name="MultipleEvents" class="com.jkoolcloud.tnt4j.streams.inputs.CharacterStream">
        <property name="FileName" value="./tnt4j-streams-core/samples/cached-values/event*.json"/>

        <property name="StreamCacheMaxSize" value="300"/>
        <property name="StreamCacheExpireDuration" value="15"/>

        <parser-ref name="EventParser"/>

        <cache>
            <entry id="CachedEventName">
                <key>EventName</key>
                <value>${EventName} in ${Transaction}</value>
            </entry>
            <entry id="SecretCache">
                <key>${Transaction}Secret</key>
                <value>${SecretValue}</value>
            </entry>
        </cache>
    </stream>

</tnt-data-source>
```

Sample configuration defines stream `MultipleEvents` reading data from JSON files using filename mask `event*.json` and referencing parser 
`EventParser`. Stream defines two cache related properties `StreamCacheMaxSize` and `StreamCacheExpireDuration`. Definitions of those 
properties can be found in chapter [Stream cache related parameters](#stream-cache-related-parameters). 

Stream definition has `cache` section, defining stored cache entries. Entry key and value can be configured using static values (e.g, 
`<key>EventName</key>`), dynamic activity value references (e.g., `<value>${SecretValue}</value>`) referencing activity entity field name, 
or combined static and dynamic values (e.g., `<value>${EventName} in ${Transaction}</value>`). Using dynamic value references in `<key>` 
definition will result having multiple cache entries filled in with values resolved from streamed activity data.

Parser `EventParser` has two fields having `locator-type="Cache"`. This means that values from those fields are resolved from stream cache 
entries referenced by `locator="SecretCache"` where locator value points to cache entry identifier. Having reference to cache entry, stream 
takes entry key definition and pre-fills it with current activity entity fields values making actual cache key value and thus mapping to 
particular cached value.

## Samples

### Running samples
When release assemblies are built, samples are located in `samples` directory, e.g., `../build/tnt4j-streams/tnt4j-streams-1.0.0/samples`.
To run particular sample:
* go to sample directory
* run `run.bat` or `run.sh` depending on Your OS

For more detailed explanation of streams and parsers configuration and usage see chapter ['Configuring TNT4J-Streams'](#configuring-tnt4j-streams)
and JavaDocs.

#### Single Log file

This sample shows how to stream activity events (orders) data from single log file.

Sample files can be found in `samples/single-log` directory.

`orders.log` file contains set of order activity events. Single file line defines data of single order activity event.

**NOTE:** records in this file are from year `2011` e.g., `12 Jul 2011`, so then getting events data in JKool Cloud
please do not forget to just to dashboard time frame to that period!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="TokenParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser">
        <property name="FieldDelim" value="|"/>
        <field name="StartTime" locator="1" format="dd MMM yyyy HH:mm:ss" locale="en-US"/>
        <field name="ServerIp" locator="2"/>
        <field name="ApplName" value="orders"/>
        <field name="Correlator" locator="3"/>
        <field name="UserName" locator="4"/>
        <field name="EventName" locator="5"/>
        <field name="EventType" locator="5">
            <field-map source="Order Placed" target="START"/>
            <field-map source="Order Received" target="RECEIVE"/>
            <field-map source="Order Processing" target="OPEN"/>
            <field-map source="Order Processed" target="SEND"/>
            <field-map source="Order Shipped" target="END"/>
        </field>
        <field name="MsgValue" locator="8"/>
    </parser>

    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        <property name="FileName" value="orders.log"/>
        <!--<property name="RangeToStream" value="1:"/>-->

        <parser-ref name="TokenParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `FileLineStream` referencing `TokenParser` shall be used.

`FileStream` reads data from `orders.log` file.

`TokenParser` uses `|` symbol as fields delimiter and maps fields to TNT4J event fields using field index locator.

**NOTE:** `StartTime` fields defines format and locale to correctly parse field data string. `EventType` uses manual
field string mapping to TNT4J event field value.

#### Multiple Log files

This sample shows how to stream activity events (orders) data from multiple log files using file name matching
wildcard pattern.

Sample files can be found in `samples/multiple-logs` directory.

`orders-in.log` and `orders-out.log` files contains set of order activity events. Single file line defines data of
single order activity event.

**NOTE:** records in this file are from year `2011` e.g., `12 Jul 2011`, so then getting events data in JKool Cloud
please do not forget to just to dashboard time frame to that period!

Sample configuration and sample idea is same as ['Single Log file'](#single-log-file) with one single difference:
```xml
    <property name="FileName" value="orders-*.log"/>
```
meaning that stream should process not one single file, but file set matching `orders-*.log` wildcard pattern.

#### OS piped stream

This sample shows how to stream activity events (orders) data received over OS pipe from another application or OS
command.

Sample files can be found in `samples/piping-stream` directory.

`orders.log` file contains set of order activity events. Single file line defines data of single order activity event.

**NOTE:** records in this file are from year `2011` e.g., `12 Jul 2011`, so then getting events data in JKool Cloud
please do not forget to just to dashboard time frame to that period!

`jk-pipe.bat` or `jk-pipe.sh` files are wrappers to `bin/tnt4j-streams` executables to minimize parameters. All what
 you need is to pass file name of stream parsers configuration, e.g., `parsers.xml`

`run.bat` or `run.sh` files uses OS piping to run sample.

Sample parser configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="TokenParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser">
        <property name="FieldDelim" value="|"/>
        <field name="StartTime" locator="1" format="dd MMM yyyy HH:mm:ss" locale="en-US"/>
        <field name="ServerIp" locator="2"/>
        <field name="ApplName" value="orders"/>
        <field name="Correlator" locator="3"/>
        <field name="UserName" locator="4"/>
        <field name="EventName" locator="5"/>
        <field name="EventType" locator="5">
            <field-map source="Order Placed" target="START"/>
            <field-map source="Order Received" target="RECEIVE"/>
            <field-map source="Order Processing" target="OPEN"/>
            <field-map source="Order Processed" target="SEND"/>
            <field-map source="Order Shipped" target="END"/>
        </field>
        <field name="MsgValue" locator="8"/>
    </parser>
</tnt-data-source>
```

For details on parser configuration see sample named ['Single Log file'](#single-log-file).

#### Zipped file lines

This sample shows how to stream activity events (Apache access log records) data from zipped file entries.

Sample files can be found in `samples/zip-stream` directory.

`sample.zip` and `sample.gz` files contains set of compressed Apache access log files.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserExt" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b %D"/>
        <property name="ConfRegexMapping" value="%*r=(((\S+) (.*?)( (\S+))?)|(-))"/>
        <property name="ConfRegexMapping" value="%*i=(.*?)"/>

        <field name="Location" locator="1" locator-type="REGroupNum"/>
        <field name="UserName" locator="3" locator-type="REGroupNum"/>
        <field name="StartTime" locator="4" locator-type="REGroupNum" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="REGroupNum"/>
        <field name="ResourceName" locator="8" locator-type="REGroupNum"/>
        <field name="CompCode" locator="12" locator-type="REGroupNum">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="REGroupNum"/>
        <field name="MsgValue" locator="13" locator-type="REGroupNum"/>
        <field name="ElapsedTime" locator="15" locator-type="REGroupNum" datatype="Number" format="#####0.000"
               locale="en-US" units="Seconds"/>
    </parser>

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>
        <property name="ConfRegexMapping" value="%*r=(((\S+) (.*?)( (\S+))?)|(-))"/>
        <property name="ConfRegexMapping" value="%*i=(.*?)"/>

        <field name="Location" locator="1" locator-type="REGroupNum"/>
        <field name="UserName" locator="3" locator-type="REGroupNum"/>
        <field name="StartTime" locator="4" locator-type="REGroupNum" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="REGroupNum"/>
        <field name="ResourceName" locator="8" locator-type="REGroupNum"/>
        <field name="CompCode" locator="12" locator-type="REGroupNum">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="REGroupNum"/>
        <field name="MsgValue" locator="13" locator-type="REGroupNum"/>
    </parser>

    <stream name="SampleZipFileStream" class="com.jkoolcloud.tnt4j.streams.inputs.ZipLineStream">
        <property name="FileName" value="./tnt4j-streams-core/samples/zip-stream/sample.zip"/>
        <!--<property name="FileName" value="./tnt4j-streams-core/samples/zip-stream/sample.zip!2/*.txt"/>-->
        <!--<property name="FileName" value="./tnt4j-streams-core/samples/zip-stream/sample.gz"/>-->
        <!--<property name="ArchType" value="GZIP"/>-->

        <parser-ref name="AccessLogParserExt"/>
        <parser-ref name="AccessLogParserCommon"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `ZipLineStream` referencing `AccessLogParserExt` and `AccessLogParserCommon` shall be
used.

`ZipLineStream` reads all entries lines from `sample.zip` file.

To filter zip file entries use zip entry name wildcard pattern, e.g., `sample.zip!2/*.txt`. In this case stream will read
just zipped files having extension `txt` from internal zip directory named `2` (from sub-directories also).

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parsers configuration section ['Apache access log parser'](#apache-access-log-parser).

#### Standard Java InputStream/Reader

This sample shows how to stream activity events (Apache access log records) data read from standard Java input.

Sample files can be found in `samples/java-stream` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserExt" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b %D"/>
        <property name="ConfRegexMapping" value="%*r=(((\S+) (.*?)( (\S+))?)|(-))"/>
        <property name="ConfRegexMapping" value="%*i=(.*?)"/>

        <field name="Location" locator="1" locator-type="REGroupNum"/>
        <field name="UserName" locator="3" locator-type="REGroupNum"/>
        <field name="StartTime" locator="4" locator-type="REGroupNum" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="REGroupNum"/>
        <field name="ResourceName" locator="8" locator-type="REGroupNum"/>
        <field name="CompCode" locator="12" locator-type="REGroupNum">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="REGroupNum"/>
        <field name="MsgValue" locator="13" locator-type="REGroupNum"/>
        <field name="ElapsedTime" locator="15" locator-type="REGroupNum" datatype="Number" format="#####0.000"
               locale="en-US" units="Seconds"/>
    </parser>

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>
        <property name="ConfRegexMapping" value="%*r=(((\S+) (.*?)( (\S+))?)|(-))"/>
        <property name="ConfRegexMapping" value="%*i=(.*?)"/>

        <field name="Location" locator="1" locator-type="REGroupNum"/>
        <field name="UserName" locator="3" locator-type="REGroupNum"/>
        <field name="StartTime" locator="4" locator-type="REGroupNum" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="REGroupNum"/>
        <field name="ResourceName" locator="8" locator-type="REGroupNum"/>
        <field name="CompCode" locator="12" locator-type="REGroupNum">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="REGroupNum"/>
        <field name="MsgValue" locator="13" locator-type="REGroupNum"/>
    </parser>

    <java-object name="SampleFileStream" class="java.io.FileInputStream">
        <param name="fileName" value="./tnt4j-streams-core/samples/zip-stream/sample.gz" type="java.lang.String"/>
    </java-object>
    <java-object name="SampleZipStream" class="java.util.zip.GZIPInputStream">
        <param name="stream" value="SampleFileStream" type="java.io.InputStream"/>
    </java-object>
    <!--java-object name="SampleFileReader" class="java.io.FileReader">
        <param name="fileName" value="./tnt4j-streams-core/samples/apache-access-single-log/access.log"
            type="java.lang.String"/>
    </java-object-->

    <stream name="SampleJavaInputStream" class="com.jkoolcloud.tnt4j.streams.inputs.JavaInputStream">
        <property name="HaltIfNoParser" value="false"/>

        <reference name="SampleZipStream"/>
        <!--reference name="SampleFileReader"/-->

        <reference name="AccessLogParserExt"/>
        <reference name="AccessLogParserCommon"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `SampleJavaInputStream` referencing `AccessLogParserExt` and `AccessLogParserCommon`
shall be used.

`SampleJavaInputStream` reads all lines from `SampleZipStream` referring FileInputStream `SampleFileStream` which reads
file `sample.gz`.

**NOTE:** that `java-object/param@value` value may be reference to configuration already defined object
like `SampleFileStream` in this particular sample.

To use `Reader` as input you should uncomment configuration lines defining and referring `SampleFileReader` and comment
out `SampleZipStream` reference.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parsers configuration section ['Apache access log parser'](#apache-access-log-parser).

#### Apache Access log single file

This sample shows how to stream Apache access log records as activity events from single log file.

Sample files can be found in `samples/apache-access-single-log` directory.

`access.log` is sample Apache access log file depicting some HTTP server activity.

**NOTE:** records in this file are from year `2004` e.g., `07/Mar/2004`, so then getting events data in JKool Cloud
please do not forget to just to dashboard time frame to that period!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserExt" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b %D"/>
        <!--property name="Pattern"
                  value="^(\S+) (\S+) (\S+) \[([\w:/]+\s[+\-]\d{4})\] &quot;(((\S+) (.*?)( (\S+)|()))|(-))&quot; (\d{3}) (\d+|-)( (\S+)|$)"/-->
        <property name="ConfRegexMapping" value="%*r=(((\S+) (.*?)( (\S+)|()))|(-))"/>
        <property name="ConfRegexMapping" value="%*i=(.*?)"/>

        <field name="Location" locator="1" locator-type="REGroupNum"/>
        <field name="UserName" locator="3" locator-type="REGroupNum"/>
        <field name="StartTime" locator="4" locator-type="REGroupNum" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="REGroupNum"/>
        <field name="ResourceName" locator="8" locator-type="REGroupNum"/>
        <field name="CompCode" locator="12" locator-type="REGroupNum">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="REGroupNum"/>
        <field name="MsgValue" locator="13" locator-type="REGroupNum"/>
        <field name="ElapsedTime" locator="15" locator-type="REGroupNum" datatype="Number" format="#####0.000" locale="en-US" units="Seconds"/>

    </parser>

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>
        <property name="ConfRegexMapping" value="%*r=(((\S+) (.*?)( (\S+)|()))|(-))"/>
        <property name="ConfRegexMapping" value="%*i=(.*?)"/>

        <field name="Location" locator="1" locator-type="REGroupNum"/>
        <field name="UserName" locator="3" locator-type="REGroupNum"/>
        <field name="StartTime" locator="4" locator-type="REGroupNum" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="REGroupNum"/>
        <field name="ResourceName" locator="8" locator-type="REGroupNum"/>
        <field name="CompCode" locator="12" locator-type="REGroupNum">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="REGroupNum"/>
        <field name="MsgValue" locator="13" locator-type="REGroupNum"/>

    </parser>

    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="FileName" value="access.log"/>
        <!--<property name="RangeToStream" value="1:"/>-->

        <parser-ref name="AccessLogParserExt"/>
        <parser-ref name="AccessLogParserCommon"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `FileLineStream` referencing `AccessLogParserExt` and `AccessLogParserCommon` shall
be used. Note that multiple parsers can be used to parse stream entries data, meaning that activity event data will be
made by first parser capable to parse entry data.

`FileStream` reads data from `access.log` file. `HaltIfNoParser` property states that stream should skip unparseable
entries and don't stop if such situation occurs.

`AccessLogParserCommon` parser is dedicated to parse Apache access log entries made using default logging configuration.

`LogPattern` defines logger pattern used to log entries to log file. Using this property parser is capable to
automatically build RegEx to parse log entry fields.

User is also allowed to manually define RegEx for log entry line using `Pattern` property.

`ConfRegexMapping` properties are used to allow user override default log pattern token-RegEx mappings and define those
manually to improve automatically build entry line RegEx.

Activity event fields mapping is performed using locator identifying RegEx pattern group index.

`AccessLogParserExt` is differs from `AccessLogParserCommon` just by having one additional log token `%D` in `LogPattern`
property.

So if for example half of log file was made using log pattern defined in `AccessLogParserCommon` parser `LogPattern`
property and the second part using log pater defined in `AccessLogParserExt` parser `LogPattern` property - stream
should be able to handle whole log file with no problems.

**NOTE:** `StartTime` fields defines format and locale to correctly parse field data string. `CompCode` uses manual
field string mapping to TNT4J event field value.

#### Apache Access log single file - named RegEx groups mappings

Sample does all the same as [Apache Access log single file](#apache-access-log-single-file), but uses named RegEx group mapping for a 
parser defined fields.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserExt" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b %D"/>
        <property name="ConfRegexMapping"><![CDATA[%*r=(?<request>((?<method>\S+) (?<uri>.*?)( (?<version>\S+))?)|(-))]]></property>

        <field name="Location" locator="hostname" locator-type="REGroupName"/>
        <field name="UserName" locator="user" locator-type="REGroupName"/>
        <field name="StartTime" locator="time" locator-type="REGroupName" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="method" locator-type="REGroupName"/>
        <field name="ResourceName" locator="uri" locator-type="REGroupName"/>
        <field name="CompCode" locator="status" locator-type="REGroupName">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="status" locator-type="REGroupName"/>
        <field name="MsgValue" locator="sizeClf" locator-type="REGroupName"/>
        <field name="ElapsedTime" locator="reqTime" locator-type="REGroupName" datatype="Number" format="#####0.000" locale="en-US"
               units="Seconds"/>
    </parser>

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>
        <property name="ConfRegexMapping"><![CDATA[%*r=(?<request>((?<method>\S+) (?<uri>.*?)( (?<version>\S+))?)|(-))]]></property>

        <field name="Location" locator="hostname" locator-type="REGroupName"/>
        <field name="UserName" locator="user" locator-type="REGroupName"/>
        <field name="StartTime" locator="time" locator-type="REGroupName" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="method" locator-type="REGroupName"/>
        <field name="ResourceName" locator="uri" locator-type="REGroupName"/>
        <field name="CompCode" locator="status" locator-type="REGroupName">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="status" locator-type="REGroupName"/>
        <field name="MsgValue" locator="sizeClf" locator-type="REGroupName"/>
    </parser>

    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="FileName" value="./tnt4j-streams-core/samples/apache-access-single-log/access.log"/>
        <property name="RestoreState" value="false"/>
        <!--<property name="RangeToStream" value="1:"/>-->

        <!--<property name="UseExecutors" value="true"/>-->
        <!--<property name="ExecutorThreadsQuantity" value="5"/>-->
        <!--<property name="ExecutorsTerminationTimeout" value="20"/>-->
        <!--<property name="ExecutorsBoundedModel" value="false"/>-->
        <!--<property name="ExecutorRejectedTaskOfferTimeout" value="20"/>-->

        <parser-ref name="AccessLogParserExt"/>
        <parser-ref name="AccessLogParserCommon"/>
    </stream>
</tnt-data-source>
```
See parsers configuration section ['Apache access log parser'](#apache-access-log-parser) for a default RegEx group names used by 
`ApacheAccessLogParser`. 

#### Apache Access log multiple files

This sample shows how to stream Apache access log records as activity events from multiple log files using file name
matching wildcard pattern.

Sample files can be found in `samples/apache-access-multi-log` directory.

`localhost_access_log.[DATE].txt` is sample Apache access log files depicting some HTTP server activity.

**NOTE:** records in this file are from year `2015` ranging from April until November, so then getting events data
in JKool Cloud please do not forget to just to dashboard time frame to that period!

Sample configuration and sample idea is same as ['Apache Access log single file'](#apache-access-log-single-file) with one single 
difference:
```xml
    <property name="FileName" value="*_access_log.2015-*.txt"/>
```
meaning that stream should process not one single file, but file set matching `*_access_log.2015-*.txt` wildcard
pattern.

#### Log file polling

This sample shows how to stream Apache access log records as activity events from file which is used for logging at
runtime. File polling technique may be used for any text file. File rolling is also supported.

Sample files can be found in `samples/log-file-polling` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b %D"/>

        <field name="Location" locator="1" locator-type="REGroupNum"/>
        <field name="UserName" locator="3" locator-type="REGroupNum"/>
        <field name="StartTime" locator="4" locator-type="REGroupNum" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="REGroupNum"/>
        <field name="ResourceName" locator="8" locator-type="REGroupNum"/>
        <field name="CompCode" locator="12" locator-type="REGroupNum">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="REGroupNum"/>
        <field name="MsgValue" locator="13" locator-type="REGroupNum"/>
        <field name="ElapsedTime" locator="14" locator-type="REGroupNum" datatype="Number" format="#####0.000" locale="en-US" units="Seconds"/>

    </parser>

    <stream name="SampleFilePollingStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        <property name="FileName"
                  value="[PATH_TO_LOGS_REPOSITORY]/logs/localhost_access_log.*.txt"/>
        <property name="FilePolling" value="true"/>
        <property name="FileReadDelay" value="20"/>
        <property name="StartFromLatest" value="true"/>
        <parser-ref name="AccessLogParserCommon"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `FilePollingStream` referencing `AccessLogParserCommon` shall be used.

`FileStream` reads data from `access.log` file. `HaltIfNoParser` property states that stream should skip unparseable
entries and don't stop if such situation occurs.

`AccessLogParserCommon` is same as in ['Apache Access log single file'](#apache-access-log-single-file) sample, so refer it for more details.

`FileName` property defines that stream should watch for files matching `localhost_access_log.*.txt` wildcard pattern.
 This is needed to properly handle file rolling.

`FilePolling` property indicates that stream polls files for changes.

`FileReadDelay` property indicates that file changes are streamed every 20 seconds.

`StartFromLatest` property indicates that stream should start from latest entry record in log file. Setting this
property to `false` would stream all log entries starting from oldest file matching wildcard pattern.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### HDFS

These samples shows how to read or poll HDFS files contents. Samples are very similar to ['Log file polling'](#log-file-polling) or
['Apache Access log single file'](#apache-access-log-single-file). Difference is that specialized stream classes are used.

* Simple HDFS file streaming

Sample files can be found in `tnt4j-streams/tnt4j-streams-hdfs/samples/hdfs-file-stream` directory.

```xml
    <stream name="SampleHdfsFileLineStream" class="com.jkoolcloud.tnt4j.streams.inputs.HdfsFileLineStream">
        <property name="FileName" value="hdfs://127.0.0.1:19000/log.txt*"/>
        <...>
    </stream>
```

To stream HDFS file lines `HdfsFileLineStream` shall be used. `FileName` is defined using URI starting `hdfs://`.

* HDFS file polling

Sample files can be found in `tnt4j-streams/tnt4j-streams-hdfs/samples/hdfs-log-file-polling` directory.

```xml
    <stream name="SampleHdfsFilePollingStream" class="com.jkoolcloud.tnt4j.streams.inputs.HdfsFileLineStream">
        <property name="FileName"
                  value="hdfs://[host]:[port]/[path]/logs/localhost_access_log.*.txt"/>
        <property name="FilePolling" value="true"/>
        <...>
    </stream>
```

To poll HDFS file `HdfsFileLineStream` shall be used with property `FilePolling` value set to `true`. `FileName` is
defined using URI starting `hdfs://`.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

* Zipped HDFS file streaming

Sample files can be found in `tnt4j-streams/tnt4j-streams-hdfs/samples/hdfs-zip-stream` directory.

```xml
    <stream name="SampleHdfsZipLineStream" class="com.jkoolcloud.tnt4j.streams.inputs.HdfsZipLineStream">
        <property name="FileName"
                  value="hdfs://[host]:[port]/[path]/sample.zip!2/*.txt"/>
        <property name="ArchType" value="ZIP"/>
        <...>
    </stream>
```

To stream HDFS zipped file lines `HdfsZipLineStream` shall be used. `FileName` is defined using URI starting `hdfs://`.

#### Apache Flume RAW data

This sample shows how to stream activity events from redirected Apache Flume output RAW data. Apache Flume output is
configured to send RAW output data as JSON to `localhost:9595`. Sample also shows how to use stacked parsers technique
to extract log entry data from JSON envelope.

Sample files can be found in `samples/apache-flume` directory.

How to install plugin into Apache Flume and configure, see [`samples/apache-flume/README.md`](tnt4j-streams-flume-plugin/samples/apache-flume/README.md)

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>

        <field name="Location" locator="1" locator-type="REGroupNum"/>
        <field name="UserName" locator="3" locator-type="REGroupNum"/>
        <field name="StartTime" locator="4" locator-type="REGroupNum" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="REGroupNum"/>
        <field name="ResourceName" locator="8" locator-type="REGroupNum"/>
        <field name="CompCode" locator="12" locator-type="REGroupNum">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="REGroupNum"/>
        <field name="MsgValue" locator="13" locator-type="REGroupNum"/>
    </parser>

    <parser name="JSONEnvelopeParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <!--<property name="ReadLines" value="false"/>-->

        <field name="MsgBody" locator="$.body" locator-type="Label">
            <parser-ref name="AccessLogParserCommon"/>
        </field>
        <field name="sinkName" locator="$.sinkName" locator-type="Label"/>
        <field name="chanelName" locator="$.chanelName" locator-type="Label"/>
        <field name="headers" locator="$.headers" locator-type="Label"/>
    </parser>

    <stream name="SampleFlumeStream" class="com.jkoolcloud.tnt4j.streams.inputs.CharacterStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Port" value="9595"/>
        <parser-ref name="JSONEnvelopeParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `CharacterStream` referencing `JSONEnvelopeParser` shall be used.

`CharacterStream` starts server socket on port defined using `Port` property. `HaltIfNoParser` property indicates that
stream should skip unparseable entries.

`JSONEnvelopeParser` transforms received JSON data package to Map with entries `MsgBody`, `sinkName`, `chanelName` and
`headers`. `MsgBody` entry value is passed to stacked parser named `AccessLogParserCommon`. `ReadLines` property
indicates that every line in parsed string represents single JSON data package.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parsers configuration section ['Apache access log parser'](#apache-access-log-parser).

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### Apache Flume Parsed Data

This sample shows how to stream activity events from Apache Flume parsed log entry output data. Apache Flume output is
configured to send parsed log entry data as JSON to `localhost:9595`.

Sample files can be found in `samples/apache-flume-parsed` directory.

How to install plugin into Apache Flume and configure, see [`samples/apache-flume-parsed/README.md`](tnt4j-streams-flume-plugin/samples/apache-flume-parsed/README.md)

`messages.json` file contains sample Apache Flume output JSON data package prepared using configuration of this sample.
This sample JSON is for you to see and better understand parsers mappings. Do not use it as Apache Flume input!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="FlumeJSONParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <!--<property name="ReadLines" value="false"/>-->

        <field name="Location" locator="$.headers.clientip" locator-type="Label"/>
        <field name="UserName" locator="$.headers.auth" locator-type="Label"/>
        <field name="StartTime" locator="$.headers.logtime" locator-type="Label" format="dd/MMM/yyyy:HH:mm:ss z"
               locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="$.headers.method" locator-type="Label"/>
        <field name="ResourceName" locator="$.headers.param" locator-type="Label"/>
        <field name="CompCode" locator="$.headers.response" locator-type="Label">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="$.headers.response" locator-type="Label"/>
        <field name="MsgValue" locator="$.headers.bytes" locator-type="Label"/>
        <field name="Message" locator="$.body" locator-type="Label"/>
        <field name="Tag" separator=",">
            <field-locator locator="$.sinkName" locator-type="Label"/>
            <field-locator locator="$.chanelName" locator-type="Label"/>
        </field>
    </parser>

    <stream name="SampleFlumeStream" class="com.jkoolcloud.tnt4j.streams.inputs.CharacterStream">
        <property name="Port" value="9595"/>
        <parser-ref name="FlumeJSONParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `CharacterStream` referencing `FlumeJSONParser` shall be used.

`CharacterStream` starts server socket on port defined using `Port` property.

`FlumeJSONParser` transforms received JSON data package to Map entries. Note that some entries like `headers` in map
has inner map as value. Fields of such entries can be accessed defining field name using `.` as field hierarchy
separator. `ReadLines` property indicates that every line in parsed string represents single JSON data package.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parsers configuration section ['Apache access log parser'](#apache-access-log-parser).

#### Logstash RAW data

This sample shows how to stream activity events from redirected Logstash output RAW data. Logstash output is
configured to send RAW output data as JSON to `localhost:9595`. Sample also shows how to use stacked parsers technique
to extract log entry data from JSON envelope.

Sample files can be found in `samples/logstash` directory.

How to setup sample environment see [`samples/logstash/README.MD`](tnt4j-streams-core/samples/logstash/README.MD)

`messages.json` file contains sample Logstash output JSON data package prepared using configuration of this sample. This
sample JSON is for you to see and better understand parsers mappings. Do not use it as Logstash input!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser"
            tags="Normal server,Delayed server">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>

        <field name="Location" locator="1" locator-type="REGroupNum"/>
        <field name="UserName" locator="3" locator-type="REGroupNum"/>
        <field name="StartTime" locator="4" locator-type="REGroupNum" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="REGroupNum"/>
        <field name="ResourceName" locator="8" locator-type="REGroupNum"/>
        <field name="CompCode" locator="12" locator-type="REGroupNum">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="REGroupNum"/>
        <field name="MsgValue" locator="13" locator-type="REGroupNum"/>
    </parser>

    <parser name="JSONEnvelopeParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <!--<property name="ReadLines" value="false"/>-->

        <field name="MsgBody" locator="$.message" locator-type="Label">
            <parser-ref name="AccessLogParserCommon"/>
        </field>
        <field name="path" locator="$.path" locator-type="Label"/>
        <field name="Tag" locator="$.tags" locator-type="Label"/>
        <field name="host" locator="$.host" locator-type="Label"/>
    </parser>

    <stream name="SampleLogstashStream" class="com.jkoolcloud.tnt4j.streams.inputs.CharacterStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="RestartOnInputClose" value="true"/>
        <property name="Port" value="9595"/>
        <parser-ref name="JSONEnvelopeParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `CharacterStream` referencing `JSONEnvelopeParser` shall be used.

`CharacterStream` starts server socket on port defined using `Port` property. `HaltIfNoParser` property indicates that
stream should skip unparseable entries.

`JSONEnvelopeParser` transforms received JSON data package to Map with entries `MsgBody`, `path`, `Tag` and `host`.
`MsgBody` entry value is passed to stacked parser named `AccessLogParserCommon`. `ReadLines` property indicates that
every line in parsed string represents single JSON data package.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parsers configuration section ['Apache access log parser'](#apache-access-log-parser).

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### Logstash parsed data

This sample shows how to stream activity events from parsed by Logstash. Logstash Grok output plugin is configured to
send parsed Apache Access log entry data as JSON to `localhost:9595`.

Sample files can be found in `samples/logstash-parsed` directory.

How to setup sample environment see [`samples/logstash-parsed/README.MD`](tnt4j-streams-core/samples/logstash-parsed/README.MD)

`messages.json` file contains sample Logstash output JSON data package prepared using configuration of this sample. This
sample JSON is for you to see and better understand parsers mappings. Do not use it as Logstash input!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="LogstashJSONParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <!--<property name="ReadLines" value="false"/>-->

        <field name="Location" locator="$.clientip" locator-type="Label"/>
        <field name="UserName" locator="$.auth" locator-type="Label"/>
        <field name="StartTime" locator="$.timestamp" locator-type="Label" format="dd/MMM/yyyy:HH:mm:ss z"
               locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="$.verb" locator-type="Label"/>
        <field name="ResourceName" locator="$.request" locator-type="Label"/>
        <field name="CompCode" locator="$.response" locator-type="Label">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="$.response" locator-type="Label"/>
        <field name="MsgValue" locator="$.bytes" locator-type="Label"/>
        <field name="Message" locator="$.message" locator-type="Label"/>
        <field name="Tag" locator="$.tags" locator-type="Label"/>
    </parser>

    <stream name="SampleLogstashStream" class="com.jkoolcloud.tnt4j.streams.inputs.CharacterStream">
        <property name="Port" value="9595"/>
        <parser-ref name="LogstashJSONParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `CharacterStream` referencing `LogstashJSONParser` shall be used.

`CharacterStream` starts server socket on port defined using `Port` property.

`LogstashJSONParser` transforms received JSON data package to Map data structure and maps map entries to activity
event fields using map entry key labels. `ReadLines` property indicates that every line in parsed string represents
single JSON data package.

#### HTTP request file

This sample shows how to stream activity events received over HTTP request as file. Sample also shows how to use
stacked parsers technique to extract message payload data.

Sample files can be found in `samples/http-file` directory.

Over HTTP sent sample file is `log.txt` - snapshot of Apache access log depicting some HTTP server activity.

How to send file data over HTTP see [`samples/http-file/README.md`](tnt4j-streams-core/samples/http-file/README.md)

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>

        <field name="Location" locator="1" locator-type="REGroupNum"/>
        <field name="UserName" locator="3" locator-type="REGroupNum"/>
        <field name="StartTime" locator="4" locator-type="REGroupNum" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="REGroupNum"/>
        <field name="ResourceName" locator="8" locator-type="REGroupNum"/>
        <field name="CompCode" locator="12" locator-type="REGroupNum">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="REGroupNum"/>
        <field name="MsgValue" locator="13" locator-type="REGroupNum"/>
    </parser>

    <parser name="SampleHttpReqParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="AccessLogParserCommon"/>
        </field>
    </parser>

    <stream name="SampleHttpStream" class="com.jkoolcloud.tnt4j.streams.inputs.HttpStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Port" value="8080"/>
        <!--<property name="UseSSL" value="true"/>-->
        <!--<property name="Keystore" value="path_to_keystore_file"/>-->
        <!--<property name="KeystorePass" value="somePassword"/>-->
        <!--<property name="KeyPass" value="somePassword"/>-->
        <parser-ref name="SampleHttpReqParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `HttpStream` referencing `SampleHttpReqParser` shall be used.

`HttpStream` starts HTTP server on port defined using `Port` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. Stream puts received request payload data as `byte[]` to map using key `ActivityData`.

`SampleHttpReqParser` by default converts `byte[]` for entry `ActivityData` to string and uses stacked parser named
`AccessLogParserCommon` to parse format.

`AccessLogParserCommon` is same as in ['Apache Access log single file'](#apache-access-log-single-file) sample, so refer it for more details.

**NOTE:** to parse some other data instead of Apache Access Log, replace `AccessLogParserCommon` with parser which
complies Your data format.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### HTTP request form

This sample shows how to stream activity events received over HTTP request as form data.

Sample files can be found in `samples/http-form` directory.

How to send HTTP form data see [`samples/http-form/README.md`](tnt4j-streams-core/samples/http-form/README.md)

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="SampleFormDataParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Location" locator="clientip" locator-type="Label"/>
        <field name="UserName" locator="auth" locator-type="Label"/>
        <field name="StartTime" locator="timestamp" locator-type="Label" format="dd/MMM/yyyy:HH:mm:ss z"
               locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="verb" locator-type="Label"/>
        <field name="ResourceName" locator="request" locator-type="Label"/>
        <field name="CompCode" locator="response" locator-type="Label">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="response" locator-type="Label"/>
        <field name="MsgValue" locator="bytes" locator-type="Label"/>
    </parser>

    <stream name="SampleHttpStream" class="com.jkoolcloud.tnt4j.streams.inputs.HttpStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Port" value="8080"/>
        <!--<property name="UseSSL" value="true"/>-->
        <!--<property name="Keystore" value="path_to_keystore_file"/>-->
        <!--<property name="KeystorePass" value="somePassword"/>-->
        <!--<property name="KeyPass" value="somePassword"/>-->
        <parser-ref name="SampleFormDataParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `HttpStream` referencing `SampleFormDataParser` shall be used.

`HttpStream` starts HTTP server on port defined using `Port` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. Stream puts received form parameters data to map and passes it to parser.

`SampleFormDataParser` performs form data mapping to TNT4J activity event data using form data parameters name labels.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### JMS text message

This sample shows how to stream activity events received over JMS transport as text messages. Sample also shows
how to use stacked parsers technique to extract message payload data.

Sample files can be found in `samples/jms-mapmessage` directory.

**NOTE:** in `jms` module `pom.xml` file uncomment `activemq-all` dependency if You want to use ActiveMQ as JMS service

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>

        <field name="Location" locator="1" locator-type="REGroupNum"/>
        <field name="UserName" locator="3" locator-type="REGroupNum"/>
        <field name="StartTime" locator="4" locator-type="REGroupNum" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="REGroupNum"/>
        <field name="ResourceName" locator="8" locator-type="REGroupNum"/>
        <field name="CompCode" locator="12" locator-type="REGroupNum">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="REGroupNum"/>
        <field name="MsgValue" locator="13" locator-type="REGroupNum"/>
    </parser>

    <parser name="SampleJMSParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJMSMessageParser">
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="AccessLogParserCommon"/>
        </field>
        <field name="Correlator" locator="Correlator" locator-type="Label"/>
    </parser>

    <stream name="SampleJMStream" class="com.jkoolcloud.tnt4j.streams.inputs.JMSStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="ServerURI" value="tcp://localhost:61616"/>
        <!--<property name="Queue" value="queue.SampleJMSQueue"/>-->
        <property name="Topic" value="topic.SampleJMSTopic"/>
        <property name="JNDIFactory" value="org.apache.activemq.jndi.ActiveMQInitialContextFactory"/>
        <property name="JMSConnFactory" value="ConnectionFactory"/>
        <parser-ref name="SampleJMSParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `JMSStream` referencing `SampleJMSParser` shall be used.

`JMSStream` connects to server defined using `ServerURI` property, and takes messages from topic defined
`Topic` property. To define wanted queue use `Queue` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. `JNDIFactory` property defines that ActiveMQ shall be used.
Stream puts received message data to map and passes it to parser.

`SampleJMSParser` maps metadata to activity event data. `ActivityData` entry value is passed to stacked parser named
`AccessLogParserCommon`.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parser configuration section ['Apache access log parser'](#apache-access-log-parser).

**NOTE:** to parse some other data instead of Apache Access Log, replace `AccessLogParserCommon` with parser which
complies Your data format.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### JMS map message

This sample shows how to stream activity events received over JMS transport as map messages.

Sample files can be found in `samples/jms-textmessage` directory.

**NOTE:** in `jms` module `pom.xml` file uncomment `activemq-all` dependency if You want to use ActiveMQ as JMS service

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="SampleJMSParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJMSMessageParser">
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="Location" locator="clientip" locator-type="Label"/>
        <field name="UserName" locator="auth" locator-type="Label"/>
        <field name="StartTime" locator="timestamp" locator-type="Label" format="dd/MMM/yyyy:HH:mm:ss z"
               locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="verb" locator-type="Label"/>
        <field name="ResourceName" locator="request" locator-type="Label"/>
        <field name="CompCode" locator="response" locator-type="Label">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="response" locator-type="Label"/>
        <field name="MsgValue" locator="bytes" locator-type="Label"/>
        <field name="Correlator" locator="Correlator" locator-type="Label"/>
    </parser>

    <stream name="SampleJMStream" class="com.jkoolcloud.tnt4j.streams.inputs.JMSStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="ServerURI" value="tcp://localhost:61616"/>
        <!--<property name="Queue" value="queue.SampleJMSQueue"/>-->
        <property name="Topic" value="topic.SampleJMSTopic"/>
        <property name="JNDIFactory" value="org.apache.activemq.jndi.ActiveMQInitialContextFactory"/>
        <property name="JMSConnFactory" value="ConnectionFactory"/>
        <parser-ref name="SampleJMSParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `JMSStream` referencing `SampleJMSParser` shall be used.

`JMSStream` connects to server defined using `ServerURI` property, and takes messages from topic defined
`Topic` property. To define wanted queue use `Queue` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. `JNDIFactory` property defines that ActiveMQ shall be used.
Stream puts received message data to map and passes it to parser.

`SampleJMSParser` maps activity event data from JMS map message using map entries key labels.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### JMS object message

This sample shows how to stream activity events received over JMS transport as serializable object messages. Sample
also shows how to use stacked parsers technique to extract message payload data.

Sample files can be found in `samples/jms-objectmessage` directory.

**NOTE:** in `jms` module `pom.xml` file uncomment `activemq-all` dependency if You want to use ActiveMQ as JMS service

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="SampleObjectParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJavaObjectParser">
        <field name="Location" locator="clientip" locator-type="Label"/>
        <field name="UserName" locator="auth" locator-type="Label"/>
        <field name="StartTime" locator="timestamp" locator-type="Label" format="dd/MMM/yyyy:HH:mm:ss z"
               locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="verb" locator-type="Label"/>
        <field name="ResourceName" locator="request" locator-type="Label"/>
        <field name="CompCode" locator="response" locator-type="Label">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="response" locator-type="Label"/>
        <field name="MsgValue" locator="bytes" locator-type="Label"/>
    </parser>

    <parser name="SampleJMSParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJMSMessageParser">
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="SampleObjectParser"/>
        </field>
        <field name="Correlator" locator="Correlator" locator-type="Label"/>
    </parser>

    <stream name="SampleJMStream" class="com.jkoolcloud.tnt4j.streams.inputs.JMSStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="ServerURI" value="tcp://localhost:61616"/>
        <!--<property name="Queue" value="queue.SampleJMSQueue"/>-->
        <property name="Topic" value="topic.SampleJMSTopic"/>
        <property name="JNDIFactory" value="org.apache.activemq.jndi.ActiveMQInitialContextFactory"/>
        <property name="JMSConnFactory" value="ConnectionFactory"/>
        <parser-ref name="SampleJMSParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `JMSStream` referencing `SampleJMSParser` shall be used.

`JMSStream` connects to server defined using `ServerURI` property, and takes messages from topic defined
`Topic` property. To define wanted queue use `Queue` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. `JNDIFactory` property defines that ActiveMQ shall be used.
Stream puts received message data to map and passes it to parser.

`SampleJMSParser` maps metadata to activity event data. `ActivityData` entry value is passed to stacked parser named
`SampleObjectParser`.

`SampleObjectParser` is able to map activity event fields values from serialized object declared fields using field
names as labels.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### Kafka client stream

This sample shows how to stream activity events received over Apache Kafka transport as messages. Sample also shows
how to use stacked parsers technique to extract message payload data.

Stream runs as Apache Kafka consumer.

Sample files can be found in `samples/kafka` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>

        <field name="Location" locator="1" locator-type="REGroupNum"/>
        <field name="UserName" locator="3" locator-type="REGroupNum"/>
        <field name="StartTime" locator="4" locator-type="REGroupNum" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="REGroupNum"/>
        <field name="ResourceName" locator="8" locator-type="REGroupNum"/>
        <field name="CompCode" locator="12" locator-type="REGroupNum">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="REGroupNum"/>
        <field name="MsgValue" locator="13" locator-type="REGroupNum"/>
    </parser>

    <parser name="KafkaMessageParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Topic" locator="ActivityTopic" locator-type="Label"/>
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="AccessLogParserCommon"/>
        </field>
    </parser>

    <stream name="SampleKafkaStream" class="com.jkoolcloud.tnt4j.streams.inputs.KafkaStream">
            <property name="HaltIfNoParser" value="false"/>
            <property name="Topic" value="TNT4JStreams"/>

            <!-- Kafka consumer properties -->
            <property name="zookeeper.connect" value="127.0.0.1:2181/tnt4j_kafka"/>
            <property name="group.id" value="TNT4JStreams"/>

            <parser-ref name="KafkaMessageParser"/>
        </stream>
</tnt-data-source>

```

Stream configuration states that `KafkaStream` referencing `KafkaMessageParser` shall be used.

`KafkaStream` connects to server defined using `zookeeper.connect` property, and takes messages from topic defined
`Topic` property. `HaltIfNoParser` property indicates that stream should skip unparseable entries.
Stream puts received message data to map and passes it to parser.

Details on ['Apache Kafka configuration'](https://kafka.apache.org/08/configuration.html).

`KafkaMessageParser` maps metadata to activity event data. `ActivityData` entry value is passed to stacked parser named
`AccessLogParserCommon`.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parsers configuration section ['Apache access log parser'](#apache-access-log-parser).

**NOTE:** to parse some other data instead of Apache Access Log, replace `AccessLogParserCommon` with parser which
complies Your data format.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### Kafka server stream

This sample shows how to stream activity events received over Apache Kafka transport as messages. Sample also shows
how to use stacked parsers technique to extract message payload data.

Stream starts Apache Kafka server and binds consumer to it to receive and process messages.

Sample files can be found in `samples/kafka-server` directory.

`tnt4j-streams-kafka/config` directory contains default configuration properties files for ZooKeeper (`zookeeper.propertoes`) and Kafka 
(`kafka-server.properties`) servers. Kafka stream is using those files referenced over Java system properties 
`-Dtnt4j.zookeeper.config=tnt4j-streams-kafka/config/zookeeper.properties` for ZooKeeper server and 
`-Dtnt4j.kafka.srv.config=tnt4j-streams-kafka/config/kafka-server.properties` for Kafka server (see `samples/kafka-server/run.bat` or `.sh` 
files as sample). **NOTE:** those file defined Kafka server properties gets merged with ones defined in stream configuration.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>

        <field name="Location" locator="1" locator-type="REGroupNum"/>
        <field name="UserName" locator="3" locator-type="REGroupNum"/>
        <field name="StartTime" locator="4" locator-type="REGroupNum" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="REGroupNum"/>
        <field name="ResourceName" locator="8" locator-type="REGroupNum"/>
        <field name="CompCode" locator="12" locator-type="REGroupNum">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="REGroupNum"/>
        <field name="MsgValue" locator="13" locator-type="REGroupNum"/>
    </parser>

    <parser name="KafkaMessageParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Topic" locator="ActivityTopic" locator-type="Label"/>
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="AccessLogParserCommon"/>
        </field>
    </parser>

    <stream name="SampleKafkaStream" class="com.jkoolcloud.tnt4j.streams.inputs.KafkaStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Topic" value="TNT4JStreams"/>
        <property name="StartServer" value="true"/>
        <property name="StartZooKeeper" value="true"/>

        <!-- Common Kafka server/consumer properties -->
        <property name="zookeeper.connect" value="127.0.0.1:2181/tnt4j_kafka"/>
        <property name="zookeeper.connection.timeout.ms" value="5000"/>

        <!-- Kafka consumer properties -->
        <property name="consumer:group.id" value="TNT4JStreams"/>
        <!--<property name="consumer:zookeeper.connection.timeout.ms" value="6000"/>-->
        <property name="consumer:consumer.timeout.ms" value="1000"/>
        <property name="consumer:auto.commit.interval.ms" value="1000"/>

        <!-- Kafka server properties -->
        <property name="server:broker.id" value="684231"/>
        <property name="server:host.name" value="localhost"/>
        <property name="server:port" value="9092"/>
        <!--<property name="server:log.flush.interval.messages" value="1"/>-->
        <!--<property name="server:log.flush.interval.ms" value="1000"/>-->
        <property name="server:zookeeper.session.timeout.ms" value="4000"/>
        <!--<property name="server:zookeeper.connection.timeout.ms" value="3000"/>-->
        <property name="server:zookeeper.sync.time.ms" value="200"/>

        <parser-ref name="KafkaMessageParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration in general is mostly same as for ['Kafka client stream'](#kafka-client-stream). 

Difference is `StartServer` stream property set to `true` stating that Apache Kafka server should be started by stream and messages consumer 
has to bind to it. `StartZooKeeper` stream property set to `true` states that ZooKeeper server shall be started on stream startup.

Also there is extended set of Apache Kafka configuration properties used by server and consumer. Kafka server dedicated properties starts 
with prefix `server:` while Kafka consumer properties - with `consumer:`. Properties having no prefix (or prefixes `:`, `common:`) are 
applied for both - server and consumer. Those properties gets merged with ones defined in properties files. **NOTE:** Stream configuration 
defined properties has priority over property files defined properties - thus property files defines **default** values used by stream. 

Details on ['Apache Kafka configuration'](https://kafka.apache.org/08/configuration.html). 

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### MQTT

This sample shows how to stream activity events received over MQTT transport as MQTT messages. Sample also shows how to
use stacked parsers technique to extract message payload data.

Sample files can be found in `samples/mqtt` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b"/>

        <field name="Location" locator="1" locator-type="REGroupNum"/>
        <field name="UserName" locator="3" locator-type="REGroupNum"/>
        <field name="StartTime" locator="4" locator-type="REGroupNum" format="dd/MMM/yyyy:HH:mm:ss z" locale="en-US"/>
        <field name="EventType" value="SEND"/>
        <field name="EventName" locator="7" locator-type="REGroupNum"/>
        <field name="ResourceName" locator="8" locator-type="REGroupNum"/>
        <field name="CompCode" locator="12" locator-type="REGroupNum">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="12" locator-type="REGroupNum"/>
        <field name="MsgValue" locator="13" locator-type="REGroupNum"/>
    </parser>

    <parser name="MqttMessageParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Topic" locator="ActivityTopic" locator-type="Label"/>
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="AccessLogParserCommon"/>
        </field>
    </parser>

    <stream name="SampleMQTTStream" class="com.jkoolcloud.tnt4j.streams.inputs.MqttStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="ServerURI" value="tcp://localhost:1883"/>
        <property name="TopicString" value="TNT4JStreams"/>
        <!--<property name="UserName" value="someUser"/>-->
        <!--<property name="Password" value="somePassword"/>-->
        <!--<property name="UseSSL" value="true"/>-->
        <!--<property name="Keystore" value="path_to_keystore_file"/>-->
        <!--<property name="KeystorePass" value="somePassword"/>-->

        <parser-ref name="MqttMessageParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `MqttStream` referencing `MqttMessageParser` shall be used.

`MqttStream` connects to server defined using `ServerURI` property, and takes messages from topic defined
`TopicString` property. `HaltIfNoParser` property indicates that stream should skip unparseable entries.
Stream puts received message data to map and passes it to parser.

`MqttMessageParser` maps metadata to activity event data. `ActivityData` entry value is passed to stacked parser named
`AccessLogParserCommon`.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in samples section 
['Apache Access log single file'](#apache-access-log-single-file) and parsers configuration section ['Apache access log parser'](#apache-access-log-parser).

**NOTE:** to parse some other data instead of Apache Access Log, replace `AccessLogParserCommon` with parser which
complies Your data format.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### WMQ Message broker

This sample shows how to stream activity events received over WMQ as message broker event messages.

Sample files can be found in `samples/message-broker` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <java-object name="MQMsgToStringPreParser" class="com.jkoolcloud.tnt4j.streams.preparsers.MQMessageToStringPreParser"/>

    <parser name="EventParser" class="com.jkoolcloud.tnt4j.streams.parsers.MessageActivityXmlParser">
        <property name="SignatureDelim" value="#!#"/>
        <property name="Namespace"
                  value="wmb=http://www.ibm.com/xmlns/prod/websphere/messagebroker/6.1.0/monitoring/event"/>

        <reference name="MQMsgToStringPreParser"/>

        <!--field name="ServerName" value="host-name-for-broker"/--> <!-- defaults to host name where jKool LLC TNT4J-Streams is running -->
        <!--field name="ServerName" locator="/wmb:event/wmb:eventPointData/wmb:messageFlowData/wmb:broker/@wmb:hostName" locator-type="Label"/--> <!-- when broker supports this -->
        <!--field name="ServerIp" locator="/wmb:event/wmb:eventPointData/ServerIp" locator-type="Label"/-->
        <field name="ApplName"
               locator="/wmb:event/wmb:eventPointData/wmb:messageFlowData/wmb:messageFlow/@wmb:uniqueFlowName"
               locator-type="Label"/>
        <field name="ResourceName" locator="/wmb:event/wmb:eventPointData/wmb:messageFlowData/wmb:node/@wmb:nodeLabel"
               locator-type="Label"/>
        <field name="Message" locator="/wmb:event/wmb:bitstreamData/wmb:bitstream" locator-type="Label"
               datatype="Binary"
               format="base64Binary"/>
        <field name="EventName" locator="/wmb:event/wmb:eventPointData/wmb:eventData/wmb:eventIdentity/@wmb:eventName"
               locator-type="Label"/>
        <field name="EventType" locator="/wmb:event/wmb:eventPointData/wmb:messageFlowData/wmb:node/@wmb:nodeType"
               locator-type="Label">
            <field-map source="ComIbmMQInputNode" target="RECEIVE"/>
            <field-map source="ComIbmMQOutputNode" target="SEND"/>
            <field-map source="ComIbmMQGetNode" target="RECEIVE"/>
            <field-map source="ComIbmJMSClientInputNode" target="RECEIVE"/>
            <field-map source="ComIbmJMSClientOutputNode" target="SEND"/>
            <field-map source="ComIbmJMSClientReplyNode" target="SEND"/>
            <field-map source="ComIbmJMSClientReceive" target="RECEIVE"/>
            <field-map source="ComIbmJMSHeader.msgnode" target="RECEIVE"/>
            <field-map source="ComIbmHTTPAsyncRequest" target="RECEIVE"/>
            <field-map source="ComIbmHTTPAsyncResponse" target="SEND"/>
            <field-map source="ComIbmHTTPHeader" target="RECEIVE"/>
            <field-map source="ComIbmWSInputNode" target="RECEIVE"/>
            <field-map source="ComIbmWSReplyNode" target="SEND"/>
            <field-map source="ComIbmWSRequestNode" target="RECEIVE"/>
            <field-map source="ComIbmSOAPInputNode" target="RECEIVE"/>
            <field-map source="ComIbmSOAPReplyNode" target="SEND"/>
            <field-map source="ComIbmSOAPRequestNode" target="RECEIVE"/>
            <field-map source="ComIbmSOAPAsyncRequestNode" target="RECEIVE"/>
            <field-map source="ComIbmSOAPAsyncResponseNode" target="SEND"/>
            <field-map source="ComIbmSOAPWrapperNode" target="CALL"/>
            <field-map source="ComIbmSOAPExtractNode" target="CALL"/>
            <field-map source="SRRetrieveEntityNode" target="CALL"/>
            <field-map source="SRRetrieveITServiceNode" target="CALL"/>
            <field-map source="ComIbmDatabaseInputNode" target="RECEIVE"/>
            <field-map source="ComIbmDatabaseNode" target="CALL"/>
            <field-map source="ComIbmDatabaseRetrieveNode" target="RECEIVE"/>
            <field-map source="ComIbmDatabaseRouteNode" target="SEND"/>
            <field-map source="ComIbmFileInputNode" target="RECEIVE"/>
            <field-map source="ComIbmFileReadNode" target="CALL"/>
            <field-map source="ComIbmFileOutputNode" target="SEND"/>
            <field-map source="ComIbmFTEInputNode" target="RECEIVE"/>
            <field-map source="ComIbmFTEOutputNode" target="SEND"/>
            <field-map source="ComIbmTCPIPClientInputNode" target="RECEIVE"/>
            <field-map source="ComIbmTCPIPClientOutputNode" target="SEND"/>
            <field-map source="ComIbmTCPIPClientRequestNode" target="RECEIVE"/>
            <field-map source="ComIbmTCPIPServerInputNode" target="RECEIVE"/>
            <field-map source="ComIbmTCPIPServerOutputNode" target="SEND"/>
            <field-map source="ComIbmTCPIPServerRequestNode" target="RECEIVE"/>
            <field-map source="ComIbmCORBARequestNode" target="RECEIVE"/>
            <field-map source="" target="CALL"/>
        </field>
        <field name="Correlator"
               locator="/wmb:event/wmb:eventPointData/wmb:eventData/wmb:eventCorrelation/@wmb:localTransactionId"
               locator-type="Label"/>
        <field name="ElapsedTime" value="0" datatype="Number"/>
        <field name="EndTime" locator="/wmb:event/wmb:eventPointData/wmb:eventData/wmb:eventSequence/@wmb:creationTime"
               locator-type="Label"
               datatype="DateTime" format="yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'" timezone="GMT"/>
        <!--field name="ReasonCode" locator="/wmb:event/wmb:eventPointData/ReasonCode" locator-type="Label" datatype="Number"/-->
        <!-- *** Use following signature definition for WMQ messages ***
        <field name="TrackingId" separator="#!#">
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='MsgType']/@wmb:value" locator-type="Label" datatype="Number"/>
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='Format']/@wmb:value" locator-type="Label"/>
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='MsgId']/@wmb:value" locator-type="Label" datatype="Binary" format="hexBinary"/>
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='UserIdentifier']/@wmb:value" locator-type="Label"/>
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='PutApplType']/@wmb:value" locator-type="Label"/>
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='PutApplName']/@wmb:value" locator-type="Label"/>
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='PutDate']/@wmb:value" locator-type="Label"/>
          <field-locator locator="/wmb:event/wmb:applicationData/wmb:simpleContent[@wmb:name='PutTime']/@wmb:value" locator-type="Label"/>
        </field>
        -->
        <!--field name="StartTime" locator="/wmb:event/wmb:eventPointData/wmb:eventData/wmb:eventSequence/@wmb:creationTime" locator-type="Label" datatype="DateTime" format="yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'" timestamp="GMT"/-->
        <field name="CompCode" locator="/wmb:event/wmb:eventPointData/wmb:eventData/wmb:eventIdentity/@wmb:eventName"
               locator-type="Label">
            <field-map source="FlowRollback" target="ERROR"/>
            <field-map source="" target="SUCCESS"/>
        </field>
        <!--field name="Tag" locator="/wmb:event/wmb:eventPointData/Tag" locator-type="Label"/-->
        <!--field name="UserName" locator="/wmb:event/wmb:eventPointData/UserName" locator-type="Label"/-->
    </parser>

    <stream name="EventStream" class="com.jkoolcloud.tnt4j.streams.inputs.WmqStream">
        <property name="QueueManager" value="QMGR"/>
        <property name="Queue" value="EVENT.QUEUE"/>
        <parser-ref name="EventParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `WmqStream` referencing `EventParser` shall be used. Stream deserialize message to
string and passes it to parser.

`QueueManager` property defines name of queue manager and `Queue` property defines name of queue to get messages.

`EventParser` is of type `MessageActivityXmlParser` meaning that it will parse messages de-serialized into XML strings.

`SignatureDelim` property defines that `#!#` should be used as signature delimiter.

`Namespace` property adds `wmb` namespace definition mapping to mapping
`http://www.ibm.com/xmlns/prod/websphere/messagebroker/6.1.0/monitoring/event`.

#### WMQ PCF messages streaming

This sample shows how to stream activity events received over WMQ as PCF messages.

Sample files can be found in `samples/pcf-message` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-wmq/config/tnt-data-source-wmq_pcf.xsd">

    <parser name="PCFEventsParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityPCFParser">
        <property name="TranslateNumValues" value="true"/>

        <field name="EventType" value="EVENT"/>

        <!-- header fields -->
        <field name="Command" locator="MQCFH.Command" locator-type="Label"/>
        <field name="MsgSeqNumber" locator="MQCFH.MsgSeqNumber" locator-type="Label"/>
        <field name="Control" locator="MQCFH.Control" locator-type="Label"/>
        <field name="CompCode" locator="MQCFH.CompCode" locator-type="Label">
            <field-map source="0" target="SUCCESS"/>
            <field-map source="1" target="WARNING"/>
            <field-map source="MQCC_OK" target="SUCCESS"/>
            <field-map source="MQCC_WARNING" target="WARNING"/>
            <field-map source="" target="ERROR"/>
        </field>
        <field name="ReasonCode" locator="MQCFH.Reason" locator-type="Label" datatype="Number"/>
        <field name="ParameterCount" locator="MQCFH.ParameterCount" locator-type="Label"/>

        <!-- message fields -->
        <field name="QMgrName" locator="MQCA_Q_MGR_NAME" locator-type="Label"/>
        <field name="ServerName" locator="MQCACF_HOST_NAME" locator-type="Label"/>
        <field name="StartTime" separator=" " datatype="DateTime" format="yyyy-MM-dd HH:mm:ss">
            <field-locator locator="MQCAMO_START_DATE" locator-type="Label"/>
            <field-locator locator="MQCAMO_START_TIME" locator-type="Label"/>
        </field>
        <field name="EndTime" separator=" " datatype="DateTime" format="yyyy-MM-dd HH:mm:ss">
            <field-locator locator="MQCAMO_END_DATE" locator-type="Label"/>
            <field-locator locator="MQCAMO_END_TIME" locator-type="Label"/>
        </field>
        <field name="CommandLevel" locator="MQIA_COMMAND_LEVEL" locator-type="Label"/>
        <field name="SequenceNumber" locator="MQIACF_SEQUENCE_NUMBER" locator-type="Label"/>
        <field name="ApplName" locator="MQCACF_APPL_NAME" locator-type="Label"/>
        <field name="ApplType" locator="MQIA_APPL_TYPE" locator-type="Label"/>
        <field name="ProcessId" locator="MQIACF_PROCESS_ID" locator-type="Label"/>
        <field name="UserId" locator="MQCACF_USER_IDENTIFIER" locator-type="Label"/>
        <field name="ApiCallerType" locator="MQIACF_API_CALLER_TYPE" locator-type="Label"/>
        <field name="ApiEnv" locator="MQIACF_API_ENVIRONMENT" locator-type="Label"/>
        <field name="ApplFunction" locator="MQCACF_APPL_FUNCTION" locator-type="Label"/>
        <field name="ApplFunctionType" locator="MQIACF_APPL_FUNCTION_TYPE" locator-type="Label"/>
        <field name="TraceDetail" locator="MQIACF_TRACE_DETAIL" locator-type="Label"/>
        <field name="TraceDataLength" locator="MQIACF_TRACE_DATA_LENGTH" locator-type="Label"/>
        <field name="PointerSize" locator="MQIACF_POINTER_SIZE" locator-type="Label"/>
        <field name="Platform" locator="MQIA_PLATFORM" locator-type="Label"/>
        <field name="Correlator" locator="MQBACF_CORREL_ID" locator-type="Label" datatype="Binary"/>
    </parser>

    <stream name="WmqStreamPCF" class="com.jkoolcloud.tnt4j.streams.inputs.WmqStreamPCF">
        <property name="Host" value="[YOUR_MQ_HOST]"/>
        <property name="Port" value="[YOUR_MQ_PORT]"/>
        <property name="QueueManager" value="[YOUR_QM_NAME]"/>
        <property name="Queue" value="[YOUR_Q_NAME]"/>
        <property name="StripHeaders" value="false"/>

        <parser-ref name="PCFEventsParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `WmqStreamPCF` referencing `PCFEventsParser` shall be used. Stream takes MQ message from QM, transforms it 
to PCF message and passes it to parser.

`Host` property defines MQ server host name or IP. `Port` property defines MQ server port.

`QueueManager` property defines name of queue manager and `Queue` property defines name of queue to get messages.

`StripHeaders` property states that MQ message headers shall be preserved. 

`PCFEventsParser` is of type `ActivityPCFParser` meaning that it will parse PCF messages.

`TranslateNumValues` property defines that parser should translate resolved numeric values to corresponding MQ constant names if possible.

#### WMQ Trace Events streaming

This sample shows how to stream activity events received over WMQ as MQ Trace Events.

Sample files can be found in `samples/trace-events` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-wmq/config/tnt-data-source-wmq_pcf.xsd">

    <parser name="TraceEventsParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityPCFParser">
        <property name="TranslateNumValues" value="true"/>

        <field name="EventType" value="EVENT"/>

        <!-- message fields -->
        <field name="QMgrName" locator="MQCA_Q_MGR_NAME" locator-type="Label"/>
        <field name="ServerName" locator="MQCACF_HOST_NAME" locator-type="Label"/>
        <field name="StartTime" separator=" " datatype="DateTime" format="yyyy-MM-dd HH:mm:ss">
            <field-locator locator="MQCAMO_START_DATE" locator-type="Label"/>
            <field-locator locator="MQCAMO_START_TIME" locator-type="Label"/>
        </field>
        <field name="EndTime" separator=" " datatype="DateTime" format="yyyy-MM-dd HH:mm:ss">
            <field-locator locator="MQCAMO_END_DATE" locator-type="Label"/>
            <field-locator locator="MQCAMO_END_TIME" locator-type="Label"/>
        </field>
        <field name="CommandLevel" locator="MQIA_COMMAND_LEVEL" locator-type="Label"/>
        <field name="SequenceNumber" locator="MQIACF_SEQUENCE_NUMBER" locator-type="Label"/>
        <field name="ApplName" locator="MQCACF_APPL_NAME" locator-type="Label"/>
        <field name="ApplType" locator="MQIA_APPL_TYPE" locator-type="Label"/>
        <field name="ProcessId" locator="MQIACF_PROCESS_ID" locator-type="Label"/>
        <field name="UserId" locator="MQCACF_USER_IDENTIFIER" locator-type="Label"/>
        <field name="ApiCallerType" locator="MQIACF_API_CALLER_TYPE" locator-type="Label"/>
        <field name="ApiEnv" locator="MQIACF_API_ENVIRONMENT" locator-type="Label"/>
        <field name="ApplFunction" locator="MQCACF_APPL_FUNCTION" locator-type="Label"/>
        <field name="ApplFunctionType" locator="MQIACF_APPL_FUNCTION_TYPE" locator-type="Label"/>
        <field name="TraceDetail" locator="MQIACF_TRACE_DETAIL" locator-type="Label"/>
        <field name="TraceDataLength" locator="MQIACF_TRACE_DATA_LENGTH" locator-type="Label"/>
        <field name="PointerSize" locator="MQIACF_POINTER_SIZE" locator-type="Label"/>
        <field name="Platform" locator="MQIA_PLATFORM" locator-type="Label"/>
        <field name="Correlator" locator="MQBACF_CORREL_ID" locator-type="Label" datatype="Binary"/>

        <!-- activity trace fields -->
        <field name="EventName" locator="MQGACF_ACTIVITY_TRACE.MQIACF_OPERATION_ID" locator-type="Label"/>
        <field name="ThreadId" locator="MQGACF_ACTIVITY_TRACE.MQIACF_THREAD_ID" locator-type="Label"/>
        <field name="OperationTime" separator=" " datatype="DateTime" format="yyyy-MM-dd HH:mm:ss">
            <field-locator locator="MQGACF_ACTIVITY_TRACE.MQCACF_OPERATION_DATE" locator-type="Label"/>
            <field-locator locator="MQGACF_ACTIVITY_TRACE.MQCACF_OPERATION_TIME" locator-type="Label"/>
        </field>
        <field name="MQTrace.ObjType" locator="MQGACF_ACTIVITY_TRACE.MQIACF_OBJECT_TYPE" locator-type="Label"/>
        <field name="ResourceName" separator="/">
            <field-locator locator="MQGACF_ACTIVITY_TRACE.MQCACF_RESOLVED_Q_MGR" locator-type="Label"/>
            <field-locator locator="MQGACF_ACTIVITY_TRACE.MQCACF_OBJECT_NAME" locator-type="Label"/>
        </field>
        <field name="MQTrace.ObjQMgrName" locator="MQGACF_ACTIVITY_TRACE.MQCACF_OBJECT_Q_MGR_NAME" locator-type="Label"/>
        <field name="MQTrace.ObjHandle" locator="MQGACF_ACTIVITY_TRACE.MQIACF_HOBJ" locator-type="Label"/>
        <field name="CompCode" locator="MQGACF_ACTIVITY_TRACE.MQIACF_COMP_CODE" locator-type="Label">
            <field-map source="0" target="SUCCESS"/>
            <field-map source="1" target="WARNING"/>
            <field-map source="MQCC_OK" target="SUCCESS"/>
            <field-map source="MQCC_WARNING" target="WARNING"/>
            <field-map source="" target="ERROR"/>
        </field>
        <field name="ReasonCode" locator="MQGACF_ACTIVITY_TRACE.MQIACF_REASON_CODE" locator-type="Label" datatype="Number"/>
        <field name="MQTrace.ConnectOptions" locator="MQGACF_ACTIVITY_TRACE.MQIACF_CONNECT_OPTIONS" locator-type="Label"/>
        <field name="MQTrace.OpenOptions" locator="MQGACF_ACTIVITY_TRACE.MQIACF_OPEN_OPTIONS" locator-type="Label"/>
        <field name="MQTrace.GetOptions" locator="MQGACF_ACTIVITY_TRACE.MQIACF_GET_OPTIONS" locator-type="Label"/>
        <field name="MQTrace.PutOptions" locator="MQGACF_ACTIVITY_TRACE.MQIACF_PUT_OPTIONS" locator-type="Label"/>
        <field name="MQTrace.CloseOptions" locator="MQGACF_ACTIVITY_TRACE.MQIACF_CLOSE_OPTIONS" locator-type="Label"/>
        <field name="MQTrace.ResolvedQName" locator="MQGACF_ACTIVITY_TRACE.MQCACF_RESOLVED_Q_NAME" locator-type="Label"/>
        <field name="MQTrace.ResolvedLocalQName" locator="MQGACF_ACTIVITY_TRACE.MQCACF_RESOLVED_LOCAL_Q_NAME" locator-type="Label"/>
        <field name="MQTrace.ResolvedLocalQMgr" locator="MQGACF_ACTIVITY_TRACE.MQCACF_RESOLVED_LOCAL_Q_MGR" locator-type="Label"/>
        <field name="MQTrace.ResolvedType" locator="MQGACF_ACTIVITY_TRACE.MQIACF_RESOLVED_TYPE" locator-type="Label"/>
        <field name="MQTrace.DynamicQName" locator="MQGACF_ACTIVITY_TRACE.MQCACF_DYNAMIC_Q_NAME" locator-type="Label"/>
        <field name="MQTrace.MsgLength" locator="MQGACF_ACTIVITY_TRACE.MQIACF_MSG_LENGTH" locator-type="Label"/>
        <field name="MQTrace.HighresTime" locator="MQGACF_ACTIVITY_TRACE.MQIAMO64_HIGHRES_TIME" locator-type="Label" datatype="Timestamp"
               units="Microseconds"/>
        <field name="MQTrace.BufferLength" locator="MQGACF_ACTIVITY_TRACE.MQIACF_BUFFER_LENGTH" locator-type="Label"/>
        <field name="MQTrace.Report" locator="MQGACF_ACTIVITY_TRACE.MQIACF_REPORT" locator-type="Label"/>
        <field name="MQTrace.MsgType" locator="MQGACF_ACTIVITY_TRACE.MQIACF_MSG_TYPE" locator-type="Label"/>
        <field name="MQTrace.Expiry" locator="MQGACF_ACTIVITY_TRACE.MQIACF_EXPIRY" locator-type="Label"/>
        <field name="MQTrace.FormatName" locator="MQGACF_ACTIVITY_TRACE.MQCACH_FORMAT_NAME" locator-type="Label"/>
        <field name="MQTrace.Priority" locator="MQGACF_ACTIVITY_TRACE.MQIACF_PRIORITY" locator-type="Label"/>
        <field name="MQTrace.Persistence" locator="MQGACF_ACTIVITY_TRACE.MQIACF_PERSISTENCE" locator-type="Label"/>
        <field name="MQTrace.MsgId" locator="MQGACF_ACTIVITY_TRACE.MQBACF_MSG_ID" locator-type="Label" datatype="Binary"/>
        <field name="Correlator" locator="MQGACF_ACTIVITY_TRACE.MQBACF_CORREL_ID" locator-type="Label" datatype="Binary"/>
        <field name="MQTrace.ReplyToQ" locator="MQGACF_ACTIVITY_TRACE.MQCACF_REPLY_TO_Q" locator-type="Label"/>
        <field name="MQTrace.ReplyToQMgr" locator="MQGACF_ACTIVITY_TRACE.MQCACF_REPLY_TO_Q_MGR" locator-type="Label"/>
        <field name="MQTrace.CodedCharsetId" locator="MQGACF_ACTIVITY_TRACE.MQIA_CODED_CHAR_SET_ID" locator-type="Label"/>
        <field name="MQTrace.Encoding" locator="MQGACF_ACTIVITY_TRACE.MQIACF_ENCODING" locator-type="Label"/>
        <field name="PutTime" separator=" " datatype="DateTime" format="yyyyMMdd HHmmss">
            <field-locator locator="MQGACF_ACTIVITY_TRACE.MQCACF_PUT_DATE" locator-type="Label"/>
            <field-locator locator="MQGACF_ACTIVITY_TRACE.MQCACF_PUT_TIME" locator-type="Label"/>
        </field>
        <field name="MQTrace.SelectorCount" locator="MQGACF_ACTIVITY_TRACE.MQIACF_SELECTOR_COUNT" locator-type="Label"/>
        <field name="MQTrace.Selectors" locator="MQGACF_ACTIVITY_TRACE.MQIACF_SELECTORS" locator-type="Label"/> <!-- int array -->
        <field name="MQTrace.ConnectionId" locator="MQGACF_ACTIVITY_TRACE.MQBACF_CONNECTION_ID" locator-type="Label" datatype="Binary"/>
        <field name="MQTrace.QMgrName" locator="MQGACF_ACTIVITY_TRACE.MQCA_Q_MGR_NAME" locator-type="Label"/>
        <field name="MQTrace.ObjNameRecsPresent" locator="MQGACF_ACTIVITY_TRACE.MQIACF_RECS_PRESENT" locator-type="Label"/>
        <field name="MQTrace.CallType" locator="MQGACF_ACTIVITY_TRACE.MQIACF_CALL_TYPE" locator-type="Label"/>
        <field name="MQTrace.CtlOperation" locator="MQGACF_ACTIVITY_TRACE.MQIACF_CTL_OPERATION" locator-type="Label"/>
        <field name="MQTrace.MQCallbackType" locator="MQGACF_ACTIVITY_TRACE.MQIACF_MQCB_TYPE" locator-type="Label"/>
        <field name="MQTrace.MQCallbackName" locator="MQGACF_ACTIVITY_TRACE.MQCACF_MQCB_NAME" locator-type="Label"/>
        <field name="MQTrace.MQCallbackFunction" locator="MQGACF_ACTIVITY_TRACE.MQBACF_MQCB_FUNCTION" locator-type="Label"
               datatype="Binary"/>
        <field name="MQTrace.MQCallbackOptions" locator="MQGACF_ACTIVITY_TRACE.MQIACF_MQCB_OPTIONS" locator-type="Label"/>
        <field name="MQTrace.MQCallbackOperation" locator="MQGACF_ACTIVITY_TRACE.MQIACF_MQCB_OPERATION" locator-type="Label"/>
        <field name="MQTrace.InvalidDestCount" locator="MQGACF_ACTIVITY_TRACE.MQIACF_INVALID_DEST_COUNT" locator-type="Label"/>
        <field name="MQTrace.UnknownDestCount" locator="MQGACF_ACTIVITY_TRACE.MQIACF_UNKNOWN_DEST_COUNT" locator-type="Label"/>
        <field name="MQTrace.MaxMsgLength" locator="MQGACF_ACTIVITY_TRACE.MQIACH_MAX_MSG_LENGTH" locator-type="Label"/>
    </parser>

    <stream name="WmqActivityTraceStream" class="com.jkoolcloud.tnt4j.streams.custom.inputs.WmqTraceStream">
        <property name="Host" value="[YOUR_MQ_HOST]"/>
        <property name="Port" value="[YOUR_MQ_PORT]"/>
        <property name="QueueManager" value="[YOUR_QM_NAME]"/>
        <property name="Queue" value="SYSTEM.ADMIN.TRACE.ACTIVITY.QUEUE"/>
        <property name="StripHeaders" value="false"/>

        <property name="TraceOperations" value="*"/>
        <!--<property name="TraceOperations" value="MQXF_*"/>-->
        <!--<property name="TraceOperations" value="MQXF_(GET|PUT|CLOSE)"/>-->
        <!--<property name="ExcludedRC" value="MQRC_NO_MSG_AVAILABLE|30737"/>-->
        <property name="SuppressBrowseGets" value="true"/>

        <parser-ref name="TraceEventsParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `WmqActivityTraceStream` referencing `TraceEventsParser` shall be used. Stream takes MQ message from QM, 
transforms it to PCF message and passes it to parser. If PCF message contains multiple MQ Trace Events, then multiple activity events will 
be made from it. 

`Host` property defines MQ server host name or IP. `Port` property defines MQ server port.

`QueueManager` property defines name of queue manager and `Queue` property defines name of queue to get messages.

`StripHeaders` property states that MQ message headers shall be preserved.
 
`TraceOperations` property defines set of traced operation names (using RegEx or wildcard). MQ Trace Events referencing operations not 
covered by this set will be filtered out from activities stream.

`ExcludedRC` property defines set of excluded MQ traces reason codes (delimited using `|` character). MQ Trace Events having reason codes 
 defined by this set will be filtered out from activities stream. Set entries may be defined using both numeric and MQ constant name values.
 
`SuppressBrowseGets` property defines flag indicating whether to exclude WMQ **BROWSE** type **GET** operation traces from streaming.

`TraceEventsParser` is of type `WmqTraceParser` meaning that it will parse PCF messages containing MQ Trace Events data contained within 
MQCFGR PCF structures.

`TranslateNumValues` property defines that parser should translate resolved numeric values to corresponding MQ constant names if possible.

#### Angulartics (AngularJS tracing)

This sample shows how to stream JavaScript events traces from Angulartics. TNT4J-Angulartics-plugin sends trace data over HTTP request 
`http://localhost:9595`. Thus to process this we will need `HttpStream` running on port `9595`.

Sample files can be found in `samples/angular-js-tracing` directory.

How to setup sample environment see [`samples/angular-js-tracing/readme.md`](tnt4j-streams-core/samples/angular-js-tracing/readme.md)

Sample trace data is available in `messages.json` file.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="JSONPayloadParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ReadLines" value="false"/>

        <field name="StartTime" locator="$.timestamp" locator-type="Label" datatype="Timestamp" units="Milliseconds"/>
        <field name="ResourceName" locator="$.url" locator-type="Label"/>
        <field name="Correlator" locator="$.sid" locator-type="Label"/>
        <field name="Correlator" locator="$.rid" locator-type="Label"/>
        <field name="EventName" locator="$.eventName" locator-type="Label"/>
        <field name="EventType" value="EVENT"/>
        <field name="ElapsedTime" locator="$.pageLoad" locator-type="Label" datatype="Number" format="#####0"/>
        <field name="Browser" locator="$.browser" locator-type="Label"/>
        <field name="EventProperties" locator="$.properties" locator-type="Label"/>
    </parser>

    <parser name="AngularticsReqParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="JSONPayloadParser"/>
        </field>
    </parser>

    <stream name="AngularticsHttpStream" class="com.jkoolcloud.tnt4j.streams.inputs.HttpStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Port" value="9595"/>
        <parser-ref name="AngularticsReqParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `HttpStream` referencing `AngularticsReqParser` shall be used.

`HttpStream` starts HTTP server on port defined using `Port` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. Stream puts received request payload data as `byte[]` to map using key `ActivityData`.

`AngularticsReqParser` by default converts `byte[]` for entry `ActivityData` to JSON format string and uses stacked
parser named `JSONPayloadParser` to parse it.

`JSONPayloadParser` transforms received JSON data string to Map and fills in activity event fields values from that map.

#### AJAX

This sample shows how to stream JavaScript events traces from AJAX. TNT4J-AJAX-interceptor sends trace data over HTTP request 
`http://localhost:9595`. Thus to process this we will need `HttpStream` running on port `9595`.

Sample files can be found in `samples/ajax` directory.

How to setup sample environment see [`samples/ajax/readme.md`](tnt4j-streams-core/samples/ajax/readme.md)

Sample trace data is available in `messages.json` file.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="JSONPayloadParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ReadLines" value="false"/>

        <field name="StartTime" locator="$.startOfLoading" locator-type="Label" datatype="Timestamp"
               units="Nanoseconds"/>
        <field name="ResourceName" locator="$.url" locator-type="Label"/>
        <field name="EventName" locator="$.eventType" locator-type="Label"/>
        <field name="Message" locator="$.message" locator-type="Label"/>
        <field name="Tag" locator="$.eventType" locator-type="Label"/>

        <field name="CompCode" locator="$.statuss" locator-type="Label">
            <field-map source="100:206" target="SUCCESS" type="Range"/>
            <field-map source="300:308" target="WARNING" type="Range"/>
            <field-map source="400:417" target="ERROR" type="Range"/>
            <field-map source="500:511" target="ERROR" type="Range"/>
        </field>
        <field name="ReasonCode" locator="$.statuss" locator-type="Label"/>

        <field name="EventType" value="EVENT"/>
        <field name="EndTime" locator="$.endOfLoading" locator-type="Label" datatype="Timestamp"
               units="Milliseconds"/>
        <field name="ElapsedTime" locator="$.elapsedTime" locator-type="Label" datatype="Number" format="#####0"/>
        <field name="ContentSize" locator="$.contentSize" locator-type="Label"/>
        <field name="IsError" locator="$.error" locator-type="Label"/>
        <field name="IsAborted" locator="$.abort" locator-type="Label"/>
    </parser>

    <parser name="AjaxEventParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="JSONPayloadParser"/>
        </field>
    </parser>

    <stream name="AjaxEventStream" class="com.jkoolcloud.tnt4j.streams.inputs.HttpStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Port" value="9595"/>
        <parser-ref name="AjaxEventParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `HttpStream` referencing `AjaxEventParser` shall be used.

`HttpStream` starts HTTP server on port defined using `Port` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. Stream puts received request payload data as `byte[]` to map using key `ActivityData`.

`AjaxEventParser` by default converts `byte[]` for entry `ActivityData` to JSON format string and uses stacked
parser named `JSONPayloadParser` to parse it.

`JSONPayloadParser` transforms received JSON data string to Map and fills in activity event fields values from that map.

#### Node.js

This sample shows how to stream JavaScript events traces from Node.js. TNT4J-njsTrace-plugin sends trace data over HTTP request 
`http://localhost:9595`. Thus to process this we will need `HttpStream` running on port `9595`.

Sample files can be found in `samples/node.js` directory.

How to setup sample environment see [`samples/node.js/readme.md`](tnt4j-streams-core/samples/node.js/readme.md)

Sample trace data is available in `messages.json` file.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="JSONPayloadParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ReadLines" value="false"/>

        <field name="ElapsedTime" locator="$.span" locator-type="Label" datatype="Number" units="Milliseconds" required="false"/>
        <field name="ResourceName" separator=",">
            <field-locator locator="$.file" locator-type="Label"/>
            <field-locator locator="$.line" locator-type="Label"/>
        </field>

        <field name="Method" locator="$.name" locator-type="Label"/>
        <field name="Message" locator="$.returnValue" locator-type="Label" required="false"/>
        <field name="EventName" value="node.js Trace"/>
        <field name="EventType" locator="$.method" locator-type="Label"/>
        <field name="Correlator" locator="$.stack[*]" locator-type="Label" separator=","/>

        <field name="Exception" locator="$.exception" locator-type="Label"/>

        <field name="CompCode" locator="$.exception" locator-type="Label">
            <field-map source="false" target="SUCCESS"/>
            <field-map source="true" target="ERROR"/>
            <field-map source="Function timed out" target="ERROR"/>
        </field>

    </parser>

    <parser name="njstraceParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="JSONPayloadParser"/>
        </field>
    </parser>

    <stream name="njstraceHttpStream" class="com.jkoolcloud.tnt4j.streams.inputs.HttpStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Port" value="9596"/>
        <parser-ref name="njstraceParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `HttpStream` referencing `njstraceParser` shall be used.

`HttpStream` starts HTTP server on port defined using `Port` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. Stream puts received request payload data as `byte[]` to map using key `ActivityData`.

`njstraceParser` by default converts `byte[]` for entry `ActivityData` to JSON format string and uses stacked
parser named `JSONPayloadParser` to parse it.

`JSONPayloadParser` transforms received JSON data string to Map and fills in activity event fields values from that map.

#### Node.js blocking event loop

This extended ['Node.js'](#nodejs) sample shows how to trace blocking event loop occurrences.

Sample files can be found in `samples/node.js-blocking-event-loop` directory.

How to setup sample environment see [`samples/node.js-blocking-event-loop/readme.md`](tnt4j-streams-core/samples/node.js-blocking-event-loop/readme.md)

Sample stream configuration is same as in ['Node.js'](#nodejs) sample.

#### JAX-WS

This sample shows how to stream responses from JAX-WS (SOAP) services as SOAP compliant XML data.

Sample files can be found in `samples/ws-stream` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-ws/config/tnt-data-source-ws.xsd">

    <parser name="WsResponseParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <property name="Namespace" value="s=http://schemas.xmlsoap.org/soap/envelope/"/>
        <property name="Namespace" value="a=http://schemas.datacontract.org/2004/07/"/>
        <property name="Namespace" value="i=http://www.w3.org/2001/XMLSchema-instance"/>
        <property name="Namespace" value="b=http://tempuri.org/"/>

        <field name="EventType" value="Event"/>
        <field name="ApplName" value="weather"/>
        <field name="GeoLocation" separator=",">
            <field-locator
                    locator="/s:Envelope/s:Body/b:GetCurrentWeatherInformationByStationIDResponse/b:GetCurrentWeatherInformationByStationIDResult/a:Latitude"
                    locator-type="Label"/>
            <field-locator
                    locator="/s:Envelope/s:Body/b:GetCurrentWeatherInformationByStationIDResponse/b:GetCurrentWeatherInformationByStationIDResult/a:Longitude"
                    locator-type="Label"/>
        </field>
        <field name="Temperature"
               locator="/s:Envelope/s:Body/b:GetCurrentWeatherInformationByStationIDResponse/b:GetCurrentWeatherInformationByStationIDResult/a:TemperatureInFahrenheit"
               locator-type="Label"/>
        <field name="Humidity"
               locator="/s:Envelope/s:Body/b:GetCurrentWeatherInformationByStationIDResponse/b:GetCurrentWeatherInformationByStationIDResult/a:RelativeHumidity"
               locator-type="Label"/>
        <field name="Wind Speed"
               locator="/s:Envelope/s:Body/b:GetCurrentWeatherInformationByStationIDResponse/b:GetCurrentWeatherInformationByStationIDResult/a:WindSpeedInMPH"
               locator-type="Label"/>
    </parser>

    <stream name="WsSampleStream" class="com.jkoolcloud.tnt4j.streams.inputs.WsStream">
        <property name="HaltIfNoParser" value="false"/>

        <scenario name="Sample WS stream scenario">
            <step name="Step 1"
                  url="http://wsdot.wa.gov/traffic/api/WeatherInformation/WeatherInformation.svc">
                <schedule-simple interval="35" units="Seconds" repeatCount="10"/>
                <request>
                    <![CDATA[
                        SOAPAction:http://tempuri.org/IWeatherInformation/GetCurrentWeatherInformationByStationID
                        <tem:GetCurrentWeatherInformationByStationID xmlns:tem="http://tempuri.org/">
                            <tem:AccessCode>aeb652b7-f6f5-49e6-9bdb-e2b737ebd507</tem:AccessCode>
                            <tem:StationID>1909</tem:StationID>
                        </tem:GetCurrentWeatherInformationByStationID>
                    ]]>
                </request>
            </step>
        </scenario>

        <parser-ref name="WsResponseParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `WsStream` referencing `WsResponseParser` shall be used. Stream takes response received
SOAP compliant XML string and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable entries.

Streaming scenario consists of one step defining service request URL, scheduler definition: `10 times with 35 seconds
interval`. `request` definition contains SOAP request header `SOAPAction` and SOAP request body data
`tem:GetCurrentWeatherInformationByStationID`.

`WsResponseParser` maps XML data values to activity event fields `GeoLocation`, `Temperature`, `Humidity` and
`Wind Speed`. Parser property `Namespace` adds additional namespaces required to parse received JAX-WS XML response
using XPath.

#### JAX-RS

##### JAX-RS JSON

This sample shows how to stream responses from JAX-RS as JSON data.

Sample files can be found in `samples/restful-stream-json` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-ws/config/tnt-data-source-ws.xsd">

    <parser name="RESTResponseParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ReadLines" value="false"/>

        <field name="EventType" value="Event"/>
        <field name="ApplName" value="weather"/>
        <field name="Location" locator="$.name" locator-type="Label"/>
        <field name="GeoLocation" separator=",">
            <field-locator locator="$.coord.lon" locator-type="Label"/>
            <field-locator locator="$.coord.lat" locator-type="Label"/>
        </field>
        <field name="Temperature" locator="$.main.temp" locator-type="Label"/>
        <field name="Humidity" locator="$.main.humidity" locator-type="Label"/>
        <field name="Wind Speed" locator="$.wind.speed" locator-type="Label"/>
    </parser>

    <stream name="RESTfulSampleJSONStream" class="com.jkoolcloud.tnt4j.streams.inputs.RestStream">
        <property name="HaltIfNoParser" value="false"/>

        <scenario name="Sample REST stream scenario">
            <step name="Step Kaunas"
                  url="http://api.openweathermap.org/data/2.5/weather?q=Kaunas&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric"
                  method="GET">
                <schedule-cron expression="0/15 * * * * ? *"/>
            </step>
            <step name="Step Vilnius"
                  url="http://api.openweathermap.org/data/2.5/weather?q=Vilnius&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric"
                  method="GET">
                <schedule-cron expression="0/30 * * * * ? *"/>
            </step>
            <step name="Step Klaipeda"
                  url="http://api.openweathermap.org/data/2.5/weather?q=Klaipeda&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric"
                  method="GET">
                <schedule-simple interval="45" units="Seconds" repeatCount="10"/>
            </step>
        </scenario>

        <parser-ref name="RESTResponseParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `RestStream` referencing `RESTResponseParser` shall be used. Stream takes response
received JSON string and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable entries.

Streaming scenario consists of three steps defining service request URL's, request methods (all `GET`) and scheduler
definitions.
Step named `Step Kaunas` defines Cron scheduler expression stating: `invoke every 15 seconds`.
Step named `Step Vilnius` defines Cron scheduler expression stating: `invoke every 30 seconds`.
Step named `Step Klaipeda` defines simple scheduler expression stating: `10 times with 45 seconds interval`.

`RESTResponseParser` maps JSON data values to activity event fields `Location`, `GeoLocation`, `Temperature`, `Humidity`
and `Wind Speed`. Parser property `ReadLines` indicates that whole parsed string represents single JSON data package.

##### JAX-RS XML

This sample shows how to stream responses from JAX-RS as XML data.

Sample files can be found in `samples/restful-stream-xml` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-ws/config/tnt-data-source-ws.xsd">

    <parser name="RESTResponseParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <field name="EventType" value="Event"/>
        <field name="ApplName" value="weather"/>
        <field name="Location" locator="/current/city/@name" locator-type="Label"/>
        <field name="GeoLocation" separator=",">
            <field-locator locator="/current/city/coord/@lon" locator-type="Label"/>
            <field-locator locator="/current/city/coord/@lat" locator-type="Label"/>
        </field>
        <field name="Temperature" locator="/current/temperature/@value" locator-type="Label"/>
        <field name="Humidity" locator="/current/humidity/@value" locator-type="Label"/>
        <field name="Wind Speed" locator="/current/wind/speed/@value" locator-type="Label"/>
    </parser>

    <stream name="RESTfulSampleXMLStream" class="com.jkoolcloud.tnt4j.streams.inputs.RestStream">
        <property name="HaltIfNoParser" value="false"/>

        <scenario name="Sample REST stream scenario">
            <step name="Step Kaunas"
                  url="http://api.openweathermap.org/data/2.5/weather?q=Kaunas&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric&amp;mode=XML"
                  method="GET">
                <schedule-cron expression="0/15 * * * * ? *"/>
            </step>
            <step name="Step Vilnius"
                  url="http://api.openweathermap.org/data/2.5/weather?q=Vilnius&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric&amp;mode=XML"
                  method="GET">
                <schedule-cron expression="0/30 * * * * ? *"/>
            </step>
            <step name="Step Klaipeda"
                  url="http://api.openweathermap.org/data/2.5/weather?q=Klaipeda&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric&amp;mode=XML"
                  method="GET">
                <schedule-simple interval="45" units="Seconds" repeatCount="10"/>
            </step>
        </scenario>

        <parser-ref name="RESTResponseParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `RestStream` referencing `RESTResponseParser` shall be used. Stream takes response
received XML string and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable entries.

Streaming scenario consists of three steps defining service request URL's, request methods (all `GET`) and scheduler
definitions.
Step named `Step Kaunas` defines Cron scheduler expression stating: `invoke every 15 seconds`.
Step named `Step Vilnius` defines Cron scheduler expression stating: `invoke every 30 seconds`.
Step named `Step Klaipeda` defines simple scheduler expression stating: `10 times with 45 seconds interval`.

`RESTResponseParser` maps XML data values to activity event fields `Location`, `GeoLocation`, `Temperature`, `Humidity`
and `Wind Speed`.

#### System command

##### Windows

This sample shows how to stream responses from executed Windows OS command.

Sample files can be found in `samples/win-cmd-stream` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-ws/config/tnt-data-source-ws.xsd">

    <parser name="ResponseParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityRegExParser">
        <property name="Pattern"
                  value="(\s*)&quot;\((.*)\)&quot;,&quot;(.*)&quot; &quot;(.*)&quot;,&quot;(.*)&quot;(.*)"/>

        <field name="EventType" value="SNAPSHOT"/>
        <field name="ProcessorTime" locator="5" locator-type="REGroupNum"/>
    </parser>

    <stream name="WinCmdStream" class="com.jkoolcloud.tnt4j.streams.inputs.CmdStream">
        <property name="HaltIfNoParser" value="false"/>

        <scenario name="Sample Win Cmd stream scenario">
            <step name="Step Windows">
                <request>typeperf "\Processor(_Total)\% Processor Time" -sc 1</request>
                <schedule-simple interval="25" units="Seconds" repeatCount="-1"/>
            </step>
        </scenario>

        <parser-ref name="ResponseParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `CmdStream` referencing `ResponseParser` shall be used. Stream takes command response
output string and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable entries.

Streaming scenario consists of one step defining system command to execute as `request` tag data and scheduler
definition stating `execute endlessly every 25 seconds`.

`ResponseParser` parses command output string using `Pattern` property defined RegEx and produces activity snapshot
containing field `ProcessorTime`.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

##### *nix

This sample shows how to stream responses from executed *nix type OS command.

Sample files can be found in `samples/unix-cmd-stream` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-ws/config/tnt-data-source-ws.xsd">

    <parser name="ResponseParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityRegExParser">
        <property name="Pattern" value="(.*)"/>

        <field name="EventType" value="SNAPSHOT"/>
        <field name="TomcatActive" locator="1" locator-type="REGroupNum"/>
    </parser>

    <stream name="UnixCmdStream" class="com.jkoolcloud.tnt4j.streams.inputs.CmdStream">
        <property name="HaltIfNoParser" value="false"/>

        <scenario name="Sample *nix Cmd stream scenario">
            <step name="Step Is Active Tomcat">
                <request>systemctl is-active tomcat7.servicesystemctl is-active tomcat7.service</request>
                <schedule-simple interval="45" units="Seconds" repeatCount="-1"/>
            </step>
        </scenario>

        <parser-ref name="ResponseParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `CmdStream` referencing `ResponseParser` shall be used. Stream takes command response
output string and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable entries.

Streaming scenario consists of one step defining system command to execute as `request` tag data and scheduler
definition stating `execute endlessly every 45 seconds`.

`ResponseParser` parses command output string using `Pattern` property defined RegEx and produces activity snapshot
containing field `TomcatActive`.

**NOTE:** Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### Redirecting TNT4J streams

This sample shows how to redirect `tnt4j-stream-jmx` (may be from multiple running instances) produced trackables to JKool Cloud over single
`TNT4J-Streams` stream instance.

Sample files can be found in `samples/stream-jmx` directory.

To redirect `tnt4j-stream-jmx` (or any other TNT4J based producer) produced trackables, producer configuration file `tnt4j.properties` 
should contain such stanza:
```properties
    event.sink.factory.EventSinkFactory: com.jkoolcloud.tnt4j.sink.impl.SocketEventSinkFactory
    event.sink.factory.EventSinkFactory.eventSinkFactory: com.jkoolcloud.tnt4j.sink.impl.NullEventSinkFactory
    event.sink.factory.EventSinkFactory.Host: IP_OF_STREAMS_RUNNING_MACHINE
    event.sink.factory.EventSinkFactory.Port: 9009
    event.formatter: com.jkoolcloud.tnt4j.format.JSONFormatter
```
**NOTE:** change `IP_OF_STREAMS_RUNNING_MACHINE` to IP of machine running `TNT4J-Streams` `RedirectTNT4JStream`.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <java-object name="JMXRedirectOutput" class="com.jkoolcloud.tnt4j.streams.outputs.JKCloudJsonOutput"/>

    <stream name="SampleJMXRoutingStream" class="com.jkoolcloud.tnt4j.streams.inputs.RedirectTNT4JStream">
        <property name="RestartOnInputClose" value="true"/>
        <property name="Port" value="9009"/>

        <reference name="JMXRedirectOutput"/>

        <tnt4j-properties>
            <property name="event.formatter" value="com.jkoolcloud.tnt4j.streams.utils.RedirectTNT4JStreamFormatter"/>
        </tnt4j-properties>
    </stream>
</tnt-data-source>
```

Stream configuration states that `SampleJMXRoutingStream` referencing `JMXRedirectOutput` shall be used. Stream takes starts server socket 
on port defined by stream property `Port`. Stream listens server socket for inbound connections (accepts multiple). When connection gets 
accepted, stream reads incoming data from connection dedicated socket.

`RestartOnInputClose` property indicates that stream should initiate new instance of server socket if listened one gets closed or fails to
accept inbound connection.

Stream referenced object `JMXRedirectOutput` sends JSON formatted data to JKool Cloud.

Stream also additionally sets one TNT4J framework property `event.formatter`. This allows us to use customized JSON formatter and avoid 
additional JSON reformatting in default TNT4J data flow.

**NOTE:** you may also re-stream any TNT4J based producer logged trackables from file. Only requirement - trackables must be serialized in JSON 
format. 

To do re-streaming from file, change sample configuration by replacing `SampleJMXRoutingStream` stream property `Port` to `FileName` 
referring file containing logged trackables in JSON format:
```xml
    <stream name="SampleJMXRoutingStream" class="com.jkoolcloud.tnt4j.streams.inputs.RedirectTNT4JStream">
        <property name="FileName" value="tnt4j-stream-activities.log"/>

        <reference name="JMXRedirectOutput"/>

        <tnt4j-properties>
            <property name="event.formatter" value="com.jkoolcloud.tnt4j.streams.utils.RedirectTNT4JStreamFormatter"/>
        </tnt4j-properties>
    </stream>
``` 

#### MS Excel document

##### Rows

This sample shows how to stream MS Excel workbook rows as activity events.

Sample files can be found in `samples/xlsx-rows` directory.

**NOTE:** records in this file are from year `2010` e.g., `12 Jul 2010`, so then getting events data in JKool Cloud
please do not forget to just to dashboard time frame to that period!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="ExcelRowParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityExcelRowParser">
        <field name="StartTime" locator="A" locator-type="Label" format="dd MMM yyyy HH:mm:ss" locale="en-US"/>
        <field name="ServerIp" locator="B" locator-type="Label"/>
        <field name="ApplName" value="orders"/>
        <field name="Correlator" locator="C" locator-type="Label"/>
        <field name="UserName" locator="D" locator-type="Label"/>
        <field name="EventName" locator="E" locator-type="Label"/>
        <field name="EventType" locator="E" locator-type="Label">
            <field-map source="Order Placed" target="START"/>
            <field-map source="Order Received" target="RECEIVE"/>
            <field-map source="Order Processing" target="OPEN"/>
            <field-map source="Order Processed" target="SEND"/>
            <field-map source="Order Shipped" target="END"/>
        </field>
        <field name="MsgValue" locator="H" locator-type="Label"/>
    </parser>

    <stream name="SampleExcelRowsStream" class="com.jkoolcloud.tnt4j.streams.inputs.ExcelRowStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="FileName" value="./tnt4j-streams-msoffice/samples/xlsx-rows/sample.xlsx"/>
        <property name="RangeToStream" value="1:"/>
        <property name="SheetsToProcess" value="Sheet*"/>

        <parser-ref name="ExcelRowParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `SampleExcelRowsStream` referencing `ExcelRowsParser` shall be used. Stream takes
workbook sheet row and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable rows.

`SampleExcelRowsStream` reads data from `./tnt4j-streams-msoffice/samples/xlsx-rows/sample.xlsx` file.

`RangeToStream` defines range of rows to be streamed from each matching sheet - `from firs row to the end`. 

`SheetsToProcess` property defines sheet name filtering mask using wildcard string. It is also allowed to use RegEx like
`Sheet(1|3|5)` (in this case just sheets with names `Sheet1`, `Sheet3` and `Sheet5` will be processed).

`ExcelRowParser` parser uses literal sheet column indicators as locators (e.g., `A`, `D`, `AB`).

**NOTE:** `StartTime` fields defines format and locale to correctly parse field data string. `EventType` uses manual
field string mapping to TNT4J event field value.

##### Sheets

This sample shows how to stream MS Excel workbook sheets as activity events.

Sample files can be found in `samples/xlsx-sheets` directory.

**NOTE:** records in this file are from year `2010` e.g., `12 Jul 2010`, so then getting events data in JKool Cloud
please do not forget to just to dashboard time frame to that period!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="ExcelSheetParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityExcelSheetParser">
        <field name="StartTime" locator="B2" locator-type="Label" format="dd MMM yyyy HH:mm:ss" locale="en-US"/>
        <field name="ServerIp" locator="B3" locator-type="Label"/>
        <field name="ApplName" value="orders"/>
        <field name="Correlator" locator="B4" locator-type="Label"/>
        <field name="UserName" locator="B5" locator-type="Label"/>
        <field name="EventName" locator="B6" locator-type="Label"/>
        <field name="EventType" locator="B6" locator-type="Label">
            <field-map source="Order Placed" target="START"/>
            <field-map source="Order Received" target="RECEIVE"/>
            <field-map source="Order Processing" target="OPEN"/>
            <field-map source="Order Processed" target="SEND"/>
            <field-map source="Order Shipped" target="END"/>
        </field>
        <field name="MsgValue" locator="B9" locator-type="Label"/>
    </parser>

    <stream name="SampleExcelSheetsStream" class="com.jkoolcloud.tnt4j.streams.inputs.ExcelSheetStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="FileName" value="./tnt4j-streams-msoffice/samples/xlsx-sheets/sample.xlsx"/>
        <property name="SheetsToProcess" value="Sheet*"/>

        <parser-ref name="ExcelSheetParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `SampleExcelSheetsStream` referencing `ExcelSheetParser` shall be used. Stream takes
workbook sheet and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable sheets.

`SampleExcelRowsStream` reads data from `./tnt4j-streams-msoffice/samples/xlsx-sheets/sample.xlsx` file.

`SheetsToProcess` property defines sheet name filtering mask using wildcard string. It is also allowed to use RegEx like
`Sheet(1|3|5)` (in this case just sheets with names `Sheet1`, `Sheet3` and `Sheet5` will be processed).

`ExcelSheetParser` parser uses literal sheet cell indicators as locators (e.g., `A1`, `D5`, `AB12` where letters
identifies column and number identifies row).

**NOTE:** `StartTime` fields defines format and locale to correctly parse field data string. `EventType` uses manual
field string mapping to TNT4J event field value.

#### Collectd performance metrics streaming

This sample shows how to stream `collectd` monitoring reports data as activity events.

Sample files can be found in `samples/collectd-json` directory (`tnt4j-streams-core` module).

**NOTE:** to use this sample `collectd` should be running `Write HTTP` plugin. See [Collectd Wiki](https://collectd.org/wiki/index.php/Plugin:Write_HTTP)
for details.

Sample report data is available in `stats.json` file. 

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="CollectdStatsDataParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <property name="ReadLines" value="false"/>
        <field name="EventType" value="SNAPSHOT"/>
        <field name="EventName" locator="type|type_instance" locator-type="Label" separator=" "/>
        <field name="Category" locator="plugin|plugin_instance" locator-type="Label" separator=" "/>
        <field name="ServerName" locator="host" locator-type="Label"/>

        <field name="${FieldNameLoc}" locator="values" locator-type="Label" value-type="${ValueTypeLoc}" split="true">
            <field-locator id="FieldNameLoc" locator="dsnames" locator-type="Label"/>
            <field-locator id="ValueTypeLoc" locator="dstypes" locator-type="Label"/>
        </field>

        <field name="StartTime" locator="time" locator-type="Label" datatype="Timestamp" units="Seconds"/>
    </parser>

     <parser name="CollectdReqBodyParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ReadLines" value="false"/>

        <field name="MsgBody" locator="$" locator-type="Label" transparent="true" split="true">
            <parser-ref name="CollectdStatsDataParser" aggregation="Join"/>
        </field>
        <field name="EventType" value="Activity"/>
        <field name="ApplName" value="collectd"/>
    </parser>

    <parser name="CollectdReqParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">
        <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="CollectdReqBodyParser"/>
        </field>
    </parser>

    <stream name="CollectdStream" class="com.jkoolcloud.tnt4j.streams.inputs.HttpStream">
        <property name="HaltIfNoParser" value="false"/>
        <property name="Port" value="9595"/>

        <parser-ref name="CollectdReqParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `HttpStream` referencing `CollectdReqParser` shall be used. Stream takes HTTP request 
received from `collectd` server and passes it to parser.

`HttpStream` starts HTTP server on port defined using `Port` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. Stream puts received request payload data as `byte[]` to map using key `ActivityData`.

`CollectdReqParser` by default converts `byte[]` for entry `ActivityData` to string and uses stacked parser named
`CollectdReqBodyParser` to parse HTTP request contained JSON format data.

`CollectdReqBodyParser` maps `collectd` report JSON data to activity (field `EventType`) containing set of snapshots (field `MsgBody`) 
carrying system metrics data. Each snapshot is parsed using stacked `CollectdStatsDataParser` parser (map parser because parent JSON parser 
already made map data structures from RAW `collectd` report JSON data).

`CollectdStatsDataParser` maps map entries to snapshot fields `EventName`, `Category`, `ServerName`, `StartTime`. Also this parser uses 
dynamic field named `${FieldNameLoc}`. Attribute values containing variable expressions `${}` indicates that these attributes can have 
dynamic values resolved from streamed data. Such variable expressions has to reference locators identifiers (`field-locator` attribute `id`) 
to resolve and fill actual data. Field attribute `split`, that if field value locator resolves collection (list or array) of values, values 
of that collection should be split into separate fields of produced activity.

#### Nagios reports streaming

This sample shows how to stream Nagios monitoring reports data as activity events.

Sample files can be found in `samples/nagios-nagios2json` directory (`tnt4j-streams-ws` module).

**NOTE:** to use this sample Nagios should be running extension `nagios2json`. See [Sample README](tnt4j-streams-ws/samples/nagios-nagios2json/readme.md)
for details. `nagios2json` extension works as `CGI` script, but can be handled as simple RESTful service.

Sample report data is available in `report.json` file. 

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/tnt4j-streams-ws/config/tnt-data-source-ws.xsd">

    <parser name="SnapshotParser"
            class="com.jkoolcloud.tnt4j.streams.parsers.ActivityMapParser">

        <field name="EventType" value="Snapshot"/>
        <field name="ApplName" value="Nagios"/>
        <field name="EventName" locator="service" locator-type="Label"/>
        <field name="Status" locator="status" locator-type="Label" value-type="enum"/>
        <field name="Message" locator="plugin_output" locator-type="Label"/>
        <field name="Category" locator="hostname" locator-type="Label"/>
        <field name="Duration" locator="duration" locator-type="Label" value-type="age"/>
        <field name="StartTime" locator="last_state_change" locator-type="Label" datatype="Timestamp" units="Seconds"/>
    </parser>

    <parser name="ResponseParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ReadLines" value="false"/>

        <field name="MsgBody" locator="$.data" locator-type="Label" transparent="true" split="true">
            <parser-ref name="SnapshotParser" aggregation="Join"/>
        </field>

        <field name="EventType" value="Activity"/>
        <field name="StartTime" locator="$.created" locator-type="Label" datatype="Timestamp" units="Seconds"/>
        <field name="ApplName" value="nagios2json"/>
        <field name="Message" separator=", ">
            <field-locator locator="$.version" locator-type="Label"/>
            <field-locator locator="$.running" locator-type="Label"/>
            <field-locator locator="$.servertime" locator-type="Label" datatype="Timestamp" units="Seconds"/>
            <field-locator locator="$.localtime" locator-type="Label"/>
        </field>
    </parser>

    <stream name="RESTfulSampleNagiosStream" class="com.jkoolcloud.tnt4j.streams.inputs.RestStream">
        <property name="HaltIfNoParser" value="false"/>

        <scenario name="Nagios2JSON stream scenario">
            <step name="Step 1"
                  url="http://[YOUR_NAGIOS_SERVER_IP]/nagios/cgi-bin/nagios2json.cgi?servicestatustypes=31"
                  method="GET"
                  username="myNagiosUserName"
                  password="myNagiosUserSecretPassword">
                <schedule-cron expression="0/15 * * * * ? *"/>
            </step>
        </scenario>

        <parser-ref name="ResponseParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `RestStream` referencing `ResponseParser` shall be used. Stream takes response
received JSON string and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable entries.

Streaming scenario step defines:
 * Nagios service request URL and `nagios2json` parameters (e.g., `servicestatustypes=31`) 
 * request method `GET` to comply `cgi`.
 * Nagios service user credentials - user name and password
 * Scheduler is set to `Cron` expression `every 15 seconds`.
 
You may also add additional steps to retrieve different reports defining different `nagios2json` parameters.
 
Nagios sends report as activity wrapping multiple metrics (snapshots).

`ResponseParser` maps Nagios report JSON data to activity (field `EventType`) containing set of snapshots (field `MsgBody`) carrying system 
state/metrics data. Each snapshot is parsed using stacked `SnapshotParser` parser (map parser because parent JSON parser already made map 
data structures from RAW Nagios report JSON data).

`SnapshotParser` maps map entries to snapshot fields `ApplName`, `EventName`, `Status`, `Message`, `Category`, `Duration` and `StartTime`.
`Status` and `Duration` fields also defines value types: `Status` is `enum`, `Duration` is `age`.
 
#### Fluentd logs streaming
 
TODO 

#### Integrating TNT4J-Streams into custom API

See [`Readme.md`](tnt4j-streams-samples/README.md) of `tnt4j-streams-samples` module.

## How to use TNT4J loggers

### tnt4j-log4j12

* in `config/log4j.properties` file change log appender to
`log4j.appender.tnt4j=com.jkoolcloud.tnt4j.logger.log4j.TNT4JAppender`. Note that there should be on line like
`log4j.appender.tnt4j=` in this file, so please comment or remove all others if available.
* in `pom.xml` file of `core` change dependencies - uncomment:
```xml
    <dependency>
        <groupId>com.jkoolcloud.tnt4j.logger</groupId>
        <artifactId>tnt4j-log4j12</artifactId>
        <version>0.1</version>
        <scope>runtime</scope>
    </dependency>
```
* when running `TNT4J-Streams` use system property `-Dlog4j.configuration` to define `log4j.properties` file location, e.g.: 
`-Dlog4j.configuration="file:./config/log4j.properties"`.

### tnt4j-logback

* make logback configuration file `config/logback.xml`.
* in `pom.xml` file of `core` change dependencies - uncomment:
```xml
    <dependency>
        <groupId>com.jkoolcloud.tnt4j.logger</groupId>
        <artifactId>tnt4j-logback</artifactId>
        <version>0.1</version>
        <scope>runtime</scope>
    </dependency>
```
and comment out log4j dependencies
* when running `TNT4J-Streams` use system property `-Dlogback.configurationFile` to define `logback.xml` file location, e.g.:
`-Dlog4j.configuration="file:./config/logback.xml"`. Change `bin/tnt-streams.bat` or `bin/tnt-streams.sh` file to pass logback configuration 
to Java:

`bat` file:
```
set LOGBACKOPTS=-Dlogback.configurationFile="file:%RUNDIR%..\config\logback.xml"
java %LOGBACKOPTS% %TNT4JOPTS% ...
```
`sh` file:
```
LOGBACKOPTS=-Dlogback.configurationFile="file:%RUNDIR%/../config/logback.xml"
java $LOGBACKOPTS $TNT4JOPTS
```

Configuring TNT4J-Streams
======================================

TNT4J-Streams configuration sources may be:
* configuration files - like `tnt4j.properties`, `log4j.properties`, `tnt-data-source.xml`, etc. 
* ZooKeeper nodes data - see [ZooKeeper stored configuration](#zookeeper-stored-configuration) chapter for more details.

Configuration data format is same for all sources now.

## TNT4J configuration

Because TNT4J-Streams is based on TNT4J first You need to configure TNT4J (if have not done this yet).
Default location of `tnt4j.properties` file is in project `config` directory. At least You must make one change:
`event.sink.factory.Token:YOUR-TOKEN` replace `YOUR-TOKEN` with JKool Cloud token assigned for You.

To define `tnt4j.properties` file location use system property `-Dtnt4j.config`, e.g., `-Dtnt4j.config="./config/tnt4j.properties"`.

For more information on TNT4J and `tnt4j.properties` see [TNT4J Wiki page](https://github.com/Nastel/TNT4J/wiki/Getting-Started) and 
[TNT4J README](https://github.com/Nastel/TNT4J/blob/master/README.md).

Details on JESL related configuration can be found in [JESL README](https://github.com/Nastel/JESL/blob/master/README.md).

### Basic TNT4J configuration of TNT4J-Streams

Filling in configuration template:
* Replace `<YOUR XXXX>` value placeholder with actual value corresponding to your environment.
* Replace `<YOUR EVENT SINK CONFIGURATION>` placeholder with actual configuration of streamed activity events sink you wish to use.

**NOTE:** It is **NOT RECOMMENDED** to use `BufferedEventSinkFactory` (or any other asynchronous sinks) with TNT4J-Streams. Streams and 
sinks are meant to act in sync, especially when sink (e.g., `JKCloud`, `Mqtt`, `Kafka`) consumer uses network communication. 

```properties
# Stanza used for TNT4J-Streams sources
{
	source: com.jkoolcloud.tnt4j.streams
	source.factory: com.jkoolcloud.tnt4j.source.SourceFactoryImpl
	source.factory.GEOADDR: <YOUR GEO ADDRESS>
	source.factory.DATACENTER: <YOUR DATA CENTER NAME>
	source.factory.RootFQN: RUNTIME=?#SERVER=?#NETADDR=?#DATACENTER=?#GEOADDR=?
	source.factory.RootSSN: tnt4j-streams

	tracker.factory: com.jkoolcloud.tnt4j.tracker.DefaultTrackerFactory
	dump.sink.factory: com.jkoolcloud.tnt4j.dump.DefaultDumpSinkFactory
	tracker.default.snapshot.category: TNT4J-Streams-event-snapshot

	# event sink configuration: destination and data format
	<YOUR EVENT SINK CONFIGURATION: JKoolCloud, Kafka, MQTT, etc.>

	event.formatter: com.jkoolcloud.tnt4j.format.JSONFormatter
	#event.formatter.Newline: true

	# Configure default sink filter based on level and time (elapsed/wait)
	##event.sink.factory.Filter: com.jkoolcloud.tnt4j.filters.EventLevelTimeFilter
	##event.sink.factory.Filter.Level: TRACE
	# Uncomment lines below to filter out events based on elapsed time and wait time
	# Timed event/activities greater or equal to given values will be logged
	##event.sink.factory.Filter.ElapsedUsec: 100
	##event.sink.factory.Filter.WaitUsec: 100

	tracking.selector: com.jkoolcloud.tnt4j.selector.DefaultTrackingSelector
	tracking.selector.Repository: com.jkoolcloud.tnt4j.repository.FileTokenRepository
}
```

#### JKoolCloud sink configuration

```properties
    #### JKool Cloud event sink factory configuration ####
    event.sink.factory: com.jkoolcloud.jesl.tnt4j.sink.JKCloudEventSinkFactory
    event.sink.factory.Filename: logs/tnt4j-streams-activities.log

    event.sink.factory.Url: http://data.jkoolcloud.com:6580
    #event.sink.factory.Url: https://data.jkoolcloud.com:6585
    #event.sink.factory.Url: https://data.jkoolcloud.com
    event.sink.factory.Token: <YOUR TOKEN>
    #### JKool Cloud event sink factory configuration end ####
```

#### Kafka sink configuration

```properties
    #### Kafka event sink factory configuration ####
    event.sink.factory: com.jkoolcloud.tnt4j.sink.impl.kafka.KafkaEventSinkFactory
    event.sink.factory.propFile: <YOUR PATH>/tnt4j-kafka.properties
    event.sink.factory.topic: <YOUR TOPIC>
    #### Kafka event sink factory configuration end ####
```

#### MQTT sink configuration

```properties
    #### MQTT event sink factory configuration ####
    event.sink.factory: com.jkoolcloud.tnt4j.sink.impl.mqtt.MqttEventSinkFactory
    event.sink.factory.mqtt-server-url: <YOUR MQTT SERVER ULR> #
    event.sink.factory.mqtt-topic: <YOUR TOPIC> #
    event.sink.factory.mqtt-user: <MQTT-USER> #
    event.sink.factory.mqtt-pwd: <MQTT-PWD>
    #### MQTT event sink factory configuration end ####
```

## Streams configuration

Streams can be configured using XML document having root element `tnt-data-source`. Definition of XML configuration
can be found in `tnt-data-source.xsd` file located in project `config` directory.

To define streams data source configuration file location, use program argument `-f:` e.g.:
```cmd
     tnt4j-streams -f:./tnt4j-streams-core/samples/single-log/tnt-data-source.xml
```
Or you can refer streams data source configuration file over System property named `tnt4j.streams.config` e.g.:
```properties
    -Dtnt4j.streams.config=./tnt4j-streams-core/samples/single-log/tnt-data-source.xml
```

Program argument `-p:` is used in common with `PipedStream` and only parsers configuration from `<tnt-data-source/>` definition is used. See 
[OS piped stream](#os-piped-stream).

Program argument `-z:` is used to define configuration file for ZooKeeper based TNT4J-Streams configuration. 
See [Loading ZooKeeper stored configuration data](#loading-zookeeper-stored-configuration-data) and [Streams registry](#streams-registry).

sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <parser name="TokenParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser">
        <property name="FieldDelim" value="|"/>
        <field name="StartTime" locator="1" format="dd MMM yyyy HH:mm:ss" locale="en-US"/>
        <field name="ServerIp" locator="2"/>
        <field name="ApplName" value="orders"/>
        <field name="Correlator" locator="3"/>
        <field name="UserName" locator="4"/>
        <field name="EventName" locator="5"/>
        <field name="EventType" locator="5">
            <field-map source="Order Placed" target="START"/>
            <field-map source="Order Received" target="RECEIVE"/>
            <field-map source="Order Processing" target="OPEN"/>
            <field-map source="Order Processed" target="SEND"/>
            <field-map source="Order Shipped" target="END"/>
        </field>
        <field name="MsgValue" locator="8"/>
    </parser>

    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        <property name="FileName" value="orders.log"/>
        <!--<property name="RangeToStream" value="1:"/>-->

        <parser-ref name="TokenParser"/>
    </stream>
</tnt-data-source>
```

As You can see from sample configuration, there are two major configuration elements defined `parser` and `stream`.
Because streams configuration is read using SAX parser referenced entities should be initialized before it is used.
Note that `stream` uses `parser` reference:
```xml
    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        <...>
        <parser-ref name="TokenParser"/>
    </stream>
```
That is why sequence of configuration elements is critical and can't be swapped.

#### Generic streams parameters

##### Stream executors related parameters

These parameters are applicable to all types of streams.

 * UseExecutors - identifies whether stream should use executor service to process activities data items asynchronously
 or not. Default value - `false`. (Optional)
    * ExecutorThreadsQuantity - defines executor service thread pool size. Default value - `4`. (Optional) Actual only if `UseExecutors` 
    is set to `true`
    * ExecutorsTerminationTimeout - time to wait (in seconds) for a executor service to terminate. Default value - `20sec`. (Optional) 
    Actual only if `UseExecutors` is set to `true`
    * ExecutorsBoundedModel - identifies whether executor service should use bounded tasks queue model. Default value - `false`. (Optional) 
    Actual only if `UseExecutors` is set to `true`
        * ExecutorRejectedTaskOfferTimeout - time to wait (in seconds) for a task to be inserted into bounded queue if max. queue size is 
        reached. Default value - `20sec`. (Optional) Actual only if `ExecutorsBoundedModel` is set to `true`.

    sample:
```xml
    <property name="UseExecutors" value="true"/>
    <property name="ExecutorThreadsQuantity" value="5"/>
    <property name="ExecutorsTerminationTimeout" value="20"/>
    <property name="ExecutorsBoundedModel" value="true"/>
    <property name="ExecutorRejectedTaskOfferTimeout" value="20"/>
```

##### Stream cache related parameters

* StreamCacheMaxSize - max. capacity of stream resolved values cache. Default value - `100`. (Optional)
* StreamCacheExpireDuration - stream resolved values cache entries expiration duration in minutes. Default value - `10`. (Optional)

    sample:
```xml
    <property name="StreamCacheMaxSize" value="500"/>
    <property name="StreamCacheExpireDuration" value="30"/>
```

##### Parseable streams parameters

These parameters are applicable to streams which uses parsers to parse incoming RAW activity data.

 * HaltIfNoParser - if set to `true`, stream will halt if none of the parsers can parse activity object RAW data.
 If set to `false` - puts log entry and continues. Default value - `false`. (Optional)

    sample:
```xml
    <property name="HaltIfNoParser" value="true"/>
```

##### Buffered streams parameters

 * BufferSize - maximal buffer queue capacity. Default value - `512`. (Optional)
 * BufferOfferTimeout - how long to wait if necessary for space to become available when adding data item to buffer
 queue. Default value - `45sec`. (Optional)

     sample:
 ```xml
     <property name="BufferSize" value="1024"/>
     <property name="BufferOfferTimeout" value="90"/>
 ```

#### File line stream parameters (and Hdfs)

 * FileName - the system-dependent file name or file name pattern defined using wildcard characters `*` and `?`. (Required)
 * FilePolling - flag `true/false` indicating whether files should be polled for changes or not. If not, then files
 are read from oldest to newest sequentially one single time. Default value - `false`. (Optional)
    * FileReadDelay - delay in seconds between file reading iterations. Actual only if `FilePolling` property is set to `true`. Default 
    value - `15sec`. (Optional)
 * RestoreState - flag `true/false` indicating whether files read state should be stored and restored on stream restart. Note, if 
 `StartFromLatest` is set to `false` - read state storing stays turned on, but previous stored read state is reset (no need to delete state 
 file manually). Default value - `false`. (Optional)
 * StartFromLatest - flag `true/false` indicating that streaming should be performed from latest file entry line. If `false` - then all 
 lines from available files are streamed on startup. Actual only if `FilePolling` or `RestoreState` properties are set to `true`. Default 
 value - `true`. (Optional)
 * RangeToStream - defines streamed data lines index range. Default value - `1:`. (Optional)

    sample:
 ```xml
    <property name="FileName" value="C:/Tomcat_7_0_34/logs/localhost_access_log.*.txt"/>
    <property name="FileReadDelay" value="5"/>
    <property name="StartFromLatest" value="true"/>
    <property name="FilePolling" value="true"/>
    <property name="RestoreState" value="true"/>
    <property name="RangeToStream" value="12:125"/>
 ```

In case using Hdfs file name is defined using URL like `hdfs://[host]:[port]/[path]`. Path may contain wildcards.

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Buffered streams parameters'](#buffered-streams-parameters).

#### Character stream parameters

 * FileName - the system-dependent file name. (Required - just one `FileName` or `Port`)
 * Port - port number to accept character stream over TCP/IP. (Required - just one `FileName` or `Port`)
 * RestartOnInputClose - flag indicating to restart stream if input socked gets closed. Default value - `false`. (Optional)

    sample:
```xml
    <property name="FileName" value="messages.json"/>
```
or
```xml
    <property name="Port" value="9595"/>
    <property name="RestartOnInputClose" value="true"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters).

**NOTE:** there can be ony one parser referenced to this stream.

#### Standard Java input stream parameters

 * InputCloseable - flag indicating if stream has to close input when stream is closing. Default value - `true`. (Optional)
 
    sample:
```xml
    <property name="InputCloseable" value="false"/>
``` 

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Parseable streams parameters'](#parseable-streams-parameters).

#### OS Piped stream parameters

This stream does not have any additional configuration parameters.

Also see ['Standard Java input stream parameters'](#standard-java-input-stream parameters).

#### Http stream parameters

 * Port - port number to run Http server. Default value - `8080`. (Optional)
 * UseSSL - flag indicating to use SSL. Default value - `false`. (Optional)
    * Keystore - keystore path. (Optional) Actual only if `UseSSL` is set to `true`.
    * KeystorePass - keystore password. (Optional) Actual only if `UseSSL` is set to `true`.
    * KeyPass - key password. (Optional) Actual only if `UseSSL` is set to `true`.

    sample:
```xml
    <property name="Port" value="8081"/>
    <property name="UseSSL" value="true"/>
    <property name="Keystore" value="path_to_keystore_file"/>
    <property name="KeystorePass" value="somePassword"/>
    <property name="KeyPass" value="somePassword"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Buffered streams parameters'](#buffered-streams-parameters).

#### JMS stream parameters

 * ServerURI - JMS server URL. (Required)
 * Queue - queue destination name. (Required - just one of `Queue` or `Topic`)
 * Topic - topic destination name. (Required - just one of `Queue` or `Topic`)
 * JNDIFactory - JNDI context factory name. (Required)
 * JMSConnFactory - JMS connection factory name. (Required)

    sample:
```xml
    <property name="ServerURI" value="tcp://localhost:61616"/>
    <property name="Topic" value="topic.SampleJMSTopic"/>
    <property name="JNDIFactory" value="org.apache.activemq.jndi.ActiveMQInitialContextFactory"/>
    <property name="JMSConnFactory" value="ConnectionFactory"/>
    <parser-ref name="SampleJMSParser"/>
```
or
```xml
    <property name="ServerURI" value="tcp://localhost:61616"/>
    <property name="Queue" value="queue.SampleJMSQueue"/>
    <property name="JNDIFactory" value="org.apache.activemq.jndi.ActiveMQInitialContextFactory"/>
    <property name="JMSConnFactory" value="ConnectionFactory"/>
    <parser-ref name="SampleJMSParser"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Buffered streams parameters'](#buffered-streams-parameters).

#### Kafka stream parameters

 * Topic - topic name to listen. (Required)
 * StartServer - flag indicating if stream has to start Kafka server on startup. Default value - `false`. (Optional)
 * StartZooKeeper - flag indicating if stream has to start ZooKeeper server on startup. Default value - `false`. (Optional)
 * List of properties used by Kafka API, e.g., zookeeper.connect, group.id. See `kafka.consumer.ConsumerConfig` class for more
 details on Kafka consumer properties.

    sample:
```xml
    <property name="Topic" value="TNT4JKafkaTestTopic"/>
    <property name="StartServer" value="true"/>
    <property name="StartZooKeeper" value="true"/>
    <property name="zookeeper.connect" value="127.0.0.1:2181/tnt4j_kafka"/>
    <property name="group.id" value="TNT4JStreams"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Parseable streams parameters'](#parseable-streams-parameters).

#### MQTT stream parameters

 * ServerURI - Mqtt server URI. (Required)
 * TopicString - the topic to subscribe to, which can include wildcards. (Required)
 * UserName - authentication user name. (Optional)
 * Password - user password. (Optional)
 * UseSSL - flag indicating to use SSL. Default value - `false`. (Optional)
    * Keystore - keystore path. (Optional) Actual only if `UseSSL` is set to `true`.
    * KeystorePass - keystore password. (Optional) Actual only if `UseSSL` is set to `true`.

    sample:
```xml
    <property name="ServerURI" value="tcp://localhost:1883"/>
    <property name="Topic" value="TNT4JStreams"/>
    <property name="UserName" value="someUser"/>
    <property name="Password" value="somePassword"/>
    <property name="UseSSL" value="true"/>
    <property name="Keystore" value="path_to_keystore_file"/>
    <property name="KeystorePass" value="somePassword"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Buffered streams parameters'](#buffered-streams-parameters).

#### WMQ Stream parameters

 * QueueManager - Queue manager name. (Optional)
 * Queue - Queue name. (Required - at least one of `Queue`, `Topic`, `Subscription`, `TopicString`)
 * Topic - Topic name. (Required - at least one of `Queue`, `Topic`, `Subscription`, `TopicString`)
 * Subscription - Subscription name. (Required - at least one of `Queue`, `Topic`, `Subscription`, `TopicString`)
 * TopicString - Topic string. (Required - at least one of `Queue`, `Topic`, `Subscription`, `TopicString`)
 * Host - WMQ connection host name. (Optional)
 * Port - WMQ connection port number. Default value - `1414`. (Optional)
 * UserName - WMQ user identifier. (Optional)
 * Password - WMQ user password. (Optional)
 * Channel - Server connection channel name. Default value - `SYSTEM.DEF.SVRCONN`. (Optional)
 * StripHeaders - identifies whether stream should strip WMQ message headers. Default value - `true`. (Optional)
 * StreamReconnectDelay - delay in seconds between queue manager reconnection or failed queue GET iterations. Default value - `15sec`. 
 (Optional)

    sample:
```xml
    <property name="QueueManager" value="QMGR"/>
    <property name="Queue" value="SYSTEM.ADMIN.TRACE.ACTIVITY.QUEUE"/>
    <property name="Host" value="wmq.sample.com"/>
    <property name="Port" value="1420"/>
    <property name="UserName" value="Administrator"/>
    <property name="Password" value="someUserPass"/>
    <property name="Channel" value="SYSTEM.DEF.SVRCONN2"/>
    <property name="StripHeaders" value="false"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters).

##### WMQ Trace Events Stream parameters

 * TraceOperations - defines traced MQ operations name filter mask (wildcard or RegEx) to process only traces of MQ operations which names 
 matches this mask. Default value - `*`. (Optional)
 * ExcludedRC - defines set of excluded MQ trace events reason codes (delimited using `|` character) to process only MQ trace events having 
 reason codes not contained in this set. Set entries may be defined using both numeric and MQ constant name values. 
 Default value - ``. (Optional)
 * SuppressBrowseGets - flag indicating whether to exclude WMQ BROWSE type GET operation traces from streaming. Default value - `false`. 
 (Optional)
 
    sample:
```xml
    <property name="TraceOperations" value="MQXF_(GET|PUT|CLOSE)"/>
    <property name="ExcludedRC" value="MQRC_NO_MSG_AVAILABLE|30737"/>
    <property name="SuppressBrowseGets" value="true"/>
``` 

Also see ['WMQ Stream parameters'](#wmq-stream-parameters).

#### Zipped file line stream parameters (and Hdfs)

 * FileName - defines zip file path and concrete zip file entry name or entry name pattern defined using characters `*`
 and `?`. Definition pattern is `zipFilePath!entryNameWildcard`. I.e.:
 `./tnt4j-streams-core/samples/zip-stream/sample.zip!2/*.txt`. (Required)
 * ArchType - defines archive type. Can be one of: `ZIP`, `GZIP`, `JAR`. Default value - `ZIP`. (Optional)

    sample:
```xml
    <property name="FileName" value="./tnt4j-streams-core/samples/zip-stream/sample.gz"/>
    <property name="ArchType" value="GZIP"/>
```

In case using Hdfs file name is defined using URL like `hdfs://[host]:[port]/[path]`. Zip entry name may contain
wildcards.

Also see ['Generic streams parameters'](#generic-streams-parameters).

#### Ws Stream parameters

This kind of stream (Rest/WS/Cmd) has no additional configuration parameters.

But has special configuration section - `scnario`. Streaming scenarios allows define `step`'s. Step defines
request/invocation/execution parameters and scheduler. Steps are invoked/executed independently of each other.

 * `scenario` tag has required attribute - `name` (any string).
    * `step` tag has required attribute - `name` (any string) and optional attributes `url` (service request URL),
    `method` (`GET`/`POST` - default value `GET`).
        * `schedule-cron` tag has required attribute `expression` (Cron expression).
        * `schedule-simple` tag has required attribute `interval` (positive integer numeric value) and optional
        attributes `units` (time units - default value `MILLISECONDS`), `repeatCount` (integer numeric value - default
        value `1`, `-1` means endless).
        * `request` is XML tag to define string represented request data (e.g., system command with parameters). To
        define XML contents it is recommended to use `CDATA`.

    sample:
```xml
    <!-- Sample scenario for RESTful services request -->
    <scenario name="Sample REST stream scenario">
        <step name="Step Kaunas"
            url="http://api.openweathermap.org/data/2.5/weather?q=Kaunas&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric"
            method="GET" >
            <schedule-cron expression="0/15 * * * * ? *"/>
        </step>
        <...>
        <step name="Step Klaipeda"
              url="http://api.openweathermap.org/data/2.5/weather?q=Klaipeda&amp;APPID=fa1fede9cbd6e26efdea1cdcbc714069&amp;units=metric"
              method="GET">
            <schedule-simple interval="45" units="Seconds" repeatCount="10"/>
        </step>
    </scenario>
    <!-- Sample scenario for SOAP services request -->
    <scenario name="Sample WS stream scenario">
        <step name="Step 1"
              url="http://wsdot.wa.gov/traffic/api/WeatherInformation/WeatherInformation.svc">
            <schedule-simple interval="35" units="Seconds" repeatCount="10"/>
            <request>
                <![CDATA[
                    SOAPAction:http://tempuri.org/IWeatherInformation/GetCurrentWeatherInformationByStationID
                    <tem:GetCurrentWeatherInformationByStationID xmlns:tem="http://tempuri.org/">
                        <tem:AccessCode>aeb652b7-f6f5-49e6-9bdb-e2b737ebd507</tem:AccessCode>
                        <tem:StationID>1909</tem:StationID>
                    </tem:GetCurrentWeatherInformationByStationID>
                ]]>
            </request>
        </step>
    </scenario>
    <!-- Sample scenario for System Cmd invocation -->
    <scenario name="Sample CMD stream scenario">
        <step name="Step Windows">
            <request>typeperf "\Processor(_Total)\% Processor Time" -sc 1</request>
            <schedule-simple interval="25" units="Seconds" repeatCount="-1"/>
        </step>
    </scenario>
```

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Buffered streams parameters'](#buffered-streams-parameters).

#### Redirect TNT4J Stream parameters

 * FileName - the system-dependent file name. (Required - just one `FileName` or `Port`)
 * Port - port number to accept character stream over TCP/IP. (Required - just one `FileName` or `Port`)
 * RestartOnInputClose - flag indicating to restart Server Socket (open new instance) if listened one gets closed or fails to accept 
 connection. (Optional)
 * BufferSize - maximal buffer queue capacity. Default value - 512. (Optional)
 * BufferOfferTimeout - how long to wait if necessary for space to become available when adding data item to buffer queue. Default value - 
 `45sec`. (Optional)
 
    sample:
```xml
    <property name="FileName" value="tnt4j-stream-activities.log"/>
```
or
```xml
    <property name="Port" value="9009"/>
    <property name="RestartOnInputClose" value="true"/>
    <property name="BufferSize" value="1024"/>
    <property name="BufferOfferTimeout" value="65"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters).

#### Ms Excel Stream parameters

 * FileName - the system-dependent file name of MS Excel document. (Required)
 * SheetsToProcess - defines workbook sheets name filter mask (wildcard or RegEx) to process only sheets which names
 matches this mask. Default value - ``. (Optional)

    sample:
```xml
    <property name="FileName" value="./tnt4j-streams-msoffice/samples/xlsx-rows/sample.xlsx"/>
    <property name="SheetsToProcess" value="Sheet(1|8|12)"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters) and ['Parseable streams parameters'](#parseable-streams-parameters).

##### Ms Excel Rows Stream parameters

 * RangeToStream - defines streamed data rows index range. Default value - `1:`. (Optional)

    sample:
```xml
    <property name="FileName" value="./tnt4j-streams-msoffice/samples/xlsx-rows/sample.xlsx"/>
    <property name="SheetsToProcess" value="Sheet(1|8|12)"/>
    <property name="RangeToStream" value="5:30"/>
```

### Parsers configuration

#### Generic parser parameters
 
 * UseActivityDataAsMessageForUnset - flag indicating weather RAW activity data shall be put into field `Message` if there is no mapping 
 defined for that field in stream parser configuration or value was not resolved by parser from RAW activity data. **NOTE:** it is 
 recommended to use it for **DEBUGGING** purposes only. For a production version of your software, remove this property form stream parser 
 configuration. Default value - `false`. (Optional)
 
    sample:
```xml
    <property name="UseActivityDataAsMessageForUnset" value="true"/>
``` 

#### Activity Name-Value parser

 * FieldDelim - fields separator. Default value - `,`. (Optional)
 * ValueDelim - value delimiter. Default value - `=`. (Optional)
 * Pattern - pattern used to determine which types of activity data string this parser supports. When `null`, all
 strings are assumed to match the format supported by this parser. Default value - `null`. (Optional)
 * StripQuotes - whether surrounding double quotes should be stripped from extracted data values. Default value -
 `true`. (Optional)

    sample:
```xml
    <property name="FieldDelim" value=";"/>
    <property name="ValueDelim" value="-"/>
    <property name="Pattern" value="(\S+)"/>
    <property name="StripQuotes" value="false"/>
```

Also see [Generic parser parameters](#generic-parser-parameters).

#### Activity RegEx parser

 * Pattern - contains the regular expression pattern that each data item is assumed to match. (Required)

    sample:
* index-capturing groups:
```xml
    <property name="Pattern" value="((\S+) (\S+) (\S+))"/>
```
* named-capturing groups:
```xml
    <property name="Pattern"><![CDATA[
        (?<CoID>.*)\.(?<ProcessArea>.*)\.(?<InterfaceID>.*)\.(?<HopNr>.*)
    ]]></property>
```

Also see [Generic parser parameters](#generic-parser-parameters).

#### Activity token parser

 * FieldDelim - fields separator. Default value - `,`. (Optional)
 * Pattern - pattern used to determine which types of activity data string this parser supports. When `null`, all
 strings are assumed to match the format supported by this parser. Default value - `null`. (Optional)
 * StripQuotes - whether surrounding double quotes should be stripped from extracted data values. Default value -
 `true`. (Optional)

    sample:
```xml
    <property name="FieldDelim" value=";"/>
    <property name="Pattern" value="(\S+)"/>
    <property name="StripQuotes" value="false"/>
```

Also see [Generic parser parameters](#generic-parser-parameters).

#### Activity XML parser

This parser uses XPath expressions as field locators. You may also use [TNT4J-Streams predefined custom XPath functions](#tnt4j-streams-predefined-custom-xpath-functions).

 * Namespace - additional XML namespace mappings. Default value - `null`. (Optional)
 * RequireDefault - indicates that all attributes are required by default. Default value - `false`. (Optional)

    sample:
```xml
    <property name="Namespace" value="xsi=http://www.w3.org/2001/XMLSchema-instance"/>
    <property name="Namespace" value="tnt4j=https://jkool.jkoolcloud.com/jKool/xsds"/>
    <property name="RequireDefault" value="true"/>
```

Also see [Generic parser parameters](#generic-parser-parameters).

#### Message activity XML parser

 * SignatureDelim - signature fields delimiter. Default value - `,`. (Optional)

    sample:
```xml
    <property name="SignatureDelim" value="#"/>
```

Also see ['Activity XML parser'](#activity-xml-parser) and [Generic parser parameters](#generic-parser-parameters).

#### Apache access log parser

 * LogPattern - access log pattern. (Optional, if RegEx `Pattern` property is defined)
 * ConfRegexMapping - custom log pattern token and RegEx mapping. (Optional, actual only if `LogPattern` property is used)

    sample:
```xml
    <property name="LogPattern" value="%h %l %u %t &quot;%r&quot; %s %b %D"/>
    <property name="ConfRegexMapping" value="%h=(\S+)"/>
    <property name="ConfRegexMapping" value="%*s=(\d{3})"/>
    <property name="ConfRegexMapping" value="%*r=(((\S+) (.*?)( (\S+)|()))|(-))"/>
    <property name="ConfRegexMapping" value="%*i=(.*?)"/>
```
 or defining complete RegEx pattern to match log lines:
```xml
    <property name="Pattern"
              value="^(\S+) (\S+) (\S+) \[([\w:/]+\s[+\-]\d{4})\] &quot;(((\S+) (.*?)( (\S+)|()))|(-))&quot; (\d{3}) (\d+|-)( (\S+)|$)"/>
```
 or defining named RegEx group mapping for log pattern token:
```xml
    <property name="ConfRegexMapping"><![CDATA[%*r=(?<request>((?<method>\S+) (?<uri>.*?)( (?<version>\S+))?)|(-))]]></property>
```

RegEx group names and log pattern tokens mapping:

|	REGroupName	|	Format String	|	Description	|
|	---	|	---	|	---	|
|	address	|	%a	|	Client IP address of the request (see the mod_remoteip module).	|
|	local_address	|	%A	|	Local IP-address.	|
|	size	|	%B	|	Size of response in bytes, excluding HTTP headers.	|
|	size_clf	|	%b	|	Size of response in bytes, excluding HTTP headers. In CLF format, e.g., a '-' rather than a 0 when no bytes are sent.	|
|	cookie	|	%{VARNAME}C	|	The contents of cookie VARNAME in the request sent to the server. Only version 0 cookies are fully supported.	|
|	req_time	|	%D	|	The time taken to serve the request, in microseconds.	|
|	env_variable	|	%{VARNAME}e	|	The contents of the environment variable VARNAME.	|
|	filename	|	%f	|	Filename.	|
|	hostname	|	%h	|	Remote hostname. Will log the IP address if HostnameLookups is set to Off, which is the default. If it logs the hostname for only a few hosts, you probably have access control directives mentioning them by name. See the Require host documentation.	|
|	protocol	|	%H	|	The request protocol.	|
|	variable	|	%{VARNAME}i	|	The contents of VARNAME: header line(s) in the request sent to the server. Changes made by other modules (e.g. mod_headers) affect this. If you're interested in what the request header was prior to when most modules would have modified it, use mod_setenvif to copy the header into an internal environment variable and log that value with the %{VARNAME}edescribed above.	|
|	keep_alive	|	%k	|	Number of keepalive requests handled on this connection. Interesting if KeepAlive is being used, so that, for example, a '1' means the first keepalive request after the initial one, '2' the second, etc...; otherwise this is always 0 (indicating the initial request).	|
|	logname	|	%l	|	Remote logname (from identd, if supplied). This will return a dash unless mod_ident is present and IdentityCheck is set On.	|
|	method	|	%m	|	The request method.	|
|	note	|	%{VARNAME}n	|	The contents of note VARNAME from another module.	|
|	header_var	|	%{VARNAME}o	|	The contents of VARNAME: header line(s) in the reply.	|
|	port	|	%{format}p	|	The canonical port of the server serving the request, or the server's actual port, or the client's actual port. Valid formats are canonical, local, or remote.	|
|	process	|	%{format}P	|	The process ID or thread ID of the child that serviced the request. Valid formats are pid, tid, and hextid. hextid requires APR 1.2.0 or higher.	|
|	query	|	%q	|	The query string (prepended with a ? if a query string exists, otherwise an empty string).	|
|	line	|	%r	|	First line of request.	|
|	resp_handler	|	%R	|	The handler generating the response (if any).	|
|	status	|	%s	|	Status. For requests that have been internally redirected, this is the status of the original request. Use %>s for the final status.	|
|	time	|	%{format}t	|	The time, in the form given by format, which should be in an extended strftime(3) format (potentially localized). If the format starts with begin: (default) the time is taken at the beginning of the request processing. If it starts with end: it is the time when the log entry gets written, close to the end of the request processing. In addition to the formats supported by strftime(3), the following format tokens are supported:	|
|	req_time_ext	|	%{UNIT}T	|	The time taken to serve the request, in a time unit given by UNIT. Valid units are ms for milliseconds, us for microseconds, and s for seconds. Using s gives the same result as %Twithout any format; using us gives the same result as %D. Combining %T with a unit is available in 2.4.13 and later.	|
|	user	|	%u	|	Remote user if the request was authenticated. May be bogus if return status (%s) is 401 (unauthorized).	|
|	uri	|	%U	|	The URL path requested, not including any query string.	|
|	server_name	|	%v	|	The canonical ServerName of the server serving the request.	|
|	server_name_ext	|	%V	|	The server name according to the UseCanonicalName setting.	|
|	con_status	|	%X	|	Connection status when response is completed:	|
|	received	|	%I	|	Bytes received, including request and headers. Cannot be zero. You need to enable mod_logio to use this.	|
|	sent	|	%O	|	Bytes sent, including headers. May be zero in rare cases such as when a request is aborted before a response is sent. You need to enable mod_logio to use this.	|

Also see [Generic parser parameters](#generic-parser-parameters).

#### Activity map parser
 
 * LocPathDelim - locator path in map delimiter. Empty value means locator value should not be delimited into path elements. 
 Default value - `.`. (Optional)
 
    sample:
```xml
    <property name="LocPathDelim" value="/"/>
``` 
 
Also see [Generic parser parameters](#generic-parser-parameters).
 
#### Activity JSON parser

 * ReadLines - indicates that complete JSON data package is single line. Default value - `true`. (Optional)

    sample:
```xml
    <property name="ReadLines" value="false"/>
```

Also see [Generic parser parameters](#generic-parser-parameters).

#### Activity JMS message parser

 * ConvertToString - flag indicating whether to convert message payload `byte[]` data to string. Applicable to `BytesMessage` and 
 `StreamMessage`. Default value - `false`. (Optional)
 
    sample:
```xml
    <property name="ConvertToString" value="true"/>
```
 
Also see ['Activity map parser'](#activity-map-parser) and [Generic parser parameters](#generic-parser-parameters).

#### Activity PCF parser

 * TranslateNumValues - indicates that parser should translate resolved numeric values to corresponding MQ constant names if possible and 
 field/locator data type is `String` (meaning translated value can be assigned to field). If value of particular field should be left as 
 number (e.g., `ReasonCode`), use field/locator attribute `datatype="Number"`. Default value - `true`. (Optional)
 
    sample:
```xml
    <property name="TranslateNumValues" value="false"/>
```

Also see [Generic parser parameters](#generic-parser-parameters).

### Pre-parsers

`TNT4J-Streams` architecture has entity `pre-parser` defining data transformation algorithm to convert RAW activity data to format, supported by 
stream used parser. Pre-parsers shall implement [`ActivityDataPreParser`](./tnt4j-streams-core/src/main/java/com/jkoolcloud/tnt4j/streams/preparsers/ActivityDataPreParser.java) 
interface. 

Sample stream configuration using pre-parsers:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <java-object name="XMLFromRFH2PreParser" class="com.jkoolcloud.tnt4j.streams.preparsers.XMLFromBinDataPreParser">
        <!--IF RAW ACTIVITY DATA STRING IS ENCODED - USE "format" PARAMETER. SEE BELOW -->
        <!--<param name="format" value="base64Binary" type="com.jkoolcloud.tnt4j.streams.fields.ActivityFieldFormatType"/>-->
        <!--<param name="format" value="base64Binary" type="java.lang.String"/>-->
    </java-object>

    <parser name="RFH2XMLParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <reference name="XMLFromRFH2PreParser"/>

        <field name="EventType" value="Event"/>
        <field name="ApplName" value="Sample"/>
        <field name="ADPCount" locator="/root/usr/ADPSegCont" locator-type="Label"/>
    </parser>

    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.BytesInputStream">
        <property name="FileName" value="./tnt4j-streams-core/samples/XML-from-bin-data/RFH2.dump"/>
        <property name="RestoreState" value="false"/>

        <parser-ref name="RFH2XMLParser"/>
    </stream>
</tnt-data-source>
```

Sample configuration defines `FileStream` reading binary data from `RFH2.dump` file. Stream refers activity xml parser `RFH2XMLParser`.

`RFH2XMLParser` parser has reference to `XMLFromRFH2PreParser`. This pre-parser builds valid XML from provided binary data having fragmented 
XML data like `RFH2` structure from IBM MQ.

**NOTE:** If RAW activity data is not `byte[]` but appears to be encoded (e.g., BASE64), use pre-parser property `foramat` to define RAW activity data 
format. It may be one of [`ActivityFieldFormatType`](./tnt4j-streams-core/src/main/java/com/jkoolcloud/tnt4j/streams/fields/ActivityFieldFormatType.java) 
enumeration values: 
* `base64Binary` - BASE64 encoded binary data
* `hexBinary` - binary data represented as bytes HEX codes sequence 
* `string` - binary data represented as plain string
* `bytes` - binary data as `byte[]` (default format value).

### External resources import into stream configuration

`TNT4J-Streams` allows to have external resources importer to stream data source configuration.

External values mapping resources import sample:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Nastel/tnt4j-streams/master/config/tnt-data-source.xsd">

    <resource-ref id="MFT_MAPPINGS" type="ValuesMap" uri="./tnt4j-streams-wmq/samples/mft_fte/mft_mappings.json"/>
    <!--<resource-ref id="MFT_MAPPINGS_COMPCODE" type="ValuesMap" uri="./tnt4j-streams-wmq/samples/mft_fte/CompCode.csv" separator=","/>-->

    <parser name="ResMappingParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityXmlParser">
        <field name="EventType" locator="name(//*[1])" locator-type="Label">
            <field-map-ref resource="MFT_MAPPINGS.EventType"/>
        </field>
        <field name="EventName" locator="name(//*[1])" locator-type="Label">
            <field-map-ref resource="MFT_MAPPINGS.EventName"/>
        </field>

        <field name="Direction" locator="name(//*[1])" locator-type="Label" transparent="true"/>

        <field name="ResourceName" formattingPattern="{0}={1};Agent={2}">
            <!--resolves FILE or QUEUE-->
            <field-locator locator="name(//*/*)" locator-type="Label">
                <field-map-ref resource="MFT_MAPPINGS.Resource"/>
            </field-locator>
            <!--resolves file or queue name -->
            <field-locator locator="ts:getFileName(/${Direction}/file)" locator-type="Label" required="false"/>
            <field-locator locator="ts:getObjectName(/${Direction}/queue)" locator-type="Label" required="false"/>
            <!-- agent-->
            <field-locator locator="/transaction/${Direction}Agent/@agent" locator-type="Label" required="false"/>
        </field>
        <field name="CompCode" locator="/transaction/status/@resultCode" locator-type="Label">
            <field-map-ref resource="MFT_MAPPINGS.CompCode"/>
        </field>
        <field name="CompCode2" locator="/transaction/status/@resultCode" locator-type="Label">
            <field-map-ref resource="MFT_MAPPINGS.CompCode2"/>
        </field>
        ...
    </parser>

    <stream name="ResMappingStream">
        ...
        <parser-ref name="ResMappingParser"/>
    </stream>
</tnt-data-source>
```

Sample shows how stream data source configuration imports (over `resource-ref` tag) external resource file 
`./tnt4j-streams-wmq/samples/mft_fte/mft_mappings.json` using relative file path.

`resource-ref` tag attributes:
* `id` - is resource identifier and mus be unique. Required.
* `type` - defines type of imported resource, and currently there is only one available value - `ValuesMap` meaning resource file defines 
source-target values mapping. Required.
* `uri` - defines absolute URL or relative path of referenced resource. Required. Supported external resource values mapping definition 
formats:
    * JSON
    ```json
    {
        "EventType": {
            "destination": "SEND",
            "source": "RECEIVE",
            "": "NOOP"
        },

        "EventName": {
            "destination": "process_destination",
            "source": "process_source",
            "": "N/A"
        },

        "Resource": {
            "file": "FILE",
            "queue": "QUEUE"
        },

        "CompCode": {
            "0": "SUCCESS",
            "40": "WARNING",
            "": "ERROR"
        },

        "CompCode2": {
            "0": "SUCCESS",
            "40": "WARNING",
            "1": "FAILURE",
            "": "ERROR"
        }
    }
    ```
    * CSV
    ```csv
    0,SUCCESS
    40,WARNING
    , ERROR
    ```
    * Properties 
    ```properties
    0:SUCCESS
    40:WARNING
    : ERROR
    ```

    **NOTE:** `JSON` can have multiple mapping definition sets, while `CSV` and `Properties` formats supports only one mapping set definition.

* `separator` - defines source and target values delimiter and is applicable to `CSV` and `Properties` formatted resources. Optional.

## ZooKeeper stored configuration

`TNT4J-Streams` has ability to load configuration not only from configuration files, but either from ZooKeeper nodes stored data. ZooKeeper 
allows handle configuration data in more centralized way.

`TNT4J-Streams` is able to load ZooKeeper stored configuration on application startup and also to monitor referenced ZooKeeper nodes for 
configuration data changes, making it possible to apply new configuration on runtime.

### Uploading configuration data to ZooKeeper

To upload files contained `TNT4J-Streams` configuration to ZooKeeper nodes there is utility called `ZKConfigInit`. It has those program 
arguments:
* `-f:` - defines file reference containing uploader utility configuration. (Required)
* `-c` - indicates to clean ZooKeeper nodes contained TNT4J-Streams configuration. (Optional)

To run it use `./bin/zk-init-cfg.bat` or `./bin/zk-init-cfg.sh` files. It uses uploader configuration file `./config/zk-init-cfg.properties`.
 
Uploader configuration properties file contents:
```properties
# ZooKeeper server parameters
zk.conn=localhost:2181
#zk.conn.timeout=5000
zk.streams.path=/tnt4j-streams

# generic app configurations
config.logger=../config/log4j.properties
config.tnt4j=../config/tnt4j.properties
config.tnt4j-kafka=./config/tnt4j-kafka.properties

# core module samples configurations
samples.core.ajax=../samples/ajax/tnt-data-source.xml
samples.core.angular-js-tracing=../samples/angular-js-tracing/tnt-data-source.xml
samples.core.single-log=../samples/single-log/tnt-data-source.xml
...
```

`zk.*` properties defines ZK connection configuration settings. 

**NOTE:** `zk.streams.path` property defines root ZK node of streams configuration. All other configuration data nodes will be uploaded 
under this initial ZK path.

**NOTE:** Do not start you configuration files referencing properties with `zk.`. Such streams configuration reference definition properties
will be ignored and files referenced by these properties wont be used in upload process. 

`config.*` and `samples.*` properties defines streams configuration files data mapping to ZK nodes. 

Mapping works this way: property name defines ZK node path (under root configuration node defined using `zk.streams.path` property) and 
property value is reference to a file containing configuration data to be uploaded to ZK. 

So in upload process property name like `samples.core.single-log` turns to ZK node path `/samples/core/single-log` (full upload ZK path 
would be `/tnt4j-streams/samples/core/single-log`). You can freely name properties according to your environment organization, but it must 
comply properties naming specification to handle it as property and also must comply ZK node path specification after "translation".

### Loading ZooKeeper stored configuration data

To make `TNT4J-Streams` load configuration from ZooKeeper use program argument `-z:` referencing `TNT4J-Streams` ZooKeeper configuration 
file, e.g., `-z:./tnt4j-streams-core/samples/zookeeper-cfg/stream-zk.properties`. 

Program argument `-z:` can be used in common with `-f:` argument. Then streams will try to load ZooKeeper stored configuration first and if
fails - then loads configuration from file referenced by `-f:` argument.

Sample streams ZooKeeper configuration contents:
```properties
# ZooKeeper server parameters
zk.conn=localhost:2181/tnt4j-streams
#zk.conn.timeout=5000

# logger configuration: log4j properties, logback xml/groovy
config.logger.zk.path=/config/logger

# TNT4J configuration: properties
config.tnt4j.zk.path=/config/tnt4j
#config.tnt4j-kafka.zk.path=/config/tnt4j-kafka

# Stream configuration: XML containing <tnt-data-source/>
config.stream.zk.path=/samples/core/single-log
```

`zk.*` properties defines ZK connection configuration settings.

`config.logger.zk.path` property defines ZK node path containing logger configuration data. If absent system property 
`-Dlog4j.configuration` (or `-Dlogback.configurationFile` - depending on logging framework used) defined configuration file will be used.
**NOTE:** currently supports `log4j`, `JUL` and `Logback` logging frameworks configuration handling.

`config.tnt4j.zk.path` property defines ZK node path containing TNT4J configuration data. If absent system property 
`-Dtnt4j.config` defined configuration file will be used.

`config.tnt4j-kafka.zk.path` property is reserved to define ZK node path containing TNT4J-Kafka configuration data. It is not currently 
used. //TBD 

`config.stream.zk.path` property defines ZK node path containing stream configuration data. If absent program argument `-f:` defined 
configuration file will be used.

### Streams registry

`TNT4J-Streams` also able to contain all ZooKeeper configuration in one single file. See [`streams-zk.properties`](config/streams-zk.properties).

File consists of ZK configuration registry entities every entity having two properties (except `zk.*` properties set used to configure ZK 
connection):
* `cfg.entity.id` + `.zk.path` - defines ZK node path containing entity configuration data. **NOTE:** actual path in ZK ensemble may not 
necessarily exist and will be created on demand. 
* `cfg.entity.id` + `.cfg.file` - defines configuration file path to upload configuration data to ZK node in case ZK node does not exist or 
is empty. 

Sample streams ZooKeeper configuration registry stile contents:
```properties
########################### ZooKeeper server parameters ###########################
# ZooKeeper connection string
zk.conn=localhost:2181/tnt4j-streams
# ZooKeeper connection timeout
#zk.conn.timeout=5000
# ZooKeeper path of TNT4J-Streams root node location
zk.streams.path=/tnt4j-streams

########################### Generic app configurations ###########################
# Logger configuration: log4j properties, logback xml/groovy
config.logger.zk.path=/config/logger
config.logger.cfg.file=../config/log4j.properties

# TNT4J configuration
config.tnt4j.zk.path=/config/tnt4j
config.tnt4j.cfg.file=../config/tnt4j.properties

# TNT4J-Kafka configuration (used by KafkaEventSink)
config.tnt4j-kafka.zk.path=/config/tnt4j-kafka
config.tnt4j-kafka.cfg.file=../config/tnt4j-kafka.properties

########################### Sample Streams configurations ###########################
### core module samples configurations
# sample stream: samples.core.ajax
samples.core.ajax.zk.path=/samples/core/ajax
samples.core.ajax.cfg.file=../samples/ajax/tnt-data-source.xml

# sample stream: samples.core.multiple-logs
samples.core.multiple-logs.zk.path=/samples/core/multiple-logs
samples.core.multiple-logs.cfg.file=../samples/multiple-logs/tnt-data-source.xml

# sample stream: samples.core.piping-stream
samples.core.piping-stream.zk.path=/samples/core/piping-stream
samples.core.piping-stream.cfg.file=../samples/piping-stream/parsers.xml

# sample stream: samples.core.single-log
samples.core.single-log.zk.path=/samples/core/single-log
samples.core.single-log.cfg.file=../samples/single-log/tnt-data-source.xml
...
```

`zk.*` properties defines ZK connection configuration settings.

`config.*` properties defines configuration entities for `logger`, `tnt4j` and `tnt4j-kafka`.

`samples.*` properties defines configuration entities for sample steams.

Also see [Loading ZooKeeper stored configuration data](#loading-zookeeper-stored-configuration-data).

When using registry style ZooKeeper configuration, missing configuration entities ZK nodes data is automatically uploaded on demand from 
entity configuration file property (ending `.cfg.file`) referenced file.

To run `TNT4J-Streams` using registry style ZooKeeper configuration define those program arguments: `-z:<cfg_file> -sid:<stream_id>`, e.g.,
```cmd
-z:.\config\streams-zk.properties -sid:samples.core.single-log
```
Streams will run sample stream `single-log`. See [Single Log file](#single-log-file) for sample details.

How to Build TNT4J-Streams
=========================================
## Modules

* (M) marked modules are mandatory 
* (O) marked modules are optional 
* (U) marked modules are utility modules (e.g., performs compiled assemblies packaging)

Modules list:
   * `Core` (M)
   * `Flume-Plugin` (O)
   * `Hdfs` (O)
   * `JMS` (O)
   * `Kafka` (O)
   * `Mqtt` (O)
   * `WMQ` (O)
   * `Ws` (O)
   * `MsOffice` (O)
   * `Samples` (O)
   * `Distribution` (OU)

All optional modules (extensions) depends to `core` module and can't be build and run without it.

**NOTE:** `Samples` module provides no additional features to TNT4J streaming framework. It contains only streams API use samples.
**NOTE:** `Distribution` module performs `maven post build` release assemblies delivery to `../build/tnt4j-streams` directory.

## Requirements
* JDK 1.7+
* [Apache Maven 3](https://maven.apache.org/)
* [TNT4J](https://github.com/Nastel/TNT4J)
* [JESL](https://github.com/Nastel/JESL)

All other required dependencies are defined in project modules `pom.xml` files. If maven is running online mode it should download these 
defined dependencies automatically.

### Manually installed dependencies
Some of required and optional dependencies may be not available in public [Maven Repository](http://repo.maven.apache.org/maven2/). In this 
case we would recommend to download those dependencies manually into module's `lib` directory and install into local maven repository by 
running maven script `lib/pom.xml` with `install` goal. For example see [`tnt4j-streams/tnt4j-streams-wmq/lib/pom.xml`](tnt4j-streams-wmq/lib/pom.xml) 
how to do this.

#### `WMQ` module

**NOTE:** Because this module requires manually downloaded libraries, it is commented out in main project pom file `tnt4j-streams/pom.xml` by 
default. If You want to use it uncomment this line of `pom.xml` file. But `WMQ` module will be ready to build only when manually downloaded 
libraries will be installed to local maven repository.

What to download manually or copy from your existing IBM MQ installation:
* IBM MQ 8.0 or 9.0

Download the above libraries and place into the `tnt4j-streams/tnt4j-streams-wmq/lib` directory like this:
```
    lib
     + ibm.mq-v8.0
         |- com.ibm.mq.allclient.jar
         |- com.ibm.mq.traceControl.jar
         |- fscontext.jar
         |- jms.jar
         |- JSON4J.jar
         |- providerutil.jar
     + ibm.mq-v9.0
         |- bcpkix-jdk15on-152.jar
         |- bcprov-jdk15on-152.jar
         |- com.ibm.mq.allclient.jar
         |- com.ibm.mq.traceControl.jar
         |- fscontext.jar
         |- jms.jar
         |- JSON4J.jar
         |- providerutil.jar
```
(O) marked libraries are optional

## Building
   * to build project and make release assemblies run maven goals `clean package`
   * to build project, make release assemblies and install to local repo run maven goals `clean install`

By default maven will build all modules defined in `tnt4j-streams/pom.xml` file. 

If You do not want to build some of optional modules, comment those out like `WMQ` module is. Or You can define maven to build your 
preferred set of modules using `-pl, --projects` argument (comma separated modules list) together with `-am, --also-make` argument, e.g.:

```cmd
mvn -pl tnt4j-streams-core,tnt4j-streams-samples,tnt4j-streams--distribution -am clean install
``` 
or
```cmd
mvn --projects tnt4j-streams-core,tnt4j-streams-samples,tnt4j-streams--distribution --also-make clean install
```

**NOTE:** modules list should be without spaces after comma!

Issuing these commands, maven will build only `tnt4j-streams-core`, `tnt4j-streams-samples` and `tnt4j-streams--distribution` modules.

Release assemblies are built to `../build/tnt4j-streams` directory.

**NOTE:** sometimes maven fails to correctly handle dependencies. If dependency configuration looks fine, but maven still complains about 
missing dependencies try to delete local maven repository by hand: e.g., on MS Windows delete contents of `c:\Users\[username]\.m2\repository` 
directory.

## Running samples

See 'Running TNT4J-Streams' chapter section ['Samples'](#samples).

Testing of TNT4J-Streams
=========================================

## Requirements
* [JUnit 4](http://junit.org/)
* [Mockito](http://mockito.org/)

## Testing using maven
Maven tests run is disabled by default. To enable Maven to run tests set Maven command line argument 
`-DskipTests=false`.

## Running manually from IDE
* in `core` module run JUnit test suite named `AllStreamsCoreTests`
* in `flume-plugin` module run JUnit test suite named `AllFlumeTests`
* in `hdfs` module run JUnit test suite named `AllHdfsStreamTests`
* in `jms` module run JUnit test suite named `AllJMSStreamTests`
* in `kafka` module run JUnit test suite named `AllKafkaStreamTests`
* in `mqtt` module run JUnit test suite named `AllMqttStreamTests`
* in `wmq` module run JUnit test suite named `AllWmqStreamTests`
* in `ws` module run JUnit test suite named `AllWsStreamTests`
* in `msoffice` module run JUnit test suite named `AllMsOfficeStreamTests`

Known Projects Using TNT4J
===============================================
* [TNT4J-Streams-Zorka](https://github.com/Nastel/tnt4j-streams-zorka)
* [TNT4J-Streams-Syslogd](https://github.com/Nastel/tnt4j-streams-syslogd)
* [TNT4J-Streams-IBM-B2Bi](https://github.com/Nastel/tnt4j-streams-ibm-b2bi)
