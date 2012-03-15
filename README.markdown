# Java Spring Lobby Server

__README__

This software is a lobby server for the Spring RTS engine. You may think of it
as something similar to an IRC server with additional protocol instructions to
manage battles.

Historically seen, this was the first, and till 2010 also the main/official
lobby server implementation.

This repository also contained two other tiny applications, which were removed
in 2010, because they were no longer of use alone or in combination with the
lobby server:
* TransferOldAccounts
* WebServer


## Building

Maven is used as the project management system.
The project description file is `pom.xml`, which contains everything
Maven needs to know about the project, in order to:

* Download dependencies
* Compile the sources
* Pack the class files together with all the dependencies into a single,
  executable jar file

### Installing Maven

You need Maven version 2 or later.
In case you already have it installed, skip this paragraph.

* _Windows_

	Download the latest stable version (not the source)
	[here](http://maven.apache.org/download.html).
	Then extract to eg. your `C:\Program Files`,
	and make sure the bin sub-dir of the extracted folder is in your `PATH`
	environment variable.

* _Unix, Linux, BSD, OS X_

	Install the `maven2` package in your systems way of doing so.
	On Ubuntu for example, you would do this:

		> sudo apt-get install maven2


### Building the software

1.	Make sure you have Maven 2 or later installed.
	You can check that with the following command:

		> mvn --version

2.	compile and package:

		> mvn package

	This may take quite some time, if you are running Maven for the first time,
	as it has to download all the dependencies for the different build steps,
	plus our own dependencies.

If everything went smoothly, this was it already!

All the output of the build process is under the `target` sub-dir.
This is also where you find the final jar files:
`target/springls*.jar`


## Running

The minimum requirement is the Java run-time environment (JRE) 6 or later.
The latest version can be found
[here](http://java.sun.com/j2se/1.6.0/download.jsp).

Use `runServer.sh` (Unix, Linux, BSD, OS X) or `runServer.bat` (Windows)
to start the server if you built it with Maven.
You should also see the documentation in these files, for further info.

### LAN mode
When the server is up and running, people from the local network will be able to
join it as well as people from the internet. Although those from the internet
will not be able to join games hosted by local clients, local clients in turn
will be able to join games hosted by outside players.
You do not need to create any accounts when joining a LAN server,
it will accept any user-name/password.

If you experience problems using the server in LAN mode (like other clients
being timed-out when connecting to your game), you should try this:
Let the player who is NOT running the server host. This is important
when the server assigns a player (who is using the same computer as the server)
the `localhost` IP (`127.0.0.1`). In this case, other players on the LAN would
receive this IP, and when they try to connect to the game host at `127.0.0.1`,
the connection attempt will fail.
The server should replace this IP with a local one (`192.168.x.y`) automatically,
but if it fails for any reason, you should use the method described above.

To be able to accept connections from outside the LAN, you will have to forward
ports 8200 (TCP) and 8201 (UDP) to the machine running the lobby.

### Logging
To configure logging details, for example using a higher verbosity level,
you have to create a custom config file.
If you are using the `runServer.*` script,
this is done most easily using the following steps:

_Unix, Linux, BSD, OS X_

	cp src/main/resources/logback.xml conf
	${EDITOR} conf/logback.xml

_Windows_

	copy src\main\resources\logback.xml conf
	notepad conf\logback.xml

For documentation about logback configuration,
see [this link](http://logback.qos.ch/manual/configuration.html).

### Configuring the DB
In additiona to the `--database` command line parameter,
you need to tell the server which database connection to use
for storing account- and other info,
by configuring JPA (Java Persistence API) details.
If you are using the `runServer.*` script,
this is done most easily using the following steps:

_Unix, Linux, BSD, OS X_

	cp conf/META-INF/persistence.xml.template conf/META-INF/persistence.xml
	${EDITOR} conf/META-INF/persistence.xml

_Windows_

	copy conf\META-INF\persistence.xml.template conf\META-INF\persistence.xml
	notepad conf\META-INF\persistence.xml

Info about the default persistence provider (Hibernate)
can be found [here](http://docs.jboss.org/hibernate/stable/entitymanager/reference/en/html/configuration.html).


## Command line arguments

Example of usage:

	> java -jar springls-*.jar --port 8200 --nat-port 8201

Arguments are case sensitive.
For the full list of arguments, use `--help`.

To stop the server, issue _[Ctrl]+[C]_.


## Running with a DB

Normal mode means, using a database or a flat file for storing the user accounts.
This is how you prepare for using a database:

_Unix:_

	> # copy the configuration template
	> cp conf/META-INF/persistence.xml.template conf/META-INF/persistence.xml
	> # edit the configuration
	> ${EDITOR} conf/META-INF/persistence.xml

_Windows:_

	> # copy the configuration template
	> copy conf\META-INF\persistence.xml.template conf\META-INF\persistence.xml
	> # edit the configuration
	> notepad conf\META-INF\persistence.xml

Info about the default persistence provider:
[Hibernate configuration](http://docs.jboss.org/hibernate/stable/entitymanager/reference/en/html/configuration.html)

When you are using MySQL, and you are experiencing problems to authenticate on
the DB, have a look [here](http://queuemetrics.com/faq.jsp#faq-009).
Instead of `localhost.localdomain`, you may face the same problem
with `127.0.0.1`.

You have to use the `--database` switch on the command-line,
because otherwise the server will run in LAN-mode, and not use the DB.


## Release a SNAPSHOT (devs only)

To release a development version to the Sonatype snapshot repository only:

		mvn clean deploy -Dgithub.downloads.dryRun=true


## Release (devs only)

### Prepare "target/" for the release process

	mvn release:clean

### Prepare the release
* asks for the version to use
* packages
* signs with GPG
* commits
* tags
* pushes to origin

		mvn release:prepare

### Perform the release (main part)
* checks-out the release tag
* builds
* deploy into sonatype staging repository
* uploads artifacts to the github download section

		mvn release:perform

### Release the site
* generates the site, and pushes it to the github gh-pages branch,
  visible under http://spring.github.com/SpringLS/

		git checkout <release-tag>
		mvn site
		git checkout master

### Promote it on Maven
Moves it from the sonatype staging to the main sonatype repo

1. using the Nexus staging plugin:

		mvn nexus:staging-close
		mvn nexus:staging-release

2. ... alternatively, using the web-interface:
	* firefox https://oss.sonatype.org
	* login
	* got to the "Staging Repositories" tab
	* select "com.springrts..."
	* "Close" it
	* select "com.springrts..." again
	* "Release" it


## Dev Notes

* Try not to edit the account file manually! If you do, do not forget that
  access numbers must be in binary form!

* Whenever you use `killClient()` within a for loop, do not forget to decrease
  loop counter as you will skip next client in the list otherwise. This was the
  cause for some of the "ambiguous data" errors. Or better, use the
  `killClientDelayed()` method.

* Note that access to long's is not guaranteed to be atomic, but you should use
  synchronization anyway, if you use multiple threads.


## Dev Links

Great article on how to handle network timeouts in Java:
http://www.javacoffeebreak.com/articles/network_timeouts/

Another one on network timeouts and alike:
http://www.mindprod.com/jgloss/socket.html

Great article on thread synchronization:
http://today.java.net/pub/a/today/2004/08/02/sync1.html

Throwing exceptions:
http://java.sun.com/docs/books/tutorial/essential/exceptions/throwing.html

Sun's tutorial on sockets:
http://java.sun.com/docs/books/tutorial/networking/sockets/

How to redirect program's output by duplicating handles in windows' command
prompt:
http://www.microsoft.com/resources/documentation/windows/xp/all/proddocs/en-us/redirection.mspx

How to get local IP address (like "192.168.1.1" and not "127.0.0.1"):
http://forum.java.sun.com/thread.jspa?threadID=619056&messageID=3477258

ip-to-country databases:
http://ip-to-country.webhosting.info
http://software77.net/cgi-bin/ip-country/geo-ip.pl

Another set of 232 country flags:
http://www.ip2location.com/free.asp

Some source code on how to build client-server with java.nio classes;
Betalord used ChatterServer.java code from the first link, found through the
second:
http://brackeen.com/javagamebook/ch06src.zip
http://www.gamedev.net/community/forums/topic.asp?topic_id=318099

Source for some simple threaded UDP server:
http://java.sun.com/docs/books/tutorial/networking/datagrams/example-1dot1/QuoteServerThread.java

How to properly document thread-safety when writing classes:
http://www-128.ibm.com/developerworks/java/library/j-jtp09263.html

Good article on immutables (like String etc.):
http://macchiato.com/columns/Durable2.html

General info on thread-safety in java:
http://mindprod.com/jgloss/threadsafe.html

How to use ZIP with java:
http://java.sun.com/developer/technicalArticles/Programming/compression/

How to download file from URL:
http://schmidt.devlib.org/java/file-download.html

Very good article on exceptions:
http://www.freshsources.com/Apr01.html

Short introduction to generics in JDK 1.5.0:
http://java.sun.com/j2se/1.5.0/docs/guide/language/generics.html


## NAT-Traversal

The primary NAT traversal technique that this lobby server implements is
_hole punching_. See these links for more info:

http://www.brynosaurus.com/pub/net/p2pnat/
http://www.potaroo.net/ietf/idref/draft-ford-natp2p/
http://www.newport-networks.com/whitepapers/nat-traversal1.html

See the source code for implementation details.


## Protocol

The most recent lobby protocol specification can be found here:
https://github.com/spring/LobbyProtocol


## Change-Log

### 0.35+
For detailed changes after 0.35, please see the SCM commit messages.

### 0.35
* added 'servermode' argument to TASSERVER command

### 0.34
* message IDs are now actually working
* added TESTLOGIN, TESTLOGINACCEPT and TESTLOGINDENY commands

### 0.33
* added "update properties" (updateProperties object)
* added SETLATESTSPRINGVERSION and RELOADUPDATEPROPERTIES commands

### 0.32
* added option to mute by ip
* replaced CLIENTPORT command with CLIENTOIPPORT command and also
  removed ip field from the ADDUSER command (this way IPs are no longer
  public unless you join a battle that uses nat traversal, where host
  needs to know your ip in order for the nat traversal trick to work)

### 0.31
* added new bot mode for accounts (increases traffic limit when using bot mode)

### 0.30
* added MAPGRADES command
* added FORCESPECTATORMODE command

### 0.26
* fixed some charset bug
* added UPDATEMOTD command
* fixed small bug with JOINBATTLE command not checking if battle is already
  in-game
* fixed minor bug with mute entries not expiring on the fly
* added MUTELISTSTART, MUTELIST, MUTELISTEND commands

### 0.25
* added -LANADMIN switch
* modified protocol to support arbitrary colors (RGB format)

### 0.23
* channel mute list now gets updated when user renames his account

### 0.22
* added SETCHANNELKEY command, also modified JOIN command to accept extra
  argument for locked channels
* added FORCELEAVECHANNEL command
* LEFT command now contains (optional) "reason" parameter
* replaced CHANNELS command with CHANNEL and ENDOFCHANNELS commands (see
  protocol description)
* limited maximum length of chat messages

### 0.20
* added CHANGEPASSWORD command
* GETINGAMETIME now also accepts no argument (to return your own in-game time)
* CHANNELTOPIC command now includes author name and time
* added -LOGMAIN switch
* added GETLASTIP command, FINDIP is available to privileged users as well now
* fixed bug with /me being available even after being muted
* CHANNELMESSAGE command now available to moderators as well

### 0.195
* fixed RING command not working for battle hosts

### 0.194
* integrated ploticus graphics generator and a simple web server to give access
  to server's statistics.
* fixed RING command (only host can ring players participating in his own
  battle, unless the target is host himself)
* fixed KICK command so that now player who's been kicked is notified about it
  (also kick command accepts "reason" string now)
* added "GETLASTLOGINTIME" command (for moderators)
* fixed bug with clients behind same NAT not getting local IPs in certain cases
* added simple UDP server to help with NAT traversing (see NATHelpServer.java)
* added UDPSOURCEPORT, CLIENTPORT and HOSTPORT commands (used with NAT
  traversing)

### 0.191
* fixed bug with server allowing clients to have several battles open at the
  same time

### 0.19
* improved server code (meaning less "ambigious" commands)
* added RENAMEACCOUNT command, also userName may now contain "[" and "]"
  characters
* added CHANNELMESSAGE command
* added MUTE, UNMUTE and MUTELIST commands
* clients behind same NAT now get local IPs instead of external one (from the
  server). This should resolve some issues with people playing games behind same
  NAT.
* added "agreement"

### 0.18
* multiple mod side support (battle status bits have changed)
* user who flood are now automatically banned by server

### 0.17
* server now keeps in-game time even after player has reached maximum level
  (max. in-game time server can record is 2^20 minutes)
* rewrote the network code to use java.nio classes. This fixes several known
  problems with server and also fixes multiplayer replay option.
* implemented simple anti-flood protection
* removed old file transfer commands

### 0.16
* added new host option - diminishing metal maker returns
* switched to Webnet77's ip-to-country database, which seems to be more
  frequently updated: http://software77.net/cgi-bin/ip-country/geo-ip.pl
* added "locked" parameter to UPDATEBATTLEINFO command
* added "multiplayer replays" support

### 0.152
* fixed small bug with server not updating rank when maximum level has been
  reached
* added ban list

### 0.151
* added OFFERUPDATEEX command
* added country code support
* added simple protection against rank exploiters
* added cpu info (LOGIN command now requires additional parameter)
* limited usernames/passwords to 20 chars

### 0.141
* fixed issue with server not notifying users about user's rank on login
* added command: CHANNELTOPIC

### 0.14
* added FORCETEAMCOLOR command
* fixed bug which allowed users to register accounts with userName/password
  containing chars from 43 to 57 (dec), which should be numbers (the correct
  number range is 48 to 57). Invalid chars are "+", ",", "-", "." and "/".
* added ranking system

### 0.13
* added AI support
* added KICKUSER command (admins only)
* fixed bug when server did not allow client to change its ally number if
  someone else used it, even if that was only a spectator.
* added away status bit
* fixed bug when server denied  request to battle, if there were maxplayers+1
  players already in the battle.
* added new commands: SERVERMSG, SERVERMSGBOX, REQUESTUPDATEFILE, GETFILE
* added some admin commands
* changed registration process so that now you can't register userName which is
  same as someone elses, if we ignore case. Usernames are still case-sensitive
  though.

