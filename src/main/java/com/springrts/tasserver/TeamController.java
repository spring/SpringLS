/*
	Copyright (c) 2011 Robin Vobruba <robin.vobruba@derisk.ch>

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

package com.springrts.tasserver;


import java.awt.Color;

/**
 * @author hoijui
 */
public class TeamController {

	/**
	 * See the 'MYBATTLESTATUS' command for valid values.
	 */
	private int battleStatus;
	/**
	 * See the 'MYBATTLESTATUS' command for valid values.
	 */
	private Color teamColor;


	public TeamController() {

		battleStatus = 0;
		teamColor = Color.BLACK;
	}

	public TeamController(int battleStatus, Color teamColor) {

		this.battleStatus = battleStatus;
		this.teamColor = teamColor;
	}


	/**
	 * See the 'MYBATTLESTATUS' command for valid values
	 * @return the battleStatus
	 */
	public int getBattleStatus() {
		return battleStatus;
	}

	/**
	 * See the 'MYBATTLESTATUS' command for valid values
	 * @param battleStatus the battleStatus to set
	 */
	public void setBattleStatus(int battleStatus) {
		this.battleStatus = battleStatus;
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
		return ((getBattleStatus() & 0x2) >> 1) == 1;
	}

	public int getTeam() {
		return (getBattleStatus() & 0x3C) >> 2;
	}

	public int getAllyTeam() {
		return (getBattleStatus() & 0x3C0) >> 6;
	}

	/**
	 * Also called mode.
	 */
	public boolean isSpectator() {
		return ((getBattleStatus() & 0x400) >> 10) == 0;
	}

	public int getHandicap() {
		return (getBattleStatus() & 0x3F800) >> 11;
	}

	public int getSync() {
		return (getBattleStatus() & 0xC00000) >> 22;
	}

	public int getSide() {
		return getBattleStatus() & 0xF000000 >> 24;
	}

	public void setReady(boolean ready) {
		battleStatus = (getBattleStatus() & 0xFFFFFFFD) | ((ready ? 1 : 0) << 1);
	}

	public void setTeam(int team) {
		battleStatus = (getBattleStatus() & 0xFFFFFFC3) | (team << 2);
	}

	public void setAllyTeam(int allyTeam) {
		battleStatus = (getBattleStatus() & 0xFFFFFC3F) | (allyTeam << 6);
	}

	/**
	 * Also called mode.
	 */
	public void setSpectator(boolean spec) {
		battleStatus = (getBattleStatus() & 0xFFFFFBFF) | ((spec ? 0 : 1) << 10);
	}

	public void setHandicap(int handicap) {
		battleStatus = (getBattleStatus() & 0xFFFC07FF) | (handicap << 11);
	}

	public void setSync(int sync) {
		battleStatus = (getBattleStatus() & 0xFF3FFFFF) | (sync << 22);
	}

	public void setSide(int side) {
		battleStatus = (getBattleStatus() & 0xF0FFFFFF) | (side << 24);
	}
}
