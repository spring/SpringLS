package com.springrts.tasserver.commands.impl;


import com.springrts.tasserver.Client;
import com.springrts.tasserver.commands.AbstractCommandProcessor;
import com.springrts.tasserver.commands.CommandProcessingException;
import com.springrts.tasserver.commands.CommandProcessor;
import com.springrts.tasserver.commands.SupportedCommand;
import java.util.List;

/**
 * @author hoijui
 */
@SupportedCommand("PING")
public class PingCommandProcessor extends AbstractCommandProcessor {

	public PingCommandProcessor() {
	}

	@Override
	public boolean process(Client client, List<String> args)
			throws CommandProcessingException {
		boolean checksOk = super.process(client, args);
		if (!checksOk) {
			return false;
		}

		client.sendLine("PONG");
		return true;
	}
}
