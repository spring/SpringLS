/*
 * Created on 8.3.2006
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
public class Client {
	public String name;
	private int status = 0;

	public Client(String name) {
		this.name = name;
	}
	
	public void setStatus(int status) {
		this.status = status;
	}
	
	public boolean isModerator() {
		return (status & 0x20) >> 5 == 1;
	}

}
