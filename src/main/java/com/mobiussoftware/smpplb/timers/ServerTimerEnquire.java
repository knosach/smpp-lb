package com.mobiussoftware.smpplb.timers;

import com.mobiussoftware.smpplb.api.ServerConnection;

public class ServerTimerEnquire implements Runnable{
	ServerConnection server;
	Long sessionId;
	

	public ServerTimerEnquire(ServerConnection server, Long sessionId) 
	{
		this.server = server;
		this.sessionId = sessionId;
	}

	@Override
	public void run() 
	{
		server.enquireTimeout(sessionId);
	}
}
