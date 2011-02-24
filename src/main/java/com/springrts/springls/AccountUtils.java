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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.List;

/**
 * Utility class for Accounts management.
 * Allows transferring account from accounts.txt to the DB or vice versa.
 * Also allows creating 41 test users, one admin and 40 user_{i},
 * all of them with username == password.
 *
 * @author hoijui
 */
public final class AccountUtils {

	private AccountUtils() {}

	public static void moveAccountsFromFStoDB() {

		AccountsService from = new FSAccountsService();
		AccountsService to   = new JPAAccountsService();
		moveAccounts(from, to);
	}

	public static void moveAccountsFromDBtoFS() {

		AccountsService from = new JPAAccountsService();
		AccountsService to   = new FSAccountsService();
		moveAccounts(from, to);
	}

	private static void moveAccounts(AccountsService from, AccountsService to) {

		System.out.println("Copy all accounts from one storage to the other ...");

		System.out.println("Loading ...");
		long begin = System.currentTimeMillis();
		from.loadAccounts();
		List<Account> accounts = from.fetchAllAccounts();
		long time = System.currentTimeMillis() - begin;
		System.out.println("Loading of " + accounts.size() + " accounts done in " + time + "ms.");

		System.out.println("Saving ...");
		begin = System.currentTimeMillis();
		to.addAccounts(accounts);
		time = System.currentTimeMillis() - begin;
		System.out.println("Saving of " + accounts.size() + " accounts done in " + time + "ms.");

		System.out.println("done.");
	}

	private static Account createTestAccount(String userName) {

		final String countryCode2L = java.util.Locale.getDefault().getCountry();
		InetAddress localIp;
		try {
			localIp = InetAddress.getLocalHost();
		} catch (java.net.UnknownHostException ex) {
			localIp = null;
		}

		String userPasswd = Misc.encodePassword(userName);
		return new Account(userName, userPasswd, localIp, countryCode2L);
	}

	public static void createAliBaba(AccountsService actSrvc) {

		System.out.println("Accounts: " + actSrvc.getAccountsSize());
		System.out.println("Creating Ali Baba ...");

		Account admin = createTestAccount("admin");
		admin.setAccess(Account.Access.ADMIN);
		admin.setAgreementAccepted(true);
		actSrvc.addAccount(admin);

		System.out.println("and the 40 thievs ...");
		for (int i = 0; i < 40; i++) {
			Account userX = createTestAccount("user_" + i);
			userX.setAgreementAccepted(true);
			actSrvc.addAccount(userX);
		}

		System.out.println("Accounts: " + actSrvc.getAccountsSize());
	}

	public static void main(String[] args) {

		String toDo = (args.length > 0 ? args[0] : "");
		if (toDo.equals("")) {
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			try {
				toDo = console.readLine();
			} catch (IOException ex) {
				System.err.println("Failed reading from the command-line");
			}
		}

		if (toDo.toUpperCase().equals("alibaba")) {
			AccountsService actSrvc = null;
			if ((args.length > 1) && (args[1].toUpperCase().equals("fs"))) {
				actSrvc = new FSAccountsService();
			} else {
				actSrvc = new JPAAccountsService();
			}
			createAliBaba(actSrvc);
		} else if (toDo.toUpperCase().equals("FS2DB")) {
			moveAccountsFromFStoDB();
		} else if (toDo.toUpperCase().equals("DB2FS")) {
			moveAccountsFromDBtoFS();
		} else {
			System.out.println("Specify one of these (be carefull! No \"Are you sure?\" protection):");
			System.out.println("\talibaba [FS] - creates one admin account and 40 users (user_0, ...), all with username==password (default: on the DB, if 2nd arg == FS -> in " + FSAccountsService.ACCOUNTS_INFO_FILEPATH + ")");
			System.out.println("\tFS2DB        - copy all accounts from " + FSAccountsService.ACCOUNTS_INFO_FILEPATH + " to the Accounts DB");
			System.out.println("\tDB2FS        - copy all accounts from the Accounts DB to " + FSAccountsService.ACCOUNTS_INFO_FILEPATH);
		}
	}
}
