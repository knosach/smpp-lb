package com.mobiussoftware.smpplb.impl;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.tlv.Tlv;

public class BinderRunnable implements Runnable 
{
	private AtomicInteger i;
	private Pdu packet;
	private ClientConnectionImpl client;
	
	private RemoteServer [] remoteServers;
	private Map<Long, ServerConnectionImpl> serverSessions;
	private Map<Long, ClientConnectionImpl> clientSessions;
	private Long sessionId;
	
	public BinderRunnable(Long sessionId, Pdu packet, Map<Long, ServerConnectionImpl> serverSessions, Map<Long, ClientConnectionImpl> clientSessions, int serverIndex, RemoteServer [] remoteServers) 
	{
		this.sessionId = sessionId;
		this.packet = packet;
		this.client = clientSessions.get(sessionId);
		this.i = new AtomicInteger(serverIndex);
		this.remoteServers = remoteServers;
		this.serverSessions = serverSessions;
		this.clientSessions = clientSessions;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void run() 
	{

		boolean connectSuccesful = true;
			while(!client.connect())
			{

				
				int serverIndex = i.incrementAndGet();
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
				BaseBindResp bindResponse = null;
				if(packet instanceof BaseBind)
				{
					
				bindResponse = (BaseBindResp) ((BaseBind) packet).createResponse();
				
				}
				else
				{ 
					
				bindResponse = (BaseBindResp) packet;
				
				}
				bindResponse.setCommandStatus(SmppConstants.STATUS_SYSERR);
				
				bindResponse.setSystemId(client.getConfig().getSystemId());
				
				// if the server supports an SMPP server version >= 3.4 AND the bind request
		        // included an interface version >= 3.4, include an optional parameter with configured sc_interface_version TLV
				if (packet instanceof BaseBind &&client.getConfig().getInterfaceVersion() >= SmppConstants.VERSION_3_4 &&  ((BaseBind) packet).getInterfaceVersion() >= SmppConstants.VERSION_3_4) 
				{
					
		            Tlv scInterfaceVersion = new Tlv(SmppConstants.TAG_SC_INTERFACE_VERSION, new byte[] { client.getConfig().getInterfaceVersion() });
		            
		            bindResponse.addOptionalParameter(scInterfaceVersion);
		        }

				//must send bind response
				
				serverSessions.get(sessionId).sendBindResponse(bindResponse);
				clientSessions.remove(sessionId);
				serverSessions.remove(sessionId);
			}

	}
}
