package com.mobiussoftware.smpplb.impl;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import com.cloudhopper.smpp.pdu.Pdu;
import com.mobiussoftware.smpplb.api.ServerConnection;

public class ServerConnectionHandlerImpl extends SimpleChannelHandler{

	private ServerConnection listener;
	public ServerConnectionHandlerImpl(ServerConnection listener)
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
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e){
		
		System.out.println("Server channel closed");
	}

}
