#!/bin/sh
#
# How To:
# All these actions require that you ran 'mvn package' successfully.
#
# * start server (LAN mode):
#   > ./runServer.sh
#
# * start server (normal mode):
#   > FULL_CP=1 ./runServer.sh
#
# * start server (debug mode):
#   > DBG_PORT=7333 FULL_CP=1 ./runServer.sh
#
# * stop server:
#   press [Ctrl]+[C]
#
# * configure logging:
#   > cp src/main/resources/logback.xml conf
#   > ${EDITOR} conf/logback.xml
#   For documentation about logback configuration, see:
#   http://logback.qos.ch/manual/configuration.html
#
# * configure the DB:
#   > cp conf/META-INF/persistence.xml.template conf/META-INF/persistence.xml
#   > ${EDITOR} conf/META-INF/persistence.xml
#   Info about the default persistence provider for SpringLS (Hibernate):
#   http://docs.jboss.org/hibernate/stable/entitymanager/reference/en/html/configuration.html
#

# move to the dir containing this script
cd $(dirname $0)

#MY_JAR_BARE=$(ls target/original-springls*)
MY_JAR_BASE=$(ls target/springls*)
MY_DEPENDENCY_CP="target/dependency/*"
# use this line for old JDKs
#MY_DEPENDENCY_CP=$(ls -1 target/dependency/*.jar | awk 'BEGIN { cp="" } { if (cp != "") {cp = cp ":"} cp = cp $1 } END { print(cp) }')

if [ ${FULL_CP} ]; then
	MY_MAIN_CP=${MY_JAR_BASE}:${MY_DEPENDENCY_CP}
else
	MY_MAIN_CP=${MY_JAR_BASE}
fi

MY_FINAL_CP=conf:${MY_MAIN_CP}
MY_MAIN_CLASS_main=com.springrts.springls.Main
MY_MAIN_CLASS_accountUtils=com.springrts.springls.accounts.AccountUtils
MY_MAIN_CLASS=${MY_MAIN_CLASS_main}

MY_OPTIONAL_OPTS=""

if [ ${DBG_PORT} ]; then
	MY_DEBUG_OPTS="-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${DBG_PORT} -Djava.compiler=NONE -Xnoagent"
else
	MY_DEBUG_OPTS=""
fi
MY_OPTIONAL_OPTS="${MY_OPTIONAL_OPTS} ${MY_DEBUG_OPTS}"

java -cp "${MY_FINAL_CP}" ${MY_OPTIONAL_OPTS} ${MY_MAIN_CLASS} $@

