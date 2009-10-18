/*
 * Created on 2006.10.15
 */

package com.springrts.tasserver;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Betalord
 * @author hoijui
 */
public abstract class AbstractAccountsService implements AccountsService {

	private static final Log s_log  = LogFactory.getLog(AbstractAccountsService.class);

	@Override
	public boolean addAccountWithCheck(Account acc) {

		if        (Account.isUsernameValid(acc.getName()) != null) {
			return false;
		} else if (Account.isPasswordValid(acc.getPassword()) != null) {
			return false;
		}
		// check for duplicate entries:
		else if (doesAccountExist(acc.getName())) {
			return false;
		}

		addAccount(acc);
		return true;
	}

	@Override
	public boolean removeAccount(String username) {

		Account acc = getAccount(username);
		if (acc == null) {
			return false;
		}
		return removeAccount(acc);
	}

	@Override
	public boolean doesAccountExist(String username) {
		return getAccount(username) != null;
	}
}
