/*
	Copyright (c) 2006 Robin Vobruba <hoijui.quaero@gmail.com>

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

package com.springrts.springls.nat;


import com.springrts.springls.Client;
import com.springrts.springls.Context;
import com.springrts.springls.ContextReceiver;
import com.springrts.springls.LiveStateListener;
import com.springrts.springls.ServerConfiguration;
import com.springrts.springls.Updateable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import org.apache.commons.configuration.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a simple UDP server that helps detecting source ports with some
 * NAT traversal techniques (e.g. "hole punching").
 *
 * @author Betalord
 * @author hoijui
 */
public class NatHelpServer implements Runnable, ContextReceiver,
		LiveStateListener, Updateable
{
	private static final Logger LOG
			= LoggerFactory.getLogger(NatHelpServer.class);
	private static final int RECEIVE_BUFFER_SIZE = 256;

	/** Has to be thread-safe */
	private List<DatagramPacket> msgList;
	private DatagramSocket socket;
	private Thread myThread;

	private Context context;

	public NatHelpServer() {

		this.msgList = Collections.synchronizedList(
				new LinkedList<DatagramPacket>());
		this.socket = null;
		this.myThread = null;
		this.context = null;
	}

	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}
	private Context getContext() {
		return context;
	}

	@Override
	public void update() {
		checkForNewPackets();
	}

	/**
	 * check UDP server for any new packets
	 */
	private void checkForNewPackets() {

		DatagramPacket packet;
		while ((packet = fetchNextPackage()) != null) {
			InetAddress address = packet.getAddress();
			int clientPort = packet.getPort();
			String data = new String(packet.getData(), packet.getOffset(),
					packet.getLength());
			LOG.trace("*** UDP packet received from {} from port {}",
					address.getHostAddress(), clientPort);
			Client client = getContext().getClients().getClient(data);
			if (client == null) {
				continue;
			}
			client.setUdpSourcePort(clientPort);
			client.sendLine(String.format("UDPSOURCEPORT %d", clientPort));
		}
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

	@Override
	public void run() {

		Configuration conf = getContext().getService(Configuration.class);
		int port = conf.getInt(ServerConfiguration.NAT_PORT);
		try {
			socket = new DatagramSocket(port);
		} catch (Exception ex) {
			LOG.warn("Unable to start UDP server on port " + port
					+ ". Ignoring ...", ex);
			return;
		}

		LOG.info("Listening for connections on UDP port {} ...", port);

		byte[] buffer = new byte[RECEIVE_BUFFER_SIZE];
		while (true) {
			try {
				if (myThread.isInterrupted()) {
					break;
				}

				// receive packet
				DatagramPacket packet = new DatagramPacket(buffer,
						RECEIVE_BUFFER_SIZE);
				socket.receive(packet);
				msgList.add(packet);
			} catch (InterruptedIOException e) {
				break;
			} catch (IOException ex) {
				if (!ex.getMessage().equalsIgnoreCase("socket closed")) {
					LOG.error("Error in UDP server", ex);
				}
			}
		}

		socket.close();
		LOG.info("UDP NAT server closed.");
	}

	public void startServer() {

		if (myThread == null) {
			myThread = new Thread(this);
			myThread.start();
		} else {
			LOG.warn("NAT help server is already running");
		}
	}

	public boolean isRunning() {
		return ((myThread != null) && myThread.isAlive());
	}

	public void stopServer() {

		if (myThread == null) {
			LOG.warn("NAT help server is not running");
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
			LOG.error("NAT help server interrupted while shutting down", ex);
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
