To host a lan game, run LanServer.bat or DebugLanServer.bat. People from 
local network will be able to join as well as people from the internet, 
although those from the internet won't be able to join games hosted by 
local clients, still local clients will be able to join games hosted by 
outside players. You don't need to create any accounts when joining a lan
server, it will accept any username/password.

If you experience problems using server in LAN mode (like other clients
being timed-out when connecting to your game), you should try doing this:
let the player which is NOT running the server host. This is important
when server assigns a player (who is using same computer as server) a 
local IP (127.0.0.1), and other players from LAN receive this IP and when 
they try to connect to game they try to connect to this IP, which is wrong. 
Server should replace this IP with 192.168.x.y automatically, but if it fails 
you should use the method described above. Also, you may try using different 
UDP source ports (you can choose it in TASClient's option screen).

If you get "Exception in thread "main" java.lang.NoClassDefFoundError: TASServer"
error when trying tu run server, then you should do this:

1) create folder C:\tass (or any other folder name with name length <= 8 chars, 
   just to make sure)
2) unzip TASServer.zip into it
3) run cmd.exe and go to C:\tass
4) run server like this:
   C:\tass\java -cp . TASServer
   (note that there is space before "-cp" and after "-cp", and also after "." 

You will also need java runtime, you can get it here (1.5.0 version):
http://java.sun.com/j2se/1.5.0/download.jsp