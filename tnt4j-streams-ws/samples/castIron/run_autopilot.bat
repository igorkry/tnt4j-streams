@echo off
set STREAMSOPTS=-Dorg.quartz.threadPool.threadCount=1
set TNT4J_PROPERTIES=".\tnt4j_autopilot.properties"
..\..\bin\tnt4j-streams.bat -f:tnt-data-source.xml