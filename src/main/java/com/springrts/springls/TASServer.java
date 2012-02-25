/*
	Copyright (c) 2005 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.springls;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.commons.configuration.Configuration;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Betalord
 * @author hoijui
 */
public final class TASServer {

	private static final Logger LOG = LoggerFactory.getLogger(TASServer.class);

	private TASServer() {}

	public static void startServerInstance(Configuration configuration) {

		Framework framework = createFramework();
		if (framework == null) {
			return;
		}
		try {
			framework.start();
		} catch (BundleException ex) {
			LOG.error("Failed to start the OSGi framework", ex);
			return;
		}

		framework.getBundleContext().registerService(Configuration.class.getName(), configuration, null);

		Context context = new Context();
		context.setFramework(framework);
		context.init();
		framework.getBundleContext().registerService(Context.class.getName(), context, null);

		new com.springrts.springls.floodprotection.Activator().start(context.getFramework().getBundleContext());

		new com.springrts.springls.accounts.Activator().start(context.getFramework().getBundleContext());

		new com.springrts.springls.bans.Activator().start(context.getFramework().getBundleContext());

		new com.springrts.springls.agreement.Activator().start(context.getFramework().getBundleContext());

		new com.springrts.springls.motd.Activator().start(context.getFramework().getBundleContext());

		context.push();

		// TODO needs adjusting due to new LAN mode accounts service
		if (configuration.getBoolean(ServerConfiguration.LAN_MODE)) {
			LOG.info("LAN mode enabled");
		}

		context.getServer().setStartTime(System.currentTimeMillis());

		new com.springrts.springls.updateproperties.Activator().start(context.getFramework().getBundleContext());

		new com.springrts.springls.ip2country.Activator().start(context.getFramework().getBundleContext());

		new com.springrts.springls.nat.Activator().start(context.getFramework().getBundleContext());

		new com.springrts.springls.statistics.Activator().start(context.getFramework().getBundleContext());

		// start server:
		if (!context.getServerThread().startServer()) {
			context.getServerThread().closeServerAndExit();
		}

		context.getCommandProcessors().init();

		context.getServerThread().run();
	}

	private static Framework createFramework() {

		Framework framework = null;

		Iterator<FrameworkFactory> frameworkFactoryIterator
				= ServiceLoader.load(FrameworkFactory.class).iterator();
		if (frameworkFactoryIterator.hasNext()) {
			FrameworkFactory frameworkFactory = frameworkFactoryIterator.next();
			framework = frameworkFactory.newFramework(createFrameworkConfig());
		} else {
			LOG.error("No OSGi framework implementation was found.");
		}

		return framework;
	}

	private static Map<String, String> createFrameworkConfig() {

		Map<String, String> config = null;

		config = new HashMap<String, String>();
		// TODO add some params to config ...

		return config;
	}
}
