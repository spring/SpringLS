/*
	Copyright (c) 2010 Robin Vobruba <hoijui.quaero@gmail.com>

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.springrts.springls.accounts;


import com.springrts.springls.Account;
import java.net.InetAddress;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JPA implementation of an accounts service.
 * Uses abstracted DB access to store data,
 * see persistence.xml for DB settings.
 *
 * @author hoijui
 */
public class JPAAccountsService extends AbstractAccountsService {

	private static final Logger LOG
			= LoggerFactory.getLogger(JPAAccountsService.class);
	private static final long DAY = 1000L * 60L * 60L * 24L;

	private EntityManagerFactory emf = null;


	public JPAAccountsService() {

		try {
			emf = Persistence.createEntityManagerFactory("springls");
		} catch (PersistenceException ex) {
			LOG.error("Failed to initialize database storage", ex);
		}
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
	public boolean isReadyToOperate() {
		return (emf != null);
	}

	@Override
	public int getAccountsSize() {

		int accounts = -1;

		EntityManager em = null;
		try {
			em = open();
			long numAccounts = (Long) (em.createNamedQuery("acc_size")
					.getSingleResult());
			accounts = (int) numAccounts;
		} catch (Exception ex) {
			LOG.error("Failed fetching number of accounts", ex);
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
			final long oneWeekAgo = System.currentTimeMillis() - (DAY * 7);
			activeSizeQuery.setParameter("oneWeekAgo", oneWeekAgo);
			activeAccounts = (int) (long) (Long)
					activeSizeQuery.getSingleResult();
		} catch (Exception ex) {
			LOG.error("Failed fetching active accounts", ex);
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
			LOG.error("Failed adding an account", ex);
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
			LOG.error("Failed adding an account", ex);
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
			LOG.error("Failed removing an account", ex);
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
			LOG.trace("Failed fetching an account by name: " + username
					+ " (user not found)", ex);
		} catch (Exception ex) {
			LOG.trace("Failed fetching an account by name: " + username, ex);
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
			Query fetchByLowerNameQuery = em.createNamedQuery(
					"acc_fetchByLowerName");
			fetchByLowerNameQuery.setParameter("lowerName",
					username.toLowerCase());
			act = (Account) fetchByLowerNameQuery.getSingleResult();
		} catch (Exception ex) {
			LOG.trace("Failed fetching an account by name (case-insensitive)",
					ex);
		} finally {
			close(em);
			em = null;
		}

		return act;
	}

	@Override
	public Account findAccountByLastIP(InetAddress ip) {

		Account act = null;

		EntityManager em = null;
		try {
			em = open();
			Query fetchByLastIpQuery = em.createNamedQuery("acc_fetchByLastIP");
			fetchByLastIpQuery.setParameter("ip", ip.getHostAddress());
			act = (Account) fetchByLastIpQuery.getSingleResult();
		} catch (Exception ex) {
			LOG.trace("Failed fetching an account by last IP", ex);
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
			LOG.error("Failed replacing an account", ex);
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
			acts = (List<Account>) (em.createNamedQuery("acc_list")
					.getResultList());
		} catch (Exception ex) {
			LOG.error("Failed fetching all accounts", ex);
		} finally {
			close(em);
			em = null;
		}

		return acts;
	}
}
