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

package com.springrts.springls;


import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

/**
 * Contains all keys and default values of externally configurable properties of
 * the server.
 * @author hoijui
 */
public final class ServerConfiguration {

	/**
	 * Main TCP port to run the server on.
	 */
	public static final String PORT = "port";
	/**
	 * The server UDP port used with some NAT traversal technique.
	 * If this port is not forwarded, the hole punching technique will not work.
	 */
	public static final String NAT_PORT = "nat.port";
	/**
	 * If this is <code>true</code>, no password authentication is used.
	 */
	public static final String LAN_MODE = "lan.mode";
	/**
	 * Whether statistics are recorded or not.
	 */
	public static final String STATISTICS_STORE = "statistics.store";
	/**
	 * LAN mode administrator user-name.
	 * This is only relevant if {@link #LAN_MODE} is <code>true</code>.
	 */
	public static final String LAN_ADMIN_USERNAME = "lan.admin.username";
	/**
	 * LAN mode administrator password.
	 * This is only relevant if {@link #LAN_MODE} is <code>true</code>.
	 */
	public static final String LAN_ADMIN_PASSWORD = "lan.admin.password";
	/**
	 * Channels whichs name matches this regex will be logged.
	 */
	public static final String CHANNELS_LOG_REGEX = "channels.log.regex";
	/**
	 * The property name for the engine version that clients should use to
	 * host games on this server.
	 * This is sent via the welcome message whenever a client connects to the
	 * server.
	 */
	public static final String ENGINE_VERSION = "engine.version";
	/**
	 * The property name for the lobby protocol version used by this lobby
	 * server.
	 * This is sent via the welcome message whenever a client connects to the
	 * server.
	 * It is stored in hte configuration, because it may change depending on
	 * which OSGi modules are installed.
	 */
	public static final String LOBBY_PROTOCOL_VERSION = "lobby.protocol.version";
	/**
	 * If this is <code>true</code>, we will use a DB instead of flat files for
	 * user management.
	 * This is only relevant if {@link #LAN_MODE} is <code>false</code>.
	 */
	public static final String USE_DATABASE = "database";

	private static final Configuration DEFAULTS = createDefaults();

	private ServerConfiguration() {
	}

	public static Configuration getDefaults() {
		return DEFAULTS;
	}

	private static Configuration createDefaults() {

		Configuration configuration = new BaseConfiguration();

		configuration.setProperty(PORT, 8200);
		configuration.setProperty(NAT_PORT, 8201);
		configuration.setProperty(LAN_MODE, false);
		configuration.setProperty(STATISTICS_STORE, false);
		configuration.setProperty(LAN_ADMIN_USERNAME, "admin");
		configuration.setProperty(LAN_ADMIN_PASSWORD, "admin");
		configuration.setProperty(CHANNELS_LOG_REGEX, "^%%%%%%%$"); // match no channel
		configuration.setProperty(ENGINE_VERSION, "*"); // all versions
		configuration.setProperty(LOBBY_PROTOCOL_VERSION, "0.35");
		configuration.setProperty(USE_DATABASE, false);

		return configuration;
	}
}
