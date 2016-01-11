package com.mobiussoftware.smpplb.timers;

import com.cloudhopper.smpp.pdu.Pdu;
import com.mobiussoftware.smpplb.api.ClientConnection;

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
