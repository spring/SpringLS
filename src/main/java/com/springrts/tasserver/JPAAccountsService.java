/*
 * Created on 2006.10.15
 */

package com.springrts.tasserver;


import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author hoijui
 */
public class JPAAccountsService extends AbstractAccountsService implements AccountsService {

	private static final Log s_log  = LogFactory.getLog(JPAAccountsService.class);

	private EntityManager em = null;
	private Query q_size = null;
	private Query q_list = null;
	private Query q_fetchByName = null;
	private Query q_fetchByLastIP = null;

	public JPAAccountsService() {

		EntityManagerFactory emf = Persistence.createEntityManagerFactory("tasserver");
		em = emf.createEntityManager();

		q_size          = em.createQuery("SELECT count(a.id) FROM Account a");
		q_list          = em.createQuery("SELECT * FROM Account");
		q_fetchByName   = em.createQuery("SELECT a FROM Account a WHERE a.name = :name");
		q_fetchByLastIP = em.createQuery("SELECT a FROM Account a WHERE a.last_ip = :ip");
	}

	@Override
	public int getAccountsSize() {

		try {
			em.getTransaction().begin();
			long numAccounts = (Long) (q_size.getSingleResult());
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
		// TODO
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
			q_fetchByName.setParameter("name", username);
			act = (Account) q_fetchByName.getSingleResult();
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
			q_fetchByLastIP.setParameter("ip", ip);
			act = (Account) q_fetchByLastIP.getSingleResult();
			em.getTransaction().commit();
		} catch (Exception ex) {
			s_log.trace("Failed fetching an account by last ip", ex);
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
			acts = (List<Account>) (q_list.getResultList());
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

		from.loadAccounts();

		List<Account> accounts = from.fetchAllAccounts();

		for (Account account : accounts) {
			to.addAccount(account);
		}

		System.out.println("done.");
	}

	private static String hashMD5(String input) {

		try {
			java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			byte[] md5hash = new byte[32];
			digest.update(input.getBytes("iso-8859-1"), 0, input.length());
			md5hash = digest.digest();

			//MD5 hash in base-64 form
			return new String(new sun.misc.BASE64Encoder().encode(md5hash)).trim();
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
			return input;
		} catch (NoSuchAlgorithmException ex) {
			ex.printStackTrace();
			return input;
		}
	}

	private static Account createTestAccount(String userName) {

		final String countryCode2L = java.util.Locale.getDefault().getCountry();
		String localIP = "?";
		try {
			localIP = java.net.InetAddress.getLocalHost().getHostAddress();
		} catch (java.net.UnknownHostException ex) {
			// ignore
		}

		String userPasswd = hashMD5(userName);
		return new Account(userName, userPasswd, localIP, countryCode2L);
	}

	public static void createAliBaba() {

		AccountsService actSrvc = new JPAAccountsService();
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

		if (toDo.equals("alibaba")) {
			createAliBaba();
		} else if (toDo.equals("FS2DB")) {
			moveAccountsFromFStoDB();
		} else if (toDo.equals("DB2FS")) {
			moveAccountsFromDBtoFS();
		} else {
			System.out.println("Specify one of these (be carefull! No \"Are you sure?\" protection):");
			System.out.println("\talibaba - creates one admin account and 40 users (user_0, ...), all with username==password");
			System.out.println("\tFS2DB   - copy all accounts from " + TASServer.ACCOUNTS_INFO_FILEPATH + " to the Accounts DB");
			System.out.println("\tDB2FS   - copy all accounts from the Accounts DB to " + TASServer.ACCOUNTS_INFO_FILEPATH);
		}
	}
}
