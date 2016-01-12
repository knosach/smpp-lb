package org.mobicents.tools.smpp.balancer.timers;

import org.mobicents.tools.smpp.balancer.api.ServerConnection;

import com.cloudhopper.smpp.pdu.Pdu;

public class ServerTimerResponse implements CancellableRunnable 
{	
	ServerConnection server;
	Pdu packet;
	private Boolean cancelled=false;	

	public ServerTimerResponse(ServerConnection server, Pdu packet) 
	{
		this.server = server;
		this.packet = packet;
	}

	@Override
	public void run() 
	{
		if(!cancelled)
			server.requestTimeout(packet);
	}

	@Override
	public void cancel() 
	{		
		this.cancelled=true;
	}

}
