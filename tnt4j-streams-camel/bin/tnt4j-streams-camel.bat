@echo off
setlocal

set RUNDIR=%~dp0
set LIBPATH=%RUNDIR%..\*;%RUNDIR%..\lib\*
set ROUTEOPTS=-Droute.properties="%RUNDIR%..\config\route.properties" -Droute.context="%RUNDIR%..\config\route-context.xml"
set LOG4JOPTS=-Dlog4j.configuration="file:%RUNDIR%..\config\log4j.properties"

@echo on
"%JAVA_HOME%\bin\java" %LOG4JOPTS% %ROUTEOPTS% -classpath "%LIBPATH%" org.apache.camel.spring.Main %*