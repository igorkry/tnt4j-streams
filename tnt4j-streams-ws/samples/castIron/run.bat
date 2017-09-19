@echo off
set RUNDIR=%~dp0
set LIBPATH=%RUNDIR%..\..\tnt4j-streams-ws-1.1.0-SNAPSHOT.jar
set STREAMSOPTS=-Dorg.quartz.threadPool.threadCount=1
..\..\bin\tnt4j-streams.bat -f:tnt-data-source.xml