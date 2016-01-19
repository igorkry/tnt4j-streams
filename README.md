# TNT4J-Streams
TNT4J Streams allows data streaming, parsing from various data sources into TNT4J event sinks.

Why TNT4J-Streams
======================================

* TNT4J-Streams can be run out of the box for a large set of data streaming without writing no additional code.
All You need is to define Your data format mapping to TNT4J event mapping in TNT4J-Streams configuration.

* It supports these major data transports:
    * File
    * Character stream over TCP/IP
    * Hdfs
    * Mqtt
    * Http
    * JMS
    * Apache Kafka
    * WMQ

* Files (also Hdfs) can be streamed:
    * as "whole at once" - when stream starts, it reads file contents line by line meaning single file line hols
    data of single activity event. After file reading completes - stream stops.
    * using file polling - when some application uses file to write data at runtime, stream waits for file changes.
    When file changes, changed (appended) lines are read by stream and interpreted as single line is single activity
    event. Stream stops only when application gets terminated or some critical runtime error occurs.

* Has customized parser to parse Apache Access Logs.

* It can be integrated with:
    * Logstash
    * Apache Flume

just by applying configuration and without additional codding.

Running TNT4J-Streams
======================================

## TNT4J-Streams can be run:
* As standalone application
    * write streams configuration file. See 'Streams configuration' chapter for more details
    * configure Your loggers
    * use `bin\tnt4j-streams.bat` or `bin\tnt4j-streams.sh` to run standalone application
* As API integrated into Your product
    * Write streams configuration file. See 'Streams configuration' chapter for more details
    * use `StreamsAgent.runFromAPI(configFileName)` in your code

## Examples:
#### Apache Access log single file
TODO

#### Apache Access log multiple files
TODO

#### Apache Flume RAW data
TODO

#### Apache Flume Parsed Data
TODO

#### Custom API
TODO

#### Datapower
TODO

#### Hdfs
TODO

#### Http request file
TODO

#### Http request form
TODO

#### JMS text message
TODO

#### JMS map message
TODO

#### JMS object message
TODO

#### Kafka
TODO

#### Log file polling
TODO

#### Logstash RAW data
TODO

#### Logstash parsed data
TODO

#### WMQ Message broker
TODO

#### WMQ MFT/FTE
TODO

#### MQTT
TODO

#### Single Log file

Sample files can be found in `samples\single-log` directory.

To run sample as standalone application use command:
 * windows
```
..\..\bin\tnt4j-streams.bat -f:tnt-data-source.xml
```
or
```
run.bat
```
 * unix
```
../../bin/tnt4j-streams -f:tnt-data-source.xml
```
or
```
run.sh
```
`orders.log` file contains order activity event as single line.

Sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

    <parser name="TokenParser" class="com.jkool.tnt4j.streams.parsers.ActivityTokenParser">
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

    <stream name="FileStream" class="com.jkool.tnt4j.streams.inputs.FileLineStream">
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

### How to use TNT4J loggers
See chapter 'Manually installed dependencies' how to install `tnt4j-log4j12` or `tnt4j-logback` dependencies.

#### TNT4J-log4j12

* in `config\log4j.properties` file change log appender to
`log4j.appender.tnt4j=com.nastel.jkool.tnt4j.logger.log4j.TNT4JAppender`. Note that there should be on line like
`log4j.appender.tnt4j=` in this file, so please comment or remove all others if available.
* in `pom.xml` file of `core` change dependencies - uncomment:
```xml
    <dependency>
        <groupId>com.nastel.jkool.tnt4j</groupId>
        <artifactId>tnt4j-log4j12</artifactId>
        <version>1.0.0</version>
        <scope>runtime</scope>
    </dependency>
```

#### TNT4J-logback

