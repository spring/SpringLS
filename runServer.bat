@ECHO OFF
REM
REM How To:
REM
REM * start server (LAN mode):
REM   > runServer.bat
REM
REM * start server (normal mode):
REM   > runServer.bat normal
REM
REM * stop server:
REM   press [Ctrl]+[C]
REM
REM * configure logging:
REM   > copy src\main\resources\log4j.properties .
REM   > edit log4j.properties
REM   For documentation about log4j configuration,
REM   see the 'Configuration' section on this site:
REM   http://logging.apache.org/log4j/1.2/manual.html
REM

IF {%1}=={normal} (
	SET MY_ECHO=Starting normal server, all dependencies
	SET MY_JAVA_ARGS=-cp ".;target\*;target\dependency\*" com.springrts.tasserver.TASServer %2 %3 %4 %5 %6 %7 %8 %9
) ELSE (
	SET MY_ECHO=Starting LAN server, only minimal dependencies
	REM SET MY_JAVA_ARGS=-jar target\tasserver-<version>.jar -LAN
	SET MY_JAVA_ARGS=-cp ".;target\*" com.springrts.tasserver.TASServer -LAN %2 %3 %4 %5 %6 %7 %8 %9
)

ECHO %MY_ECHO%
java %MY_JAVA_ARGS%
