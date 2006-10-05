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
	String title;
	
	public ServerNotification(String title) {
		this(title, "$TASServer");
	}

	public ServerNotification(String title, String author) {
		time = System.currentTimeMillis();
		this.title = new String(title);
		this.author = new String(author);
		message = "";
	}

	public ServerNotification(String title, String author, String firstLine) {
		this(title, author);
		this.addLine(firstLine);
	}
	
	public void addLine(String line) {
		message += (message.equals("") ? line : "\r\n" + line);
	}

}