* make logback configuration file `config\logback.xml`.
* change `bin\tnt-streams.bat` or `bin\tnt-streams.sh` file to pass logback configuration to Java:

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
        <groupId>com.nastel.jkool.tnt4j</groupId>
        <artifactId>tnt4j-logback</artifactId>
        <version>1.0.0</version>
        <scope>runtime</scope>
    </dependency>
    <!-- logback logger shall be used -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.1.3</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-core</artifactId>
        <version>1.1.3</version>
        <scope>runtime</scope>
    </dependency>
```
and comment out log4j dependencies

Configuring TNT4J-Streams
======================================

Because TNT4J-Streams is based on TNT4J first You need to configure TNT4J (if have not done this yet).
Default location of `tnt4j.properties` file is in project `config` directory. At least You must make one change:
`event.sink.factory.Token:YOUR-TOKEN` replace `YOUR-TOKEN` with jKoolCloud token assigned for You.

For more information on TNT4J and `tnt4j.properties` see (https://github.com/Nastel/TNT4J/wiki/Getting-Started).

## Streams configuration

Streams can be configured using XML document having root element `tnt-data-source`. Definition of XML configuration
can be found in `tnt-data-source.xsd` file located in project `config` directory.

sample stream configuration:
```xml
<?xml version="1.0" encoding="utf-8"?>
<tnt-data-source
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:noNamespaceSchemaLocation="../../../config/tnt-data-source.xsd">

    <parser name="TokenParser" class="com.jkool.tnt4j.streams.parsers.ActivityTokenParser">
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

    <stream name="FileStream" class="com.jkool.tnt4j.streams.inputs.FileLineStream">
        <property name="FileName" value="orders.log"/>
        <parser-ref name="TokenParser"/>
    </stream>
</tnt-data-source>
```

As You can see from sample configuration there are two major configuration elements defined `parser` and `stream`.
Because streams configuration is read using SAX parser referenced entities should be initialized before it is used.
Note that `stream` uses `parser` reference:
```xml
    <stream name="FileStream" class="com.jkool.tnt4j.streams.inputs.FileLineStream">
        ...
        <parser-ref name="TokenParser"/>
    </stream>
```
That is why sequence of configuration elements is critical and can't be swapped.

#### Generic stream parameters:

 * HaltIfNoParser - if set to `true`, stream will halt if none of the parsers can parse activity object RAW data.
 If set to `false` - puts log entry and continues. Default value - `true`. (Optional)

    sample:
```xml
    <property name="HaltIfNoParser" value="false"/>
```

##### Stream executors related parameters:

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

#### File line stream parameters:

 * FileName - concrete file name or file name pattern defined using characters `*` and `?`. (Required)

    sample:
 ```xml
    <property name="FileName" value="*_access_log.2015-*.txt"/>
 ```

#### File polling stream parameters:

 * FileName - concrete file name or file name pattern defined using characters `*` and `?`. (Required)
 * StartFromLatest - flag `true/false` indicating that streaming should be performed from latest log entry. If `false` -
  then latest log file is streamed from beginning. Default value - `true`. (Optional)
 * FileReadDelay - delay is seconds between log file reading iterations. Default value - `15sec`. (Optional)

     sample:
 ```xml
    <property name="FileName" value="C:/Tomcat_7_0_34/logs/localhost_access_log.*.txt"/>
    <property name="FileReadDelay" value="5"/>
    <property name="StartFromLatest" value="true"/>
 ```

#### Character stream parameters:

 * FileName - concrete file name. (Required - just one `FileName` or `Port`)
 * Port - port number to accept character stream over TCP/IP. (Required - just one `FileName` or `Port`)

    sample:
```xml
    <property name="FileName" value="messages.json"/>
```
or
```xml
    <property name="Port" value="9595"/>
```

#### Http stream parameters:

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

#### JMS stream parameters:

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

#### Kafka stream parameters:

 * Topic - regex of topic name to listen. (Required)
 * List of properties used by Kafka API. i.e zookeeper.connect, group.id. See `kafka.consumer.ConsumerConfig` for more
 details on Kafka consumer properties.

    sample:
```xml
    <property name="Topic" value="TNT4JStreams"/>
    <property name="zookeeper.connect" value="127.0.0.1:2181"/>
    <property name="group.id" value="TNT4JStreams"/>
