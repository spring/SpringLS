/*
	Copyright (c) 2006 Robin Vobruba <hoijui.quaero@gmail.com>

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.springrts.tasserver;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Betalord
 * @author hoijui
 */
public class ServerNotifications implements ContextReceiver {

	private static final Logger LOG
			= LoggerFactory.getLogger(ServerNotifications.class);
	/**
	 * This will also get saved with notifications, just in case the format
	 * of notification files changes in the future.
	 */
	public static final String NOTIFICATION_SYSTEM_VERSION = "1.0";

	private static final String DEFAULT_DIR = "./notifs";
	private final File notificationsDir;

	private Context context = null;


	public ServerNotifications() {
		this.notificationsDir = new File(DEFAULT_DIR);
	}


	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}

	/** create notifications dir if it does not yet exist */
	private void ensureNotifsDirExists() {

		synchronized (notificationsDir) {
			if (!notificationsDir.exists()) {
				boolean success = notificationsDir.mkdir();
				if (!success) {
					LOG.error("Unable to create folder: {}", notificationsDir);
				} else {
					LOG.info("Created missing folder: {}", notificationsDir);
				}
			}
		}
	}

	private static synchronized File findWriteableNotifFile(final String baseFileName) {

		File notifFile = null;

		int counter = 1;
		File tmpFile = null;
		do {
			tmpFile = new File(baseFileName + "_" + counter);
			counter++;
		} while (tmpFile.exists());

		try {
			if (tmpFile.createNewFile()) {
				notifFile = tmpFile;
			} else {
				throw new IOException("File already exists");
			}
		} catch (IOException ex) {
			LOG.error("Failed creating notification-file: " + tmpFile, ex);
		}

		return notifFile;
	}

	/**
	 * NOTE: This method may be called from multiple threads simultaneously!
	 */
	public boolean addNotification(ServerNotification sn) {

		if (context.getServer().isLanMode()) {
			// ignore notifications if server is running in lan mode!
			return false;
		}

		ensureNotifsDirExists();

		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		final String baseFileName
				= notificationsDir + "/" + dateFormat.format(new Date());

		File notifFile = findWriteableNotifFile(baseFileName);

		if (notifFile == null) {
			LOG.error("Unable to find/create a notification file. Server"
					+ " notification will not be saved!");
			context.getClients().sendToAllAdministrators("SERVERMSG [broadcast"
					+ " to all admins]: Serious problem: Unable to find/create"
					+ " a notification file (notification dropped)");
			return false;
		}

		Writer outF = null;
		BufferedWriter out = null;
		try {
			outF = new FileWriter(notifFile, true);
			out = new BufferedWriter(outF);
			out.write(NOTIFICATION_SYSTEM_VERSION);
			out.write(Misc.EOL);
			out.write(sn.toString());
			out.close();
		} catch (IOException ex) {
			LOG.error("Unable to write file <" + notifFile
					+ ">. Server notification will not be saved!", ex);
			context.getClients().sendToAllAdministrators("SERVERMSG [broadcast"
					+ " to all admins]: Serious problem: Unable to save server"
					+ " notification (notification dropped).");
			return false;
		} finally {
			try {
				if (out != null) {
					out.close();
				} else if (outF != null) {
					outF.close();
				}
			} catch (IOException ex) {
				LOG.warn("Failed to close notification file writer: "
						+ notifFile.getAbsolutePath(), ex);
			}
		}

		return true;
	}
}
