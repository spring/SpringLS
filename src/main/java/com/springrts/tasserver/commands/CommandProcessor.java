package com.springrts.tasserver.commands;


import com.springrts.tasserver.Client;
import com.springrts.tasserver.ContextReceiver;
import java.util.List;

/**
 * A Command processor is responsible to handle a single type of command,
 * specified by {@link SupportedCommand}.
 * @author hoijui
 */
public interface CommandProcessor extends ContextReceiver {

	/**
	 * Process one call of the command.
	 * This is invoked whenever a command with the name specified
	 * in <code>SupportedCommand</code> is received from a client.
	 * @param args arguments to the command, this does not include
	 *             the command name its self
	 * @return true, if the command was valid and successfully executed
	 */
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException;
}
