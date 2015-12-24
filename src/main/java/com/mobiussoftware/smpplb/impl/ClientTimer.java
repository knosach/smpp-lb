package com.mobiussoftware.smpplb.impl;

import com.cloudhopper.smpp.pdu.Pdu;
import com.mobiussoftware.smpplb.api.ClientConnection;

public class ClientTimer implements Runnable {
	
	ClientConnection client;
	Pdu packet;
	

	public ClientTimer(ClientConnection client, Pdu packet) 
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
