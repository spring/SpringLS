/*
 * Created on 4. February 2010
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
 * JPA implementation of a ban entries service.
 * Uses abstracted DB access to store data.
 * @see persistence.xml
 *
 * @author hoijui
 */
public class JPABanService implements BanService {

	private static final Log s_log  = LogFactory.getLog(JPABanService.class);

	private EntityManagerFactory emf;

	public JPABanService() {
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
	public int getBansSize() {

		EntityManager em = null;
		try {
			em = begin();
			long numBans = (Long) (em.createNamedQuery("size").getSingleResult());
			commit(em);
			return (int)numBans;
		} catch (Exception ex) {
			s_log.error("Failed fetching number of bans", ex);
		} finally {
			close(em);
			em = null;
		}

		return -1;
	}

	@Override
	public int getActiveBansSize() {

		int activeBans = -1;

		EntityManager em = null;
		try {
			em = begin();
			long numBans = (Long) (em.createNamedQuery("size_active").getSingleResult());
			commit(em);
			activeBans = (int) numBans;
		} catch (Exception ex) {
			s_log.error("Failed fetching number of bans", ex);
		} finally {
			close(em);
			em = null;
		}

		return activeBans;
	}

	@Override
	public void addBanEntry(BanEntry ban) {

		EntityManager em = null;
		try {
			em = begin();
			em.persist(ban);
			commit(em);
		} catch (Exception ex) {
			s_log.error("Failed adding a ban", ex);
			rollback(em);
		} finally {
			close(em);
			em = null;
		}
	}

	@Override
	public boolean removeBanEntry(BanEntry ban) {

		boolean removed = false;

		EntityManager em = null;
		try {
			em = begin();
			em.remove(ban);
			commit(em);
			removed = true;
		} catch (Exception ex) {
			s_log.error("Failed removing a ban", ex);
			rollback(em);
		} finally {
			close(em);
			em = null;
		}

		return removed;
	}

	@Override
	public BanEntry getBanEntry(String username, long IP, int userID) {

		BanEntry ban = null;

		EntityManager em = null;
		try {
			em = begin();
			Query q_fetch = em.createNamedQuery("fetch");
			q_fetch.setParameter("username", username);
			q_fetch.setParameter("ip", IP);
			q_fetch.setParameter("userId", userID);
			ban = (BanEntry) q_fetch.getSingleResult();
			commit(em);
		} catch (Exception ex) {
			s_log.trace("Failed fetching a ban", ex);
			ban = null;
		} finally {
			close(em);
			em = null;
		}

		return ban;
	}

	@Override
	public boolean mergeBanEntryChanges(BanEntry ban) {

		boolean replaced = false;

		EntityManager em = null;
		try {
			em = begin();
			em.merge(ban);
			commit(em);
			replaced = true;
		} catch (Exception ex) {
			s_log.error("Failed replacing a ban", ex);
			rollback(em);
		} finally {
			close(em);
			em = null;
		}

		return replaced;
	}

	@Override
	public List<BanEntry> fetchAllBanEntries() {

		List<BanEntry> bans = null;

		EntityManager em = null;
		try {
			em = begin();
			bans = (List<BanEntry>) (em.createNamedQuery("list").getResultList());
			commit(em);
		} catch (Exception ex) {
			s_log.error("Failed fetching all bans", ex);
			bans = null;
		} finally {
			close(em);
			em = null;
		}

		return bans;
	}

	@Override
	public List<BanEntry> fetchActiveBanEntries() {

		List<BanEntry> bans = null;

		EntityManager em = null;
		try {
			em = begin();
			bans = (List<BanEntry>) (em.createNamedQuery("list_active").getResultList());
			commit(em);
		} catch (Exception ex) {
			s_log.error("Failed fetching all bans", ex);
			bans = null;
		} finally {
			close(em);
			em = null;
		}

		return bans;
	}
}
