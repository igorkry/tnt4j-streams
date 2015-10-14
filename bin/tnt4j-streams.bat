@echo off
set RUNDIR=%~p0
set CLASSPATH="%RUNDIR%..\tnt4j-streams.jar;%RUNDIR%..\lib\*"
set TNT4JOPTS=-Dtnt4j.config="%RUNDIR%..\config\tnt4j.properties"
java %TNT4JOPTS% -classpath %CLASSPATH% com.jkool.tnt4j.streams.StreamsAgent %*