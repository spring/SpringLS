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

package com.springrts.springls.nat;


import com.springrts.springls.Context;
import com.springrts.springls.Updateable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This class implements a simple bundle activator for the
 * <tt>NatHelpServer</tt>.
 */
public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) {

		Context springLsContext = Context.getService(context, Context.class);

		NatHelpServer natHelpServer = new NatHelpServer();

		natHelpServer.receiveContext(springLsContext);
		natHelpServer.startServer();

		springLsContext.addContextReceiver(natHelpServer);
		springLsContext.addLiveStateListener(natHelpServer);

		context.registerService(new String[] {
				NatHelpServer.class.getName(),
				Updateable.class.getName()
				},
				natHelpServer, null);
	}

	@Override
	public void stop(BundleContext context) {
	}
}
