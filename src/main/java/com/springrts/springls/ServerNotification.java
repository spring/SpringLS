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

package com.springrts.springls;

import com.springrts.springls.util.Misc;


/**
 * The correct use of ServerNotification is to first create the object and then
 * add message lines to it. Finally, add it to server notifications.
 * @see ServerNotifications#addNotification(ServerNotification)
 *
 * @author Betalord
 * @author hoijui
 */
public class ServerNotification {

	/**
	 * Milliseconds passed since 1st Jan 1970
	 * @see System.currentTimeMillis()
	 */
	private long time = 0;
	private String title;
	private String author;
	/**
	 * This contains either an empty string or it starts with a new-line.
	 */
	private StringBuilder message;

	public ServerNotification(String title) {
		this(title, "$" + Server.getApplicationName());
	}

	public ServerNotification(String title, String author, String firstLine) {

		this.time    = System.currentTimeMillis();
		this.title   = title;
		this.author  = author;
		this.message = new StringBuilder(firstLine);
	}

	public ServerNotification(String title, String author) {
		this(title, author, "");
	}

	public void addLine(String line) {
		message.append(Misc.EOL).append(line);
	}

	/**
	 * This method is thread-safe; or at least it is if not called from multiple
	 * threads with the same Exception object.
	 * It has to be thread-safe, since multiple threads may call it.
	 */
	public void addException(Exception ex) {

		message.append(ex.toString());

		StackTraceElement[] trace = ex.getStackTrace();
		for (int i = 0; i < trace.length; i++) {
			message.append(Misc.EOL).append("\tat ")
					.append(trace[i].toString());
		}
	}

	@Override
	public String toString() {

		StringBuilder str = new StringBuilder();

		str.append(getAuthor()).append(Misc.EOL);
		str.append(getTitle()).append(Misc.EOL);
		str.append(getTime()).append(Misc.EOL);
		str.append(getMessage());

		return str.toString();
	}

	/**
	 * Milliseconds passed since 1st Jan 1970
	 * @see java.lang.System#currentTimeMillis()
	 * @return the time
	 */
	public long getTime() {
		return time;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * Either an empty string or starts with a new-line.
	 * @return the message
	 */
	public String getMessage() {
		return message.toString();
	}
}
