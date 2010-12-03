/*
 * Created on 2006.10.15
 */

package com.springrts.tasserver;


import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
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

	private EntityManagerFactory emf = null;


	public JPAAccountsService() {

		emf = Persistence.createEntityManagerFactory("tasserver");
	}

	private EntityManager open() {

		EntityManager em = emf.createEntityManager();
		return em;
	}
	private void begin(EntityManager em) {
		em.getTransaction().begin();
	}
	private void commit(EntityManager em) {
		em.getTransaction().commit();
	}
	private void rollback(EntityManager em) {

		if (em == null) {
			s_log.error("Failed to create an entity manager");
		} else {
			try {
				if (em.isOpen() && em.getTransaction().isActive()) {
					em.getTransaction().rollback();
				} else {
					s_log.error("Failed to rollback a transaction: no active connection or transaction");
				}
			} catch (PersistenceException ex) {
				s_log.error("Failed to rollback a transaction", ex);
			}
		}
	}
	private void close(EntityManager em) {

		if (em == null) {
			s_log.error("Failed to create an entity manager");
		} else {
			try {
				if (em.isOpen()) {
					em.close();
				}
			} catch (IllegalStateException ex) {
				s_log.error("Failed to close an entity manager", ex);
			}
		}
	}


	@Override
	public boolean isReadyToOperate() {
		return true;
	}

	@Override
	public int getAccountsSize() {

		int accounts = -1;

		EntityManager em = null;
		try {
			em = open();
			long numAccounts = (Long) (em.createNamedQuery("acc_size").getSingleResult());
			accounts = (int) numAccounts;
		} catch (Exception ex) {
			s_log.error("Failed fetching number of accounts", ex);
		} finally {
			close(em);
			em = null;
		}

		return accounts;
	}

	@Override
	public int getActiveAccountsSize() {

		int activeAccounts = -1;

		EntityManager em = null;
		try {
			em = open();
			Query activeSizeQuery = em.createNamedQuery("acc_size_active");
			final long oneWeekAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 7);
			activeSizeQuery.setParameter("oneWeekAgo", oneWeekAgo);
			activeAccounts = (int) (long) (Long) (activeSizeQuery.getSingleResult());
		} catch (Exception ex) {
			s_log.error("Failed fetching active accounts", ex);
			activeAccounts = -1;
		} finally {
			close(em);
			em = null;
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

		EntityManager em = null;
		try {
			em = open();
			begin(em);
			em.persist(acc);
			commit(em);
		} catch (Exception ex) {
			s_log.error("Failed adding an account", ex);
			rollback(em);
		} finally {
			close(em);
			em = null;
		}
	}

	@Override
	public void addAccounts(Iterable<Account> accs) {

		EntityManager em = null;
		try {
			em = open();
			begin(em);

			for (Account acc : accs) {
				em.merge(acc);
			}

			commit(em);
		} catch (Exception ex) {
			s_log.error("Failed adding an account", ex);
			rollback(em);
		} finally {
			close(em);
			em = null;
		}
	}

	@Override
	public boolean removeAccount(Account acc) {

		boolean removed = false;

		EntityManager em = null;
		try {
			em = open();
			begin(em);
			em.remove(acc);
			commit(em);
			removed = true;
		} catch (Exception ex) {
			s_log.error("Failed removing an account", ex);
			rollback(em);
		} finally {
			close(em);
			em = null;
		}

		return removed;
	}

	@Override
	public Account getAccount(String username) {

		Account act = null;

		EntityManager em = null;
		try {
			em = open();
			Query fetchByNameQuery = em.createNamedQuery("acc_fetchByName");
			fetchByNameQuery.setParameter("name", username);
			act = (Account) fetchByNameQuery.getSingleResult();
		} catch (NoResultException ex) {
			s_log.trace("Failed fetching an account by name: " + username + " (user not found)", ex);
		} catch (Exception ex) {
			s_log.trace("Failed fetching an account by name: " + username, ex);
		} finally {
			close(em);
			em = null;
		}

		return act;
	}

	@Override
	public Account findAccountNoCase(String username) {

		Account act = null;

		EntityManager em = null;
		try {
			em = open();
			Query fetchByLowerNameQuery = em.createNamedQuery("acc_fetchByLowerName");
			fetchByLowerNameQuery.setParameter("lowerName", username.toLowerCase());
			act = (Account) fetchByLowerNameQuery.getSingleResult();
		} catch (Exception ex) {
			s_log.trace("Failed fetching an account by name (case-insensitive)", ex);
		} finally {
			close(em);
			em = null;
		}

		return act;
	}

	@Override
	public Account findAccountByLastIP(String ip) {

		Account act = null;

		EntityManager em = null;
		try {
			em = open();
			Query fetchByLastIpQuery = em.createNamedQuery("acc_fetchByLastIP");
			fetchByLastIpQuery.setParameter("ip", ip);
			act = (Account) fetchByLastIpQuery.getSingleResult();
		} catch (Exception ex) {
			s_log.trace("Failed fetching an account by last ip", ex);
		} finally {
			close(em);
			em = null;
		}

		return act;
	}

	@Override
	public boolean mergeAccountChanges(Account account, String oldName) {

		boolean replaced = false;

		EntityManager em = null;
		try {
			em = open();
			begin(em);
			em.merge(account);
			commit(em);
			replaced = true;
		} catch (Exception ex) {
			s_log.error("Failed replacing an account", ex);
			rollback(em);
		} finally {
			close(em);
			em = null;
		}

		return replaced;
	}

	@Override
	public List<Account> fetchAllAccounts() {

		List<Account> acts = null;

		EntityManager em = null;
		try {
			em = open();
			acts = (List<Account>) (em.createNamedQuery("acc_list").getResultList());
		} catch (Exception ex) {
			s_log.error("Failed fetching all accounts", ex);
		} finally {
			close(em);
			em = null;
		}

		return acts;
	}
}
