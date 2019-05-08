BASEDIR=$(dirname "$0")
echo Executing init script from $BASEDIR

$BASEDIR/gradlew -p $BASEDIR :integrationTests:build :integrationTests:dumpRobotFrameworkClasspath
cat $BASEDIR/integrationTests/build/cdd.robot.classpath.txt > $BASEDIR/cdd.robot.init.out