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


import java.awt.Color;

/**
 * This is used for "native" Skirmish AIs and also for Lua AIs,
 * while the "native" ones can be of any language, as of spring 0.79,
 * for example C, C++ or Java.
 * @author Betalord
 * @author hoijui
 */
public class Bot extends TeamController {

	/**
	 * The human readable name of this Bot.
	 * This is specified by the user adding the bot,
	 * and may be an arbitrary string.
	 * By default it is usually something like "Bot1".
	 * This should be though of as the bots equivalent
	 * to the humans account name, at least for the users.
	 */
	private String name;
	/**
	 * Name of the owner of this bot.
	 * For Lua AIs (synced), this is always the account name of the client
	 * which added the bot. While this is also true for "native"
	 * bots (unsynced), it there also denotes the client running the bot.
	 */
	private String ownerName;
	/**
	 * Short name (machine friendly) of the bots implementation.
	 * Examples: "KAIK", "AAI", "C.R.A.I.G."
	 */
	private String shortName;
	/**
	 * Version (machine friendly) of the bots implementation.
	 * Examples: "0.13", "0.900", "<not-versioned>"
	 * For Lua bots this is always "<not-versioned>".
	 */
	private String version;

	public Bot(String name, String ownerName, String specifier,
			int battleStatus, Color teamColor)
	{
		super(battleStatus, teamColor);

		this.name = name;
		this.ownerName = ownerName;
		String[] specifierParts = specifier.split("\\|");
		this.shortName = specifierParts[0];
		this.version = ((specifierParts.length > 1) ? specifierParts[1] : "");
	}

	public static boolean isValidName(String name) {
		return name.matches("^[A-Za-z0-9_]+$");
	}

	public String getSpecifier() {
		return (shortName + "|" + version);
	}

	/**
	 * The human readable name of this Bot.
	 * This is specified by the user adding the bot,
	 * and may be an arbitrary string.
	 * By default it is usually something like "Bot1".
	 * This should be though of as the bots equivalent
	 * to the humans account name, at least for the users.
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Name of the owner of this bot.
	 * For Lua AIs (synced), this is always the account name of the client
	 * which added the bot. While this is also true for "native"
	 * bots (unsynced), it there also denotes the client running the bot.
	 * @return the ownerName
	 */
	public String getOwnerName() {
		return ownerName;
	}
}
