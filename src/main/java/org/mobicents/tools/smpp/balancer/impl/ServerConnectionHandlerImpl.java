package org.mobicents.tools.smpp.balancer.impl;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.mobicents.tools.smpp.balancer.api.ServerConnection;

import com.cloudhopper.smpp.pdu.Pdu;

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
}
