package org.mobicents.tools.smpp.balancer.timers;

import org.mobicents.tools.smpp.balancer.api.ServerConnection;

public class ServerTimerConnectionCheck implements CancellableRunnable
{
	ServerConnection server;
	Long sessionId;
	private Boolean cancelled=false;
	
	public ServerTimerConnectionCheck(ServerConnection server, Long sessionId){
		this.server = server;
		this.sessionId = sessionId;

	}
	@Override
	public void run() 
	{
		if(!cancelled)
			server.connectionCheck(sessionId);		
	}

	@Override
	public void cancel() 
	{
		this.cancelled=true;
	}
}
