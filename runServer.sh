#!/bin/sh
#
# How To:
# All these actions require that you ran 'mvn package' successfully.
#
# * start server (LAN mode):
#   > ./runServer.sh
#
# * start server (normal mode, using a database):
#   > FULL_CP=1 ./runServer.sh --database
#
# * start server (debug mode):
#   > DBG_PORT=7333 FULL_CP=1 ./runServer.sh
#
# * start server (daemon mode):
#   > ./runServer.sh --daemon
#
# * stop server:
#   press [Ctrl]+[C]
#
# * stop server (daemon mode):
#   > kill $(cat server.pid)
#

PID_FILE=server.pid

# move to the dir containing this script
cd $(dirname $0)

#MY_JAR_BARE=$(ls target/original-springls*)
MY_JAR_BASE=$(ls target/springls*stand-alone.jar)
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

USERS_ARGS=$@
# check if the user supplied args contain "--daemon"
# NOTE This is quite hacky, but non-bash shells freak out
#   if we try to supply something like "-p 8200" to the test program
DAEMON_ARG_GIVEN=$(echo "${USERS_ARGS}" | sed -e "s/.*\\([ \t]\\|^\\)--daemon\\([ \t]\\|$\\).*/ZZZZZZZZZZZZZZZ/")
DAEMON_ARG_GIVEN=$(echo "${DAEMON_ARG_GIVEN}" | sed -e "s/.*[^Z].*//g")
# remove "--daemon", if it is present
USERS_ARGS=$(echo "${USERS_ARGS}" | sed -e "s/\\([ \t]\\|^\\)--daemon\\([ \t]\\|$\\)/ /")

if [ -z "${DAEMON_ARG_GIVEN}" ]; then
	echo "Run in-shell..."
	# run normal (not as daemon) if --daemon switch was not given
	java -cp "${MY_FINAL_CP}" ${MY_OPTIONAL_OPTS} ${MY_MAIN_CLASS} ${USERS_ARGS}
else
	echo "Run as daemon..."
	nohup java -cp "${MY_FINAL_CP}" ${MY_OPTIONAL_OPTS} ${MY_MAIN_CLASS} ${USERS_ARGS} > /dev/null &
	# output the PID to file ${PID_FILE}
	echo $! > ${PID_FILE}
	# Note: The PID file is not deleted on process end
fi

