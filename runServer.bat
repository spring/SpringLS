@ECHO OFF
REM
REM How To:
REM All these actions require that you ran 'mvn package' successfully.
REM
REM * start server (LAN mode):
REM   > runServer.bat
REM
REM * start server (normal mode):
REM   > runServer.bat normal
REM
REM * start server (debug mode):
REM   > SET DBG_PORT=7333
REM   > runServer.bat normal
REM
REM * stop server:
REM   press [Ctrl]+[C]
REM
REM * configure logging:
REM   > copy src\main\resources\logback.xml conf
REM   > edit conf\logback.xml
REM   For documentation about logback configuration, see:
REM   http://logback.qos.ch/manual/configuration.html
REM
REM * configure the DB:
REM * > copy conf\META-INF\persistence.xml.template conf\META-INF\persistence.xml
REM   > notepad conf\META-INF\persistence.xml
REM   Info about the default persistence provider for SpringLS (Hibernate):
REM   http://docs.jboss.org/hibernate/stable/entitymanager/reference/en/html/configuration.html
REM

SET MY_MAIN_CLASS_main=com.springrts.springls.Main
SET MY_MAIN_CLASS_accountUtils=com.springrts.springls.accounts.AccountUtils
SET MY_MAIN_CLASS=%MY_MAIN_CLASS_main%

IF {%1}=={normal} (
	SET MY_ECHO=Starting normal server, all dependencies
	SET MY_JAVA_ARGS=-cp "conf;target\*;target\dependency\*" %MY_MAIN_CLASS% %2 %3 %4 %5 %6 %7 %8 %9
) ELSE (
	SET MY_ECHO=Starting LAN server, only minimal dependencies
	REM SET MY_JAVA_ARGS=-jar target\springls-<version>.jar -LAN
	SET MY_JAVA_ARGS=-cp "conf;target\*" %MY_MAIN_CLASS% -LAN %2 %3 %4 %5 %6 %7 %8 %9
)

SET MY_OPTIONAL_OPTS=

IF {%DBG_PORT%}!={} (
	SET MY_DEBUG_OPTS=-Xdebug -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=%DBG_PORT% -Djava.compiler=NONE -Xnoagent
) ELSE (
	SET MY_DEBUG_OPTS=
)
SET MY_OPTIONAL_OPTS=%MY_OPTIONAL_OPTS% %MY_DEBUG_OPTS%

ECHO %MY_ECHO%
java %MY_OPTIONAL_OPTS% %MY_JAVA_ARGS%
