package com.mobiussoftware.smpplb.timers;

import com.mobiussoftware.smpplb.api.ServerConnection;

public class ServerTimerConnection implements Runnable{
	ServerConnection server;
	Long sessionId;

	public ServerTimerConnection(ServerConnection server, Long sessionId){
		this.server = server;
		this.sessionId = sessionId;
	}
	@Override
	public void run() {
		
		server.connectionTimeout(sessionId);
		
	}
	

	
}
