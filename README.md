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
    When file changes, changed (appended) lines are read by stream and interpreted as single line is single activity event.
    Stream stops only when application gets terminated or some critical runtime error occurs.

* Has customized parser to parse Apache Access Logs.

* It can be integrated into:
    * Logstash
    * Apache Flume

just applying configuration and without additional codding.

Running TNT4J-Streams
======================================

## TNT4J-Streams can be run:
* As standalone application
* As API integrated into Your product

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
TODO

Configuring TNT4J-Streams
======================================

Because TNT4J-Streams is based on TNT4J first You need to configure TNT4J (if have not done this yet).
Default location of `tnt4j.properties` file is in project `config` directory. At least You must make one change:
`event.sink.factory.Token:YOUR-TOKEN` replace `YOUR-TOKEN` with jKoolCloud token assinged for You.

For more information on TNT4J and `tnt4j.properties` see (https://github.com/Nastel/TNT4J/wiki/Getting-Started).

### Generic stream parameters:

 * HaltIfNoParser - if set to `true`, stream will halt if none of the parsers can parse activity object RAW data.
 If set to `false` - puts log entry and continues. Default value - `true`. (Optional)

    sample:
```xml
    <property name="HaltIfNoParser" value="false"/>
```

#### Stream executors related parameters:

 * UseExecutors - identifies whether stream should use executor service to process activities data items asynchronously or not. Default value - `false`. (Optional)
    * ExecutorThreadsQuantity - defines executor service thread pool size. Default value - `4`. (Optional)
    * ExecutorsTerminationTimeout - time to wait (in seconds) for a executor service to terminate. Default value - `20sec`. (Optional)
    * ExecutorsBoundedModel - identifies whether executor service should use bounded tasks queue model. Default value - `false`. (Optional)
        * ExecutorRejectedTaskOfferTimeout - time to wait (in seconds) for a task to be inserted into bounded queue if max. queue size is reached. Default value - `20sec`. (Optional)
           Actual only if `ExecutorsBoundedModel` is set to `true`.

    sample:
```xml
    <property name="UseExecutors" value="true"/>
    <property name="ExecutorThreadsQuantity" value="5"/>
    <property name="ExecutorsTerminationTimeout" value="20"/>
    <property name="ExecutorsBoundedModel" value="true"/>
    <property name="ExecutorRejectedTaskOfferTimeout" value="20"/>
```

### File line stream parameters:

 * FileName - concrete file name or file name pattern defined using characters `*` and `?`. (Required)

    sample:
 ```xml
    <property name="FileName" value="*_access_log.2015-*.txt"/>
 ```

### File polling stream parameters:

 * FileName - concrete file name or file name pattern defined using characters `*` and `?`. (Required)
 * StartFromLatest - flag `true/false` indicating that streaming should be performed from latest log entry. If `false` - then
 latest log file is streamed from beginning. Default value - `true`. (Optional)
 * FileReadDelay - delay is seconds between log file reading iterations. Default value - `15sec`. (Optional)

     sample:
 ```xml
    <property name="FileName" value="C:/Tomcat_7_0_34/logs/localhost_access_log.*.txt"/>
    <property name="FileReadDelay" value="5"/>
    <property name="StartFromLatest" value="true"/>
 ```

### Character stream parameters:

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

### Http stream parameters:

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

### JMS stream parameters:

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

### Kafka stream parameters:

 * Topic - regex of topic name to listen. (Required)
 * List of properties used by Kafka API. i.e zookeeper.connect, group.id. See `kafka.consumer.ConsumerConfig` for more details on Kafka consumer properties.

    sample:
```xml
    <property name="Topic" value="TNT4JStreams"/>
    <property name="zookeeper.connect" value="127.0.0.1:2181"/>
    <property name="group.id" value="TNT4JStreams"/>
```

### MQTT stream parameters:

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

### WMQ Stream parameters:

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

How to Build TNT4J-Streams
=========================================

## Requirements
* JDK 1.6+
* Apache ANT (http://ant.apache.org/)
* TNT4J (https://github.com/Nastel/TNT4J)

### TNT4J-Streams depends on the following external packages:
#### Core:
* Apache commons codec 1.9 (http://commons.apache.org/proper/commons-codec/)
* Apache commons collections 3.2.1 (http://commons.apache.org/proper/commons-collections/)
* Apache commons configuration 1.10 (http://commons.apache.org/proper/commons-configuration/)
* Apache commons io 2.4 (http://commons.apache.org/proper/commons-io/)
* Apache commons lang 2.6 (http://commons.apache.org/proper/commons-lang/)
* Apache commons lang3 3.3.2 (http://commons.apache.org/proper/commons-lang/)
* Apache commons logging 1.1.3 (http://commons.apache.org/proper/commons-logging/)
* Apache commons net 3.3 (http://commons.apache.org/proper/commons-net/)
* Google GSON 2.3.1 (https://github.com/google/gson)
* Google Guava Libraries 18.0 (https://code.google.com/p/guava-libraries/)
* SLF4J 1.7.12 (http://www.slf4j.org/)
* Java UUID Generator (JUG) 3.1.3 (http://wiki.fasterxml.com/JugHome/)

#### Http Extension:
* Apache HttpComponents client 4.5.1 (https://hc.apache.org/httpcomponents-client-4.5.x/)
* Apache HttpComponents core 4.4.3 (https://hc.apache.org/httpcomponents-core-4.4.x/)

#### JMS Extension:
* Javax JMS API (https://java.net/projects/jms-spec/pages/Home)

#### Hdfs Extension:
* Apache Hadoop 2.6.2 (https://hadoop.apache.org/)
* Apache commons cli 1.3.1 (http://commons.apache.org/proper/commons-cli/)
* Javax Servlet API (https://java.net/projects/servlet-spec/)
* Google protocol buffers 2.6.2 (https://developers.google.com/protocol-buffers/)

#### WMQ Extension:
* IBM MQ API 7.5 ()
* Javax connector

#### Kafka Extension:
* Apache Kafka 2.10 (http://kafka.apache.org/)
* Yammer Metrics 2.2.0 (http://mvnrepository.com/artifact/com.yammer.metrics/metrics-core/)
* Scala Library 2.10.1 (http://www.scala-lang.org/)
* ZKClient 0.7 (https://github.com/sgroschupf/zkclient)
* Apache Zookeeper 3.3.4 (https://zookeeper.apache.org/)

#### MQTT Extension:
* Eclipse Paho Client MQTTV3 1.0.2 (http://www.eclipse.org/paho/)

#### Apache Flume Plugin:
* Apache Flume 1.6.0 (https://flume.apache.org/)

### To build TNT4J-Streams:
* Download the above libraries and place into the tnt4j-streams/lib folder
* Compile and build using ANT:
	* ant all (run "ant rebuild" for clean builds)
	* Check ../build/tnt4j-streams for output
	* JavaDoc will be located under ../build/tnt4j-streams/doc

Testing of TNT4J-Streams
=========================================

## Requirements
* JUnit 4 (http://junit.org/)
* Mockito (http://mockito.org/)

## Running
* Run JUnit test suite named "StreamsAllTests"
