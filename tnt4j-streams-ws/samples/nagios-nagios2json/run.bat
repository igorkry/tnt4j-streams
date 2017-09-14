@echo off
set RUNDIR=%~p0
set LIBPATH=%RUNDIR%..\..\tnt4j-streams-ws-1.1.0-SNAPSHOT.jar
..\..\bin\tnt4j-streams.bat -f:tnt-data-source.xml