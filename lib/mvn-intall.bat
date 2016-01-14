set MAVEN_HOME=C:\maven
set PATH=%PATH%;%MAVEN_HOME%\bin

; TNT4J and JESL libraries
call mvn install:install-file -Dfile=tnt4j-api.jar -DgroupId=com.nastel.jkool.tnt4j -DartifactId=tnt4j-api -Dversion=1.0.0 -Dpackaging=jar
call mvn install:install-file -Dfile=jkool-jesl.jar -DgroupId=com.nastel.jkool.tnt4j -DartifactId=jkool-jesl -Dversion=1.0.0 -Dpackaging=jar

; TNT4J logger libraries
call mvn install:install-file -Dfile=tnt4j-log4j12\tnt4j-log4j12.jar -DgroupId=com.nastel.jkool.tnt4j -DartifactId=tnt4j-log4j12 -Dversion=1.0.0 -Dpackaging=jar
call mvn install:install-file -Dfile=tnt4j-logback\tnt4j-logback.jar -DgroupId=com.nastel.jkool.tnt4j -DartifactId=tnt4j-logback -Dversion=1.0.0 -Dpackaging=jar

; libraries required by WMQ
call mvn install:install-file -Dfile=ibm.mq/com.ibm.mq.jar -DgroupId=com.ibm -DartifactId=com.ibm.mq -Dversion=7.5 -Dpackaging=jar
call mvn install:install-file -Dfile=ibm.mq/com.ibm.mq.commonservices.jar -DgroupId=com.ibm -DartifactId=com.ibm.mq.commonservices -Dversion=7.5 -Dpackaging=jar
call mvn install:install-file -Dfile=ibm.mq/com.ibm.mq.headers.jar -DgroupId=com.ibm -DartifactId=com.ibm.mq.headers -Dversion=7.5 -Dpackaging=jar
call mvn install:install-file -Dfile=ibm.mq/com.ibm.mq.jmqi.jar -DgroupId=com.ibm -DartifactId=com.ibm.mq.jmqi -Dversion=7.5 -Dpackaging=jar
call mvn install:install-file -Dfile=ibm.mq/com.ibm.mq.pcf.jar -DgroupId=com.ibm -DartifactId=com.ibm.mq.pcf -Dversion=7.5 -Dpackaging=jar

; libraries required by Zorka
call mvn install:install-file -Dfile=zorka\zico-util.jar -DgroupId=zorka -DartifactId=zico-util -Dversion=1.0.0 -Dpackaging=jar