/*
 * Created on 6.3.2006
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

public class Channel {
	public String name;
	public String topic = ""; // "" means topic is disabled
	public String logFileName;
	
	public Channel(String name) {
		this.name = name;
		logFileName = "#" + name + ".log";
		Misc.outputLog(logFileName, "");
		Misc.outputLog(logFileName, "Log started on " + Misc.easyDateFormat("dd/MM/yy"));
	}
}
