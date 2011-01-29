# Java Spring Lobby Server - Transfer old accounts

__README__

This project is deprecated.

This is a tool to convert the old accoutns fromat to the "new" one, while old is
till 2005 and new is till 2010, so the "new" format in this tool is already old
by now. It is file based, while the new one is DB/SQL/JPA based, and at least
theoretically compatible with aegis python servers format.


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
`target/tasserver-transferoldaccounts*.jar`


## Running

If you are on windows, or in an X session, you may just double-click the jar.
If not, you may run:

	> java -jar target/tasserver-transferoldaccounts*.jar

