/*
 * Created on 2006.10.15
 */

package com.springrts.tasserver;


import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author hoijui
 */
public class JPAAccountsService extends AbstractAccountsService implements AccountsService {

	private static final Log s_log  = LogFactory.getLog(JPAAccountsService.class);

	private EntityManager em = null;

	public JPAAccountsService() {

		EntityManagerFactory emf = Persistence.createEntityManagerFactory("tasserver");
		em = emf.createEntityManager();
	}

	@Override
	public int getAccountsSize() {

		try {
			em.getTransaction().begin();
			long numAccounts = (Long) (em.createQuery("SELECT count(a.id) FROM Account a").getSingleResult());
			em.getTransaction().commit();
			return (int)numAccounts;
		} catch (Exception ex) {
			s_log.error("Failed fetching number of accounts", ex);
			em.getTransaction().rollback();
		}

		return -1;
	}

	@Override
	public int getActiveAccountsSize() {

		final long oneWeekAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 7);
		final Account.Rank lowRank = Account.Rank.Newbie;
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean loadAccounts() {
		return true;
	}

	@Override
	public void saveAccounts(boolean block) {}

	@Override
	public void saveAccountsIfNeeded() {}

	@Override
	public void addAccount(Account acc) {

		try {
			em.getTransaction().begin();
			em.persist(acc);
			em.getTransaction().commit();
		} catch (Exception ex) {
			s_log.error("Failed adding an account", ex);
			em.getTransaction().rollback();
		}
	}

	@Override
	public boolean removeAccount(Account acc) {

		boolean removed = false;

		try {
			em.getTransaction().begin();
			em.remove(acc);
			em.getTransaction().commit();
			removed = true;
		} catch (Exception ex) {
			s_log.error("Failed adding an account", ex);
			em.getTransaction().rollback();
		}

		return removed;
	}

	@Override
	public Account getAccount(String username) {

		Account act = null;

		try {
			em.getTransaction().begin();
			act = (Account) (em.createQuery("SELECT a FROM Account a WHERE a.username == " + username).getSingleResult());
			em.getTransaction().commit();
		} catch (Exception ex) {
			em.getTransaction().rollback();
			s_log.error("Failed fetching an account", ex);
			act = null;
		}

		return act;
	}

	@Override
	public Account findAccountNoCase(String username) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Account findAccountByLastIP(String ip) {

		Account act = null;

		try {
			em.getTransaction().begin();
			act = (Account) (em.createQuery("SELECT a FROM Account a WHERE a.last_ip == " + ip).getSingleResult());
			em.getTransaction().commit();
		} catch (Exception ex) {
			s_log.trace("Failed fetching an account with by last ip", ex);
			em.getTransaction().rollback();
			act = null;
		}

		return act;
	}

	@Override
	public boolean mergeAccountChanges(Account account, String oldName) {

		boolean replaced = false;

		try {
			em.getTransaction().begin();
			em.merge(account);
			em.getTransaction().commit();
			replaced = true;
		} catch (Exception ex) {
			s_log.error("Failed replacing an account", ex);
			em.getTransaction().rollback();
		}

		return replaced;
	}

	@Override
	public List<Account> fetchAllAccounts() {

		List<Account> acts = null;

		try {
			em.getTransaction().begin();
			acts = (List<Account>) (em.createQuery("SELECT * FROM Account").getResultList());
			em.getTransaction().commit();
		} catch (Exception ex) {
			em.getTransaction().rollback();
			s_log.error("Failed fetching an account", ex);
			acts = null;
		}

		return acts;
	}

	public static void moveAccountsFromFStoDB() {

		AccountsService from = new FSAccountsService();
		AccountsService to = new JPAAccountsService();
		moveAccounts(from, to);
	}
	public static void moveAccountsFromDBtoFS() {

		AccountsService from = new FSAccountsService();
		AccountsService to = new JPAAccountsService();
		moveAccounts(from, to);
	}
	private static void moveAccounts(AccountsService from, AccountsService to) {

		System.out.println("Copy all accounts from one storage to the other ...");
		List<Account> accounts = from.fetchAllAccounts();

		for (Account account : accounts) {
			to.addAccount(account);
		}

		System.out.println("done.");
	}

	public static void createAliBaba() {

		AccountsService actSrvc = new JPAAccountsService();
		System.out.println("Accounts: " + actSrvc.getAccountsSize());
		System.out.println("Creating Ali Baba ...");

		final String countryCode2L = java.util.Locale.getDefault().getCountry();
		String localIP = "?";
		try {
			localIP = java.net.InetAddress.getLocalHost().getHostAddress();
		} catch (java.net.UnknownHostException ex) {
			// ignore
		}

		Account admin = new Account("admin", "admin", localIP, countryCode2L);
		admin.setAccess(Account.Access.ADMIN);
		admin.setAgreementAccepted(true);
		actSrvc.addAccount(admin);

		System.out.println("and the 40 thievs ...");
		for (int i=0; i < 40; i++) {
			Account user_x = new Account("user_" + i, "user_" + i, localIP, countryCode2L);
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

		if (toDo.equals("alibaba")) {
			createAliBaba();
		} else if (toDo.equals("FS2DB")) {
			moveAccountsFromDBtoFS();
		} else if (toDo.equals("DB2FS")) {
			moveAccountsFromFStoDB();
		} else {
			System.out.println("Specify one of these (be carefull! No \"Are you sure?\" protection):");
			System.out.println("\talibaba - creates one admin account and 40 users (user_0, ...), all with username==password");
			System.out.println("\tFS2DB   - copy all accounts from " + TASServer.ACCOUNTS_INFO_FILEPATH + " to the Accounts DB");
			System.out.println("\tDB2FS   - copy all accounts from the Accounts DB to " + TASServer.ACCOUNTS_INFO_FILEPATH);
		}
	}
}
