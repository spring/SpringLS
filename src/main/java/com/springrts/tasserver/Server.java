package com.springrts.tasserver;


import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collection;
import java.util.LinkedList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Contains settings specific for one version of the engine.
 */
public class Server implements ContextReceiver {

	private static final Log s_log  = LogFactory.getLog(Server.class);

	private Context context = null;

	/**
	 * If true, no password authentication is used.
	 */
	private boolean lanMode;

	/**
	 * When the server instance was started.
	 * @see System.currentTimeMillis()
	 */
	private long startTime;

	/**
	 * Default LAN administrator user name.
	 * Can be overwritten with -LANADMIN switch.
	 * Used only when server is running in LAN mode!
	 * @see #DEFAULT_LAN_ADMIN_PASSWORD
	 */
	private static final String DEFAULT_LAN_ADMIN_USERNAME = "admin";
	/**
	 * LAN mode administrator user-name.
	 * Only relevant if {@link getLanMode()} is <code>true</code>.
	 */
	private String lanAdminUsername;

	/**
	 * Default LAN administrator password.
	 * @see #DEFAULT_LAN_ADMIN_USERNAME
	 */
	private static final String DEFAULT_LAN_ADMIN_PASSWORD = "admin";
	/**
	 * LAN mode administrator password.
	 * Only relevant if {@link getLanMode()} is <code>true</code>.
	 */
	private String lanAdminPassword;

	/** Default TCP server port. */
	private static final int DEFAULT_PORT = 8200;
	/**
	 * Main TCP port to run the server on.
	 */
	private int port;

	/**
	 * If true, we will use a DB instead of flat files for user management.
	 */
	private boolean useUserDB;

	private CharsetDecoder asciiDecoder;
 	private CharsetEncoder asciiEncoder;

	private FloodProtection floodProtection;

	private boolean loginEnabled;

	/** in milli-seconds */
	private static final int TIMEOUT_CHECK = 5000;
	/**
	 * Time ({@link java.lang.System#currentTimeMillis()}) when we last checked
	 * for timeouts from clients.
	 */
	private long lastTimeoutCheck;
	/**
	 * After this time in milli-seconds of inactivity, a client is getting
	 * killed.
	 */
	private int timeoutLength;

