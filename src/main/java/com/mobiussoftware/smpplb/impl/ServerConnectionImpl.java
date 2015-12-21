package com.mobiussoftware.smpplb.impl;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.GenericNack;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoder;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoderContext;
import com.cloudhopper.smpp.transcoder.PduTranscoder;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.mobiussoftware.smpplb.api.LbServerListener;
import com.mobiussoftware.smpplb.api.ServerConnection;

public class ServerConnectionImpl implements ServerConnection {
	
	private static final Logger logger = LoggerFactory.getLogger(ServerConnectionImpl.class);
	
	private ServerState serverState = ServerState.OPEN;
	private LbServerListener lbServerListener;
	private Long sessionId;
    private SmppSessionConfiguration config = new SmppSessionConfiguration();
	private Channel channel;
	private final PduTranscoder transcoder;
    
    public ServerConnectionImpl(Long sessionId, Channel channel, LbServerListener lbServerListener)
    {
    	
    	this.lbServerListener = lbServerListener;
    	this.channel = channel;
    	this.sessionId = sessionId;
    	this.transcoder = new DefaultPduTranscoder(new DefaultPduTranscoderContext());
    	
    }
    
    public SmppSessionConfiguration getConfig() 
    {
		return config;
	}

	public enum ServerState 
    {    	
    	OPEN, BINDING, BOUND, UNBINDING, CLOSED    	
    }
	
	@SuppressWarnings("rawtypes")
	@Override
	public void packetReceived(Pdu packet) 
	{
		switch(serverState)
		{
							
			case OPEN:
				//can get SmppConstants.CMD_ID_GENERIC_NACK
				Boolean correctPacket=false;
				
				switch(packet.getCommandId()){
				
				case SmppConstants.CMD_ID_BIND_RECEIVER:
					correctPacket=true;
					config.setType(SmppBindType.RECEIVER);
					break;
				case SmppConstants.CMD_ID_BIND_TRANSCEIVER:
					correctPacket=true;
					config.setType(SmppBindType.TRANSCEIVER);
					break;
				case SmppConstants.CMD_ID_BIND_TRANSMITTER:
					correctPacket=true;
					config.setType(SmppBindType.TRANSMITTER);
					break;
				}
				
				if(!correctPacket)
				{
					//send genericNack to ESME because of incorrect SMPP bind command
					logger.error("Unable to convert a BaseBind request into SmppSessionConfiguration");
					sendGenericNack(packet);
					serverState = ServerState.CLOSED;
				}
				else
				{
					BaseBind bindRequest = (BaseBind)packet;
					config.setName("LoadBalancerSession." + bindRequest.getSystemId() + "." + bindRequest.getSystemType());
					config.setSystemId(bindRequest.getSystemId());
					config.setPassword(bindRequest.getPassword());
					config.setSystemType(bindRequest.getSystemType());
					config.setAddressRange(bindRequest.getAddressRange());
					config.setInterfaceVersion(bindRequest.getInterfaceVersion());
					//send correct bind to server
					lbServerListener.bindRequested(sessionId, this);
					
					serverState = ServerState.BINDING;
					
				}
				break;
			case BINDING:
				logger.error("Server received packet in incorrect state (BINDING)");
				break;
			case BOUND:
				correctPacket=false;
				switch(packet.getCommandId())
				{
			        case SmppConstants.CMD_ID_UNBIND:
			        	correctPacket=true;
			        	//send unbind to server
						lbServerListener.unbindRequested(sessionId, packet);
						serverState = ServerState.UNBINDING;
			        	break;
			        case SmppConstants.CMD_ID_CANCEL_SM:
			        case SmppConstants.CMD_ID_DATA_SM:
			        case SmppConstants.CMD_ID_QUERY_SM:
			        case SmppConstants.CMD_ID_REPLACE_SM:
			        case SmppConstants.CMD_ID_SUBMIT_SM:
			        case SmppConstants.CMD_ID_SUBMIT_MULTI:
			        case SmppConstants.CMD_ID_GENERIC_NACK:
			        case SmppConstants.CMD_ID_ENQUIRE_LINK:
			        case SmppConstants.CMD_ID_ENQUIRE_LINK_RESP:
			        	 correctPacket=true;
			        	logger.info("Get SMPP request");
			        	//send correct SMPP packet to server
						lbServerListener.smppEntityRequested(sessionId, packet);
						break;
				}
				
				if(!correctPacket)
				{
					//send genericNack to ESME because of incorrect SMPP message
					sendGenericNack(packet);
					
				}

				break;
			case UNBINDING:
				logger.error("Server received packet in incorrect state (UNBINDING)");
				break;
			case CLOSED:
				logger.error("Server received packet in incorrect state (CLOSED)");
				break;
		}		
		
	}
	@Override
	public void sendBindResponse(Pdu packet){
		
		
        ChannelBuffer buffer = null;
		try {
			buffer = transcoder.encode(packet);
			
		} catch (UnrecoverablePduException | RecoverablePduException e) {
			logger.error("Encode error: ", e);
		}
		channel.write(buffer);
		serverState = ServerState.BOUND;
		
	}
	
	@Override
	public void sendUnbindResponse(Pdu packet){

        ChannelBuffer buffer = null;
		try {
			buffer = transcoder.encode(packet);
			
		} catch (UnrecoverablePduException | RecoverablePduException e) {
			logger.error("Encode error: ", e);
		}
		channel.write(buffer);
		serverState = ServerState.CLOSED;
		
	}
	@Override
	public void sendResponse(Pdu packet){
		
        ChannelBuffer buffer = null;
		try {
			buffer = transcoder.encode(packet);
			
		} catch (UnrecoverablePduException | RecoverablePduException e) {
			logger.error("Encode error: ", e);
		}
		channel.write(buffer);

	}
	
	private void sendGenericNack(Pdu packet){
		GenericNack genericNack = new GenericNack();
		genericNack.setSequenceNumber(packet.getSequenceNumber());
		genericNack.setCommandStatus(SmppConstants.STATUS_INVCMDID);
		ChannelBuffer buffer = null;
		try {
			buffer = transcoder.encode(genericNack);
			
		} catch (UnrecoverablePduException | RecoverablePduException e) {
			logger.error("Encode error: ", e);
		}
		channel.write(buffer);
		
	}

}
