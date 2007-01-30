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
	private Vector/*String*/ operators = new Vector();
	private Vector/*String*/ clients = new Vector();
	public Vector/*int*/ lineCount = new Vector(); // here we store number of lines that a user sent to the channel during last x seconds, for each user
	public Vector/*String*/ lastLine = new Vector(); // here we store last line of text for each client. If client will repeat same line consecutively, we will increase the weight on spam counter more heavily than otherwise (which will result in user getting sanctioned faster) 
	public boolean antispam; // if true, users will be automatically muted if spamming is detected from them
	
	public Channel(String name) {
		this.name = name;
		logFileName = "#" + name + ".log";
	}
	
	public boolean isFounder(String name) {
		return name.equals(founder);
	}
	
	public boolean isOperator(String name) {
		return (operators.indexOf(name) != -1);
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
	
	public void renameFounder(String newFounder) {
		founder = newFounder;
	}
	
	public boolean renameOperator(String oldOp, String newOp) {
		int index = operators.indexOf(oldOp);
		if (index == -1) return false; // operator does not exist!
		operators.set(index, newOp);
		return true;
	}
	
	public Vector getOperatorList() {
		return operators;
	}
	
	public void sendMessage(String msg) {
		ChanServ.sendLine("SAY " + name + " " + msg);
	}
	
	public int clientCount() {
		return clients.size();
	}
	
	public void clearClients() {
		clients.clear();
		lineCount.clear();
		lastLine.clear();
	}
	
	public String getClient(int index) {
		try{
			return (String)clients.get(index);
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}
	
	public void addClient(String client) {
		clients.add(client);
		int tmp = 0;
		lineCount.add(tmp);
		lastLine.add(new String());
	}
	
	public void removeClient(String client) {
		int i = clients.indexOf(client);
		if (i == -1) return ; // should not happen!
		clients.remove(i);
		lineCount.remove(i);
		lastLine.remove(i);
	}
	
	public boolean clientExists(String client) {
		return clients.indexOf(client) != -1;
	}
	
	// will increase line count for specified client (used with anti-spam protection)
	public void incClientLine(String client, String line) {
		int i = clients.indexOf(client);
		if (i == -1) return ; // should not happen!
		// punish extra for repeated line (+2, for new line only +1):
		lineCount.set(i, (Integer)lineCount.get(i) + (((String)lastLine.get(i)).equals(line) ? 2 : 1));
		lastLine.set(i, line);
	}
	
}
