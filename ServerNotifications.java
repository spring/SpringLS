/*
 * Created on 2006.9.30
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 */

/**
 * @author Betalord
 *
 */

import java.io.*;

public class ServerNotifications {

	public static final String NOTIFICATION_SYSTEM_VERSION = "1.0"; // this will also get saved with notifications just in case format of notification files change in the future
	
	public static boolean addNotification(ServerNotification sn) {
		if (TASServer.LAN_MODE) return false; // ignore notifications if server is running in lan mode!
		String fname = TASServer.SERVER_NOTIFICATION_FOLDER + "/" + Misc.easyDateFormat("yyyyMMdd");
		int counter = 1;
		while ((new File(fname + "_" + counter)).exists()) counter++;
		fname += "_" + counter;
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(fname, true));
			out.write(NOTIFICATION_SYSTEM_VERSION + "\r\n");
			out.write(sn.author + "\r\n");
			out.write(sn.time + "\r\n");
			out.write(sn.message);
			out.close();			
		} catch (IOException e) {
			System.out.println("Error: unable to write file <" + fname + ">. Server notification will not be saved!");
			TASServer.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Serious problem: Unable to save server notification (notification dropped).");
			return false;
		}

		return true;
	}

}
