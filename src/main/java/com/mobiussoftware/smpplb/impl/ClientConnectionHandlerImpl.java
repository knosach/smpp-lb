package com.mobiussoftware.smpplb.impl;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.Pdu;
import com.mobiussoftware.smpplb.impl.ClientConnectionImpl.ClientState;

public class ClientConnectionHandlerImpl extends SimpleChannelHandler  
{	
	
	private static final Logger logger = LoggerFactory.getLogger(ClientConnectionHandlerImpl.class);
	private ClientConnectionImpl listener = null;
	private boolean correctDisconnect = false;
	
	public ClientConnectionHandlerImpl(ClientConnectionImpl listener)
	{
		this.listener = listener;
	}
	
	@Override
     public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) 
	 {		 
		if (e.getMessage() instanceof Pdu) 
		{
			
	            Pdu pdu = (Pdu)e.getMessage();
	            
	            if((pdu.getCommandId() == SmppConstants.CMD_ID_UNBIND_RESP)&&listener.getClientState() == ClientState.UNBINDING)
	            	correctDisconnect = true;
	            
	            listener.packetReceived(pdu);
	    }
     }
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
	{
		logger.error(e.toString());
		
	}
	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e)
	{

		
		//if disconnect was not correct try to reconnect
		if(!correctDisconnect&&!listener.isEnquireLinkSent())
		{
			
			listener.rebind();

		}

	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e){
		
		System.out.println("Client channel closed");
	}

}