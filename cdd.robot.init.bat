echo Executing init script from %~dp0
cmd /C %~dp0gradlew.bat -p %~dp0 :integrationTests:build :integrationTests:dumpRobotFrameworkClasspath
type %~dp0integrationTests\build\cdd.robot.classpath.txt > %~dp0cdd.robot.init.out