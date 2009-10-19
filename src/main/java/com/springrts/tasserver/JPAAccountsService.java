/*
 * Created on 2006.10.15
 */

package com.springrts.tasserver;


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

	/** WARNING: caller must check if username/password is valid etc. himself! */
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
}
