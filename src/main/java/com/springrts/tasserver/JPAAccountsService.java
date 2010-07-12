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

	private EntityManager begin() {

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		return em;
	}
	private void commit(EntityManager em) {
		em.getTransaction().commit();
	}
	private void rollback(EntityManager em) {

		if (em == null) {
			s_log.error("Failed to create an entity manager");
		} else {
			try {
				if (em.getTransaction().isActive()) {
					em.getTransaction().rollback();
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
			em = begin();
			long numAccounts = (Long) (em.createNamedQuery("acc_size").getSingleResult());
			commit(em);
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
			em = begin();
			Query q_size_active = em.createNamedQuery("acc_size_active");
			final long oneWeekAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 7);
			q_size_active.setParameter("oneWeekAgo", oneWeekAgo);
			activeAccounts = (int) (long) (Long) (q_size_active.getSingleResult());
			commit(em);
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
			em = begin();
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
			em = begin();

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
			em = begin();
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
			em = begin();
			Query q_fetchByName = em.createNamedQuery("acc_fetchByName");
			q_fetchByName.setParameter("name", username);
			act = (Account) q_fetchByName.getSingleResult();
			commit(em);
		} catch (NoResultException ex) {
			s_log.trace("Failed fetching an account by name: " + username + " (user not found)", ex);
			act = null;
		} catch (Exception ex) {
			s_log.trace("Failed fetching an account by name: " + username, ex);
			act = null;
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
			em = begin();
			Query q_fetchByLowerName = em.createNamedQuery("acc_fetchByLowerName");
			q_fetchByLowerName.setParameter("lowerName", username.toLowerCase());
			act = (Account) q_fetchByLowerName.getSingleResult();
			commit(em);
		} catch (Exception ex) {
			s_log.trace("Failed fetching an account by name (case-insensitive)", ex);
			act = null;
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
			em = begin();
			Query q_fetchByLastIP = em.createNamedQuery("acc_fetchByLastIP");
			q_fetchByLastIP.setParameter("ip", ip);
			act = (Account) q_fetchByLastIP.getSingleResult();
			commit(em);
		} catch (Exception ex) {
			s_log.trace("Failed fetching an account by last ip", ex);
			act = null;
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
			em = begin();
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
			em = begin();
			acts = (List<Account>) (em.createNamedQuery("acc_list").getResultList());
			commit(em);
		} catch (Exception ex) {
			s_log.error("Failed fetching all accounts", ex);
			acts = null;
		} finally {
			close(em);
			em = null;
		}

		return acts;
	}
}
