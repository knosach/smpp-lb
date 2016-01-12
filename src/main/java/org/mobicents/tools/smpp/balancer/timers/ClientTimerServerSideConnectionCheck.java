package org.mobicents.tools.smpp.balancer.timers;

import org.mobicents.tools.smpp.balancer.api.ClientConnection;

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