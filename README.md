# TNT4J-Streams
TNT4J Streams allows data streaming, parsing from various data sources into TNT4J event sinks.

Why TNT4J-Streams
======================================

* TNT4J-Streams can be run out of the box for a large set of data streaming without writing no additional code.
All You need is to define Your data format mapping to TNT4J event mapping in TNT4J-Streams configuration.

* It supports the following data sources:
    * File
    * Character stream over TCP/IP
    * HDFS
    * MQTT
    * HTTP
    * JMS
    * Apache Kafka
    * Apache Flume
    * Logstash
    * WMQ
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

* Has customized parser to parse Apache Access Logs.

* It can be integrated with:
    * Logstash
    * Apache Flume
    * Angulartics
    * Collectd
    * Nagios    

just by applying configuration and without additional coding.

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

## TNT4J-Streams can be run:
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
* `fieled-map` tag is used to perform manual mapping from streamed data value `source` to field value `target.`

sample:
```xml
    <parser name="TokenParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityTokenParser">
        ...
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
     * Name to assign to activity entry. Examples are operation, method, API
     * call, event, etc.
     */
    EventName(String.class),

    /**
     * Type of activity - Value must match values in
     * {@link com.jkoolcloud.tnt4j.core.OpType} enumeration.
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
     * Indicates completion status of the activity - Value must match values in
     * {@link com.jkoolcloud.tnt4j.core.OpCompCode} enumeration.
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
     * Indicates completion status of the activity - Value can either be label
     * from {@link com.jkoolcloud.tnt4j.core.OpLevel} enumeration or a numeric
     * value.
     */
    Severity(Enum.class),

    /**
     * Location that activity occurred at.
     */
    Location(String.class),

    /**
     * Identifier used to correlate/relate activity entries to group them into
     * logical entities.
     */
    Correlator(String[].class),

    /**
     * User-defined label to associate with the activity, generally for locating
     * activity.
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
     * Identifier used to uniquely identify the data associated with this
     * activity.
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
     * Identifier used to uniquely identify parent activity associated with this
     * activity.
     */
    ParentId(String.class);
```

NOTE: `EventType` field is mandatory and can't have value `null`.

NOTE: Custom fields values can be found as activity event properties:

sample:
```xml
    <field name="Transport" locator="ActivityTransport" locator-type="Label"/>
    <field name="Topic" locator="TopicName" locator-type="Label"/>
```

### Field locator types

```java
    /**
     * Indicates that raw data value is the value of a named property of the
     * current stream.
     */
    StreamProp,
    
    /**
     * Indicates that raw data value is at a specified index location, offset,
     * etc. This is a generic index/offset value whose interpretation is up to
     * the specific parser applying the locator.
     */
    Index,
    
    /**
     * Indicates that raw data value is the value of a particular key or label.
     * Examples of this are XPath expressions for XML elements, and where each
     * element of a raw activity data string is a name/value pair.
     */
    Label,
    
    /**
     * Indicates that raw data value is the value of a specific regular
     * expression group, for parsers that interpret the raw activity data using
     * a regular expression pattern defined as a sequence of groups.
     */
    REGroupNum,
    
    /**
     * Indicates that raw data value is the value of a specific regular
     * expression match, for parsers that interpret the raw activity data using
     * a regular expression pattern defined as a sequence of repeating match
     * patterns.
     */
    REMatchNum
```

NOTE: `Index` is default value and may be suppressed in field/locator definition:

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

NOTE: activity event will contain all fields processed by all stacked parsers.

To define stacked parser You have to define `parser-ref` tad in parser `field` definition.

sample:
```xml
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

    <parser name="AccessLogParserCommon" class="com.jkoolcloud.tnt4j.streams.custom.parsers.ApacheAccessLogParser">
        ...
    </parser>

    <parser name="SampleJMSParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJMSMessageParser">
        ...
        <field name="MsgBody" locator="ActivityData" locator-type="Label">
            <parser-ref name="AccessLogParserCommon"/>
        </field>
        ...
    </parser>

    <stream name="SampleJMStream" class="com.jkoolcloud.tnt4j.streams.inputs.JMSStream">
        ...
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

## Samples

### Running samples
When release assemblies are built, samples are located in `samples` directory i.e.
`../build/tnt4j-streams/tnt4j-streams-1.0.0/samples`.
To run desired sample:
* go to sample directory
* run `run.bat` or `run.sh` depending on Your OS

For more detailed explanation of streams and parsers configuration and usage see chapter ['Configuring TNT4J-Streams'](#configuring-tnt4j-streams)
and JavaDocs.

#### Single Log file

This sample shows how to stream activity events (orders) data from single log file.

Sample files can be found in `samples/single-log` directory.

`orders.log` file contains set of order activity events. Single file line defines data of single order activity event.

NOTE: records in this file are from year `2011` i.e. `12 Jul 2011`, so then getting events data in JKoolCloud
please do not forget to just to dashboard time frame to that period!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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
        <parser-ref name="TokenParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `FileLineStream` referencing `TokenParser` shall be used.

`FileStream` reads data from `orders.log` file.

`TokenParser` uses `|` symbol as fields delimiter and maps fields to TNT4J event fields using field index locator.

Note: `StartTime` fields defines format and locale to correctly parse field data string. `EventType` uses manual
field string mapping to TNT4J event field value.

#### Multiple Log files

This sample shows how to stream activity events (orders) data from multiple log files using file name matching
wildcard pattern.

Sample files can be found in `samples/multiple-logs` directory.

`orders-in.log` and `orders-out.log` files contains set of order activity events. Single file line defines data of
single order activity event.

NOTE: records in this file are from year `2011` i.e. `12 Jul 2011`, so then getting events data in JKoolCloud
please do not forget to just to dashboard time frame to that period!

Sample configuration and sample idea is same as 'Single Log file' with one single difference:
```xml
    <property name="FileName" value="orders-*.log"/>
