/*
 * Created on 10. February 2010
 */

package com.springrts.tasserver;


import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Utility class for Accounts management.
 * Allows transferring account from accounts.txt to the DB or vice versa.
 * Also allows creating 41 test users, one admin and 40 user_{i},
 * all of them with username == password.
 *
 * @author hoijui
 */
public class AccountUtils {

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
		String localIP = "?";
		try {
			localIP = java.net.InetAddress.getLocalHost().getHostAddress();
		} catch (java.net.UnknownHostException ex) {
			// ignore
		}

		String userPasswd = Misc.encodePassword(userName);
		return new Account(userName, userPasswd, localIP, countryCode2L);
	}

	public static void createAliBaba(AccountsService actSrvc) {

		System.out.println("Accounts: " + actSrvc.getAccountsSize());
		System.out.println("Creating Ali Baba ...");

		Account admin = createTestAccount("admin");
		admin.setAccess(Account.Access.ADMIN);
		admin.setAgreementAccepted(true);
		actSrvc.addAccount(admin);

		System.out.println("and the 40 thievs ...");
		for (int i=0; i < 40; i++) {
			Account user_x = createTestAccount("user_" + i);
			user_x.setAgreementAccepted(true);
			actSrvc.addAccount(user_x);
		}

		System.out.println("Accounts: " + actSrvc.getAccountsSize());
	}

	public static void main(String[] args) {

		String toDo = (args.length > 0 ? args[0] : "");
		if (toDo.equals("")) {
			java.util.Scanner console = new java.util.Scanner(System.in);
			toDo = console.nextLine();
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
			System.out.println("\talibaba [FS] - creates one admin account and 40 users (user_0, ...), all with username==password (default: on the DB, if 2nd arg == FS -> in " + TASServer.ACCOUNTS_INFO_FILEPATH + ")");
			System.out.println("\tFS2DB        - copy all accounts from " + TASServer.ACCOUNTS_INFO_FILEPATH + " to the Accounts DB");
			System.out.println("\tDB2FS        - copy all accounts from the Accounts DB to " + TASServer.ACCOUNTS_INFO_FILEPATH);
		}
	}
}
