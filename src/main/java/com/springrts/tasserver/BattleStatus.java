
package com.springrts.tasserver;


public class BattleStatus {
	public int TeamNo;
	public int AllyNo;
	public Boolean Ready;
	public int Mode;
	public int Handicap;
	public int Side;
	
	public Boolean Update(String s) {
		int oldTeamNo = TeamNo;
		int oldAllyNo = AllyNo;
		Boolean oldReady = Ready;
		int oldMode = Mode;
		int oldHandicap = Handicap;
		int oldSide = Side;
		
		String[] items = s.split("\t");
		
		try {
			TeamNo = Integer.valueOf(items[0]);
			assert(TeamNo>=0);
			assert(TeamNo<=200);
			AllyNo = Integer.valueOf(items[1]);
			assert(AllyNo>=0);
			assert(AllyNo<=200);
			Ready = Integer.valueOf(items[2]) != 0;
			Mode = Integer.valueOf(items[3]);
			Handicap = Integer.valueOf(items[4]);
			Side = Integer.valueOf(items[5]);
			assert(Side>=0);
			return true;
		} catch (Exception e) {
			TeamNo = oldTeamNo;
			AllyNo = oldAllyNo;
			Ready = oldReady;
			Mode = oldMode;
			Handicap = oldHandicap;
			Side = oldSide;
			return false;
		}
	}
	
	public Boolean isSpectator() {
		return Mode == 0;
	}
	
	public String toString() {
		int rdy;
		if (Ready) {
			rdy = 1;
		} else {
			rdy = 0;
		}
		return TeamNo+"\t"+AllyNo+"\t"+rdy+"\t"+Mode+"\t"+Handicap+"\t"+Side;
	}
}
