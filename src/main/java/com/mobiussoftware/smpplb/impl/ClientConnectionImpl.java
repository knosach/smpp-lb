package com.mobiussoftware.smpplb.impl;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.channel.SmppChannelConstants;
import com.cloudhopper.smpp.channel.SmppSessionPduDecoder;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BindReceiver;
import com.cloudhopper.smpp.pdu.BindTransceiver;
import com.cloudhopper.smpp.pdu.BindTransmitter;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.ssl.SslConfiguration;
import com.cloudhopper.smpp.ssl.SslContextFactory;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoder;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoderContext;
import com.cloudhopper.smpp.transcoder.PduTranscoder;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.mobiussoftware.smpplb.api.ClientConnection;
import com.mobiussoftware.smpplb.api.LbClientListener;

public class ClientConnectionImpl implements ClientConnection
{
	private static final Logger logger = LoggerFactory.getLogger(ClientConnectionImpl.class);
	
    private Channel channel;
	public Channel getChannel() {
		return channel;
	}

	private ClientBootstrap clientBootstrap;
    private ClientConnectionHandlerImpl clientConnectionHandler;
    private SmppSessionConfiguration config;
    private final PduTranscoder transcoder;
    private AtomicInteger sequenceNumberGenerator = new AtomicInteger(1);
    private ClientState clientState=ClientState.INITIAL;
    private LbClientListener lbClientListener;
    private Long sessionId;
    private NioClientSocketChannelFactory connectionFactory;

    public enum ClientState 
    {    	
    	INITIAL, OPEN, BINDING, BOUND, UNBINDING, CLOSED    	
    }
    
    
	public  ClientConnectionImpl(Long sessionId,SmppSessionConfiguration config, LbClientListener clientListener) 
	{
		  this.sessionId = sessionId;
		  this.config = config;
		  this.transcoder = new DefaultPduTranscoder(new DefaultPduTranscoderContext());
		  this.lbClientListener=clientListener;
		  this.connectionFactory = new NioClientSocketChannelFactory();
		  this.clientConnectionHandler = new ClientConnectionHandlerImpl(this);	
		  
          this.clientBootstrap = new ClientBootstrap(connectionFactory);
          if (config.isUseSsl()) 
          {
      	    SslConfiguration sslConfig = config.getSslConfiguration();
      	    if (sslConfig == null) throw new IllegalStateException("sslConfiguration must be set");
      	    try 
      	    {
      	    	SslContextFactory factory = new SslContextFactory(sslConfig);
      	    	SSLEngine sslEngine = factory.newSslEngine();
      	    	sslEngine.setUseClientMode(true);
      	    	channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_SSL_NAME, new SslHandler(sslEngine));
      	    } 
      	    catch (Exception e) 
      	    {
      	    	logger.error("Unable to create SSL session]: " + e.getMessage(), e);
      	    	//throw new SmppChannelConnectException("Unable to create SSL session]: " + e.getMessage(), e);
      	    }
          }
          
