This repository contains three java applications:
* TASServer           : a spring lobby server
* ChanServ            : a spring lobby bot
* TransferOldAccounts : small helper application for migration
                        (only really interesting for the online server admins)

The rest of this Readme is about TASServer only


 Building
-----------

Maven is used as the project management system.
The project description file is pom.xml, which contains everything
Maven needs to know about the project, in order to:
* Download dependencies
* Compile the sources
* Pack the class files together with all the dependencies into a single,
  executable Jar file

build steps:
1. Make sure you have Maven 2 installed. You can check that with the following command:
mvn --version
2. compile and package TASServer:
mvn package
This may take wuite some time, if you are runnig Maven for the first time,
as it has to downlaod all the dependencies for the different build steps,
plus the dependencies of TASServer.
3. If everything went smoothly, this was it already!
All the output of the build process is under the target/ subdir.
This is also where you find the final jar file:
target/tasserver*.jar


 Running
---------

If you are running it under Linux (.jar file), run it from the console:
java -jar TASServer.jar
If you are running it from Windows, simply double-click the .exe file.
Alternative is to extract .jar file and run it manually from the console:
java TASServer
Or simply by running either LanServer.bat or DebugLanServer.bat on Windows.

When server is up and running, people from local network will be able to 
join it as well as people from the internet, although those from the internet 
won't be able to join games hosted by local clients, still local clients will 
be able to join games hosted by outside players. You don't need to create any 
accounts when joining a lan server, it will accept any username/password.

If you experience problems using server in LAN mode (like other clients
being timed-out when connecting to your game), you should try doing this:
let the player who is NOT running the server host. This is important
when server assigns a player (who is using same computer as the server) a 
local IP (127.0.0.1), and other players from LAN receive this IP and when 
they try to connect to the game they try to connect to this IP, which is wrong. 
Server should replace this IP with local one (e.g.: 192.168.x.y) automatically, 
but if it fails for any reason you should use the method described above.

To be able to accept connections from outside the LAN, you'll have to forward 
ports 8200 (TCP) and 8201 (UDP).

You will also need java runtime, you can get it here (1.5.0 version):
http://java.sun.com/j2se/1.5.0/download.jsp


 Command line arguments
------------------------

-PORT [number]
  Server will host on port [number]. If command is omitted,
  default port will be used.

-LAN
  Server will run in LAN mode, meaning any user can login as
  long as he uses unique username (password is ignored).
  Note: Server will accept users from outside the local network too.

-DEBUG [number]
  Use 0 for no verbose, 1 for normal and 2 for extensive verbose.

-STATISTICS
  Server will create and save statistics on disk on predefined intervals.

-NATPORT [number]
  Server will use this port with some NAT traversal techniques. If command is omitted,
  default port will be used.

-LOGMAIN
  Server will log all conversations from channel #main to MainChanLog.log

-LANADMIN [username] [password]
  Will override default lan admin account. Use this account to set up your lan server
  at runtime.

-LOADARGS [filename]
  Will read command-line arguments from the specified file. You can freely combine actual
  command-line arguments with the ones from the file (if duplicate args are specified, the last
  one will prevail).

-LATESTSPRINGVERSION [version]
  Will set latest Spring version to this string. By default no value is set (defaults to "*").
  This is used to tell clients which version is the latest one so that they know when to update.


Example of usage (main lobby server uses these arguments):
java TASServer -DEBUG 1 -natport 8201 -logmain -port 8200 | tee ./logs/TASServer.log

 Running server in normal mode
-------------------------------
For LAN mode, this section is not relevant.

When you want to run normal server, some external .jar files have to be added
to classpath. Here is the current list with links where to get them:

* mysql-connector-java-5.1.5-bin.jar (http://www.mysql.com/products/connector/j/)
* commons-pool-1.4.jar (http://commons.apache.org/pool/)
* commons-dbcp-1.2.2.jar (http://commons.apache.org/dbcp/)
