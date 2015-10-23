@echo off
set RUNDIR=%~p0
set CLASSPATH="%RUNDIR%..\tnt4j-streams.jar;%RUNDIR%..\lib\*"
set TNT4JOPTS=-Dtnt4j.config="%RUNDIR%..\config\tnt4j.properties"
set LOG4JOPTS=-Dlog4j.configuration="file:%RUNDIR%..\config\log4j.properties"
java %LOG4JOPTS% %TNT4JOPTS% -classpath %CLASSPATH% com.jkool.tnt4j.streams.StreamsAgent %*