          this.clientBootstrap.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_PDU_DECODER_NAME, new SmppSessionPduDecoder(transcoder));
          this.clientBootstrap.getPipeline().addLast(SmppChannelConstants.PIPELINE_CLIENT_CONNECTOR_NAME, this.clientConnectionHandler);
 		  
	}
	
	@Override
	public Boolean connect() 
	{
		
		ChannelFuture channelFuture = null;
		try 
        {
			channelFuture = clientBootstrap.connect(new InetSocketAddress(config.getHost(), config.getPort())).sync();
            channel = channelFuture.getChannel();
        }
        catch(Exception ex)
        {
        	logger.error("Connection failed!", ex);
        	return false;
        }   
		
		clientState=ClientState.OPEN;
		
		return true;		
	}
		
	@SuppressWarnings("rawtypes")
	@Override
	public void bind()
	{
		//CREATE PACKET
		 BaseBind bind = null;
	        if (config.getType() == SmppBindType.TRANSCEIVER) 
	            bind = new BindTransceiver();
	        else if (config.getType() == SmppBindType.RECEIVER)
	            bind = new BindReceiver();
	        else if (config.getType() == SmppBindType.TRANSMITTER)
	            bind = new BindTransmitter();

	        bind.setSystemId(config.getSystemId());
	        bind.setPassword(config.getPassword());
	        bind.setSystemType(config.getSystemType());
	        bind.setInterfaceVersion(config.getInterfaceVersion());
	        bind.setAddressRange(config.getAddressRange());
	        // assign the next PDU sequence 
	        sequenceNumberGenerator.compareAndSet(Integer.MAX_VALUE, 1);
	        bind.setSequenceNumber(sequenceNumberGenerator.getAndIncrement());
  
	        ChannelBuffer buffer = null;
			try {
				buffer = transcoder.encode(bind);
				
			} catch (UnrecoverablePduException | RecoverablePduException e) {
				logger.error("Encode error: ", e);
			}
		    channel.write(buffer);
		    
		    clientState=ClientState.BINDING;			
		    
	}

	@Override
	public void packetReceived(Pdu packet) {
		switch (clientState) {

		case INITIAL:
		case OPEN:
			logger.error("Received packet in initial or open state");
			break;
		case BINDING:
			Boolean correctPacket = false;
			switch (config.getType()) {
			case TRANSCEIVER:
				if (packet.getCommandId() == SmppConstants.CMD_ID_BIND_TRANSCEIVER_RESP)
					correctPacket = true;
				break;
			case RECEIVER:
				if (packet.getCommandId() == SmppConstants.CMD_ID_BIND_RECEIVER_RESP)
					correctPacket = true;
				break;
			case TRANSMITTER:
				if (packet.getCommandId() == SmppConstants.CMD_ID_BIND_TRANSMITTER_RESP)
					correctPacket = true;
				break;

			}

			if (!correctPacket)
				logger.error("Received invalid packet in binding state, packet type: " + packet.getCommandId());
			else {
				if (packet.getCommandStatus() == SmppConstants.STATUS_OK) {

					lbClientListener.bindSuccesfull(sessionId, packet);
					logger.info("Bound succesfully");
					clientState = ClientState.BOUND;

				} else {

					// HANDLE ERROR
					// EITHER RETRY OR FORWARD ERROR TO CLIENT AND CLOSE THE CLIENT CONNECTION AND MAPPING
					logger.info("Client " + config.getSystemId() + " bound unsuccesful, error code: " + packet.getCommandStatus());
					lbClientListener.bindFailed(sessionId, packet);
					channel.close();
					clientState = ClientState.CLOSED;
				}
			}
			break;
			
		case BOUND:
			correctPacket = false;
			switch (packet.getCommandId()) {
			case SmppConstants.CMD_ID_CANCEL_SM_RESP:
			case SmppConstants.CMD_ID_DATA_SM_RESP:
			case SmppConstants.CMD_ID_QUERY_SM_RESP:
			case SmppConstants.CMD_ID_REPLACE_SM_RESP:
			case SmppConstants.CMD_ID_SUBMIT_SM_RESP:
			case SmppConstants.CMD_ID_SUBMIT_MULTI_RESP:
			case SmppConstants.CMD_ID_ENQUIRE_LINK:
			case SmppConstants.CMD_ID_ENQUIRE_LINK_RESP:
			case SmppConstants.CMD_ID_GENERIC_NACK:
				correctPacket = true;
				logger.info("Get SMPP response");
				this.lbClientListener.smppEntityResponse(sessionId, packet);
				break;
			}
			if (!correctPacket){
				logger.error("Received invalid packet in bound state, packet type: " + packet.getCommandId());
				//как тут обрабатывать и надо ли
			}
			break;
			
		case UNBINDING:
			correctPacket = false;

			if (packet.getCommandId() == SmppConstants.CMD_ID_UNBIND_RESP)
				correctPacket = true;

			if (!correctPacket)
				logger.error("Received invalid packet in unbinding state,packet type:" + packet.getCommandId());
			else {
				logger.info("Unbind succesfully");
				this.lbClientListener.unbindSuccesfull(sessionId, packet);
				/////is it correct?////////////////////////////////////////////////////////////////////////
				channel.close();
				
				clientState = ClientState.CLOSED;
			}
			break;
		case CLOSED:
			logger.error("Received packet in closed state");
			break;
		}
	}
	
	@Override
	public void sendUnbindRequest(Pdu packet){
		
		ChannelBuffer buffer = null;
		try {
			buffer = transcoder.encode(packet);
			
		} catch (UnrecoverablePduException | RecoverablePduException e) {
			logger.error("Encode error: ", e);
		}
		channel.write(buffer);
		clientState = ClientState.UNBINDING;
		
	}
	
	@Override
	public void sendSmppRequest(Pdu packet) {
		
		ChannelBuffer buffer = null;
		try {
			buffer = transcoder.encode(packet);
			
		} catch (UnrecoverablePduException | RecoverablePduException e) {
			logger.error("Encode error: ", e);
		}
		channel.write(buffer);
		
	}
	
}