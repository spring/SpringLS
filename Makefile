#!/usr/bin/env make
# Author: Tobi Vollebregt

.PHONY: all clean distclean start

# Compile all .java files into .class files, then put them in the .jar file.
all TASServer.jar:
	javac *.java
	jar cef TASServer TASServer.jar *.class
	@echo All Done!

# Remove all .class files.
clean:
	rm -f *.class

# Remove all generated files.
distclean: clean
	rm -f TASServer.jar

# Start the server in the background, building it if it doesn't exist yet.
start: TASServer.jar
	java -jar TASServer.jar &
