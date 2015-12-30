package com.mobiussoftware.smpplb.core;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.pdu.Pdu;
import com.mobiussoftware.smpplb.api.LbClientListener;
import com.mobiussoftware.smpplb.api.LbServerListener;
import com.mobiussoftware.smpplb.impl.BinderRunnable;
import com.mobiussoftware.smpplb.impl.ClientConnectionImpl;
import com.mobiussoftware.smpplb.impl.RemoteServer;
import com.mobiussoftware.smpplb.impl.ServerConnectionImpl;

public class LbDispatcher implements LbClientListener, LbServerListener {

	
	private Map<Long, ServerConnectionImpl> serverSessions = new ConcurrentHashMap<Long, ServerConnectionImpl>();
	private Map<Long, ClientConnectionImpl> clientSessions = new ConcurrentHashMap<Long, ClientConnectionImpl>();
	
	private ScheduledExecutorService reconnectExecutor = Executors.newScheduledThreadPool(2);
	
	private RemoteServer [] remoteServers;
	private AtomicInteger i = new AtomicInteger(0);
	private Properties properties;
	private ScheduledExecutorService monitorExecutor; 
	private ExecutorService handlerService = Executors.newCachedThreadPool();
	private long reconnectPeriod;
	
	public LbDispatcher(Properties properties, ScheduledExecutorService monitorExecutor)
	{
		this.reconnectPeriod = Long.parseLong(properties.getProperty("reconnectPeriod"));
		this.properties = properties;
		this.monitorExecutor = monitorExecutor;
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
 		int serverIndex = i.getAndIncrement();
		serverSessions.put(sessionId,serverConnection);
		SmppSessionConfiguration sessionConfig = serverConnection.getConfig();
		sessionConfig.setHost(remoteServers[serverIndex].getIP());
		sessionConfig.setPort(remoteServers[serverIndex].getPort());
		clientSessions.put(sessionId, new ClientConnectionImpl(sessionId, sessionConfig, this, monitorExecutor, properties, packet, serverIndex));
		handlerService.execute(new BinderRunnable(sessionId, packet, serverSessions, clientSessions, serverIndex, remoteServers));

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
		serverSessions.get(sessionID).sendBindResponse(packet);
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

	@Override
	public void connectionLost(Long sessionId, Pdu packet, int serverIndex) {
		
		serverSessions.get(sessionId).reconnectState(true);
		reconnectExecutor.schedule(new BinderRunnable(sessionId, packet, serverSessions, clientSessions, serverIndex, remoteServers), reconnectPeriod, TimeUnit.MILLISECONDS);
		
	}

	@Override
	public void reconnectSuccesful(Long sessionId) {
		
		serverSessions.get(sessionId).reconnectState(false);
	}


	@Override
	public void checkConnection(Long sessionId) {
		serverSessions.get(sessionId).generateEnquireLink();
		clientSessions.get(sessionId).generateEnquireLink();
		
	}


	@Override
	public void enquireLinkReceivedFromServer(Long sessionId) {
		serverSessions.get(sessionId).restartEnquireTimer();
		
	}

}
