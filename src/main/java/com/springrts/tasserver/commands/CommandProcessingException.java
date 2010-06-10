package com.springrts.tasserver.commands;

/**
 * @see CommandProcessor.process()
 * @author hoijui
 */
public class CommandProcessingException extends Exception {

	public CommandProcessingException(String message) {
		super(message);
	}

	public CommandProcessingException(String message, Throwable t) {
		super(message, t);
	}
}
