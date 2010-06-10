package com.springrts.tasserver.commands;


import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Describes a command sent from the client to the server.
 * Each {@link CommandProcessor} implementation (class) has to be
 * annotated with this.
 * @author hoijui
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SupportedCommand {
	/** Name of the supported command */
	public String value();
}
