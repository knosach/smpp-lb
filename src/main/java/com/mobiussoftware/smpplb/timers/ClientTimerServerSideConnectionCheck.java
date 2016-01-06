package com.mobiussoftware.smpplb.timers;

import com.mobiussoftware.smpplb.api.ClientConnection;

public class ClientTimerServerSideConnectionCheck implements Runnable{
	ClientConnection client;
	Long sessionId;
	public ClientTimerServerSideConnectionCheck(ClientConnection client, Long sessionId){
		this.client = client;
		this.sessionId = sessionId;

	}
	@Override
	public void run() {
		
		client.connectionCheckServerSide(sessionId);
		
	}

}
