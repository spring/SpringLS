/*
 * Created on 2005.8.24
 */

package com.springrts.tasserver;


/**
 * This is used for "native" Skirmish AIs and also for Lua AIs,
 * while the "native" ones can be of any language, as of spring 0.79,
 * for example C, C++ or Java.
 * @author Betalord
 * @author hoijui
 */
public class Bot {

	/**
	 * The human readable name fo this Bot.
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
	/**
	 * See MYBATTLESTATUS command for actual values of battleStatus.
	 * Note: some bits of battle status are not used with Bot
	 */
	private int battleStatus;
	private int teamColor;

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

	/**
	 * The human readable name fo this Bot.
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

	/**
	 * See MYBATTLESTATUS command for actual values of battleStatus.
	 * Note: some bits of battle status are not used with Bot
	 * @return the battleStatus
	 */
	public int getBattleStatus() {
		return battleStatus;
	}

	/**
	 * See MYBATTLESTATUS command for actual values of battleStatus.
	 * Note: some bits of battle status are not used with Bot
	 * @param battleStatus the battleStatus to set
	 */
	public void setBattleStatus(int battleStatus) {
		this.battleStatus = battleStatus;
	}

	/**
	 * @return the teamColor
	 */
	public int getTeamColor() {
		return teamColor;
	}

	/**
	 * @param teamColor the teamColor to set
	 */
	public void setTeamColor(int teamColor) {
		this.teamColor = teamColor;
	}
}
