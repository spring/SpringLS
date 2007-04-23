#!/usr/bin/env make
# Author: Tobi Vollebregt

.PHONY: all clean distclean start

# Compile all .java files into .class files, then put them in the .jar file.
all:
	javac *.java
	@echo All Done!

clean:

# Remove all generated .class files.
distclean:
	rm -f *.class

# Start the server in the background.
start: all
	java TASServer -lan -debug 2 &
