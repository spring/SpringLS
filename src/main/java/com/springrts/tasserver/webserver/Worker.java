/*
	Copyright (c) 2010 Robin Vobruba <robin.vobruba@derisk.ch>

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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author hoijui
 */
class Worker extends WebServer implements Runnable {

	private static final int BUF_SIZE = 2048;
	private static final byte[] EOL = {(byte) '\r', (byte) '\n'};

	/* buffer to use for requests */
	private byte[] buf;
	/* Socket to client we're handling */
	private Socket s;

	Worker() {
		buf = new byte[BUF_SIZE];
		s = null;
	}

	synchronized void setSocket(Socket s) {
		this.s = s;
		notify();
	}

	public synchronized void run() {
		while (true) {
			if (s == null) {
				/* nothing to do */
				try {
					wait();
				} catch (InterruptedException e) {
					/* should not happen */
					continue;
				}
			}
			try {
				handleClient();
			} catch (Exception e) {
				e.printStackTrace();
			}
			/* go back in wait queue if there's fewer
			 * than numHandler connections.
			 */
			s = null;
			List<Worker> pool = WebServer.threads;
			synchronized (pool) {
				if (pool.size() >= WebServer.workers) {
					/* too many threads, exit this one */
					return;
				} else {
					pool.add(this);
				}
			}
		}
	}

	void handleClient() throws IOException {
		InputStream is = new BufferedInputStream(s.getInputStream());
		PrintStream ps = new PrintStream(s.getOutputStream());
		/* we will only block in read for this many milliseconds
		 * before we fail with java.io.InterruptedIOException,
		 * at which point we will abandon the connection.
		 */
		s.setSoTimeout(WebServer.timeout);
		s.setTcpNoDelay(true);
		/* zero out the buffer from last time */
		for (int i = 0; i < BUF_SIZE; i++) {
			buf[i] = 0;
		}
		try {
			/* We only support HTTP GET/HEAD, and don't
			 * support any fancy HTTP options,
			 * so we're only interested really in
			 * the first line.
			 */
			int nread = 0, r = 0;

			outerloop:
			while (nread < BUF_SIZE) {
				r = is.read(buf, nread, BUF_SIZE - nread);
				if (r == -1) {
					/* EOF */
					return;
				}
				int i = nread;
				nread += r;
				for (; i < nread; i++) {
					if (buf[i] == (byte) '\n' || buf[i] == (byte) '\r') {
						/* read one line */
						break outerloop;
					}
				}
			}

			/* are we doing a GET or just a HEAD */
			boolean doingGet;
			/* beginning of file name */
			int index;
			if (buf[0] == (byte) 'G'
					&& buf[1] == (byte) 'E'
					&& buf[2] == (byte) 'T'
					&& buf[3] == (byte) ' ')
			{
				doingGet = true;
				index = 4;
			} else if (buf[0] == (byte) 'H'
					&& buf[1] == (byte) 'E'
					&& buf[2] == (byte) 'A'
					&& buf[3] == (byte) 'D'
					&& buf[4] == (byte) ' ')
			{
				doingGet = false;
				index = 5;
			} else {
				/* we don't support this method */
				ps.print("HTTP/1.0 " + HttpConstants.HTTP_BAD_METHOD
						+ " unsupported method type: ");
				ps.write(buf, 0, 5);
				ps.write(EOL);
				ps.flush();
				s.close();
				return;
			}

			int i = 0;
			/* find the file name, from:
			 * GET /foo/bar.html HTTP/1.0
			 * extract "/foo/bar.html"
			 */
			for (i = index; i < nread; i++) {
				if (buf[i] == (byte) ' ') {
					break;
				}
			}
			String fname = (new String(buf, 0, index,
					i - index)).replace('/', File.separatorChar);
			if (fname.startsWith(File.separator)) {
				fname = fname.substring(1);
			}
			File targ = new File(WebServer.root, fname);
			if (targ.isDirectory()) {
				File ind = new File(targ, "index.html");
				if (ind.exists()) {
					targ = ind;
				}
			}
			boolean ok = printHeaders(targ, ps);
			if (doingGet) {
				if (ok) {
					sendFile(targ, ps);
				} else {
					send404(targ, ps);
				}
			}
		} finally {
			s.close();
		}
	}

