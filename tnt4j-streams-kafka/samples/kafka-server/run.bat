@echo off
setlocal

set RUNDIR=%~dp0
set ZKOPTS=-Dtnt4j.zookeeper.config="%RUNDIR%..\..\config\zookeeper.properties"
set KAFKAOPTS=-Dtnt4j.kafka.srv.config="%RUNDIR%..\..\config\kafka-server.properties"
set STREAMSOPTS=%ZKOPTS% %KAFKAOPTS%

@echo on
..\..\bin\tnt4j-streams.bat -f:tnt-data-source.xml