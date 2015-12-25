package com.mobiussoftware.smpplb.impl;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudhopper.smpp.pdu.Pdu;
import com.mobiussoftware.smpplb.api.ClientConnection;

public class ClientConnectionHandlerImpl extends SimpleChannelHandler  
{	
	
	private static final Logger logger = LoggerFactory.getLogger(ClientConnectionHandlerImpl.class);
	private ClientConnection listener;
	public ClientConnectionHandlerImpl(ClientConnection listener)
	{
		this.listener=listener;
	}
	
	@Override
     public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) 
	 {		 
		if (e.getMessage() instanceof Pdu) 
		{
			
	            Pdu pdu = (Pdu)e.getMessage();
	            this.listener.packetReceived(pdu);
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
		System.out.println("Client channelDisconnected");
	}
	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
	{
		System.out.println("Client channelClosed");
	}
	

}