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

	private EntityManager em = null;
	private Query q_size = null;
	private Query q_size_active = null;
	private Query q_list = null;
	private Query q_list_active = null;
	private Query q_fetch = null;

	public JPABanService() {

		EntityManagerFactory emf = Persistence.createEntityManagerFactory("tasserver");
		em = emf.createEntityManager();

		q_size        = em.createQuery("SELECT count(b.id) FROM BanEntry b");
		q_size_active = em.createQuery("SELECT count(b.id) FROM BanEntry b WHERE ((b.enabled = TRUE) AND (b.expireDate IS NULL OR b.expireDate > CURRENT_TIMESTAMP))");
		q_list        = em.createQuery("SELECT b FROM BanEntry b");
		q_list_active = em.createQuery("SELECT b FROM BanEntry b WHERE ((b.enabled = TRUE) AND (b.expireDate IS NULL OR b.expireDate > CURRENT_TIMESTAMP))");
		q_fetch       = em.createQuery("SELECT b FROM BanEntry b WHERE ((b.username = :username) OR ((b.ipStart <= :ip) AND (b.ipStart >= :ip)) OR (b.userId >= :userId))");
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
	public int getBansSize() {

		try {
			begin();
			long numBans = (Long) (q_size.getSingleResult());
			commit();
			return (int)numBans;
		} catch (Exception ex) {
			s_log.error("Failed fetching number of bans", ex);
		}

		return -1;
	}

	@Override
	public int getActiveBansSize() {

		try {
			begin();
			long numBans = (Long) (q_size_active.getSingleResult());
			commit();
			return (int)numBans;
		} catch (Exception ex) {
			s_log.error("Failed fetching number of bans", ex);
		}

		return -1;
	}

	@Override
	public void addBanEntry(BanEntry ban) {

		try {
			begin();
			em.persist(ban);
			commit();
		} catch (Exception ex) {
			s_log.error("Failed adding a ban", ex);
			rollback();
		}
	}

	@Override
	public boolean removeBanEntry(BanEntry ban) {

		boolean removed = false;

		try {
			begin();
			em.remove(ban);
			commit();
			removed = true;
		} catch (Exception ex) {
			s_log.error("Failed removing a ban", ex);
			rollback();
		}

		return removed;
	}

	@Override
	public BanEntry getBanEntry(String username, long IP, int userID) {

		BanEntry ban = null;

		try {
			begin();
			q_fetch.setParameter("username", username);
			q_fetch.setParameter("ip", IP);
			q_fetch.setParameter("userId", userID);
			ban = (BanEntry) q_fetch.getSingleResult();
			commit();
		} catch (Exception ex) {
			s_log.trace("Failed fetching a ban", ex);
			ban = null;
		}

		return ban;
	}

	@Override
	public boolean mergeBanEntryChanges(BanEntry ban) {

		boolean replaced = false;

		try {
			begin();
			em.merge(ban);
			commit();
			replaced = true;
		} catch (Exception ex) {
			s_log.error("Failed replacing a ban", ex);
			rollback();
		}

		return replaced;
	}

	@Override
	public List<BanEntry> fetchAllBanEntries() {

		List<BanEntry> bans = null;

		try {
			begin();
			bans = (List<BanEntry>) (q_list.getResultList());
			commit();
		} catch (Exception ex) {
			s_log.error("Failed fetching all bans", ex);
			bans = null;
		}

		return bans;
	}

	@Override
	public List<BanEntry> fetchActiveBanEntries() {

		List<BanEntry> bans = null;

		try {
			begin();
			bans = (List<BanEntry>) (q_list_active.getResultList());
			commit();
		} catch (Exception ex) {
			s_log.error("Failed fetching all bans", ex);
			bans = null;
		}

		return bans;
	}
}
