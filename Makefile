#!/usr/bin/env make
# Author: Tobi Vollebregt

.PHONY: all clean distclean start stop

# Compile all .java files into .class files, then put them in the .jar file.
all:
	javac *.java
	@echo All Done!

clean:

# Remove all generated .class files.
distclean:
	rm -f *.class

# Start the server in the background.
start:
	umask 077 && \
		java -Xms256m -Xmx256m -classpath .:/usr/share/java/mysql-connector-java.jar \
		TASServer -lan -debug 2 -loadargs lanadmin.txt \
		| grep -v -E '^(\[<-.*\] "PING"$$)|(\[->.*\])' \
		| ([ -z "`which cronolog`" ] && cat || cronolog 'logs/%Y%m%d.log') &

# Stop the server cleanly.
stop:
	-/bin/echo -e "LOGIN `awk '{print $$2" "$$3}' lanadmin.txt` 0 0 0\nSTOPSERVER" | telnet localhost 8200
