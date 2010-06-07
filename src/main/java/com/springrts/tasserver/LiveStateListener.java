package com.springrts.tasserver;

public interface LiveStateListener {

	public void starting();
	public void started();

	public void stopping();
	public void stopped();
}
