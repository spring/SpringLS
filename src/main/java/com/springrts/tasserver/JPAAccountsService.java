/*
 * Created on 2006.10.15
 */

package com.springrts.tasserver;


import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JPA implementation of an accounts service.
 * Uses abstracted DB access to store data.
 * @see persistence.xml
 *
 * @author hoijui
 */
public class JPAAccountsService extends AbstractAccountsService implements AccountsService {

	private static final Log s_log  = LogFactory.getLog(JPAAccountsService.class);

	private EntityManager em = null;
	private Query q_size = null;
	private Query q_size_active = null;
	private Query q_list = null;
	private Query q_fetchByName = null;
	private Query q_fetchByLowerName = null;
	private Query q_fetchByLastIP = null;


	public JPAAccountsService() {

		EntityManagerFactory emf = Persistence.createEntityManagerFactory("tasserver");
		em = emf.createEntityManager();

		q_size          = em.createQuery("SELECT count(a.id) FROM Account a");
		q_size_active   = em.createQuery("SELECT count(a.id) FROM Account a WHERE ((a.inGameTime >= :minInGameTime) AND (a.lastLogin > :oneWeekAgo))");
		q_size_active.setParameter("minInGameTime", Account.Rank.Beginner.getRequiredTime());
		q_list          = em.createQuery("SELECT a FROM Account a");
		q_fetchByName   = em.createQuery("SELECT a FROM Account a WHERE a.name = :name");
		q_fetchByLowerName = em.createQuery("SELECT a FROM Account a WHERE (LOWER(a.name) = :lowerName)");
		q_fetchByLastIP = em.createQuery("SELECT a FROM Account a WHERE a.lastIP = :ip");
	}

	private void begin() {
		em.getTransaction().begin();
	}
	private void commit() {
		em.getTransaction().commit();
	}
	private void rollback() {

		try {
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		} catch (PersistenceException ex) {
			s_log.error("Failed to rollback a transaction", ex);
		}
	}


	@Override
	public boolean isReadyToOperate() {
		return true;
	}

	@Override
	public int getAccountsSize() {

		try {
			begin();
			long numAccounts = (Long) (q_size.getSingleResult());
			commit();
			return (int)numAccounts;
		} catch (Exception ex) {
			s_log.error("Failed fetching number of accounts", ex);
			rollback();
		}

		return -1;
	}

	@Override
	public int getActiveAccountsSize() {

		int activeAccounts = -1;

		try {
			begin();
			final long oneWeekAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 7);
			q_size_active.setParameter("oneWeekAgo", oneWeekAgo);
			activeAccounts = (int) (long) (Long) (q_size_active.getSingleResult());
			commit();
		} catch (Exception ex) {
			s_log.error("Failed fetching active accounts", ex);
			rollback();
			activeAccounts = -1;
		}

		return activeAccounts;
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
			begin();
			em.persist(acc);
			commit();
		} catch (Exception ex) {
			s_log.error("Failed adding an account", ex);
			rollback();
		}
	}

	@Override
	public void addAccounts(Iterable<Account> accs) {

		try {
			begin();

			for (Account acc : accs) {
				em.merge(acc);
			}

			commit();
		} catch (Exception ex) {
			s_log.error("Failed adding an account", ex);
			rollback();
		}
	}

	@Override
	public boolean removeAccount(Account acc) {

		boolean removed = false;

		try {
			begin();
			em.remove(acc);
			commit();
			removed = true;
		} catch (Exception ex) {
			s_log.error("Failed removing an account", ex);
			rollback();
		}

		return removed;
	}

	@Override
	public Account getAccount(String username) {

		Account act = null;

		try {
			begin();
			q_fetchByName.setParameter("name", username);
			act = (Account) q_fetchByName.getSingleResult();
			commit();
		} catch (Exception ex) {
			s_log.trace("Failed fetching an account by name: " + username, ex);
			rollback();
			act = null;
		}

		return act;
	}

	@Override
	public Account findAccountNoCase(String username) {

		Account act = null;

		try {
			begin();
			q_fetchByLowerName.setParameter("lowerName", username.toLowerCase());
			act = (Account) q_fetchByLowerName.getSingleResult();
			commit();
		} catch (Exception ex) {
			s_log.trace("Failed fetching an account by name (case-insensitive)", ex);
			rollback();
			act = null;
		}

		return act;
	}

	@Override
	public Account findAccountByLastIP(String ip) {

		Account act = null;

		try {
			begin();
			q_fetchByLastIP.setParameter("ip", ip);
			act = (Account) q_fetchByLastIP.getSingleResult();
			commit();
		} catch (Exception ex) {
			s_log.trace("Failed fetching an account by last ip", ex);
			rollback();
			act = null;
		}

		return act;
	}

	@Override
	public boolean mergeAccountChanges(Account account, String oldName) {

		boolean replaced = false;

		try {
			begin();
			em.merge(account);
			commit();
			replaced = true;
		} catch (Exception ex) {
			s_log.error("Failed replacing an account", ex);
			rollback();
		}

		return replaced;
	}

	@Override
	public List<Account> fetchAllAccounts() {

		List<Account> acts = null;

		try {
			begin();
			acts = (List<Account>) (q_list.getResultList());
			commit();
		} catch (Exception ex) {
			s_log.error("Failed fetching all accounts", ex);
			rollback();
			acts = null;
		}

		return acts;
	}
}
