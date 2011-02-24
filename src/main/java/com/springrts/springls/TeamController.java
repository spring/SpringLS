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


import java.awt.Color;

/**
 * @author hoijui
 */
public class TeamController {

	private boolean ready;
	private int team;
	private int allyTeam;
	private boolean spectator;
	private int handicap;
	/** 0 = unknown, 1 = synced, 2 = unsynced */
	private int sync;
	private int side;
	/**
	 * See the 'MYBATTLESTATUS' command for valid values.
	 */
	private Color teamColor;


	public TeamController() {

		setDefaultBattleStatus();
		teamColor = Color.BLACK;
	}

	public TeamController(int battleStatus, Color teamColor) {

		setBattleStatus(battleStatus);
		this.teamColor = teamColor;
	}


	/**
	 * See the 'MYBATTLESTATUS' command for valid values
	 * @return the battleStatus
	 */
	public int getBattleStatus() {

		int battleStatus = 0;

		battleStatus += (isReady() ? 1 : 0)     << 1;
		battleStatus +=  getTeam()              << 2;
		battleStatus +=  getAllyTeam()          << 6;
		battleStatus += (isSpectator() ? 0 : 1) << 10;
		battleStatus +=  getHandicap()          << 11;
		battleStatus +=  getSync()              << 22;
		battleStatus +=  getSide()              << 24;

		return battleStatus;
	}

	/**
	 * See the 'MYBATTLESTATUS' command for valid values
	 * @param battleStatus the battleStatus to set
	 */
	public void setBattleStatus(int battleStatus) {

		setReady(((battleStatus     & 0x2)       >> 1) == 1);
		setTeam((battleStatus       & 0x3C)      >> 2);
		setAllyTeam((battleStatus   & 0x3C0)     >> 6);
		setSpectator(((battleStatus & 0x400)     >> 10) == 0);
		setHandicap((battleStatus   & 0x3F800)   >> 11);
		setSync((battleStatus       & 0xC00000)  >> 22);
		setSide((battleStatus       & 0xF000000) >> 24);
	}

	public void setDefaultBattleStatus() {

		ready = false;
		team = 0;
		allyTeam = 0;
		spectator = true;
		handicap = 0;
		sync = 0;
		side = 0;
	}

	/**
	 * See the 'MYBATTLESTATUS' command for valid values.
	 * @return the teamColor
	 */
	public Color getTeamColor() {
		return teamColor;
	}

	/**
	 * See the 'MYBATTLESTATUS' command for valid values.
	 * @param teamColor the teamColor to set
	 */
	public void setTeamColor(Color teamColor) {
		this.teamColor = teamColor;
	}

	public boolean isReady() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	public int getTeam() {
		return team;
	}

	public void setTeam(int team) {
		this.team = team;
	}

	public int getAllyTeam() {
		return allyTeam;
	}

	public void setAllyTeam(int allyTeam) {
		this.allyTeam = allyTeam;
	}

	/**
	 * Also called mode.
	 */
	public boolean isSpectator() {
		return spectator;
	}

	/**
	 * Also called mode.
	 */
	public void setSpectator(boolean spectator) {
		this.spectator = spectator;
	}

	public int getHandicap() {
		return handicap;
	}

	public void setHandicap(int handicap) {
		this.handicap = handicap;
	}

	public int getSync() {
		return sync;
	}

	public void setSync(int sync) {
		this.sync = sync;
	}

	public int getSide() {
		return side;
	}

	public void setSide(int side) {
		this.side = side;
	}
}
