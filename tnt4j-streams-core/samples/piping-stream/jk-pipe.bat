@echo off
setlocal

set PARSERS_CFG=%*
if "%1"=="" goto set_default
goto run_stream

:set_default
set PARSERS_CFG=parsers.xml

:run_stream
@echo on
..\..\bin\tnt4j-streams.bat -p:%PARSERS_CFG%