```
meaning that stream should process not one single file, but file set matching `orders-*.log` wildcard pattern.

#### OS piped stream

This sample shows how to stream activity events (orders) data received over OS pipe from another application or OS
command.

Sample files can be found in `samples/piping-stream` directory.

`orders.log` file contains set of order activity events. Single file line defines data of single order activity event.

NOTE: records in this file are from year `2011` i.e. `12 Jul 2011`, so then getting events data in JKoolCloud
please do not forget to just to dashboard time frame to that period!

`jk-pipe.bat` or `jk-pipe.sh` files are wrappers to `bin/tnt4j-streams` executables to minimize parameters. All what
 you need is to pass file name of stream parsers configuration i.e. `parsers.xml`

`run.bat` or `run.sh` files uses OS piping to run sample.

Sample parser configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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
        <property name="FileName" value=".\tnt4j-streams-core\samples\zip-stream\sample.zip"/>
        <!--<property name="FileName" value=".\tnt4j-streams-core\samples\zip-stream\sample.zip!2/*.txt"/>-->
        <!--<property name="FileName" value=".\tnt4j-streams-core\samples\zip-stream\sample.gz"/>-->
        <!--<property name="ArchType" value="GZIP"/>-->

        <parser-ref name="AccessLogParserExt"/>
        <parser-ref name="AccessLogParserCommon"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `ZipLineStream` referencing `AccessLogParserExt` and `AccessLogParserCommon` shall be
used.

`ZipLineStream` reads all entries lines from `sample.zip` file.

To filter zip file entries use zip entry name wildcard pattern i.e. `sample.zip!2/*.txt`. In this case stream will read
just zipped files having extension `txt` from internal zip directory named `2` (from sub-directories also).

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in section
'Apache Access log single file' and 'Parsers configuration # Apache access log parser'.

#### Standard Java InputStream/Reader

This sample shows how to stream activity events (Apache access log records) data read from standard Java input.

Sample files can be found in `samples/java-stream` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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
        <param name="fileName" value=".\tnt4j-streams-core\samples\zip-stream\sample.gz" type="java.lang.String"/>
    </java-object>
    <java-object name="SampleZipStream" class="java.util.zip.GZIPInputStream">
        <param name="stream" value="SampleFileStream" type="java.io.InputStream"/>
    </java-object>
    <!--java-object name="SampleFileReader" class="java.io.FileReader">
        <param name="fileName" value=".\tnt4j-streams-core\samples\apache-access-single-log\access.log"
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

NOTE: that `java-object/param@value` value may be reference to configuration already defined object
like `SampleFileStream` in this particular sample.

To use `Reader` as input you should uncomment configuration lines defining and referring `SampleFileReader` and comment
out `SampleZipStream` reference.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in section
'Apache Access log single file' and 'Parsers configuration # Apache access log parser'.

#### Apache Access log single file

This sample shows how to stream Apache access log records as activity events from single log file.

Sample files can be found in `samples/apache-access-single-log` directory.

`access.log` is sample Apache access log file depicting some HTTP server activity.

NOTE: records in this file are from year `2004` i.e. `07/Mar/2004`, so then getting events data in JKoolCloud
please do not forget to just to dashboard time frame to that period!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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

Note: `StartTime` fields defines format and locale to correctly parse field data string. `CompCode` uses manual
field string mapping to TNT4J event field value.

#### Apache Access log multiple files

This sample shows how to stream Apache access log records as activity events from multiple log files using file name
matching wildcard pattern.

Sample files can be found in `samples/apache-access-multi-log` directory.

`localhost_access_log.[DATE].txt` is sample Apache access log files depicting some HTTP server activity.

NOTE: records in this file are from year `2015` ranging from April until November, so then getting events data
in JKoolCloud please do not forget to just to dashboard time frame to that period!

Sample configuration and sample idea is same as 'Apache Access log single file' with one single difference:
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
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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

`AccessLogParserCommon` is same as in 'Apache Access log single file' sample, so for more details see
['Apache Access log single file'](#apache-access-log-single-file) section.

`FileName` property defines that stream should watch for files matching `localhost_access_log.*.txt` wildcard pattern.
 This is needed to properly handle file rolling.

`FilePolling` property indicates that stream polls files for changes.

`FileReadDelay` property indicates that file changes are streamed every 20 seconds.

`StartFromLatest` property indicates that stream should start from latest entry record in log file. Setting this
property to `false` would stream all log entries starting from oldest file matching wildcard pattern.

NOTE: Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### HDFS

These samples shows how to read or poll HDFS files contents. Samples are very similar to 'Log file polling' or
'Apache Access log single file'. Difference is that specialized stream classes are used.

* Simple HDFS file streaming

Sample files can be found in `tnt4j-streams/tnt4j-streams-hdfs/samples/hdfs-file-stream` directory.

```xml
    <stream name="SampleHdfsFileLineStream" class="com.jkoolcloud.tnt4j.streams.inputs.HdfsFileLineStream">
        <property name="FileName" value="hdfs://127.0.0.1:19000/log.txt*"/>
        ...
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
        ...
    </stream>
```

To poll HDFS file `HdfsFileLineStream` shall be used with property `FilePolling` value set to `true`. `FileName` is
defined using URI starting `hdfs://`.

NOTE: Stream stops only when critical runtime error/exception occurs or application gets terminated.

* Zipped HDFS file streaming

Sample files can be found in `tnt4j-streams/tnt4j-streams-hdfs/samples/hdfs-zip-stream` directory.

```xml
    <stream name="SampleHdfsZipLineStream" class="com.jkoolcloud.tnt4j.streams.inputs.HdfsZipLineStream">
        <property name="FileName"
                  value="hdfs://[host]:[port]/[path]/sample.zip!2/*.txt"/>
        <property name="ArchType" value="ZIP"/>
        ...
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
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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
        <property name="ReadLines" value="true"/>

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

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in section
'Apache Access log single file' and 'Parsers configuration # Apache access log parser'.

NOTE: Stream stops only when critical runtime error/exception occurs or application gets terminated.

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
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

    <parser name="FlumeJSONParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ReadLines" value="true"/>

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

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in section
'Apache Access log single file' and 'Parsers configuration # Apache access log parser'.

#### Logstash RAW data

This sample shows how to stream activity events from redirected Logstash output RAW data. Logstash output is
configured to send RAW output data as JSON to `localhost:9595`. Sample also shows how to use stacked parsers technique
to extract log entry data from JSON envelope.

Sample files can be found in `samples/logstash` directory.

How to configure Logstash see [`samples/logstash/README.MD`](tnt4j-streams-core/samples/logstash/README.MD)

`messages.json` file contains sample Logstash output JSON data package prepared using configuration of this sample. This
sample JSON is for you to see and better understand parsers mappings. Do not use it as Logstash input!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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
        <property name="ReadLines" value="true"/>

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

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in section
'Apache Access log single file' and 'Parsers configuration # Apache access log parser'.

NOTE: Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### Logstash parsed data

This sample shows how to stream activity events from parsed by Logstash. Logstash Grok output plugin is configured to
send parsed Apache Access log entry data as JSON to `localhost:9595`.

Sample files can be found in `samples/logstash-parsed` directory.

How to configure Logstash see [`samples/logstash-parsed/README.MD`](tnt4j-streams-core/samples/logstash-parsed/README.MD)

`messages.json` file contains sample Logstash output JSON data package prepared using configuration of this sample. This
sample JSON is for you to see and better understand parsers mappings. Do not use it as Logstash input!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

    <parser name="LogstashJSONParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <property name="ReadLines" value="true"/>

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
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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

`AccessLogParserCommon` is same as in 'Apache Access log single file' sample, so for more details see
['Apache Access log single file'](#apache-access-log-single-file) section.

NOTE: to parse some other data instead of Apache Access Log, replace `AccessLogParserCommon` with parser which
complies Your data format.

NOTE: Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### HTTP request form

This sample shows how to stream activity events received over HTTP request as form data.

Sample files can be found in `samples/http-form` directory.

How to send HTTP form data see [`samples/http-form/README.md`](tnt4j-streams-core/samples/http-form/README.md)

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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

NOTE: Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### JMS text message

This sample shows how to stream activity events received over JMS transport as text messages. Sample also shows
how to use stacked parsers technique to extract message payload data.

Sample files can be found in `samples/jms-mapmessage` directory.

NOTE: in `jms` module `pom.xml` file uncomment `activemq-all` dependency if You want to use ActiveMQ as JMS service

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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
`Topic` property. To define desired queue use `Queue` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. `JNDIFactory` property defines that ActiveMQ shall be used.
Stream puts received message data to map and passes it to parser.

`SampleJMSParser` maps metadata to activity event data. `ActivityData` entry value is passed to stacked parser named
`AccessLogParserCommon`.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in section
'Apache Access log single file' and 'Parsers configuration # Apache access log parser'.

NOTE: to parse some other data instead of Apache Access Log, replace `AccessLogParserCommon` with parser which
complies Your data format.

NOTE: Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### JMS map message

This sample shows how to stream activity events received over JMS transport as map messages.

Sample files can be found in `samples/jms-textmessage` directory.

NOTE: in `jms` module `pom.xml` file uncomment `activemq-all` dependency if You want to use ActiveMQ as JMS service

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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
`Topic` property. To define desired queue use `Queue` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. `JNDIFactory` property defines that ActiveMQ shall be used.
Stream puts received message data to map and passes it to parser.

`SampleJMSParser` maps activity event data from JMS map message using map entries key labels.

NOTE: Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### JMS object message

This sample shows how to stream activity events received over JMS transport as serializable object messages. Sample
also shows how to use stacked parsers technique to extract message payload data.

Sample files can be found in `samples/jms-objectmessage` directory.

NOTE: in `jms` module `pom.xml` file uncomment `activemq-all` dependency if You want to use ActiveMQ as JMS service

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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
`Topic` property. To define desired queue use `Queue` property. `HaltIfNoParser` property indicates that stream
should skip unparseable entries. `JNDIFactory` property defines that ActiveMQ shall be used.
Stream puts received message data to map and passes it to parser.

`SampleJMSParser` maps metadata to activity event data. `ActivityData` entry value is passed to stacked parser named
`SampleObjectParser`.

`SampleObjectParser` is able to map activity event fields values from serialized object declared fields using field
names as labels.

NOTE: Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### Kafka

This sample shows how to stream activity events received over Apache Kafka transport as messages. Sample also shows
how to use stacked parsers technique to extract message payload data.

Sample files can be found in `samples/kafka` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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
        <property name="zookeeper.connect" value="127.0.0.1:2181"/>
        <property name="group.id" value="TNT4JStreams"/>
        <parser-ref name="KafkaMessageParser"/>
    </stream>
</tnt-data-source>

```

Stream configuration states that `KafkaStream` referencing `KafkaMessageParser` shall be used.

`KafkaStream` connects to server defined using `zookeeper.connect` property, and takes messages from topic defined
`Topic` property. `HaltIfNoParser` property indicates that stream should skip unparseable entries.
Stream puts received message data to map and passes it to parser.

`KafkaMessageParser` maps metadata to activity event data. `ActivityData` entry value is passed to stacked parser named
`AccessLogParserCommon`.

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in section
'Apache Access log single file' and 'Parsers configuration # Apache access log parser'.

NOTE: to parse some other data instead of Apache Access Log, replace `AccessLogParserCommon` with parser which
complies Your data format.

NOTE: Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### MQTT

This sample shows how to stream activity events received over MQTT transport as MQTT messages. Sample also shows how to
use stacked parsers technique to extract message payload data.

Sample files can be found in `samples/mqtt` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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

Details on `AccessLogParserCommon` (or `ApacheAccessLogParser` in general) can be found in section
'Apache Access log single file' and 'Parsers configuration # Apache access log parser'.

NOTE: to parse some other data instead of Apache Access Log, replace `AccessLogParserCommon` with parser which
complies Your data format.

NOTE: Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### WMQ Message broker

This sample shows how to stream activity events received over WMQ as MQ messages.

Sample files can be found in `samples/message-broker` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

    <parser name="EventParser" class="com.jkoolcloud.tnt4j.streams.parsers.MessageActivityXmlParser">
        <property name="SignatureDelim" value="#!#"/>
        <property name="Namespace"
                  value="wmb=http://www.ibm.com/xmlns/prod/websphere/messagebroker/6.1.0/monitoring/event"/>
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

`QueueManager` property defines name of queue manager.

`Queue` property defines name of queue to get messages.

`EventParser` is of type `MessageActivityXmlParser` meaning that it will parse messages de-serialized into XML strings.

`SignatureDelim` property defines that `#!#` should be used as signature delimiter.

`Namespace` property adds `wmb` namespace definition mapping to mapping
`http://www.ibm.com/xmlns/prod/websphere/messagebroker/6.1.0/monitoring/event`.

#### Angulartics (AngularJS tracing)

This sample shows how to stream JavaScript events traces from Angulartics. TNT4J-Angulartics-plugin sends
trace data over HTTP request `http://localhost:9595`. Thus to process this we will need `HttpStream` running on port
`9595`.

Sample files can be found in `samples/angular-js-tracing` directory.

How to configure Angulartics see [`samples/angular-js-tracing/readme.md`](tnt4j-streams-core/samples/angular-js-tracing/readme.md)

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

    <parser name="JSONPayloadParser" class="com.jkoolcloud.tnt4j.streams.parsers.ActivityJsonParser">
        <field name="StartTime" locator="$.timestamp" locator-type="Label" datatype="Timestamp" units="Milliseconds"/>
        <field name="ResourceName" locator="$.url" locator-type="Label"/>
        <field name="Correlator" locator="$.sid" locator-type="Label"/>
        <field name="Correlator" locator="$.rid" locator-type="Label"/>
        <field name="EventName" locator="$.eventName" locator-type="Label"/>
        <field name="EventType" value="EVENT"/>
        <field name="ElapsedTime" locator="$.pageLoad" locator-type="Label" datatype="Timestamp" units="Milliseconds"/>
        <field name="browser" locator="$.browser" locator-type="Label"/>
        <field name="eventProperties" locator="$.properties" locator-type="Label"/>
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

#### JAX-WS

This sample shows how to stream responses from JAX-WS (SOAP) services as SOAP compliant XML data.

Sample files can be found in `samples/ws-stream` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../config/tnt-data-source-ws.xsd">

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
        xsi:noNamespaceSchemaLocation="../../config/tnt-data-source-ws.xsd">

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
        xsi:noNamespaceSchemaLocation="../../config/tnt-data-source-ws.xsd">

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
        xsi:noNamespaceSchemaLocation="../../config/tnt-data-source-ws.xsd">

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

NOTE: Stream stops only when critical runtime error/exception occurs or application gets terminated.

##### *nix

This sample shows how to stream responses from executed *nix type OS command.

Sample files can be found in `samples/unix-cmd-stream` directory.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../config/tnt-data-source-ws.xsd">

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

NOTE: Stream stops only when critical runtime error/exception occurs or application gets terminated.

#### MS Excel document

##### Rows

This sample shows how to stream MS Excel workbook rows as activity events.

Sample files can be found in `samples/xlsx-rows` directory.

NOTE: records in this file are from year `2010` i.e. `12 Jul 2010`, so then getting events data in JKoolCloud
please do not forget to just to dashboard time frame to that period!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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
        <property name="FileName" value=".\tnt4j-streams-msoffice\samples\xlsx-rows\sample.xlsx"/>
        <property name="FirstRowAsHeader" value="false"/>
        <property name="SheetsToProcess" value="Sheet*"/>

        <parser-ref name="ExcelRowParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `SampleExcelRowsStream` referencing `ExcelRowsParser` shall be used. Stream takes
workbook sheet row and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable rows.

`SampleExcelRowsStream` reads data from `.\tnt4j-streams-msoffice\samples\xlsx-rows\sample.xlsx` file.

`FirstRowAsHeader` property indicates that there is no table header row in sheets.

`SheetsToProcess` property defines sheet name filtering mask using wildcard string. It is also allowed to use RegEx like
`Sheet(1|3|5)` (in this case just sheets with names `Sheet1`, `Sheet3` and `Sheet5` will be processed).

`ExcelRowParser` parser uses literal sheet column indicators as locators (i.e. `A`, `D`, `AB`).

Note: `StartTime` fields defines format and locale to correctly parse field data string. `EventType` uses manual
field string mapping to TNT4J event field value.

##### Sheets

This sample shows how to stream MS Excel workbook sheets as activity events.

Sample files can be found in `samples/xlsx-sheets` directory.

NOTE: records in this file are from year `2010` i.e. `12 Jul 2010`, so then getting events data in JKoolCloud
please do not forget to just to dashboard time frame to that period!

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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
        <property name="FileName" value=".\tnt4j-streams-msoffice\samples\xlsx-sheets\sample.xlsx"/>
        <property name="SheetsToProcess" value="Sheet*"/>

        <parser-ref name="ExcelSheetParser"/>
    </stream>
</tnt-data-source>
```

Stream configuration states that `SampleExcelSheetsStream` referencing `ExcelSheetParser` shall be used. Stream takes
workbook sheet and passes it to parser.

`HaltIfNoParser` property indicates that stream should skip unparseable sheets.

`SampleExcelRowsStream` reads data from `.\tnt4j-streams-msoffice\samples\xlsx-sheets\sample.xlsx` file.

`SheetsToProcess` property defines sheet name filtering mask using wildcard string. It is also allowed to use RegEx like
`Sheet(1|3|5)` (in this case just sheets with names `Sheet1`, `Sheet3` and `Sheet5` will be processed).

`ExcelSheetParser` parser uses literal sheet cell indicators as locators (i.e. `A1`, `D5`, `AB12` where letters
identifies column and number identifies row).

Note: `StartTime` fields defines format and locale to correctly parse field data string. `EventType` uses manual
field string mapping to TNT4J event field value.

#### Collectd performance metrics streaming

TODO

#### Nagios reports streaming

This sample shows how to stream Nagios monitoring reports data as activity events.

Sample files can be found in `samples/nagios-nagios2json` directory (`tnt4j-streams-ws` module).

NOTE: to use this sample Nagios should be running extension `nagios2json`. See [Sample README](tnt4j-streams-ws/samples/nagios-nagios2json/readme.md)
for details. `nagios2json` extension works as `CGI` script, but can be handled as simple RESTful service.

Sample report data is available in `report.json` file. 

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../config/tnt-data-source-ws.xsd">

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

    <parser name="ResponseParser"
            class="com.jkoolcloud.tnt4j.streams.custom.parsers.SnapshotsJsonParser">
        <property name="ReadLines" value="false"/>
        <property name="ChildrenField" value="MsgBody"/>

        <field name="MsgBody" locator="$.data" locator-type="Label" transparent="true">
            <parser-ref name="SnapshotParser"/>
        </field>

        <field name="EventType" value="Activity"/>
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
 * Nagios service request URL and `nagios2json` parameters (i.e. `servicestatustypes=31`) 
 * request method `GET` to comply `cgi`.
 * Nagios service user credentials - user name and password
 * Scheduler is set to `Cron` expression `every 15 seconds`.
 
You may also add additional steps to retrieve different reports defining different `nagios2json` parameters.
 
Nagios sends report as activity wrapping multiple metrics (snapshots).  

`ResponseParser` maps Nagios report JSON data to activity (field `EventType`) containing set of snapshots (property `ChildrenField` 
referencing field `MsgBody`) carrying system state/metrics data. Each snapshot is parsed using stacked `SnapshotParser` parser (map parser 
because parent JSON parser already made map data structures from RAW Nagios report JSON data).

`SnapshotParser` maps map entries to snapshot fields `ApplName`, `EventName`, `Status`, `Message`, `Category`, `Duration` and `StartTime`.
`Status` and `Duration` fields also defines value types: `Status` is `enum`, `Duration` is `age`.    

#### Integrating TNT4J-Streams into custom API

See [`Readme.md`](tnt4j-streams-samples/README.md) of `tnt4j-streams-samples` module.

### How to use TNT4J loggers

#### tnt4j-log4j12

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

#### tnt4j-logback

* make logback configuration file `config/logback.xml`.
* change `bin/tnt-streams.bat` or `bin/tnt-streams.sh` file to pass logback configuration to Java:

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

Configuring TNT4J-Streams
======================================

Because TNT4J-Streams is based on TNT4J first You need to configure TNT4J (if have not done this yet).
Default location of `tnt4j.properties` file is in project `config` directory. At least You must make one change:
`event.sink.factory.Token:YOUR-TOKEN` replace `YOUR-TOKEN` with jKoolCloud token assigned for You.

For more information on TNT4J and `tnt4j.properties` see [TNT4J Wiki page](https://github.com/Nastel/TNT4J/wiki/Getting-Started).
Details on JESL related configuration can be found in [JESL README](https://github.com/Nastel/JESL/blob/master/README.md).

## Streams configuration

Streams can be configured using XML document having root element `tnt-data-source`. Definition of XML configuration
can be found in `tnt-data-source.xsd` file located in project `config` directory.

sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

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
        <parser-ref name="TokenParser"/>
    </stream>
</tnt-data-source>
```

As You can see from sample configuration, there are two major configuration elements defined `parser` and `stream`.
Because streams configuration is read using SAX parser referenced entities should be initialized before it is used.
Note that `stream` uses `parser` reference:
```xml
    <stream name="FileStream" class="com.jkoolcloud.tnt4j.streams.inputs.FileLineStream">
        ...
        <parser-ref name="TokenParser"/>
    </stream>
```
That is why sequence of configuration elements is critical and can't be swapped.

#### Generic streams parameters

##### Stream executors related parameters

These parameters are applicable to all types of streams.

 * UseExecutors - identifies whether stream should use executor service to process activities data items asynchronously
 or not. Default value - `false`. (Optional)
    * ExecutorThreadsQuantity - defines executor service thread pool size. Default value - `4`. (Optional)  Actual only
    if `UseExecutors` is set to `true`
    * ExecutorsTerminationTimeout - time to wait (in seconds) for a executor service to terminate. Default value -
    `20sec`. (Optional) Actual only if `UseExecutors` is set to `true`
    * ExecutorsBoundedModel - identifies whether executor service should use bounded tasks queue model. Default value -
    `false`. (Optional)  Actual only if `UseExecutors` is set to `true`
        * ExecutorRejectedTaskOfferTimeout - time to wait (in seconds) for a task to be inserted into bounded queue if
        max. queue size is reached. Default value - `20sec`. (Optional)
           Actual only if `ExecutorsBoundedModel` is set to `true`.

    sample:
```xml
    <property name="UseExecutors" value="true"/>
    <property name="ExecutorThreadsQuantity" value="5"/>
    <property name="ExecutorsTerminationTimeout" value="20"/>
    <property name="ExecutorsBoundedModel" value="true"/>
    <property name="ExecutorRejectedTaskOfferTimeout" value="20"/>
```

##### Parseable streams parameters

These parameters are applicable to streams which uses parsers to parse input data.

 * HaltIfNoParser - if set to `true`, stream will halt if none of the parsers can parse activity object RAW data.
 If set to `false` - puts log entry and continues. Default value - `true`. (Optional)

    sample:
```xml
    <property name="HaltIfNoParser" value="false"/>
```

##### Buffered streams parameters

 * BufferSize - maximal buffer queue capacity. Default value - `512`. (Optional)
 * BufferOfferTimeout - how long to wait if necessary for space to become available when adding data item to buffer
 queue. Default value - 45sec. (Optional)

     sample:
 ```xml
     <property name="BufferSize" value="1024"/>
     <property name="BufferOfferTimeout" value="90"/>
 ```

#### File line stream parameters (and Hdfs)

 * FileName - the system-dependent file name or file name pattern defined using wildcard characters `*` and `?`. (Required)
 * FilePolling - flag `true/false` indicating whether files should be polled for changes or not. If not, then files
 are read from oldest to newest sequentially one single time. Default value - `false`. (Optional)
    * StartFromLatest - flag `true/false` indicating that streaming should be performed from latest file entry line. If
    `false` - then all lines from available files are streamed on startup. Actual just if `FilePolling` property is set
    to `true`. Default value - `true`. (Optional)
    * FileReadDelay - delay is seconds between file reading iterations. Actual just if `FilePolling` property is set to
    `true`. Default value - 15sec. (Optional)
 * RestoreState - flag `true/false` indicating whether files read state should be stored and restored on stream restart.
 Default value - `true`. (Optional)

    sample:
 ```xml
    <property name="FileName" value="C:/Tomcat_7_0_34/logs/localhost_access_log.*.txt"/>
    <property name="FileReadDelay" value="5"/>
    <property name="StartFromLatest" value="true"/>
    <property name="FilePolling" value="true"/>
    <property name="RestoreState" value="false"/>
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

NOTE: there can be ony one parser referenced to this stream.

#### OS Piped stream parameters

This stream does not have any additional configuration parameters.

Also see ['Generic streams parameters'](#generic-streams-parameters).

#### Http stream parameters

 * Port - port number to run Http server. Default value - `8080`. (Optional)
 * UseSSL - flag identifying to use SSL. Default value - `false`. (Optional)
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

 * Topic - regex of topic name to listen. (Required)
 * List of properties used by Kafka API. i.e zookeeper.connect, group.id. See `kafka.consumer.ConsumerConfig` class for more
 details on Kafka consumer properties.

    sample:
```xml
    <property name="Topic" value="TNT4JStreams"/>
    <property name="zookeeper.connect" value="127.0.0.1:2181"/>
    <property name="group.id" value="TNT4JStreams"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters).

#### MQTT stream parameters

 * ServerURI - Mqtt server URI. (Required)
 * TopicString - the topic to subscribe to, which can include wildcards. (Required)
 * UserName - authentication user name. (Optional)
 * Password - user password. (Optional)
 * UseSSL - flag identifying to use SSL. Default value - `false`. (Optional)
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
 * Channel - Channel name. Default value - `SYSTEM.DEF.SVRCONN`. (Optional)
 * StripHeaders - identifies whether stream should strip WMQ message headers. Default value - `true`. (Optional)

    sample:
```xml
    <property name="QueueManager" value="QMGR"/>
    <property name="Queue" value="EVENT.QUEUE"/>
    <property name="Host" value="wmq.sample.com"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters).

#### Zipped file line stream parameters (and Hdfs):

 * FileName - defines zip file path and concrete zip file entry name or entry name pattern defined using characters `*`
 and `?`. Definition pattern is `zipFilePath!entryNameWildcard`. I.e.:
 `.\tnt4j-streams-core\samples\zip-stream\sample.zip!2/*.txt`. (Required)
 * ArchType - defines archive type. Can be one of: `ZIP`, `GZIP`, `JAR`. Default value - `ZIP`. (Optional)

    sample:
```xml
    <property name="FileName" value=".\tnt4j-streams-core\samples\zip-stream\sample.gz"/>
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
        * `request` is XML tag to define string represented request data (i.e. system command with parameters). To
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
        ...
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

#### Ms Excel Stream parameters

 * FileName - the system-dependent file name of MS Excel document. (Required)
 * SheetsToProcess - defines workbook sheets name filter mask (wildcard or RegEx) to process only sheets which names
 matches this mask. Default value - ''. (Optional)

    sample:
```xml
    <property name="FileName" value=".\tnt4j-streams-msoffice\samples\xlsx-rows\sample.xlsx"/>
    <property name="SheetsToProcess" value="Sheet(1|8|12)"/>
```

Also see ['Generic streams parameters'](#generic-streams-parameters) and 'Parseable streams parameters'.

##### Ms Excel Rows Stream parameters

 * FirstRowAsHeader - flag `true/false` indicating whether first row in sheet is used to define table columns titles.
 If `true` then first sheet row is skipped from streaming. Default value - `false`. (Optional)

    sample:
```xml
    <property name="FileName" value=".\tnt4j-streams-msoffice\samples\xlsx-rows\sample.xlsx"/>
    <property name="SheetsToProcess" value="Sheet(1|8|12)"/>
```

### Parsers configuration

#### Activity Name-Value parser:

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

#### Activity RegEx parser:

 * Pattern - contains the regular expression pattern that each data item is assumed to match. (Required)

    sample:
```xml
    <property name="Pattern" value="((\S+) (\S+) (\S+))"/>
```

#### Activity token parser:

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

#### Activity XML parser:

 * Namespace - additional XML namespace mappings. Default value - `null`. (Optional)
 * RequireDefault - indicates that all attributes are required by default. Default value - `false`. (Optional)

    sample:
```xml
    <property name="Namespace" value="xsi=http://www.w3.org/2001/XMLSchema-instance"/>
    <property name="Namespace" value="tnt4j=https://jkool.jkoolcloud.com/jKool/xsds"/>
    <property name="RequireDefault" value="true"/>
```

#### Message activity XML parser:

 * SignatureDelim - signature fields delimiter. Default value - `,`. (Optional)

    sample:
```xml
    <property name="SignatureDelim" value="#"/>
```

#### Apache access log parser:

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
 or
```xml
    <property name="Pattern"
              value="^(\S+) (\S+) (\S+) \[([\w:/]+\s[+\-]\d{4})\] &quot;(((\S+) (.*?)( (\S+)|()))|(-))&quot; (\d{3}) (\d+|-)( (\S+)|$)"/>
```

#### Activity JSON parser:

 * ReadLines - indicates that complete JSON data package is single line. Default value - `true`. (Optional)

    sample:
```xml
    <property name="ReadLines" value="false"/>
```

How to Build TNT4J-Streams
=========================================
## Modules

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

(M) marked modules are mandatory, (O) marked modules - optional.

All optional modules (extensions) depends to `core` module and can't be build and run without it.

If You want to build and use optional modules, uncomment those in root TNT4J-Streams `pom.xml` file. I.e. to use `WMQ`
module uncomment `tnt4j-streams-wmq` module.

NOTE: `Samples` module provides no additional features to TNT4J streaming framework. It contains only streams API use samples.

## Requirements
* JDK 1.6+
* Apache Maven 3 (https://maven.apache.org/)
* TNT4J (https://github.com/Nastel/TNT4J)
* JESL (https://github.com/Nastel/JESL)

All other required dependencies are defined in project modules `pom.xml` files. If maven is running
online mode it should download these defined dependencies automatically.

### Manually installed dependencies
Some of required and optional dependencies may be not available in public Maven Repository
(http://repo.maven.apache.org/maven2/). In this case we would recommend to download those dependencies manually into
module's `lib` directory and install into local maven repository by running maven script `lib/pom.xml` with `package`
goal. For example see [`tnt4j-streams/tnt4j-streams-wmq/lib/pom.xml`](tnt4j-streams-wmq/lib/pom.xml) how to do this.

#### `WMQ` module

What to download manually:
* IBM MQ 7.5

Download the above libraries and place into the `tnt4j-streams/tnt4j-streams-wmq/lib` directory like this:
```
    lib
     + ibm.mq
         |- com.ibm.mq.commonservices.jar
         |- com.ibm.mq.headers.jar
         |- com.ibm.mq.jar
         |- com.ibm.mq.jmqi.jar
         |- com.ibm.mq.pcf.jar
```
(O) marked libraries are optional

## Building
   * to build project and make release assemblies run maven goals `clean package`
   * to build project, make release assemblies and install to local repo run maven goals `clean install`

Release assemblies are built to `../build/tnt4j-streams` directory.

NOTE: sometimes maven fails to correctly handle dependencies. If dependency configuration looks
fine, but maven still complains about missing dependencies try to delete local maven repository
by hand: i.e. delete contents of `c:\Users\[username]\.m2\repository` directory.

## Running samples

See 'Running TNT4J-Streams' chapter section ['Samples'](#samples).

Testing of TNT4J-Streams
=========================================

## Requirements
* JUnit 4 (http://junit.org/)
* Mockito (http://mockito.org/)

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
* TNT4J-Streams-Zorka - (https://github.com/Nastel/tnt4j-streams-zorka)
