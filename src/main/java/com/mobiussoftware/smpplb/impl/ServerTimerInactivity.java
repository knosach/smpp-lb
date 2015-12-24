package com.mobiussoftware.smpplb.impl;

import com.mobiussoftware.smpplb.api.ServerConnection;


public class ServerTimerInactivity implements Runnable {
		
		ServerConnection server;
		Long sessionId;
		

		public ServerTimerInactivity(ServerConnection server, Long sessionId) 
		{
			this.server = server;
			this.sessionId = sessionId;
		}

		@Override
		public void run() 
		{
			server.inactivityTimeout(sessionId);
		}

	
}
