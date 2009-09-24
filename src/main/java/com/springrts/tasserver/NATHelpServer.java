/*
 * Created on 14.1.2006
 */

package com.springrts.tasserver;


import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This is a simple UDP server that helps detecting source ports with some
 * NAT traversal techniques (e.g. "hole punching").
 *
 * @author Betalord
 */
public class NATHelpServer extends Thread {

	private DatagramSocket socket = null;
	private int port;
	static public Vector<DatagramPacket> msgList = new Vector<DatagramPacket>(); // must be thread-safe

	public NATHelpServer(int port) {
		this.port = port;
	}

	@Override
	public void run() {
		try {
			socket = new DatagramSocket(port);
		} catch (Exception e) {
			System.out.println("Unable to start UDP server on port " + port + ". Ignoring ...");
			return;
		}

		System.out.println("UDP server started on port " + port);

		while (true) {
			try {
				if (isInterrupted()) {
					break;
				}
				byte[] buf = new byte[256];

				// receive packet
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				msgList.add(packet);
			} catch (InterruptedIOException e) {
				break;
			} catch (IOException e) {
				if (e.getMessage().equalsIgnoreCase("socket closed")) {
					// server stopped gracefully!
				} else {
					System.out.println("ERROR in UDP server. Stack trace:");
					e.printStackTrace();
				}
			}
		}

		socket.close();
		System.out.println("UDP NAT server closed.");
	}

	public void stopServer() {
		if (isInterrupted()) {
			return; // already in process of shutting down
		}
		interrupt();
		socket.close();
	}
}
