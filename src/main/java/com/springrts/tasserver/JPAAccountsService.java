/*
 * Created on 2006.10.15
 */

package com.springrts.tasserver;


import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
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


	@Override
	public boolean isReadyToOperate() {
		return true;
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

		int activeAccounts = -1;

		try {
			em.getTransaction().begin();
			final long oneWeekAgo = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 7);
			q_size_active.setParameter("oneWeekAgo", oneWeekAgo);
			activeAccounts = (int) (long) (Long) (q_size_active.getSingleResult());
			em.getTransaction().commit();
		} catch (Exception ex) {
			em.getTransaction().rollback();
			s_log.error("Failed fetching active accounts", ex);
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
			em.getTransaction().begin();
			em.persist(acc);
			em.getTransaction().commit();
		} catch (Exception ex) {
			s_log.error("Failed adding an account", ex);
			em.getTransaction().rollback();
		}
	}

	@Override
	public void addAccounts(Iterable<Account> accs) {

		try {
			em.getTransaction().begin();

			for (Account acc : accs) {
				em.merge(acc);
			}

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
			s_log.error("Failed removing an account", ex);
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
			s_log.trace("Failed fetching an account by name: " + username, ex);
			act = null;
		}

		return act;
	}

	@Override
	public Account findAccountNoCase(String username) {

		Account act = null;

		try {
			em.getTransaction().begin();
			q_fetchByLowerName.setParameter("lowerName", username.toLowerCase());
			act = (Account) q_fetchByLowerName.getSingleResult();
			em.getTransaction().commit();
		} catch (Exception ex) {
			s_log.trace("Failed fetching an account by name (case-insensitive)", ex);
			em.getTransaction().rollback();
			act = null;
		}

		return act;
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
			s_log.error("Failed fetching all accounts", ex);
			acts = null;
		}

		return acts;
	}
}
