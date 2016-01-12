package org.mobicents.tools.smpp.balancer.impl;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudhopper.smpp.pdu.Pdu;

public class ClientConnectionHandlerImpl extends SimpleChannelHandler  
{	
	
	private static final Logger logger = LoggerFactory.getLogger(ClientConnectionHandlerImpl.class);
	private ClientConnectionImpl listener = null;
	
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
	            listener.packetReceived(pdu);
	    }
     }
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
	{
		logger.error(e.getCause().getMessage(),e);		
	}
	
	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e)
	{
		listener.rebind();
	}
}