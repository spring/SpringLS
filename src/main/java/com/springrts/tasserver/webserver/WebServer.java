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

package com.springrts.tasserver.webserver;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * This web server code was copied from SUN's
 * <a href="http://java.sun.com/developer/technicalArticles/Networking/Webserver/">
 * simple web server tutorial</a>
 *
 * Use "www.server.properties" file for server config.
 *
 * An example of a very simple, multi-threaded HTTP server.
 * Implementation notes are in WebServer.html, and also
 * as comments in the source code.
 * @author Betalord
 */
class WebServer {

	/* static class data/methods */

	/* print to stdout */
	protected static void p(String s) {
		System.out.println(s);
	}

	/* print to the log file */
	protected static void log(String s) {
		synchronized (log) {
			log.println(s);
			log.flush();
		}
	}
	private static PrintStream log = null;
	/* our server's configuration information is stored
	 * in these properties
	 */
	private static Properties props = new Properties();

	/* Where worker threads stand idle */
	protected static List<Worker> threads = Collections.synchronizedList(new LinkedList<Worker>());

	/* the web server's virtual root */
	protected static File root;

	/* timeout on client connections */
	protected static int timeout = 0;

	/* max # worker threads */
	protected static int workers = 5;


	/* load www-server.properties from java.home */
	static void loadProps() throws IOException {
		/*        File f = new File
		(System.getProperty("java.home")+File.separator+
		"lib"+File.separator+"www-server.properties");
		 */
		File f = new File("www-server.properties");

		if (f.exists()) {
			InputStream is = new BufferedInputStream(new FileInputStream(f));
			props.load(is);
			is.close();
			String r = props.getProperty("root");
			if (r != null) {
				root = new File(r);
				if (!root.exists()) {
					throw new Error(root + " doesn't exist as server root");
				}
			}
			r = props.getProperty("timeout");
			if (r != null) {
				timeout = Integer.parseInt(r);
			}
			r = props.getProperty("workers");
			if (r != null) {
				workers = Integer.parseInt(r);
			}
			r = props.getProperty("log");
			if (r != null) {
				p("opening log file: " + r);
				log = new PrintStream(new BufferedOutputStream(
						new FileOutputStream(r)));
			}
		}

		/* if no properties were specified, choose defaults */
		if (root == null) {
			root = new File(System.getProperty("user.dir"));
		}
		if (timeout <= 1000) {
			timeout = 5000;
		}
		if (workers < 25) {
			workers = 5;
		}
		if (log == null) {
			p("logging to stdout");
			log = System.out;
		}
	}

	static void printProps() {
		p("root=" + root);
		p("timeout=" + timeout);
		p("workers=" + workers);
	}

	public static void main(String[] a) throws Exception {
		int port = 8080;
		if (a.length > 0) {
			port = Integer.parseInt(a[0]);
		}
		loadProps();
		printProps();
		/* start worker threads */
		for (int i = 0; i < workers; ++i) {
			Worker w = new Worker();
			(new Thread(w, "worker #" + i)).start();
			threads.add(w);
		}

		ServerSocket ss = new ServerSocket(port);
		while (true) {

			Socket s = ss.accept();

			Worker w = null;
			synchronized (threads) {
				if (threads.isEmpty()) {
					Worker ws = new Worker();
					ws.setSocket(s);
					(new Thread(ws, "additional worker")).start();
				} else {
					w = threads.get(0);
					threads.remove(0);
					w.setSocket(s);
				}
			}
		}
	}
}
