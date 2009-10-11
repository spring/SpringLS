#!/bin/sh
#
# How To:
# * start server (LAN mode):
#   > runServer.sh
# * start server (normal mode):
#   > FULL_CP=1 runServer.sh
# * stop server:
#   press [Ctrl]+[C]
# * configure logging:
#   > cp src/main/resources/log4j.properties .
#   > edit log4j.properties
#   for documentation about log4j configuration,
#   see the 'Configuration' section on this site:
#   http://logging.apache.org/log4j/1.2/manual.html

MY_JAR_BARE=$(ls target/original-tasserver*)
MY_JAR_BASE=$(ls target/tasserver*)
MY_DEPENDENCY_CP="target/dependency/*"
# use this line for old JDKs
#MY_DEPENDENCY_CP=$(ls -1 target/dependency/*.jar | awk 'BEGIN { cp="" } { if (cp != "") {cp = cp ":"} cp = cp $1 } END { print(cp) }')

if [ ${FULL_CP} ]; then
	MY_MAIN_CP=${MY_JAR_BARE}:${MY_DEPENDENCY_CP}
else
	MY_MAIN_CP=${MY_JAR_BASE}
fi

MY_FINAL_CP=.:${MY_MAIN_CP}
MY_MAIN_CLASS=com.springrts.tasserver.TASServer

java -cp "${MY_FINAL_CP}" ${MY_MAIN_CLASS} $@

