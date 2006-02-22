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