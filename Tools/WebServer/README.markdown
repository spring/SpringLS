# Java Spring Lobby Server - static content web-server

__README__

This is a mini sub-project. It consists of a very basic web-server that is able
to server static content.


## Building

Maven is used as the project management system.
The project description file is `pom.xml`, which contains everything
Maven needs to know about the project, in order to:

* Download dependencies
* Compile the sources
* Pack the class files together with all the dependencies into a single,
  executable jar file

### Build steps

1.	Make sure you have Maven 2 or later installed.
	You can check that with the following command:

		> mvn --version

2.	compile and package:

		> mvn package

	This may take some time, if you are running Maven for the first time,
	as it has to download all the dependencies for the different build steps.

If everything went smoothly, this was it already!

All the output of the build process is under the `target` sub-dir.
This is also where you find the final jar file:
`target/web-server*.jar`


## Running

If you are on windows, or in an X session, you may just double-click the jar.
If not, you may run:

	> java -jar target/web-server*.jar

