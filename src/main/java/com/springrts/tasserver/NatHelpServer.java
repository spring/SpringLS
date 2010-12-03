/*
 * Created on 14.1.2006
 */

package com.springrts.tasserver;


import java.io.IOException;
import java.io.InterruptedIOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

/**
 * This is a simple UDP server that helps detecting source ports with some
 * NAT traversal techniques (e.g. "hole punching").
 *
 * @author Betalord
 */
public class NatHelpServer implements Runnable, LiveStateListener {

	private static final Log s_log  = LogFactory.getLog(NatHelpServer.class);
	private static final int RECEIVE_BUFFER_SIZE = 256;

	/**
	 * Default UDP port used with some NAT traversal technique.
	 */
	public static final int DEFAULT_PORT = 8201;
	/**
	 * Server UDP port used with some NAT traversal technique.
	 * If this port is not forwarded, the hole punching technique will not work.
	 */
	private int port;
	/** Has to be thread-safe */
	private List<DatagramPacket> msgList;
	private DatagramSocket socket;
	private Thread myThread;


	public NatHelpServer() {

		this.port = DEFAULT_PORT;
		this.msgList = Collections.synchronizedList(new LinkedList<DatagramPacket>());
		this.socket = null;
		this.myThread = null;
	}


	@Override
	public void starting() {}
	@Override
	public void started() {}

	@Override
	public void stopping() {

		if (isRunning()) {
			stopServer();
		}
	}
	@Override
	public void stopped() {}

	/**
	 * Nat traversal port the server runs on.
	 * @return the natTraversalPort
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Nat traversal port the server runs on.
	 * @param port the NAT traversal port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public void run() {

		try {
			socket = new DatagramSocket(port);
		} catch (Exception e) {
			s_log.warn("Unable to start UDP server on port " + port + ". Ignoring ...", e);
			return;
		}

		s_log.info(new StringBuilder("Listening for connections on UDP port ")
				.append(port)
				.append(" ...").toString());

		byte[] buffer = new byte[RECEIVE_BUFFER_SIZE];
		while (true) {
			try {
				if (myThread.isInterrupted()) {
					break;
				}

				// receive packet
				DatagramPacket packet = new DatagramPacket(buffer, RECEIVE_BUFFER_SIZE);
				socket.receive(packet);
				msgList.add(packet);
			} catch (InterruptedIOException e) {
				break;
			} catch (IOException e) {
				if (e.getMessage().equalsIgnoreCase("socket closed")) {
					// server stopped gracefully!
				} else {
					s_log.error("Error in UDP server", e);
				}
			}
		}

		socket.close();
		s_log.info("UDP NAT server closed.");
	}

	public void startServer() {

		if (myThread == null) {
			myThread = new Thread(this);
			myThread.start();
		} else {
			s_log.warn("NAT help server is already running");
		}
	}

	public boolean isRunning() {
		return ((myThread != null) && myThread.isAlive());
	}

	public void stopServer() {

		if (myThread == null) {
			s_log.warn("NAT help server is not running");
			return;
		}

		if (myThread.isInterrupted()) {
			// we are already in the process of shutting down
			return;
		}
		myThread.interrupt();
		try {
			// give it 1 second to shut down gracefully
			myThread.join(1000);
			myThread = null;
		} catch (InterruptedException ex) {
			s_log.error("NAT help server interrupted while shutting down", ex);
		}
		socket.close();
	}

	/**
	 * Returns the oldest package, and removes it from the internal storage.
	 * This method will be called by an other thread then the NAT-server one.
	 * @return the next oldest package, or <code>null</code>, if none is left.
	 */
	public DatagramPacket fetchNextPackage() {

		DatagramPacket packet = null;

		if (msgList.size() > 0) {
			packet = msgList.remove(0);
		}

		return packet;
	}
}