```

#### MQTT stream parameters:

 * ServerURI - Mqtt server URI. (Required)
 * Topic - topic name to listen. (Required)
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

#### WMQ Stream parameters:

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
    <property name="Namespace" value="xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance"/>
    <property name="Namespace" value="xmlns:tnt4j=https://jkool.jkoolcloud.com/jKool/xsds"/>
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
    <property name="ConfRegexMapping" value="%*r=(((\S+) (\S+) (\S+))|-)"/>
    <property name="ConfRegexMapping" value="%*i=(\S+)"/>
```
 or
```xml
     <property name="Pattern"
                  value="^(\S+) (\S+) (\S+) \[([\w:/]+\s[+\-]\d{4})\] &quot;(((\S+) (\S+) (\S+))|-)&quot; (\d{3}) (\d+|-)( (\S+)|$)"/>
```

How to Build TNT4J-Streams
=========================================

## Requirements
* JDK 1.6+
* Apache Maven 3 (https://maven.apache.org/)
* TNT4J (https://github.com/Nastel/TNT4J)
* JESL (https://github.com/Nastel/JESL)

All other required dependencies are defined in project modules `pom.xml` files. If maven is running
online mode it should download these defined dependencies automatically.

### Manually installed dependencies
Some of required and optional dependencies may be not available in public Maven Repository (http://mvnrepository.com/).
In this case we would recommend to download those dependencies manually into `lib` directory and install into local
maven repository by running `mvn install` command. See `lib\mvn-install.bat` how to do this.

So what to download manually:
* TNT4J
* JESL
* IBM MQ 7.5 - if You wish to use WMQ module. If not just comment out that module in main `pom.xml` file.
* TNT4J-log4j12 - if You wish to use this logger. See 'How to use TNT4J loggers' section for more details.
* TNT4J-logback - if You wish to use this logger. See 'How to use TNT4J loggers' section for more details.

Download the above libraries and place into the `tnt4j-streams/lib directory` directory like this:
```
    lib
     + ibm.mq (O)
     |- com.ibm.mq.commonservices.jar
     |- com.ibm.mq.headers.jar
     |- com.ibm.mq.jar
     |- com.ibm.mq.jmqi.jar
     |-com.ibm.mq.pcf.jar
     jkool-jesl.jar
     tnt4j-api.jar
     tnt4j-log4j12.jar (O)
     tnt4j-logback.jar (O)
```

(O) marked libraries are optional

## Building
   * to build project run maven goals `clean package`
   * to make release assembly run maven goals `clean package javadoc:aggregate verify`

Release assembly is built to `../build/tnt4j-streams` directory.

## Running samples
When release assembly is built samples are located in `samples` directory i.e. `../build/tnt4j-streams/tnt4j-streams-1.0.0/samples`.
To run desired sample:
   * go to sample directory
   * run `run.bat` or `run.sh` depending on Your OS.

Testing of TNT4J-Streams
=========================================

## Requirements
* JUnit 4 (http://junit.org/)
* Mockito (http://mockito.org/)

## Testing using maven
Maven runs tests automatically while building project. To skip test phase add Maven parameter `-Dmaven.test.skip=true`
or select 'Skip tests' UI element in your IDE  'Maven Run' configuration.

## Running manually
* in `core` module run JUnit test suite named `AllStreamsCoreTests`
* in `flume-plugin` module run JUnit test suite named `AllFlumeTests`
* in `hdfs` module run JUnit test suite named `AllHdfsStreamTests`
* in `jms` module run JUnit test suite named `AllJMSStreamTests`
* in `kafka` module run JUnit test suite named `AllKafkaStreamTests`
* in `mqtt` module run JUnit test suite named `AllMqttStreamTests`
* in `wmq` module run JUnit test suite named `AllWmqStreamTests`
* in `zorka` module run JUnit test suite named `AllZorkaTests`

