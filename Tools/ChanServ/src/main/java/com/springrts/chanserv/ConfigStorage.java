
package com.springrts.chanserv;

/**
 * @author hoijui
 */
public interface ConfigStorage {

	public void loadConfig(String fileName);

	public void saveConfig(String fileName);
}
