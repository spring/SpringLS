/*
 * Created on 2006.9.30
 */

package com.springrts.tasserver;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;

/**
 * @author Betalord
 */
public class ServerNotifications {

	private static final Log s_log  = LogFactory.getLog(ServerNotifications.class);
	/**
	 * This will also get saved with notifications, just in case the format
	 * of notification files changes in the future.
	 */
	public static final String NOTIFICATION_SYSTEM_VERSION = "1.0";

	/**
	 * NOTE: This method may be called from multiple threads simultaneously,
	 *       so do not remove the 'synchronized' identifier!
	 */
	public static synchronized boolean addNotification(ServerNotification sn) {

		if (TASServer.LAN_MODE) {
			return false; // ignore notifications if server is running in lan mode!
		}
		String fname = TASServer.SERVER_NOTIFICATION_FOLDER + "/" + Misc.easyDateFormat("yyyyMMdd");
		int counter = 1;
		while ((new File(fname + "_" + counter)).exists()) {
			counter++;
		}
		fname += "_" + counter;
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(fname, true));
			out.write(NOTIFICATION_SYSTEM_VERSION + "\r\n");
			out.write(sn.toString());
			out.close();
		} catch (IOException e) {
			s_log.error("Unable to write file <" + fname + ">. Server notification will not be saved!");
			Clients.sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Serious problem: Unable to save server notification (notification dropped).");
			return false;
		}

		return true;
	}
}
