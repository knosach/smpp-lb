package com.mobiussoftware.smpplb.timers;

import com.cloudhopper.smpp.pdu.Pdu;
import com.mobiussoftware.smpplb.api.ServerConnection;

public class ServerTimerResponse implements Runnable {
	
	ServerConnection server;
	Pdu packet;
	

	public ServerTimerResponse(ServerConnection server, Pdu packet) 
	{
		this.server = server;
		this.packet = packet;
	}

	@Override
	public void run() 
	{
		server.requestTimeout(packet);
	}

}
