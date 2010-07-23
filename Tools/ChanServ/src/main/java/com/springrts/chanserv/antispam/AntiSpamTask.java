
package com.springrts.chanserv.antispam;


import com.springrts.chanserv.ChanServ;
import java.util.Map.Entry;
import java.util.TimerTask;

/**
 * @author Betalord
 */
class AntiSpamTask extends TimerTask {

	/** Here we will reduce penalty points for all users */
	@Override
	public void run() {

		if (!ChanServ.isConnected()) {
			return;
		}

		synchronized (AntiSpamSystem.spamRecords) {
			for (Entry<String, SpamRecord> record : AntiSpamSystem.spamRecords.entrySet()) {
				SpamRecord rec = record.getValue();
				// reduce by 1.0 penalty point each second
				rec.setPenaltyPoints(Math.max(0, rec.getPenaltyPoints() - 1.0));
				if (rec.getPenaltyPoints() == 0) {
					AntiSpamSystem.spamRecords.remove(record.getKey());
				}
			}
		}
	}
}
