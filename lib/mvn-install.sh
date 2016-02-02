#!/bin/sh

echo installing TNT4J and JESL libraries...
mvn install:install-file -Dfile=tnt4j-api.jar -DgroupId=com.nastel.jkool.tnt4j -DartifactId=tnt4j-api -Dversion=1.0.0 -Dpackaging=jar
mvn install:install-file -Dfile=jkool-jesl.jar -DgroupId=com.nastel.jkool.tnt4j -DartifactId=jkool-jesl -Dversion=1.0.0 -Dpackaging=jar

echo installing TNT4J logger libraries...
#mvn install:install-file -Dfile=tnt4j-log4j12.jar -DgroupId=com.nastel.jkool.tnt4j -DartifactId=tnt4j-log4j12 -Dversion=1.0.0 -Dpackaging=jar
#mvn install:install-file -Dfile=tnt4j-logback.jar -DgroupId=com.nastel.jkool.tnt4j -DartifactId=tnt4j-logback -Dversion=1.0.0 -Dpackaging=jar

echo installing libraries required by WMQ...
#mvn install:install-file -Dfile=ibm.mq/com.ibm.mq.jar -DgroupId=com.ibm -DartifactId=com.ibm.mq -Dversion=7.5 -Dpackaging=jar
#mvn install:install-file -Dfile=ibm.mq/com.ibm.mq.commonservices.jar -DgroupId=com.ibm -DartifactId=com.ibm.mq.commonservices -Dversion=7.5 -Dpackaging=jar
#mvn install:install-file -Dfile=ibm.mq/com.ibm.mq.headers.jar -DgroupId=com.ibm -DartifactId=com.ibm.mq.headers -Dversion=7.5 -Dpackaging=jar
#mvn install:install-file -Dfile=ibm.mq/com.ibm.mq.jmqi.jar -DgroupId=com.ibm -DartifactId=com.ibm.mq.jmqi -Dversion=7.5 -Dpackaging=jar
#mvn install:install-file -Dfile=ibm.mq/com.ibm.mq.pcf.jar -DgroupId=com.ibm -DartifactId=com.ibm.mq.pcf -Dversion=7.5 -Dpackaging=jar

echo DONE!

read -p "Press [Enter] key to exit..."