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


import com.springrts.springls.commands.CommandProcessors;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a simple bundle activator for the
 * <tt>UpdateProperties</tt> service.
 */
public class Activator implements BundleActivator {

	private final Logger log = LoggerFactory.getLogger(Activator.class);

	@Override
	public void start(BundleContext context) {

		UpdateProperties updateProperties = new UpdateProperties();
		String updatePropsFile = UpdateProperties.DEFAULT_FILENAME;
		if (updateProperties.read(updatePropsFile)) {
			log.info("\"Update properties\" read from {}", updatePropsFile);
		}

		context.registerService(UpdateProperties.class.getName(),
				updateProperties, null);

		try {
			CommandProcessors.add(context, CommandProcessors.load(
					ReloadUpdatePropertiesCommandProcessor.class));
			CommandProcessors.add(context, CommandProcessors.load(
					RequestUpdateFileCommandProcessor.class));
		} catch (Exception ex) {
			log.error(
					"Failed to install the UpdateProperties command-processors."
					+ " These commands will not be available.", ex);
		}
	}

	@Override
	public void stop(BundleContext context) {
	}
}
