/*
 * Created on 2005.6.17
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 * ---- NOTES ----
 * - Each account is uniquely identified by its username (I also used int ID in previous versions,
 *   but since all usernames must be unique, we don't need another unique identifier).
 */

/**
 * @author Betalord
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

public class Account {

	/*
	 * access bits (31 effective bits, last one is a sign and we don't want to use it):
	 * * bits 0 - 3 (4 bits): access level
	 *     0 - none (should not be used for logged-in clients)
	 *     1 - normal (limited)
	 *     2 - privileged
	 *     3 - admin 
	 * * bits 3 - 7 (4 bits): reserved for future use.
	 * * bits 8 - 28 (20 bits): in-game time (how many minutes did client spent in-game).
	 * * bit 29: agreement bit. It tells us whether user has already
	 *     read the "terms of use" and agreed to it. If not, we should
	 *     first send him the agreement and wait until he confirms it (before
	 *     allowing him to log on the server).    
	 * * bit 30: reserved for future use
	 * * bit 31: unused (integer sign)
	 * 
	 */
	
	
	/*
	 * current rank categories:
	 * < 5h = newbie
	 * 5h - 15h = beginner
	 * 15h - 30h = avarage player
	 * 30h - 100h = experienced player
	 * > 100h = highly experienced player
	 * 
	 * */
	private static int rank1Limit = 60*5; // in minutes
	private static int rank2Limit = 60*15; // in minutes
	private static int rank3Limit = 60*30; // in minutes
	private static int rank4Limit = 60*100; // in minutes
	
	public static int NIL_ACCESS = 0; // for clients that haven't logged in yet
	public static int NORMAL_ACCESS = 1;
	public static int PRIVILEGED_ACCESS = 2;
	public static int ADMIN_ACCESS = 3;
	
	public String user;
	public String pass;
	public int access; // access type. Bit 31 must be 0 (due to int being signed number - we don't want to use any binary complement conversions).
	public long lastLogin; // time (System.currentTimeMillis()) of the last login
	public String lastIP; // the most recent IP used to log into this account
	public long registrationDate; // date when user registered this account. In miliseconds (refers to System.currentTimeMillis()). 0 means registration date is unknown (clients who registered in some early version when this field was not yet implemented).
	public MapGradeList mapGrades; // list of map grades
	
	public Account(String user, String pass, int access, long lastLogin, String lastIP, long registrationDate, MapGradeList mapGrades)
	{
		this.user = user;
		this.pass = pass;
		this.access = access;
		this.lastLogin = lastLogin;
		this.lastIP = lastIP;
		this.registrationDate = registrationDate;
		this.mapGrades = mapGrades;
	}
	
	public Account(Account acc) {
		this.user = new String(acc.user);
		this.pass = new String(acc.pass);
		this.access = acc.access;
		this.lastLogin = acc.lastLogin;
		this.lastIP = acc.lastIP;
		this.registrationDate = acc.registrationDate;
		this.mapGrades = MapGradeList.createFromString(acc.mapGrades.toString());
	}
	
	public String toString() {
		return user + " " + pass + " " + Integer.toString(access, 2) + " " + lastLogin + " " + lastIP + " " + registrationDate + " " + mapGrades.toString();
	}
	
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Account)) return false;
		return this.user.equals(((Account)o).user);
	}	
	
	public int accessLevel() {
		return access & 0x7;
	}

	/* returns in-game time in minutes */
	public int getInGameTime() {
		return (access & 0x7FFFF8) >> 3;
	}
	
	public void setInGameTime(int mins) {
		access = (access & 0xFF800007) | (mins << 3);
	}
	
	public boolean getAgreement() {
		return ((access & 0x800000) >> 23) == 1;
	}

	/* must be 1 (true) or 0 (false) */
	public void setAgreement(boolean agreed) {
		int agr = agreed ? 1 : 0;
		
		access = (access & 0xFF7FFFFF) | (agr << 23);
	} 
	
	public int getRank() {
		if (getInGameTime() >= rank4Limit) return 4;
		else if (getInGameTime() > rank3Limit) return 3;
		else if (getInGameTime() > rank2Limit) return 2;
		else if (getInGameTime() > rank1Limit) return 1;
		else return 0;
	}
	
	/* adds mins minutes to client's in-game time (this time is used to calculate 
	 * player's rank). Returns true if player's rank was changed, false otherwise. */
	public boolean addMinsToInGameTime(int mins) {
		int tmp = getRank();
		setInGameTime(getInGameTime() + mins);
		return tmp != getRank();
	}
	
}
