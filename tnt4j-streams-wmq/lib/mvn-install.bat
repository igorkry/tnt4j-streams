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

echo installing libraries required by WMQ...
call %MVN% install:install-file -Dfile=ibm.mq/com.ibm.mq.jar -DgroupId=com.ibm -DartifactId=com.ibm.mq -Dversion=7.5 -Dpackaging=jar
call %MVN% install:install-file -Dfile=ibm.mq/com.ibm.mq.commonservices.jar -DgroupId=com.ibm -DartifactId=com.ibm.mq.commonservices -Dversion=7.5 -Dpackaging=jar
call %MVN% install:install-file -Dfile=ibm.mq/com.ibm.mq.headers.jar -DgroupId=com.ibm -DartifactId=com.ibm.mq.headers -Dversion=7.5 -Dpackaging=jar
call %MVN% install:install-file -Dfile=ibm.mq/com.ibm.mq.jmqi.jar -DgroupId=com.ibm -DartifactId=com.ibm.mq.jmqi -Dversion=7.5 -Dpackaging=jar
call %MVN% install:install-file -Dfile=ibm.mq/com.ibm.mq.pcf.jar -DgroupId=com.ibm -DartifactId=com.ibm.mq.pcf -Dversion=7.5 -Dpackaging=jar

echo DONE!

pause