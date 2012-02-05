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

package com.springrts.springls.accounts;


import com.springrts.springls.Account;
import com.springrts.springls.Context;
import com.springrts.springls.ContextReceiver;
import com.springrts.springls.ServerNotification;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Note that it is vital that everything here is synchronized with main server
 * thread.
 * Currently dupAccounts list is cloned from the original accounts list, so we
 * do not have to worry about thread-safety here. This is the easiest way, since
 * otherwise we would have to make sure no account is added or removed while we
 * are saving accounts to disk.
 * The second thing for which we must ensure thread-safety is the
 * Account.toString() method. The only problem there is the call to
 * MapGradeList.toString(), which could potentially cause problems. Other fields
 * used in Account.toString() are mostly atomic, or if they are not, it does not
 * hurt us really - an example is the 'long' type, which consists of two 32 bit
 * <code>int</code>s, and is thus not atomic, but it will not cause corruption
 * in the accounts file, as it is used right now. So it is essential to ensure
 * that the MapGrading class is thread-safe, or at least its toString() method.
 *
 * @author Betalord
 */
public class FSSaveAccountsThread extends Thread implements ContextReceiver {

	private static final Logger LOG
			= LoggerFactory.getLogger(FSSaveAccountsThread.class);

	private Context context = null;

	/**
	 * Where to save the accounts to.
	 */
	private File saveFile;

	/**
	 * Duplicated accounts.
	 * Needed to ensure thread safety as well as accounts state consistency.
	 */
	private List<Account> dupAccounts;


	public FSSaveAccountsThread(File saveFile, List<Account> dupAccounts) {

		this.saveFile = saveFile;
		this.dupAccounts = dupAccounts;
	}


	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}

	@Override
	public void run() {

		LOG.info("Dumping accounts to disk in a separate thread ...");
		long time = System.currentTimeMillis();

		Writer outF = null;
		Writer outB = null;
		PrintWriter out = null;
		try {
			outF = new FileWriter(saveFile);
			outB = new BufferedWriter(outF);
			out = new PrintWriter(outB);

			for (int i = 0; i < dupAccounts.size(); i++) {
				out.println(FSAccountsService.toPersistentString(
						dupAccounts.get(i)));
			}
		} catch (IOException ex) {
			LOG.error("Failed writing accounts info to "
					+ saveFile.getAbsolutePath() + "!", ex);

			// add server notification:
			ServerNotification sn = new ServerNotification(
					"Error saving accounts");
			sn.addLine("Serious error: accounts info could not be saved to"
					+ " disk. Exception trace:");
			sn.addException(ex);
			context.getServerNotifications().addNotification(sn);

			return;
		} finally {
			try {
				if (out != null) {
					out.close();
				} else if (outB != null) {
					outB.close();
				} else if (outF != null) {
					outF.close();
				}
			} catch (IOException ex) {
				LOG.trace("Failed closing file writer to accounts file '{}';"
						+ " reason: {}."
						, saveFile.getAbsolutePath(), ex.getMessage());
			}
		}

		LOG.info("{} accounts information written to {} successfully ({} ms).",
				new Object[] {
					dupAccounts.size(),
					saveFile.getAbsolutePath(),
					(System.currentTimeMillis() - time)
				});

		// let the garbage collector free the duplicate accounts list
		dupAccounts = null;
	}
}
