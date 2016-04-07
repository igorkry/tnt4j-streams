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
call %MVN% install:install-file -Dfile=tnt4j-api.jar -DgroupId=com.nastel.jkool.tnt4j -DartifactId=tnt4j-api -Dversion=1.0.0 -Dpackaging=jar
call %MVN% install:install-file -Dfile=jkool-jesl.jar -DgroupId=com.nastel.jkool.tnt4j -DartifactId=jkool-jesl -Dversion=1.0.0 -Dpackaging=jar

echo installing TNT4J logger libraries...
call %MVN% install:install-file -Dfile=tnt4j-log4j12.jar -DgroupId=com.nastel.jkool.tnt4j -DartifactId=tnt4j-log4j12 -Dversion=1.0.0 -Dpackaging=jar
rem call %MVN% install:install-file -Dfile=tnt4j-logback.jar -DgroupId=com.nastel.jkool.tnt4j -DartifactId=tnt4j-logback -Dversion=1.0.0 -Dpackaging=jar

echo DONE!

pause