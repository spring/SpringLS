package com.springrts.tasserver;

/**
 * Contains settings specific for one version of the engine.
 */
public class Engine {

	/**
	 * This is sent via welcome message to every new client
	 * which connects to the server.
	 */
	private final String version;

	/**
	 * Max number of teams per game supported by the engine.
	 * Should be equal to maxTeams in springs GlobalConstants.h.
	 */
	private final int maxTeams;

	/**
	 * Max number of ally teams per game supported by the engine.
	 * Should be equal to maxTeams in springs GlobalConstants.h.
	 */
	private final int maxAllyTeams;


	public Engine(String version, int maxTeams, int maxAllyTeams) {

		this.version = version;
		this.maxTeams = maxTeams;
		this.maxAllyTeams = maxAllyTeams;
	}
	public Engine(String version, int maxTeams) {
		this(version, maxTeams, maxTeams);
	}
	public Engine(String version) {
		this(version, 255);
	}
	public Engine() {
		this("*");
	}


	/**
	 * This is sent via welcome message to every new client
	 * which connects to the server.
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Max number of teams per game supported by the engine.
	 * Should be equal to maxTeams in springs GlobalConstants.h.
	 * @return the maxTeams
	 */
	public int getMaxTeams() {
		return maxTeams;
	}

	/**
	 * Max number of ally teams per game supported by the engine.
	 * Should be equal to maxTeams in springs GlobalConstants.h.
	 * @return the maxAllyTeams
	 */
	public int getMaxAllyTeams() {
		return maxAllyTeams;
	}
}
