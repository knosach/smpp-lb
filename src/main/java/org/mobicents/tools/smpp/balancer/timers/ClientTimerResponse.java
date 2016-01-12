package org.mobicents.tools.smpp.balancer.timers;

import org.mobicents.tools.smpp.balancer.api.ClientConnection;

import com.cloudhopper.smpp.pdu.Pdu;

public class ClientTimerResponse implements CancellableRunnable {
	
	ClientConnection client;
	private Boolean cancelled=false;
	Pdu packet;	

	public ClientTimerResponse(ClientConnection client, Pdu packet) 
	{
		this.client = client;
		this.packet = packet;
	}

	@Override
	public void run() 
	{
		if(!cancelled)
			client.requestTimeout(packet);
	}

	@Override
	public void cancel() 
	{
		this.cancelled=true;
	}
}
