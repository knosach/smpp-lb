package com.mobiussoftware.smpplb.impl;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.GenericNack;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoder;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoderContext;
import com.cloudhopper.smpp.transcoder.PduTranscoder;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.mobiussoftware.smpplb.api.LbServerListener;
import com.mobiussoftware.smpplb.api.ServerConnection;
import com.mobiussoftware.smpplb.timers.ServerTimer;
import com.mobiussoftware.smpplb.timers.ServerTimerConnection;
import com.mobiussoftware.smpplb.timers.ServerTimerEnquire;
import com.mobiussoftware.smpplb.timers.ServerTimerInactivity;
import com.mobiussoftware.smpplb.timers.TimerData;

public class ServerConnectionImpl implements ServerConnection {
	
	private static final Logger logger = LoggerFactory.getLogger(ServerConnectionImpl.class);
	
	private ServerState serverState = ServerState.OPEN;
	private LbServerListener lbServerListener;
	private Long sessionId;
    private SmppSessionConfiguration config = new SmppSessionConfiguration();
	private Channel channel;
	private final PduTranscoder transcoder;
	private Map<Integer, TimerData> paketMap =  new ConcurrentHashMap <Integer, TimerData>();
	private ScheduledFuture<?> connectionTimer;
	private ScheduledFuture<?> inactivityTimer;
	private ScheduledFuture<?> enquireTimer;
	
	private long timeoutResponse;
	private long timeoutConnection;
	private long timeoutInactivity;
	private long timeoutEnquire;
	private ScheduledExecutorService monitorExecutor;

    
    public ServerConnectionImpl(Long sessionId, Channel channel, LbServerListener lbServerListener, Properties properties, ScheduledExecutorService monitorExecutor)
    {
    	this.lbServerListener = lbServerListener;
    	this.channel = channel;
    	this.sessionId = sessionId;
    	this.transcoder = new DefaultPduTranscoder(new DefaultPduTranscoderContext());
    	this.timeoutResponse = Long.parseLong(properties.getProperty("timeoutResponse"));
    	this.timeoutConnection = Long.parseLong(properties.getProperty("timeoutConnection"));
    	this.timeoutInactivity = Long.parseLong(properties.getProperty("timeoutInactivity"));
    	this.timeoutEnquire = Long.parseLong(properties.getProperty("timeoutEnquire"));
    	this.monitorExecutor = monitorExecutor;
    	this.connectionTimer =  monitorExecutor.schedule(new ServerTimerConnection(this, sessionId),timeoutConnection,TimeUnit.MILLISECONDS);
    }
    
    public SmppSessionConfiguration getConfig() 
    {
		return config;
	}

	public enum ServerState 
    {    	
    	OPEN, BINDING, BOUND, REBINDING, UNBINDING, CLOSED    	
    }
	
