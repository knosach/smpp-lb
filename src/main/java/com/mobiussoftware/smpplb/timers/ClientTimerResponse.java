package com.mobiussoftware.smpplb.timers;

import com.cloudhopper.smpp.pdu.Pdu;
import com.mobiussoftware.smpplb.api.ClientConnection;

public class ClientTimerResponse implements Runnable {
	
	ClientConnection client;
	Pdu packet;
	

	public ClientTimerResponse(ClientConnection client, Pdu packet) 
	{
		this.client = client;
		this.packet = packet;
	}

	@Override
	public void run() 
	{
		client.requestTimeout(packet);
	}

}
