/*
 * Created on 2006.9.30
 *
 * Correct use of ServerNotification is to first create the object and then
 * add message lines to it. Finally, add it to server notifications by
 * calling ServerNotifications.addNotification(). 
 */

/**
 * @author Betalord
 *
 */

public class ServerNotification {

	long time = 0; // referes to System.currentTimeMillis() - miliseconds passed since 1st Jan 1970
	String author;
	String message;
	
	public ServerNotification(String author) {
		time = System.currentTimeMillis();
		this.author = new String(author);
		message = "";
	}

	public ServerNotification(String author, String firstLine) {
		this(author);
		this.addLine(firstLine);
	}
	
	public void addLine(String line) {
		message += (message.equals("") ? line : "\r\n" + line);
	}

}
