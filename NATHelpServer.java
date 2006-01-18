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

	private boolean running = false;
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
			return ;
		}
		
		running = true;
		System.out.println("UDP server started on port " + port);
		
		while (running) {
	        try {
	            byte[] buf = new byte[256];

	            // receive packet
	            DatagramPacket packet = new DatagramPacket(buf, buf.length);
	            socket.receive(packet);
	            msgList.add(packet);
	        } catch (IOException e) {
	        	System.out.println("ERROR in UDP NAT server. Stack trace:");
	            e.printStackTrace();
	        }
		}

		socket.close();
		running = false;
	}
	
	public void stopServer() {
		if (!running) return ;
		running = false;
	}

}
