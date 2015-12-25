package com.mobiussoftware.smpplb.core;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.tlv.Tlv;
import com.mobiussoftware.smpplb.api.LbClientListener;
import com.mobiussoftware.smpplb.api.LbServerListener;
import com.mobiussoftware.smpplb.impl.ClientConnectionImpl;
import com.mobiussoftware.smpplb.impl.ServerConnectionImpl;

public class LbDispatcher implements LbClientListener, LbServerListener {

	
	private Map<Long, ServerConnectionImpl> serverSessions = new ConcurrentHashMap<Long, ServerConnectionImpl>();
	private Map<Long, ClientConnectionImpl> clientSessions = new ConcurrentHashMap<Long, ClientConnectionImpl>();
	
	private RemoteServer [] remoteServers;
	private AtomicInteger i = new AtomicInteger(0);
	private Properties properties;
	private ScheduledExecutorService monitorExecutor; 
	private ExecutorService handlerService = Executors.newCachedThreadPool();
	
	public LbDispatcher(Properties properties, ScheduledExecutorService monitorExecutor)
	{
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
		clientSessions.put(sessionId, new ClientConnectionImpl(sessionId, sessionConfig, this, monitorExecutor, properties));
		handlerService.execute(new BinderRunnable(packet, clientSessions.get(sessionId)));
		
		
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
	
	private class BinderRunnable implements Runnable 
	{
		
		Pdu packet;
		ClientConnectionImpl client;
		
		public BinderRunnable(Pdu packet, ClientConnectionImpl client) 
		{
			
			this.packet = packet;
			this.client = client;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public void run() 
		{

			boolean connectSuccesful = true;
				while(!client.connect())
				{

					int serverIndex = i.getAndIncrement();
					if(serverIndex == remoteServers.length)
					{
						connectSuccesful = false;
						break;
					}
					client.getConfig().setHost(remoteServers[serverIndex].getIP());
					client.getConfig().setPort(remoteServers[serverIndex].getPort());

				}
				
				if(connectSuccesful){
				
		        	client.bind();
				}else{
					BaseBindResp bindResponse = (BaseBindResp) ((BaseBind) packet).createResponse();
					
					bindResponse.setCommandStatus(SmppConstants.STATUS_SYSERR);
					bindResponse.setSystemId(client.getConfig().getSystemId());
					// if the server supports an SMPP server version >= 3.4 AND the bind request
			        // included an interface version >= 3.4, include an optional parameter with configured sc_interface_version TLV
					if (client.getConfig().getInterfaceVersion() >= SmppConstants.VERSION_3_4 && ((BaseBind) packet).getInterfaceVersion() >= SmppConstants.VERSION_3_4) 
					{
			            Tlv scInterfaceVersion = new Tlv(SmppConstants.TAG_SC_INTERFACE_VERSION, new byte[] { client.getConfig().getInterfaceVersion() });
			            bindResponse.addOptionalParameter(scInterfaceVersion);
			        }

					serverSessions.get(client.getSessionId()).sendBindResponse(bindResponse);
					clientSessions.remove(client.getSessionId());
					serverSessions.remove(client.getSessionId());
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
