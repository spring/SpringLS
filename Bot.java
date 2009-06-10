/*
 * Created on 2005.8.24
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
 * @author Betalord
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Bot {
	public String name;
	public String ownerName;
	public String AIDll;
	public BattleStatus battleStatus; // see MYBATTLESTATUS command for actual values of battleStatus (Note: some bits of battle status are not used with Bot)
	public int teamColor;
	
	public Bot(String name, String ownerName, String AIDll, int battleStatus, int teamColor) {
		this.name = new String(name);
		this.ownerName = new String(ownerName);
		this.AIDll = new String(AIDll);
		this.battleStatus = new BattleStatus();
		this.teamColor = teamColor;
	}
}
