* Install Kafka (if not yet)
* Add `tnt4j-streams-kafka-[VERSION]-all.jar` to Kafka class-path:
    * If you can put files into `<KAFKA_INSTALL_DIR>/libs` dir, then just copy it there. 
    * If you wish to have streams lib isolated, then put jar to dedicated directory (e.g. `<KAFKA_INSTALL_DIR>/libs/tnt4j`). Then alter 
    `<KAFKA_INSTALL_DIR>/bin/kafka-run-class` script file: 
        * *NIX: `<KAFKA_INSTALL_DIR>/bin/kafka-run-class.sh` by adding section after `# classpath addition for release` section:
        ```bash
         # classpath addition for tnt4j
         for file in "$base_dir"/libs/tnt4j/*;
         do
           if should_include_file "$file"; then
             CLASSPATH="$CLASSPATH":"$file"
           fi
         done
        ```
        * WIN: `<KAFKA_INSTALL_DIR>/bin/windows/kafka-run-class.bat` by adding section after `rem Classpath addition for release` section:
        ```cmd
        rem Classpath addition for tnt4j
        for %%i in (%BASE_DIR%\libs\tnt4j\*) do (
            call :concat %%i
        )
        ```
        **NOTE:** there is sample `.bat` file provided next to this manual, use it as reference.
* Alter `<KAFKA_INSTALL_DIR>/config/consumer.properties` by adding:
```properties
interceptor.classes=com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaCInterceptor
```
* Alter `<KAFKA_INSTALL_DIR>/config/procuder.properties` by adding:
```properties
interceptor.classes=com.jkoolcloud.tnt4j.streams.custom.kafka.interceptors.TNTKafkaPInterceptor
```
* Alter `<KAFKA_INSTALL_DIR>/config/log4j.properties` by adding:
```properties
######################## TNT4J ########################

### direct log messages to file ###
log4j.appender.tnt4jAppender=org.apache.log4j.RollingFileAppender
log4j.appender.tnt4jAppender.File=${kafka.logs.dir}/tnt4j-streams.log
log4j.appender.tnt4jAppender.maxFileSize=10MB
log4j.appender.tnt4jAppender.maxBackupIndex=2
log4j.appender.tnt4jAppender.layout=org.apache.log4j.EnhancedPatternLayout
log4j.appender.tnt4jAppender.layout.ConversionPattern=%d{ISO8601} %-5p [%t!%c{1}] - %m%n
log4j.appender.tnt4jAppender.Threshold=TRACE
#log4j.appender.tnt4jAppender.bufferSize=512

log4j.logger.com.jkoolcloud.tnt4j.streams=DEBUG, tnt4jAppender
### if streams are not subject to log ###
#log4j.logger.com.jkoolcloud.tnt4j.streams=OFF
#log4j.additivity.com.jkoolcloud.tnt4j.streams=false
```
* Alter `<KAFKA_INSTALL_DIR>/config/tools-log4j.properties` by adding (to disable streams logging to sample consumer/producer console):
```properties
######################## TNT4J ########################
log4j.logger.com.jkoolcloud.tnt4j.streams=OFF
log4j.additivity.com.jkoolcloud.tnt4j.streams=false
``` 
* Run Kafka provided console producer/consumer
```cmd
kafka-console-consumer --bootstrap-server localhost:9092 --consumer.config ../../onfig/consumer.properties --topic tnt4j_streams_kafka_intercept_test_page_visits --from-beginning
```

```cmd
kafka-console-producer --producer.config ../../config/producer.properties --broker-list localhost:9092 --topic tnt4j_streams_kafka_intercept_test_page_visits
```
