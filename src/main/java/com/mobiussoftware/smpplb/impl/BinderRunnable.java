package com.mobiussoftware.smpplb.impl;

import java.util.Map;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.GenericNack;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.tlv.Tlv;
import com.mobiussoftware.smpplb.impl.ClientConnectionImpl.ClientState;

public class BinderRunnable implements Runnable 
{
	private int index;
	private Pdu packet;
	private ClientConnectionImpl client;
	private RemoteServer [] remoteServers;
	private Map<Long, ServerConnectionImpl> serverSessions;
	private Map<Long, ClientConnectionImpl> clientSessions;
	private Long sessionId;
	private int firstServer;
	
	public BinderRunnable(Long sessionId, Pdu packet, Map<Long, ServerConnectionImpl> serverSessions, Map<Long, ClientConnectionImpl> clientSessions, int serverIndex, RemoteServer [] remoteServers) 
	{
		this.sessionId = sessionId;
		this.packet = packet;
		this.client = clientSessions.get(sessionId);
		this.firstServer = serverIndex;
		this.index = serverIndex;
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
				if(++index == firstServer)
				{
					connectSuccesful = false;
					break;
				}
				if(index==remoteServers.length) index = 0;
				client.getConfig().setHost(remoteServers[index].getIP());
				client.getConfig().setPort(remoteServers[index].getPort());
			}
			
			if(connectSuccesful){
				
	        	client.bind();
	        	
			}else{

				BaseBindResp bindResponse = (BaseBindResp) ((BaseBind) packet).createResponse();
				bindResponse.setCommandStatus(SmppConstants.STATUS_SYSERR);
				bindResponse.setSystemId(client.getConfig().getSystemId());
				// if the server supports an SMPP server version >= 3.4 AND the bind request
		        // included an interface version >= 3.4, include an optional parameter with configured sc_interface_version TLV
				if (client.getConfig().getInterfaceVersion() >= SmppConstants.VERSION_3_4 &&  ((BaseBind) packet).getInterfaceVersion() >= SmppConstants.VERSION_3_4) 
				{
		            Tlv scInterfaceVersion = new Tlv(SmppConstants.TAG_SC_INTERFACE_VERSION, new byte[] { client.getConfig().getInterfaceVersion() });
		            bindResponse.addOptionalParameter(scInterfaceVersion);
		        }
				
				//must send bind response
				
				if(client.getClientState() == ClientState.INITIAL)
				{
				serverSessions.get(sessionId).sendBindResponse(bindResponse);
				}else
				{
					GenericNack genericNack = new GenericNack();
					genericNack.setSequenceNumber(packet.getSequenceNumber());
					genericNack.setCommandStatus(SmppConstants.STATUS_SYSERR);
					/////check state in server impl!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
					serverSessions.get(sessionId).sendResponse(genericNack);
				}
				clientSessions.remove(sessionId);
				serverSessions.remove(sessionId);
			}

	}
}