	@SuppressWarnings("rawtypes")
	@Override
	public void packetReceived(Pdu packet) {
		switch (serverState) {

		case OPEN:
			// can get SmppConstants.CMD_ID_GENERIC_NACK
			Boolean correctPacket = false;

			switch (packet.getCommandId()) {

			case SmppConstants.CMD_ID_BIND_RECEIVER:
				correctPacket = true;
				config.setType(SmppBindType.RECEIVER);
				break;
			case SmppConstants.CMD_ID_BIND_TRANSCEIVER:
				correctPacket = true;
				config.setType(SmppBindType.TRANSCEIVER);
				break;
			case SmppConstants.CMD_ID_BIND_TRANSMITTER:
				correctPacket = true;
				config.setType(SmppBindType.TRANSMITTER);
				break;
			}

			if (!correctPacket) {
				// send genericNack to ESME because of incorrect SMPP bind command
 
				logger.error("Unable to convert a BaseBind request into SmppSessionConfiguration");
				sendGenericNack(packet);
				serverState = ServerState.CLOSED;
			} else {
				
				enquireTimer =  monitorExecutor.schedule(new ServerTimerEnquire(this, sessionId),timeoutEnquire,TimeUnit.MILLISECONDS);
				
				if(connectionTimer!=null)
					connectionTimer.cancel(true);
				
				BaseBind bindRequest = (BaseBind) packet;
				config.setName("LoadBalancerSession." + bindRequest.getSystemId() + "."	+ bindRequest.getSystemType());
				config.setSystemId(bindRequest.getSystemId());
				config.setPassword(bindRequest.getPassword());
				config.setSystemType(bindRequest.getSystemType());
				config.setAddressRange(bindRequest.getAddressRange());
				config.setInterfaceVersion(bindRequest.getInterfaceVersion());
				lbServerListener.bindRequested(sessionId, this, bindRequest);
				serverState = ServerState.BINDING;

			}
			break;
			
		case BINDING:
			logger.error("Server received packet in incorrect state (BINDING)");
			break;
			
		case BOUND:
			correctPacket = false;
			switch (packet.getCommandId()) {
			case SmppConstants.CMD_ID_UNBIND:
				
				correctPacket = true;
				
				if(inactivityTimer!=null)
					inactivityTimer.cancel(true);
				
				restartEnquireTimer();
				
///////////////////is it correct//////////////////////////////////////////////////
				paketMap.clear();
				
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
				correctPacket = true;
				
				restartEnquireTimer();
				
				if(inactivityTimer!=null)
					inactivityTimer.cancel(true);
				lbServerListener.smppEntityRequested(sessionId, packet);
				break;
				
			case SmppConstants.CMD_ID_DATA_SM_RESP:
			case SmppConstants.CMD_ID_DELIVER_SM_RESP:
			case SmppConstants.CMD_ID_ENQUIRE_LINK_RESP:
				correctPacket = true;
				
				restartEnquireTimer();
				
				inactivityTimer =  monitorExecutor.schedule(new ServerTimerInactivity(this, sessionId),timeoutInactivity,TimeUnit.MILLISECONDS);
				
				if(paketMap.containsKey(packet.getSequenceNumber()))
					paketMap.remove(packet.getSequenceNumber()).getScheduledFuture().cancel(true);
				
				lbServerListener.smppEntityResponseFromClient(sessionId, packet);
				break;

			}

			if (!correctPacket) {
				// send genericNack to ESME because of incorrect SMPP message
				sendGenericNack(packet);
			}

			break;
			
		case REBINDING:
			
			sendGenericNack(packet);
			
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
		inactivityTimer =  monitorExecutor.schedule(new ServerTimerInactivity(this, sessionId),timeoutInactivity,TimeUnit.MILLISECONDS);
		restartEnquireTimer();
		
        ChannelBuffer buffer = null;
		try {
			buffer = transcoder.encode(packet);
			
		} catch (UnrecoverablePduException | RecoverablePduException e) {
			logger.error("Encode error: ", e);
		}
		
			
				channel.write(buffer);
		
			

		if(packet.getCommandStatus()==SmppConstants.STATUS_OK)
		serverState = ServerState.BOUND;
		
	}
	
	@Override
	public void sendUnbindResponse(Pdu packet){
		
		enquireTimer.cancel(true);

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
		
		restartEnquireTimer();
		
        ChannelBuffer buffer = null;
		try {
			buffer = transcoder.encode(packet);
			
		} catch (UnrecoverablePduException | RecoverablePduException e) {
			logger.error("Encode error: ", e);
		}
		channel.write(buffer);
		
		inactivityTimer =  monitorExecutor.schedule(new ServerTimerInactivity(this, sessionId),timeoutInactivity,TimeUnit.MILLISECONDS);

	}
	
	private void sendGenericNack(Pdu packet){
		
		restartEnquireTimer();
		GenericNack genericNack = new GenericNack();
		genericNack.setSequenceNumber(packet.getSequenceNumber());
		if(serverState == ServerState.REBINDING)
		{
			genericNack.setCommandStatus(SmppConstants.STATUS_SYSERR);
		}
		else
		{
		    genericNack.setCommandStatus(SmppConstants.STATUS_INVCMDID);
		}
		ChannelBuffer buffer = null;
		try {
			buffer = transcoder.encode(genericNack);
			
		} catch (UnrecoverablePduException | RecoverablePduException e) {
			logger.error("Encode error: ", e);
		}
		channel.write(buffer);
		
	}

	public void sendRequest(Pdu packet) {
		
		restartEnquireTimer();
		
		ChannelBuffer buffer = null;
		try {
			buffer = transcoder.encode(packet);
			
		} catch (UnrecoverablePduException | RecoverablePduException e) {
			logger.error("Encode error: ", e);
		}
		
		if(inactivityTimer!=null)
			inactivityTimer.cancel(true);
		
		paketMap.put(packet.getSequenceNumber(), new TimerData(packet, monitorExecutor.schedule(new ServerTimer(this ,packet),timeoutResponse,TimeUnit.MILLISECONDS)));
		channel.write(buffer);
	}
	
	public void reconnectState(boolean isReconnect) {
		if (isReconnect)
			serverState = ServerState.REBINDING;
		else
			serverState = ServerState.BOUND;

	}
	
	@Override
	public void requestTimeout(Pdu packet) 
	{
		switch(packet.getCommandId()){
		case SmppConstants.CMD_ID_CANCEL_SM:
        case SmppConstants.CMD_ID_DELIVER_SM:
        case SmppConstants.CMD_ID_ENQUIRE_LINK:
        
        	if(!paketMap.containsKey(packet.getSequenceNumber()))
        		logger.info("(requestTimeout)We take SMPP response from client in time");
    		else
    			logger.info("(requestTimeout)We did NOT take SMPP response from client in time");
			break;
		}
	}

	@Override
	public void connectionTimeout(Long sessionId) {
		
		if(connectionTimer.isCancelled())
    		logger.info("(connectionTimeout)Session initialization succesful for sessionId: " + sessionId);
		else
			logger.info("(connectionTimeout)Session initialization failed for sessionId: " + sessionId);
		
		
	}

	@Override
	public void inactivityTimeout(Long sessionId) {
		
		if(inactivityTimer.isCancelled())
    		logger.info("(inactivityTimeout)Session in good shape for sessionId: " + sessionId);
		else
			logger.info("(inactivityTimeout)Session in bad shape for sessionId: " + sessionId + ". The resulting behaviour is to either close the session or issue an unbind request.");
			//close session		
	}

	@Override
	public void enquireTimeout(Long sessionId) {
		if(enquireTimer.isCancelled())
			
    		logger.info("(enquireTimeout)Time between operations is ok for sessionId : " + sessionId);
		else
			logger.info("(enquireTimeout)We should check connection for sessionId: " + sessionId + ". We must generate enquire_link.");
		
		lbServerListener.checkConnection(sessionId);
		//restart inactivityTimeout
		
		if(inactivityTimer!=null)
			inactivityTimer.cancel(true);
		inactivityTimer =  monitorExecutor.schedule(new ServerTimerInactivity(this, sessionId),timeoutInactivity,TimeUnit.MILLISECONDS);
		
		
		
	}
	
	public void restartEnquireTimer()
	{
		//////////////////////////////
		enquireTimer.cancel(true);
		enquireTimer =  monitorExecutor.schedule(new ServerTimerEnquire(this, sessionId),timeoutEnquire,TimeUnit.MILLISECONDS);
	}

	public void generateEnquireLink() {
		
		
//		ChannelBuffer buffer = null;
//		try {
//			buffer = transcoder.encode(new EnquireLink());
//			
//		} catch (UnrecoverablePduException | RecoverablePduException e) {
//			logger.error("Encode error: ", e);
//		}
//		channel.write(buffer);
		
	}

}
