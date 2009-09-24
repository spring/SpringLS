/*
 * Created on 2005.8.24
 */

package com.springrts.tasserver;


/**
 * @author Betalord
 */
public class Bot {

	public String name;
	public String ownerName;
	public String AIDll;
	public int battleStatus; // see MYBATTLESTATUS command for actual values of battleStatus (Note: some bits of battle status are not used with Bot)
	public int teamColor;

	public Bot(String name, String ownerName, String AIDll, int battleStatus, int teamColor) {
		this.name = new String(name);
		this.ownerName = new String(ownerName);
		this.AIDll = new String(AIDll);
		this.battleStatus = battleStatus;
		this.teamColor = teamColor;
	}
}
