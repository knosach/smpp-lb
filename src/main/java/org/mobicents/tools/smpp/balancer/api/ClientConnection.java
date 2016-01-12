package org.mobicents.tools.smpp.balancer.api;

import com.cloudhopper.smpp.pdu.Pdu;

public interface ClientConnection {

	public void bind();
	public Boolean connect();
	public void rebind();
	public void packetReceived(Pdu packet);
	public void sendUnbindRequest(Pdu packet);
	public void sendSmppRequest(Pdu packet);
	public void requestTimeout(Pdu packet);
	public void sendSmppResponse(Pdu packet);
	public void sendUnbindResponse(Pdu packet);
	public void connectionCheckServerSide(Long sessionId);	

}
