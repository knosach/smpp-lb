package com.mobiussoftware.smpplb.timers;

import com.mobiussoftware.smpplb.api.ClientConnection;

public class ClientTimerServerSideConnectionCheck implements CancellableRunnable
{
	ClientConnection client;
	Long sessionId;
	private Boolean cancelled=false;
	public ClientTimerServerSideConnectionCheck(ClientConnection client, Long sessionId)
	{
		this.client = client;
		this.sessionId = sessionId;
	}
	
	public void cancel()
	{
		this.cancelled=true;
	}
	
	@Override
	public void run() 
	{
		if(!cancelled)
			client.connectionCheckServerSide(sessionId);		
	}
}