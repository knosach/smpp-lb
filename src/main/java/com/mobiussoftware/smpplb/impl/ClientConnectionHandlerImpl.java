package com.mobiussoftware.smpplb.impl;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.cloudhopper.smpp.pdu.Pdu;
import com.mobiussoftware.smpplb.api.ClientConnection;

public class ClientConnectionHandlerImpl extends SimpleChannelHandler  
{	
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
	
}