	public Server() {

		lanMode = false;
		startTime = System.currentTimeMillis();
		lanAdminUsername = DEFAULT_LAN_ADMIN_USERNAME;
		lanAdminPassword = Misc.encodePassword(DEFAULT_LAN_ADMIN_PASSWORD);
		port = DEFAULT_PORT;
		useUserDB = false;
		floodProtection = new FloodProtection();
		loginEnabled = true;
		timeoutLength = 50000;
		lastTimeoutCheck = System.currentTimeMillis();
	}


	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}

	public boolean setCharset(String newCharset)
			throws IllegalCharsetNameException, UnsupportedCharsetException
	{
		CharsetDecoder dec;
		CharsetEncoder enc;

		dec = Charset.forName(newCharset).newDecoder();
		enc = Charset.forName(newCharset).newEncoder();

		asciiDecoder = dec;
		asciiDecoder.replaceWith("?");
		asciiDecoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		asciiDecoder.onMalformedInput(CodingErrorAction.REPLACE);

		asciiEncoder = enc;
		asciiEncoder.replaceWith(new byte[]{(byte) '?'});
		asciiEncoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		asciiEncoder.onMalformedInput(CodingErrorAction.REPLACE);

		return true;
	}

	/**
	 * Returns the servers up-time.
	 * @return the servers up-time in milli-seconds
	 */
	public long getUpTime() {
		return System.currentTimeMillis() - getStartTime();
	}

	public CharsetDecoder getAsciiDecoder() {
		return asciiDecoder;
	}

	public CharsetEncoder getAsciiEncoder() {
		return asciiEncoder;
	}

	public void closeServerAndExit() {

		s_log.info("Server stopping ...");

		context.stopping();

		// add server notification:
		ServerNotification sn = new ServerNotification("Server stopped");
		sn.addLine("Server has just been stopped. See server log for more info.");
		context.getServerNotifications().addNotification(sn);

		context.stopped();
		s_log.info("Server stopped.");
		System.exit(0);
	}

	/**
	 * If true, no password authentication is used.
	 * @return the lanMode
	 */
	public boolean isLanMode() {
		return lanMode;
	}

	/**
	 * If true, no password authentication is used.
	 * @param lanMode the lanMode to set
	 */
	public void setLanMode(boolean lanMode) {
		this.lanMode = lanMode;
	}

	/**
	 * When the server instance was started.
	 * @see java.lang.System#currentTimeMillis()
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * When the server instance was started.
	 * @see java.lang.System#currentTimeMillis()
	 * @param startTime the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	/**
	 * LAN mode administrator user-name.
	 * Only relevant if {@link #isLanMode()} is <code>true</code>.
	 * @return the lanAdminUsername
	 */
	public String getLanAdminUsername() {
		return lanAdminUsername;
	}

	/**
	 * LAN mode administrator user-name.
	 * Only relevant if {@link #isLanMode()} is <code>true</code>.
	 * @param lanAdminUsername the lanAdminUsername to set
	 */
	public void setLanAdminUsername(String lanAdminUsername) {
		this.lanAdminUsername = lanAdminUsername;
	}

	/**
	 * LAN mode administrator password.
	 * Only relevant if {@link #isLanMode()} is <code>true</code>.
	 * @return the lanAdminPassword
	 */
	public String getLanAdminPassword() {
		return lanAdminPassword;
	}

	/**
	 * LAN mode administrator password.
	 * Only relevant if {@link #isLanMode()} is <code>true</code>.
	 * @param lanAdminPassword the lanAdminPassword to set
	 */
	public void setLanAdminPassword(String lanAdminPassword) {
		this.lanAdminPassword = lanAdminPassword;
	}

	/**
	 * Main port to run the server on.
	 * @return the serverPort
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Main port to run the server on.
	 * @param port the server port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * If true, we will use a DB instead of flat files for user management.
	 * @return the useUserDB
	 */
	public boolean isUseUserDB() {
		return useUserDB;
	}

	/**
	 * If true, we will use a DB instead of flat files for user management.
	 * @param useUserDB the useUserDB to set
	 */
	public void setUseUserDB(boolean useUserDB) {
		this.useUserDB = useUserDB;
	}

	/**
	 * Returns the flood-protection system.
	 */
	public FloodProtection getFloodProtection() {
		return floodProtection;
	}

	public boolean isLoginEnabled() {
		return loginEnabled;
	}

	public void setLoginEnabled(boolean loginEnabled) {
		this.loginEnabled = loginEnabled;
	}

	/**
	 * After this time in milli-seconds of inactivity, a client is getting
	 * killed.
	 */
	public int getTimeoutLength() {
		return timeoutLength;
	}

	/**
	 * After this time in milli-seconds of inactivity, a client is getting
	 * killed.
	 */
	public void setTimeoutLength(int timeoutLength) {
		this.timeoutLength = timeoutLength;
	}

	/**
	 * Checks if the time-out check period has passed already,
	 * and if so, resets the last-check-time.
	 * @return true if the time-out check period has passed since
	 *         the last successful call to this method
	 */
	public Collection<Client> getTimedOutClients() {

		Collection<Client> timedOutClients = new LinkedList<Client>();

		boolean timeOut = (System.currentTimeMillis() - lastTimeoutCheck > TIMEOUT_CHECK);

		if (timeOut) {
			lastTimeoutCheck = System.currentTimeMillis();
			long now = System.currentTimeMillis();
			for (int i = 0; i < context.getClients().getClientsSize(); i++) {
				Client client = context.getClients().getClient(i);
				if (now - client.getTimeOfLastReceive() > timeoutLength) {
					timedOutClients.add(client);
				}
			}
		}

		return timedOutClients;
	}
}
