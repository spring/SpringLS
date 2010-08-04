
package com.springrts.chanserv.antispam;


import com.springrts.chanserv.Context;
import java.util.Map.Entry;
import java.util.TimerTask;

/**
 * @author Betalord
 */
class AntiSpamTask extends TimerTask {

	private Context context;

	AntiSpamTask(Context context) {
		this.context = context;
	}

	/** Here we will reduce penalty points for all users */
	@Override
	public void run() {

		if (!context.getChanServ().isConnected()) {
			return;
		}

		synchronized (DefaultAntiSpamSystem.spamRecords) {
			for (Entry<String, SpamRecord> record : DefaultAntiSpamSystem.spamRecords.entrySet()) {
				SpamRecord rec = record.getValue();
				// reduce by 1.0 penalty point each second
				rec.setPenaltyPoints(Math.max(0, rec.getPenaltyPoints() - 1.0));
				if (rec.getPenaltyPoints() == 0) {
					DefaultAntiSpamSystem.spamRecords.remove(record.getKey());
				}
			}
		}
	}
}
