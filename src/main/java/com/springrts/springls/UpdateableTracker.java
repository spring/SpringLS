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

/*
 * Copied from an example from the Apache Software Foundation (ASF).
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.springrts.springls;


import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Extends the <tt>ServiceTracker</tt> to create a tracker for
 * <tt>Updateable</tt> services. The tracker is responsible for
 * listening for the arrival/departure of <tt>Updateable</tt>
 * services and informing the application about the availability
 * of them.
 */
public class UpdateableTracker extends ServiceTracker {

	/**
	 * Constructs a tracker that uses the specified bundle context to
	 * track services and notifies the specified application object about
	 * changes.
	 * @param context The bundle context to be used by the tracker.
	 */
	public UpdateableTracker(BundleContext context) {
		super(context, Updateable.class.getName(), null);
	}

	private ServerThread getServerThread() {
		return Context.getService(context, Context.class).getServerThread();
	}

	/**
	 * Overrides the <tt>ServiceTracker</tt> functionality to inform
	 * the application object about the added service.
	 * @param ref The service reference of the added service.
	 * @return The service object to be used by the tracker.
	 */
	@Override
	public Object addingService(ServiceReference ref) {

		Updateable updateable = (Updateable) context.getService(ref);
		getServerThread().addUpdateable(updateable);
		return updateable;
	}

	/**
	 * Overrides the <tt>ServiceTracker</tt> functionality to inform
	 * the application object about the removed service.
	 * @param ref The service reference of the removed service.
	 * @param svc The service object of the removed service.
	 */
	@Override
	public void removedService(ServiceReference ref, Object svc) {

		Updateable updateable = (Updateable) svc;
		getServerThread().removeUpdateable(updateable);
	}
}
