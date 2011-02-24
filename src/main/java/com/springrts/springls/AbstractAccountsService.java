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

package com.springrts.springls;


/**
 * @author hoijui
 */
public abstract class AbstractAccountsService implements AccountsService {

	private Context context;
	private boolean started;
	private boolean registrationEnabled;


	protected AbstractAccountsService() {

		this.context = null;
		this.started = false;
		this.registrationEnabled = false;
	}


	@Override
	public void receiveContext(Context context) {
		this.context = context;
	}
	protected Context getContext() {
		return context;
	}

	@Override
	public void update() {
		saveAccountsIfNeeded();
	}

	@Override
	public void starting() {}
	@Override
	public void started() {
		started = true;
	}

	@Override
	public void stopping() {

		if (started) {
			// We need to check if initialization has completed,
			// so that we do not save an empty accounts arra,
			// and therefore overwrite actual accounts
			saveAccounts(true);
		}
	}
	@Override
	public void stopped() {
		started = false;
	}

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

	@Override
	public boolean isRegistrationEnabled() {
		return registrationEnabled;
	}

	@Override
	public boolean setRegistrationEnabled(boolean registrationEnabled) {
		this.registrationEnabled = registrationEnabled;
		return true;
	}

	@Override
	public Account verifyLogin(String username, String password) {

		Account account = null;

		account = getAccount(username);
		if ((account != null) && !account.getPassword().equals(password)) {
			account = null;
		}

		return account;
	}
}
