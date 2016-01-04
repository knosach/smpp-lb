package com.mobiussoftware.smpplb.timers;

import com.mobiussoftware.smpplb.api.ServerConnection;

public class ServerTimerConnectionCheck implements Runnable{
	ServerConnection server;
	Long sessionId;
	public ServerTimerConnectionCheck(ServerConnection server, Long sessionId){
		this.server = server;
		this.sessionId = sessionId;

	}
	@Override
	public void run() {
		
		server.connectionCheck(sessionId);
		
	}

}
