/*
	Copyright (c) 2011 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.springls.updateproperties;


import com.springrts.springls.Context;
import com.springrts.springls.ContextReceiver;
import com.springrts.springls.ServerConfiguration;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores a list of Spring versions and server responses to them.
 * We use it when a client does not have the latest Spring version or
 * lobby client, and requests an update from us.
 * The properties file should normally contain at least the "default" key,
 * which contains a standard response in case no suitable response is found.
 * Each text field associated with a key contains a full string that will be
 * sent to the client as a response, so it should contain a full server command.
 * @author hoijui
 */
public class UpdateProperties implements ContextReceiver {

	public static final String DEFAULT_FILENAME = "updates.xml";
	private static final String DEFAULT_RESPONSE =
			"SERVERMSGBOX No update available."
			+ " Please download the latest version of the software from"
			+ " the official Spring web site: http://springrts.com";

	private static final Logger LOG
			= LoggerFactory.getLogger(UpdateProperties.class);

	private Context context;

	private Properties updateProperties;


	public UpdateProperties() {

		context = null;
		updateProperties = null;
	}

	@Override
	public void receiveContext(Context context) {

		this.context = context;
	}
	protected Context getContext() {
		return context;
	}

	public boolean read(String fileName) {

		boolean success = false;

		updateProperties = new Properties();

		File updatePropsFile = new File(fileName);
		if (updatePropsFile.exists()) {
			FileInputStream fStream = null;
			try {
				fStream = new FileInputStream(updatePropsFile);
				updateProperties.loadFromXML(fStream);
				success = true;
			} catch (IOException ex) {
				LOG.warn("Could not read from file '" + fileName + "'.", ex);
			} finally {
				if (fStream != null) {
					try {
						fStream.close();
					} catch (IOException ex) {
						LOG.trace("Failed to close file input stream: "
								+ fileName, ex);
					}
				}
			}
		} else {
			Configuration conf = getContext().getService(Configuration.class);
			String engineVersion = conf.getString(ServerConfiguration.ENGINE_VERSION);
			// Not having update properties is only possibly a problem
			// when we try to enforce a certain engine version.
			if (!engineVersion.equals("*")) {
				LOG.warn("Could not find file '{}';"
						+ " not using any update properties.",
						fileName.toString());
			}
		}

		return success;
	}

	/**
	 * Returns a lobby command to be sent back to the client, as a response to
	 * the current engine version the client is using.
	 * @param engineVersion for example "0.82.6.1"
	 * @return
	 *   a full lobby protocol command, to be sent back to the client as is.
	 *   it is guaranteed that <code>null</code> is never returned.
	 */
	public String getResponse(String engineVersion) {

		String response = updateProperties.getProperty(engineVersion);

		if (response == null) {
			// use general response ("default"), if it exists.
			response = updateProperties.getProperty("default");
		}
		if (response == null) {
			// if still no response has been found,
			// use some default response
			response = DEFAULT_RESPONSE;
		}

		return response;
	}
}
