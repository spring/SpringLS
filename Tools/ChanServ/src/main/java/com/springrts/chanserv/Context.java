
package com.springrts.chanserv;

import com.springrts.chanserv.antispam.AntiSpamSystem;

/**
 * @author hoijui
 */
public class Context {

	private ChanServ chanServ;
	private Configuration configuration;
	private ConfigStorage configStorage;
	private AntiSpamSystem antiSpamSystem;

	public ChanServ getChanServ() {
		return chanServ;
	}

	public void setChanServ(ChanServ chanServ) {
		this.chanServ = chanServ;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public ConfigStorage getConfigStorage() {
		return configStorage;
	}

	public void setConfigStorage(ConfigStorage configStorage) {
		this.configStorage = configStorage;
	}

	public AntiSpamSystem getAntiSpamSystem() {
		return antiSpamSystem;
	}

	public void setAntiSpamSystem(AntiSpamSystem antiSpamSystem) {
		this.antiSpamSystem = antiSpamSystem;
	}
}
