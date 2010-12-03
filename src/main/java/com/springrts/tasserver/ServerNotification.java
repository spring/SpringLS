/*
 * Created on 2006.9.30
 */

package com.springrts.tasserver;


/**
 * Correct use of ServerNotification is to first create the object and then
 * add message lines to it. Finally, add it to server notifications by
 * calling ServerNotifications.addNotification().
 *
 * @author Betalord
 */
public class ServerNotification {

	/**
	 * Miliseconds passed since 1st Jan 1970
	 * @see System.currentTimeMillis()
	 */
	private long time = 0;
	private String title;
	private String author;
	/**
	 * Either an empty string or starts with a new-line.
	 */
	private StringBuilder message;

	public ServerNotification(String title) {
		this(title, "$TASServer");
	}

	public ServerNotification(String title, String author, String firstLine) {

		this.time    = System.currentTimeMillis();
		this.title   = title;
		this.author  = author;
		this.message = new StringBuilder(firstLine);
	}

	public ServerNotification(String title, String author) {
		this(title, author, "");
	}

	public void addLine(String line) {
		message.append("\r\n").append(line);
	}

	@Override
	public String toString() {

		StringBuilder str = new StringBuilder();

		str.append(getAuthor()).append("\r\n");
		str.append(getTitle()).append("\r\n");
		str.append(getTime()).append("\r\n");
		str.append(getMessage());

		return str.toString();
	}

	/**
	 * Miliseconds passed since 1st Jan 1970
	 * @see java.lang.System#currentTimeMillis()
	 * @return the time
	 */
	public long getTime() {
		return time;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * Either an empty string or starts with a new-line.
	 * @return the message
	 */
	public String getMessage() {
		return message.toString();
	}
}
