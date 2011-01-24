/*
 * Created on 4. February 2010
 */

package com.springrts.tasserver;


import java.net.InetAddress;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA implementation of a ban entries service.
 * Uses abstracted DB access to store data,
 * see persistence.xml for DB settings.
 *
 * @author hoijui
 */
public class JPABanService implements BanService {

	private static final Logger LOG
			= LoggerFactory.getLogger(JPABanService.class);

	private EntityManagerFactory emf;

	public JPABanService() {
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
			LOG.error("Failed to create an entity manager");
		} else {
			try {
				if (em.isOpen() && em.getTransaction().isActive()) {
					em.getTransaction().rollback();
				} else {
					LOG.error("Failed to rollback a transaction: no active"
							+ " connection or transaction");
				}
			} catch (PersistenceException ex) {
				LOG.error("Failed to rollback a transaction", ex);
			}
		}
	}
	private void close(EntityManager em) {

		if (em == null) {
			LOG.error("Failed to create an entity manager");
		} else {
			try {
				if (em.isOpen()) {
					em.close();
				}
			} catch (IllegalStateException ex) {
				LOG.error("Failed to close an entity manager", ex);
			}
		}
	}

	@Override
	public int getBansSize() {

		EntityManager em = null;
		try {
			em = open();
			long numBans = (Long) (em.createNamedQuery("ban_size")
					.getSingleResult());
			return (int)numBans;
		} catch (Exception ex) {
			LOG.error("Failed fetching number of bans", ex);
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
			em = open();
			long numBans = (Long) (em.createNamedQuery("ban_size_active")
					.getSingleResult());
			activeBans = (int) numBans;
		} catch (Exception ex) {
			LOG.error("Failed fetching number of bans", ex);
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
			em = open();
			begin(em);
			em.persist(ban);
			commit(em);
		} catch (Exception ex) {
			LOG.error("Failed adding a ban", ex);
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
			em = open();
			begin(em);
			em.remove(ban);
			commit(em);
			removed = true;
		} catch (Exception ex) {
			LOG.error("Failed removing a ban", ex);
			rollback(em);
		} finally {
			close(em);
			em = null;
		}

		return removed;
	}

	@Override
	public BanEntry getBanEntry(String username, InetAddress ip, int userId) {

		BanEntry ban = null;

		EntityManager em = null;
		try {
			em = open();
			Query fetchQuery = em.createNamedQuery("ban_fetch");
			fetchQuery.setParameter("username", username);
			fetchQuery.setParameter("ip", Misc.ip2Long(ip));
			fetchQuery.setParameter("userId", userId);
			ban = (BanEntry) fetchQuery.getSingleResult();
		} catch (Exception ex) {
			LOG.trace("Failed fetching a ban", ex);
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
			em = open();
			begin(em);
			em.merge(ban);
			commit(em);
			replaced = true;
		} catch (Exception ex) {
			LOG.error("Failed replacing a ban", ex);
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
			em = open();
			bans = (List<BanEntry>) (em.createNamedQuery("ban_list")
					.getResultList());
		} catch (Exception ex) {
			LOG.error("Failed fetching all bans", ex);
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
			em = open();
			bans = (List<BanEntry>) (em.createNamedQuery("ban_list_active")
					.getResultList());
		} catch (Exception ex) {
			LOG.error("Failed fetching all bans", ex);
			bans = null;
		} finally {
			close(em);
			em = null;
		}

		return bans;
	}
}
