@echo on
setlocal
rem set JMS_IMPL_LIBPATH=.\lib\rabbitMQ\*
rem set JMS_IMPL_LIBPATH=.\lib\solaceLib\*
rem set JMS_IMPL_LIBPATH=.\lib\IBMMQ\*
rem set JMS_IMPL_LIBPATH=.\lib\ActiveMQ\*
set LIBPATH=%LIBPATH%;%JMS_IMPL_LIBPATH%
..\..\bin\tnt4j-streams.bat -f:tnt-data-source.xml