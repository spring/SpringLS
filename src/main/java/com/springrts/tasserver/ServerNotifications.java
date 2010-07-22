/*
 * Created on 2006.9.30
 */

package com.springrts.tasserver;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Betalord
 */
public class ServerNotifications implements ContextReceiver {

	private final Log s_log  = LogFactory.getLog(ServerNotifications.class);
	/**
	 * This will also get saved with notifications, just in case the format
	 * of notification files changes in the future.
	 */
	public static final String NOTIFICATION_SYSTEM_VERSION = "1.0";

	private static final DateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

	private static final String DEFAULT_DIR = "./notifs";
	private File notificationsDir;

	private Context context = null;


	public ServerNotifications() {
		this.notificationsDir = new File(DEFAULT_DIR);
	}


	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}

	private void ensureNotifsDirExists() {

		// create notifications dir if it does not yet exist
		if (!context.getServer().isLanMode()) {
			if (!notificationsDir.exists()) {
				boolean success = notificationsDir.mkdir();
				if (!success) {
					s_log.error("Unable to create folder: " + notificationsDir);
				} else {
					s_log.info("Created missing folder: " + notificationsDir);
				}
			}
		}
	}

	/**
	 * NOTE: This method may be called from multiple threads simultaneously,
	 *       so do not remove the 'synchronized' identifier!
	 */
	public synchronized boolean addNotification(ServerNotification sn) {

		if (context.getServer().isLanMode()) {
			return false; // ignore notifications if server is running in lan mode!
		}
		ensureNotifsDirExists();
		String fname = notificationsDir + "/" + SHORT_DATE_FORMAT.format(new Date());
		int counter = 1;
		while (new File(fname + "_" + counter).exists()) {
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
			context.getClients().sendToAllAdministrators("SERVERMSG [broadcast to all admins]: Serious problem: Unable to save server notification (notification dropped).");
			return false;
		}

		return true;
	}
}
