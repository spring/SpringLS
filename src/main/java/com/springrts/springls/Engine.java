/*
	Copyright (c) 2010 Robin Vobruba <hoijui.quaero@gmail.com>

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

/**
 * Contains settings specific for one version of the engine.
 * @author hoijui
 */
public class Engine {

	/**
	 * This is sent via welcome message to every new client
	 * which connects to the server.
	 */
	private final String version;

	/**
	 * Max number of teams per game supported by the engine.
	 * Should be equal to maxTeams in springs GlobalConstants.h.
	 */
	private final int maxTeams;

	/**
	 * Max number of ally teams per game supported by the engine.
	 * Should be equal to maxTeams in springs GlobalConstants.h.
	 */
	private final int maxAllyTeams;


	public Engine(String version, int maxTeams, int maxAllyTeams) {

		this.version = version;
		this.maxTeams = maxTeams;
		this.maxAllyTeams = maxAllyTeams;
	}
	public Engine(String version, int maxTeams) {
		this(version, maxTeams, maxTeams);
	}
	public Engine(String version) {
		this(version, 255);
	}
	public Engine() {
		this("*");
	}


	/**
	 * This is sent via welcome message to every new client
	 * which connects to the server.
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Max number of teams per game supported by the engine.
	 * Should be equal to maxTeams in springs GlobalConstants.h.
	 * @return the maxTeams
	 */
	public int getMaxTeams() {
		return maxTeams;
	}

	/**
	 * Max number of ally teams per game supported by the engine.
	 * Should be equal to maxTeams in springs GlobalConstants.h.
	 * @return the maxAllyTeams
	 */
	public int getMaxAllyTeams() {
		return maxAllyTeams;
	}
}
