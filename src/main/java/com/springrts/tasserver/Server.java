package com.springrts.tasserver;


import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
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


	public Server() {

		lanMode = false;
		startTime = System.currentTimeMillis();
		lanAdminUsername = DEFAULT_LAN_ADMIN_USERNAME;
		lanAdminPassword = Misc.encodePassword(DEFAULT_LAN_ADMIN_PASSWORD);
		port = DEFAULT_PORT;
		useUserDB = false;
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
	 * @see System.currentTimeMillis()
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * When the server instance was started.
	 * @see System.currentTimeMillis()
	 * @param startTime the startTime to set
	 */
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	/**
	 * LAN mode administrator user-name.
	 * Only relevant if {@link getLanMode()} is <code>true</code>.
	 * @return the lanAdminUsername
	 */
	public String getLanAdminUsername() {
		return lanAdminUsername;
	}

	/**
	 * LAN mode administrator user-name.
	 * Only relevant if {@link getLanMode()} is <code>true</code>.
	 * @param lanAdminUsername the lanAdminUsername to set
	 */
	public void setLanAdminUsername(String lanAdminUsername) {
		this.lanAdminUsername = lanAdminUsername;
	}

	/**
	 * LAN mode administrator password.
	 * Only relevant if {@link getLanMode()} is <code>true</code>.
	 * @return the lanAdminPassword
	 */
	public String getLanAdminPassword() {
		return lanAdminPassword;
	}

	/**
	 * LAN mode administrator password.
	 * Only relevant if {@link getLanMode()} is <code>true</code>.
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
}
