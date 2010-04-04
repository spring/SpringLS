# TA Spring Server

__README__

This repository contains three Java applications:

* _TASServer_
	Spring lobby server

* _ChanServ_
	Spring lobby bot

* _TransferOldAccounts_
	Small helper application for migration,
	only really interesting for the online server admins

The rest of this Readme is about TASServer only.


## Building

Maven is used as the project management system.
The project description file is `pom.xml`, which contains everything
Maven needs to know about the project, in order to:

* Download dependencies
* Compile the sources
* Pack the class files together with all the dependencies into a single,
  executable jar file

### Installing Maven 2

In case you already have it installed, skip this paragraph.

* _Windows_

	Download the latest stable version (not the source)
	[here](http://maven.apache.org/download.html).
	Then extract to eg. your `C:\Program Files\`,
	and make sure the bin sub-dir of the extracted folder is in your `PATH`
	environment variable.

* _Unix, Linux, BSD, OS X_

	Install the `maven2` package in your systems way of doing so.
	On Ubuntu for example, you would do this:

		> sudu apt-get install maven2


### TASServer build steps

1.	Make sure you have Maven 2 installed. You can check that with the following command:

		> mvn --version

2.	compile and package TASServer:

		> mvn package

	This may take quite some time, if you are running Maven for the first time,
	as it has to download all the dependencies for the different build steps,
	plus the dependencies of TASServer.

If everything went smoothly, this was it already!

All the output of the build process is under the `target` sub-dir.
This is also where you find the final jar file:
`target/tasserver*.jar`


## Running

Use `runServer.sh|.bat` to start the server, if you built it with Maven,
and see the documentation in these files for further info.

When server is up and running, people from the local network will be able to 
join it as well as people from the internet, although those from the internet 
will not be able to join games hosted by local clients, still local clients will 
be able to join games hosted by outside players. You do not need to create any 
accounts when joining a LAN server, it will accept any user-name/password.

If you experience problems using server in LAN mode (like other clients
being timed-out when connecting to your game), you should try this:
Let the player who is NOT running the server host. This is important
when the server assigns a player (who is using the same computer as the server)
the `localhost` IP (`127.0.0.1`). In this case, other players on the LAN would
receive this IP and when they try to connect to the game host at `127.0.0.1`,
the connection attempt will fail.
The server should replace this IP with a local one (`192.168.x.y`) automatically,
but if it fails for any reason, you should use the method described above.

To be able to accept connections from outside the LAN, you will have to forward 
ports 8200 (TCP) and 8201 (UDP) to the machine running the lobby.

You will also need the Java run-time environment (JRE) 6 or later.
The latest version can be found [here](http://java.sun.com/j2se/1.6.0/download.jsp).


## Command line arguments

	-PORT [number]
Server will host on port [number]. If command is omitted,
default port will be used.

	-LAN
Server will run in LAN mode, meaning any user can login as
long as he uses unique user-name (password is ignored).
Note: Server will accept users from outside the local network too.

	-DEBUG [number]
Use 0 for no verbose, 1 for normal and 2 for extensive verbose.

	-STATISTICS
Server will create and save statistics on disk on predefined intervals.

	-NATPORT [number]
Server will use this port with some NAT traversal techniques. If command is omitted,
default port will be used.

	-LOGMAIN
Server will log all conversations from channel `#main` to `MainChanLog.log`

	-LANADMIN [user-name] [password]
Will override default LAN admin account. Use this account to set up your LAN server
at runtime.

	-LOADARGS [file-name]
Will read command-line arguments from the specified file. You can freely combine actual
command-line arguments with the ones from the file (if duplicate arguments are specified, the last
one will prevail).

	-LATESTSPRINGVERSION [version]
Will set latest Spring version to this string. By default no value is set (defaults to "*").
This is used to tell clients which version is the latest one so that they know when to update.


Example of usage (main lobby server uses these arguments):

	> java TASServer -DEBUG 1 -natport 8201 -logmain -port 8200 | tee ./logs/TASServer.log


## Running server in normal mode

For LAN mode, this section is not relevant.

When you want to run normal server, some external .jar files have to be added
to classpath. Here is the current list with links where to get them:

* [mysql-connector-java-5.1.5-bin.jar](http://www.mysql.com/products/connector/j/)
* [commons-pool-1.4.jar](http://commons.apache.org/pool/)
* [commons-dbcp-1.2.2.jar](http://commons.apache.org/dbcp/)
