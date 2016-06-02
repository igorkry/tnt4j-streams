@echo OFF

setlocal

if "%M2_HOME%" == "" goto try_maven_home
set MVN=%M2_HOME%\bin\mvn
if not exist "%MVN%" goto try_maven_home
goto run_mvn

:try_maven_home
if "%MAVEN_HOME%" == "" goto try_path
set MVN=%MAVEN_HOME%\bin\mvn
if not exist "%MVN%" goto try_path
goto run_mvn

:try_path
set MVN=C:\maven\bin\mvn

:run_mvn

echo installing TNT4J and JESL libraries...
call %MVN% install:install-file -Dfile=tnt4j-api.jar -DgroupId=com.jkoolcloud -DartifactId=tnt4j -Dversion=2.2 -Dpackaging=jar
call %MVN% install:install-file -Dfile=jkool-jesl.jar -DgroupId=com.jkoolcloud -DartifactId=jesl -Dversion=0.1.5 -Dpackaging=jar

echo installing TNT4J logger libraries...
call %MVN% install:install-file -Dfile=tnt4j-log4j12.jar -DgroupId=com.jkoolcloud.tnt4j.logger -DartifactId=log4j -Dversion=0.1 -Dpackaging=jar
rem call %MVN% install:install-file -Dfile=tnt4j-logback.jar -DgroupId=com.jkoolcloud.tnt4j.logger -DartifactId=logback -Dversion=0.1 -Dpackaging=jar

echo DONE!

pause