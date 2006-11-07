/*
 * Created on 14.1.2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 * 
 * This is a simple UDP server that helps detecting source ports with some
 * NAT traversal techniques (e.g. "hole punching").
 * 
 */

/**
 * @author Betalord
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class NATHelpServer extends Thread {

	private DatagramSocket socket = null;
	private int port;
	static public Vector msgList = new Vector(); // must be thread-safe
	
	public NATHelpServer(int port) {
		this.port = port;
	}
	
	public void run() {
		try {
			socket = new DatagramSocket(port);
		} catch (Exception e) {
			System.out.println("Unable to start UDP server on port " + port + ". Ignoring ...");
			return ;
		}
		
		System.out.println("UDP server started on port " + port);
		
		while (true) {
	        try {
	        	if (isInterrupted()) break;
	            byte[] buf = new byte[256];

	            // receive packet
	            DatagramPacket packet = new DatagramPacket(buf, buf.length);
	            socket.receive(packet);
	            msgList.add(packet);
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
		if (isInterrupted()) return; // already in process of shutting down
		interrupt();
		socket.close();
	}

}