	boolean printHeaders(File targ, PrintStream ps) throws IOException {
		boolean ret = false;
		int rCode = 0;
		if (!targ.exists()) {
			rCode = HttpConstants.HTTP_NOT_FOUND;
			ps.print("HTTP/1.0 " + HttpConstants.HTTP_NOT_FOUND + " not found");
			ps.write(EOL);
			ret = false;
		} else {
			rCode = HttpConstants.HTTP_OK;
			ps.print("HTTP/1.0 " + HttpConstants.HTTP_OK + " OK");
			ps.write(EOL);
			ret = true;
		}
		log("From " + s.getInetAddress().getHostAddress() + ": GET "
				+ targ.getAbsolutePath() + "-->" + rCode);
		ps.print("Server: Simple java");
		ps.write(EOL);
		ps.print("Date: " + (new Date()));
		ps.write(EOL);
		if (ret) {
			if (!targ.isDirectory()) {
				ps.print("Content-length: " + targ.length());
				ps.write(EOL);
				ps.print("Last Modified: " + (new Date(targ.lastModified())));
				ps.write(EOL);
				String name = targ.getName();
				int ind = name.lastIndexOf('.');
				String ct = null;
				if (ind > 0) {
					ct = extToType.get(name.substring(ind));
				}
				if (ct == null) {
					ct = "unknown/unknown";
				}
				ps.print("Content-type: " + ct);
				ps.write(EOL);
			} else {
				ps.print("Content-type: text/html");
				ps.write(EOL);
			}
		}
		return ret;
	}

	void send404(File targ, PrintStream ps) throws IOException {
		ps.write(EOL);
		ps.write(EOL);
		ps.println("Not Found\n\n"
				+ "The requested resource was not found.\n");
	}

	void sendFile(File targ, PrintStream ps) throws IOException {
		InputStream is = null;
		ps.write(EOL);
		if (targ.isDirectory()) {
			listDirectory(targ, ps);
			return;
		} else {
			is = new FileInputStream(targ.getAbsolutePath());
		}

		try {
			int n;
			while ((n = is.read(buf)) > 0) {
				ps.write(buf, 0, n);
			}
		} finally {
			is.close();
		}
	}

	/** mapping of file extensions to content-types */
	private static Map<String, String> extToType = new HashMap<String, String>();


	static {
		fillMap();
	}

	static void setSuffix(String k, String v) {
		extToType.put(k, v);
	}

	static void fillMap() {
		setSuffix("", "content/unknown");
		setSuffix(".uu", "application/octet-stream");
		setSuffix(".exe", "application/octet-stream");
		setSuffix(".ps", "application/postscript");
		setSuffix(".zip", "application/zip");
		setSuffix(".sh", "application/x-shar");
		setSuffix(".tar", "application/x-tar");
		setSuffix(".snd", "audio/basic");
		setSuffix(".au", "audio/basic");
		setSuffix(".wav", "audio/x-wav");
		setSuffix(".gif", "image/gif");
		setSuffix(".jpg", "image/jpeg");
		setSuffix(".jpeg", "image/jpeg");
		setSuffix(".htm", "text/html");
		setSuffix(".html", "text/html");
		setSuffix(".text", "text/plain");
		setSuffix(".c", "text/plain");
		setSuffix(".cc", "text/plain");
		setSuffix(".c++", "text/plain");
		setSuffix(".h", "text/plain");
		setSuffix(".pl", "text/plain");
		setSuffix(".txt", "text/plain");
		setSuffix(".log", "text/plain");
		setSuffix(".java", "text/plain");
	}

	void listDirectory(File dir, PrintStream ps) throws IOException {
		ps.println("<TITLE>Directory listing</TITLE><P>\n");
		ps.println("<A HREF=\"..\">Parent Directory</A><BR>\n");
		String[] list = dir.list();
		for (int i = 0; list != null && i < list.length; i++) {
			File f = new File(dir, list[i]);
			if (f.isDirectory()) {
				ps.println("<A HREF=\"" + list[i] + "/\">" + list[i] + "/</A><BR>");
			} else {
				ps.println("<A HREF=\"" + list[i] + "\">" + list[i] + "</A><BR");
			}
		}
		ps.println("<P><HR><BR><I>" + (new Date()) + "</I>");
	}
}
