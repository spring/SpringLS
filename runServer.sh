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
# * stop server:
#   press [Ctrl]+[C]
#
# * configure logging:
#   > cp src/main/resources/log4j.properties conf
#   > ${EDITOR} conf/log4j.properties
#   For documentation about log4j configuration,
#   see the 'Configuration' section on this site:
#   http://logging.apache.org/log4j/1.2/manual.html
#
# * configure the DB:
#   > cp conf/META-INF/persistence.xml.template conf/META-INF/persistence.xml
#   > ${EDITOR} conf/META-INF/persistence.xml
#   Info about the default persistence provider for TASServer (Hibernate):
#   http://docs.jboss.org/hibernate/stable/entitymanager/reference/en/html/configuration.html
#

#MY_JAR_BARE=$(ls target/original-tasserver*)
MY_JAR_BASE=$(ls target/tasserver*)
MY_DEPENDENCY_CP="target/dependency/*"
# use this line for old JDKs
#MY_DEPENDENCY_CP=$(ls -1 target/dependency/*.jar | awk 'BEGIN { cp="" } { if (cp != "") {cp = cp ":"} cp = cp $1 } END { print(cp) }')

if [ ${FULL_CP} ]; then
	MY_MAIN_CP=${MY_JAR_BASE}:${MY_DEPENDENCY_CP}
else
	MY_MAIN_CP=${MY_JAR_BASE}
fi

MY_FINAL_CP=conf:${MY_MAIN_CP}
MY_MAIN_CLASS_main=com.springrts.tasserver.TASServer
MY_MAIN_CLASS_admn=com.springrts.tasserver.JPAAccountsService
MY_MAIN_CLASS=${MY_MAIN_CLASS_main}

java -cp "${MY_FINAL_CP}" ${MY_MAIN_CLASS} $@

