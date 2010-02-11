/*
 * Created on 11. June 2009
 */

package com.springrts.tasserver;


/**
 *
 * @author Satirik
 */
public class BattleStatus {

	private int teamNo;
	private int allyNo;
	private boolean ready;
	private int mode;
	private int handicap;
	private int side;
	
	public boolean Update(String s) {

		int oldTeamNo = getTeamNo();
		int oldAllyNo = getAllyNo();
		boolean oldReady = isReady();
		int oldMode = getMode();
		int oldHandicap = getHandicap();
		int oldSide = getSide();
		
		String[] items = s.split("\t");
		
		try {
			setTeamNo((int) Integer.valueOf(items[0]));
			assert(getTeamNo()>=0);
			assert(getTeamNo()<=200);
			setAllyNo((int) Integer.valueOf(items[1]));
			assert(getAllyNo()>=0);
			assert(getAllyNo()<=200);
			setReady(Integer.valueOf(items[2]) != 0);
			setMode((int) Integer.valueOf(items[3]));
			setHandicap((int) Integer.valueOf(items[4]));
			setSide((int) Integer.valueOf(items[5]));
			assert(getSide()>=0);
			return true;
		} catch (Exception e) {
			setTeamNo(oldTeamNo);
			setAllyNo(oldAllyNo);
			setReady(oldReady);
			setMode(oldMode);
			setHandicap(oldHandicap);
			setSide(oldSide);
			return false;
		}
	}
	
	public boolean isSpectator() {
		return (getMode() == 0);
	}

	/**
	 * @return the teamNo
	 */
	public int getTeamNo() {
		return teamNo;
	}

	/**
	 * @param teamNo the teamNo to set
	 */
	public void setTeamNo(int teamNo) {
		this.teamNo = teamNo;
	}

	/**
	 * @return the allyNo
	 */
	public int getAllyNo() {
		return allyNo;
	}

	/**
	 * @param allyNo the allyNo to set
	 */
	public void setAllyNo(int allyNo) {
		this.allyNo = allyNo;
	}

	/**
	 * @return the ready
	 */
	public boolean isReady() {
		return ready;
	}

	/**
	 * @param ready the ready to set
	 */
	public void setReady(boolean ready) {
		this.ready = ready;
	}

	/**
	 * @return the mode
	 */
	public int getMode() {
		return mode;
	}

	/**
	 * @param mode the mode to set
	 */
	public void setMode(int mode) {
		this.mode = mode;
	}

	/**
	 * @return the handicap
	 */
	public int getHandicap() {
		return handicap;
	}

	/**
	 * @param handicap the handicap to set
	 */
	public void setHandicap(int handicap) {
		this.handicap = handicap;
	}

	/**
	 * @return the side
	 */
	public int getSide() {
		return side;
	}

	/**
	 * @param side the side to set
	 */
	public void setSide(int side) {
		this.side = side;
	}

	@Override
	public String toString() {
		return new StringBuilder(getTeamNo())
				.append("\t").append(getAllyNo())
				.append("\t").append(isReady() ? 1 : 0)
				.append("\t").append(getMode())
				.append("\t").append(getHandicap())
				.append("\t").append(getSide()).toString();
	}
}
