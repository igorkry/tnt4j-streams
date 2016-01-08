# tnt4j-streams
TNT4J Streams allows data streaming, parsing from various data sources into TNT4J event sinks.

Configuring TNT4J-Streams
======================================

Stream executors related parameters:

 * UseExecutors - identifies whether stream should use executor service to process activities data items asynchronously or not. Default - false.
    * ExecutorThreadsQuantity - defines executor service thread pool size. Default - 4
    * ExecutorsTerminationTimeout - time to wait (in seconds) for a executor service to terminate. Default - 20sec.
    * ExecutorsBoundedModel - identifies whether executor service should use bounded tasks queue model. Default - false.
    *   ExecutorRejectedTaskOfferTimeout - time to wait (in seconds) for a task to be inserted into bounded queue if max. queue size is reached. Default - 20sec.
                                           Actual only if ExecutorsBoundedModel is set to true.

    sample:
        &lt;property name="UseExecutors" value="true"/&gt;
        &lt;property name="ExecutorThreadsQuantity" value="5"/&gt;
        &lt;property name="ExecutorsTerminationTimeout" value="20"/&gt;
        &lt;property name="ExecutorsBoundedModel" value="true"/&gt;
        &lt;property name="ExecutorRejectedTaskOfferTimeout" value="20"/&gt;

How to Build TNT4J-Streams
=========================================

Requirements
* JDK 1.6+
* Apache ANT (http://ant.apache.org/)
* TNT4J (https://github.com/Nastel/TNT4J)

TNT4J-Streams depends on the following external packages:
Core:
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

Http Extension:
* Apache HttpComponents client 4.5.1 (https://hc.apache.org/httpcomponents-client-4.5.x/)
* Apache HttpComponents core 4.4.3 (https://hc.apache.org/httpcomponents-core-4.4.x/)

JMS Extension:
* Javax JMS API (https://java.net/projects/jms-spec/pages/Home)

Hdfs Extension:
* Apache Hadoop 2.6.2 (https://hadoop.apache.org/)
* Apache commons cli 1.3.1 (http://commons.apache.org/proper/commons-cli/)
* Javax Servlet API (https://java.net/projects/servlet-spec/)
* Google protocol buffers 2.6.2 (https://developers.google.com/protocol-buffers/)

WMQ Extension:
* IBM MQ API 7.5 ()
* Javax connector

Kafka Extension:
* Apache Kafka 2.10 (http://kafka.apache.org/)
* Yammer Metrics 2.2.0 (http://mvnrepository.com/artifact/com.yammer.metrics/metrics-core/)
* Scala Library 2.10.1 (http://www.scala-lang.org/)
* ZKClient 0.7 (https://github.com/sgroschupf/zkclient)
* Apache Zookeeper 3.3.4 (https://zookeeper.apache.org/)

MQTT Extension:
* Eclipse Paho Client MQTTV3 1.0.2 (http://www.eclipse.org/paho/

Apache Flume Plugin:
* Apache Flume 1.6.0 (https://flume.apache.org/)

To build TNT4J-Streams:
* Download the above libraries and place into the tnt4j-streams/lib folder
* Compile and build using ANT:
	* ant all (run "ant rebuild" for clean builds)
	* Check ../build/tnt4j-streams for output
	* JavaDoc will be located under ../build/tnt4j-streams/doc

Testing of TNT4J-Streams
=========================================

Requirements
* JUnit 4 (http://junit.org/)
* Mockito (http://mockito.org/)

Running
* Run JUnit test suite named "StreamsAllTests"
