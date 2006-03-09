/*
 * Created on 6.3.2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 * Static channels are those for which we don't want ChanServ to moderate them,
 * only idle there so it logs all chats (for example, #main).
 * 
 */

/**
 * @author Betalord
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

import java.util.*;

public class Channel {
	public String name;
	public String topic = ""; // "" means topic is disabled
	public String logFileName;
	public boolean joined = false; // are we in this channel right now?
	public boolean isStatic = true; // if true, then this channel is a static one and not registered one (we can't register this channel at all)
	public String key = ""; // if "" then no key is set (channel is unlocked)
	public String founder; // username of the founder of this channel. Founder is the "owner" of the channel, he can assign operators etc.
	Vector/*String*/ operators = new Vector();
	
	public Channel(String name) {
		this.name = name;
		logFileName = "#" + name + ".log";
	}
	
	public boolean isOperator(String name) {
		return (!(operators.indexOf(name) == -1));
	}
	
	public boolean addOperator(String name) {
		if (isOperator(name)) return false;
		operators.add(name);
		return true;
	}
	
	public boolean removeOperator(String name) {
		if (!isOperator(name)) return false;
		operators.remove(name);
		return true;
	}
	
	public Vector getOperatorList() {
		return operators;
	}
	
}
