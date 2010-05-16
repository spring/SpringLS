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
public class NATHelpServer extends Thread {

	private static final Log s_log  = LogFactory.getLog(NATHelpServer.class);

	private static final int buffer_size = 256;
	private DatagramSocket socket = null;
	private int port;
	/** Has to be thread-safe */
	static public List<DatagramPacket> msgList = Collections.synchronizedList(new LinkedList<DatagramPacket>());

	public NATHelpServer(int port) {
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

		s_log.info("UDP server started on port " + port);

		byte[] buffer = new byte[buffer_size];
		while (true) {
			try {
				if (isInterrupted()) {
					break;
				}

				// receive packet
				DatagramPacket packet = new DatagramPacket(buffer, buffer_size);
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

	public void stopServer() {
		if (isInterrupted()) {
			return; // already in process of shutting down
		}
		interrupt();
		socket.close();
	}
}
