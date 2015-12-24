package com.mobiussoftware.smpplb.core;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.pdu.Pdu;
import com.mobiussoftware.smpplb.api.LbClientListener;
import com.mobiussoftware.smpplb.api.LbServerListener;
import com.mobiussoftware.smpplb.impl.ClientConnectionImpl;
import com.mobiussoftware.smpplb.impl.ServerConnectionImpl;

public class LbDispatcher implements LbClientListener, LbServerListener {

	private static final Logger logger = LoggerFactory.getLogger(LbDispatcher.class);
	
	private Map<Long, ServerConnectionImpl> serverSessions = new ConcurrentHashMap<Long, ServerConnectionImpl>();
	private Map<Long, ClientConnectionImpl> clientSessions = new ConcurrentHashMap<Long, ClientConnectionImpl>();
	
	private RemoteServer [] remoteServers;
	private AtomicInteger i = new AtomicInteger(0);
	private long timeoutResponse;
	private ScheduledExecutorService monitorExecutor; 
	private ExecutorService handlerService = Executors.newCachedThreadPool();
	
	public LbDispatcher(Properties properties, ScheduledExecutorService monitorExecutor)
	{
		this.monitorExecutor = monitorExecutor;
		this.timeoutResponse = Long.parseLong(properties.getProperty("timeoutResponse"));
		
		String [] s = properties.getProperty("remoteServers").split(",");
		remoteServers = new RemoteServer[s.length];
		String [] sTmp = new String[2];
		for(int i = 0; i < s.length; i++)
		{
			sTmp = s[i].split(":");
			remoteServers[i] = new RemoteServer(sTmp[0].trim(),Integer.parseInt(sTmp[1].trim()));
			System.out.println(remoteServers[i]);
		}
	}

	@Override
	public void bindRequested(Long sessionId, ServerConnectionImpl serverConnection, Pdu packet)  
	{
		//Round-robin
		
 		i.compareAndSet(remoteServers.length, 0);
		serverSessions.put(sessionId,serverConnection);
		SmppSessionConfiguration sessionConfig = serverConnection.getConfig();
		sessionConfig.setHost(remoteServers[i.get()].getIP());
		sessionConfig.setPort(remoteServers[i.getAndIncrement()].getPort());
		clientSessions.put(sessionId, new ClientConnectionImpl(sessionId, sessionConfig, this, monitorExecutor, timeoutResponse));
		handlerService.execute(new BinderRunnable(clientSessions.get(sessionId)));
		
		
	}
		
	@Override
	public void unbindRequested(Long sessionID, Pdu packet) 
	{
		clientSessions.get(sessionID).sendUnbindRequest(packet);
		
	}


	@Override
	public void bindSuccesfull(long sessionID, Pdu packet) 
	{
		serverSessions.get(sessionID).sendBindResponse(packet);
	}
	
	@Override
	public void unbindSuccesfull(long sessionID, Pdu packet) 
	{
		serverSessions.get(sessionID).sendUnbindResponse(packet);
		clientSessions.remove(sessionID);
		serverSessions.remove(sessionID);

	}

	@Override
	public void bindFailed(long sessionID, Pdu packet) 
	{
		serverSessions.get(sessionID).sendUnbindResponse(packet);
		clientSessions.remove(sessionID);
		serverSessions.remove(sessionID);
	}

	@Override
	public void smppEntityRequested(Long sessionID, Pdu packet) 
	{
		clientSessions.get(sessionID).sendSmppRequest(packet);

	}

	@Override
	public void smppEntityResponse(Long sessionID, Pdu packet) 
	{
		serverSessions.get(sessionID).sendResponse(packet);

	}
	
	@Override
	public void smppEntityRequestFromServer(Long sessionId, Pdu packet) {
		
		serverSessions.get(sessionId).sendRequest(packet);
	}
	
	@Override
	public void smppEntityResponseFromClient(Long sessionId, Pdu packet) {
		clientSessions.get(sessionId).sendSmppResponse(packet);
		
	}
	
	public class BinderRunnable implements Runnable 
	{
		ClientConnectionImpl client;

		public BinderRunnable(ClientConnectionImpl client) 
		{
			this.client = client;
		}

		@Override
		public void run() 
		{
				if(client.connect())
				{
		        	client.bind();
				}
				else
				{
					//notify error
					logger.error("Connection failed!");					
				}
		}
	}
	
	 private class RemoteServer{
		private String ip;
		private int port;
		
		RemoteServer(String ip,int port){
			this.ip = ip;
			this.port = port;
		}

		public String getIP() {
			return ip;
		}

		public int getPort() {
			return port;
		}
		public String toString()
		{
			return ip + "  " + port;
			
		}
		
	}

}
