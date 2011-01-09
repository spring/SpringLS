/*
	Copyright (c) 2011 Robin Vobruba <robin.vobruba@derisk.ch>

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


import java.io.File;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Agreement which is sent to user upon first login.
 * User must send CONFIRMAGREEMENT command to confirm the agreement
 * before the server allows him to log in.
 * See LOGIN command implementation for more details.
 * @author hoijui
 */
public class Agreement {

	private static final Log s_log  = LogFactory.getLog(Agreement.class);

	private String content;
	private static final String DEFAULT_FILE_NAME = "agreement.rtf";

	public Agreement() {
		content = null;
	}

	/** Reads agreement from disk (if file is found) */
	public boolean read() {

		boolean success = false;

		String newAgreement = null;
		try {
			newAgreement = Misc.readTextFile(new File(DEFAULT_FILE_NAME));
			if (newAgreement.length() > 2) {
				content = newAgreement;
				success = true;
				s_log.info("Using agreement from file '" + DEFAULT_FILE_NAME + "'.");
			} else {
				s_log.warn("Agreement in file '" + DEFAULT_FILE_NAME + "' is too short.");
			}
		} catch (IOException ex) {
			s_log.warn("Could not find or read from file '" + DEFAULT_FILE_NAME + "'. Using no agreement.");
			s_log.debug("... reason:", ex);
		}

		return success;
	}

	public void sendToClient(Client client) {

		client.beginFastWrite();
		String[] sl = content.split("\n");
		for (int i = 0; i < sl.length; i++) {
			client.sendLine(new StringBuilder("AGREEMENT ").append(sl[i]).toString());
		}
		client.sendLine("AGREEMENTEND");
		client.endFastWrite();
	}

	public boolean isSet() {
		return (content != null);
	}
}
