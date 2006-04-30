To host a lan game, run LanServer.bat or DebugLanServer.bat. People from 
local network will be able to join as well as people from the internet, 
although those from the internet won't be able to join games hosted by 
local clients, still local clients will be able to join games hosted by 
outside players. You don't need to create any accounts when joining a lan
server, it will accept any username/password.

If you experience problems using server in LAN mode (like other clients
being timed-out when connecting to your game), you should try doing this:
let the player which is NOT running the server host. This is important
when server assigns a player (who is using same computer as the server) a 
local IP (127.0.0.1), and other players from LAN receive this IP and when 
they try to connect to the game they try to connect to this IP, which is wrong. 
Server should replace this IP with local one (e.g.: 192.168.x.y) automatically, 
but if it fails for any reason you should use the method described above.

To be able to accept connections from outside the LAN, you'll have to forward 
ports 8200 (TCP) and 8201 (UDP).

You will also need java runtime, you can get it here (1.5.0 version):
http://java.sun.com/j2se/1.5.0/download.jsp

---------------------------------------------------------------------------

Command line arguments:


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


Example of usage (main lobby server uses these settings):
java TASServer -DEBUG 1 -natport 8201 -logmain -port 8200 | tee ./logs/TASServer.log