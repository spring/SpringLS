/*
 * Created on 2005.8.24
 */

package com.springrts.tasserver;


/**
 * This is used for "native" Skirmish AIs and also for Lua AIs,
 * while the "native" ones can be of any language, as of spring 0.79,
 * for example C, C++ or Java.
 * TODO: actualize to fit spring (versions > 0.79) -> aiDll field was replaced
 * @author Betalord
 */
public class Bot {

	public String name;
	public String ownerName;
	private String shortName;
	private String version;
	/**
	 * See MYBATTLESTATUS command for actual values of battleStatus.
	 * Note: some bits of battle status are not used with Bot
	 */
	public int battleStatus;
	public int teamColor;

	public Bot(String name, String ownerName, String specifier, int battleStatus, int teamColor) {

		this.name = new String(name);
		this.ownerName = new String(ownerName);
		String[] specifierParts = specifier.split("\\|");
		this.shortName = specifierParts[0];
		this.version = ((specifierParts.length > 1) ? specifierParts[1] : "");
		this.battleStatus = battleStatus;
		this.teamColor = teamColor;
	}

	public String getSpecifier() {
		return (shortName + "|" + version);
	}
}
