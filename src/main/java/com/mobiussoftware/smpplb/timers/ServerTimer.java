package com.mobiussoftware.smpplb.timers;

import com.cloudhopper.smpp.pdu.Pdu;
import com.mobiussoftware.smpplb.api.ServerConnection;

public class ServerTimer implements Runnable {
	
	ServerConnection server;
	Pdu packet;
	

	public ServerTimer(ServerConnection server, Pdu packet) 
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
