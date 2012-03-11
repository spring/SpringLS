/*
	Copyright (c) 2010 Robin Vobruba <hoijui.quaero@gmail.com>

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.springrts.springls;


import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains settings specific for one version of the engine.
 * @author hoijui
 */
public class Server {

	/**
	 * When the server instance was started.
	 * @see java.lang.System#currentTimeMillis()
	 */
	private long startTime;

	private CharsetDecoder asciiDecoder;
 	private CharsetEncoder asciiEncoder;

	private boolean loginEnabled;
	/**
	 * After this time in milli-seconds of inactivity, a client is getting
	 * killed.
	 */
	private int timeoutLength;

	/**
	 * The address to redirect clients to, or <code>null</code> if redirection
	 * is disabled.
	 */
	private InetAddress redirectAddress;

	/**
	 * The list of compatibility flags (see command LOGIN) supported
	 * by this server.
	 */
	private Set<String> supportedCompFlags;

	public Server() {

		startTime = System.currentTimeMillis();
		loginEnabled = true;
		timeoutLength = 50000;
		redirectAddress = null;
		supportedCompFlags = new HashSet<String>();
	}

	public static String getApplicationName() {
		return "SpringLS";
	}

	/**
	 * Initializes the ASCII-Decoder and ASCII-Encoder.
	 */
	public boolean setCharset(String newCharset) {

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

	/**
	 * Any chat messages (channel or private chat messages) longer than this are
	 * considered flooding.
	 * Used with basic anti-flood protection.
	 * Used with the following commands:
	 * SAY, SAYEX, SAYPRIVATE, SAYBATTLE, SAYBATTLEEX
	 * @return the maximum allowed length of a chat message in characters
	 */
	public int getMaxChatMessageLength() {
		return 1024;
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
	 * True if clients get redirected.
	 * @return if clients get redirected
	 */
	public boolean isRedirectActive() {
		return (redirectAddress != null);
	}

	/**
	 * Disables redirecting of clients.
	 */
	public void disableRedirect() {
		redirectAddress = null;
	}

	/**
	 * The address to redirect clients to, or <code>null</code> if redirection
	 * is disabled.
	 * @return the redirectAddress
	 */
	public InetAddress getRedirectAddress() {
		return redirectAddress;
	}

	/**
	 * The address to redirect clients to, or <code>null</code> if redirection
	 * is disabled.
	 * @param redirectAddress the redirectAddress to set
	 */
	public void setRedirectAddress(InetAddress redirectAddress) {
		this.redirectAddress = redirectAddress;
	}

	/**
	 * The list of compatibility flags (see command LOGIN) supported
	 * by this server.
	 * @return the server supported compatibility flags
	 */
	public Set<String> getSupportedCompFlags() {
		return supportedCompFlags;
	}
}
