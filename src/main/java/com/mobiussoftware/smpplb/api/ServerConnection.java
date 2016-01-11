package com.mobiussoftware.smpplb.api;

import com.cloudhopper.smpp.pdu.Pdu;

public interface ServerConnection {

	public void packetReceived(Pdu packet);
	public void sendBindResponse(Pdu packet);
	public void sendUnbindResponse(Pdu packet);
	public void sendResponse(Pdu packet);
	public void requestTimeout(Pdu packet);
	public void connectionTimeout(Long sessionId);
	public void enquireTimeout(Long sessionId);
	public void connectionCheck(Long sessionId);
	public void sendUnbindRequest(Pdu packet);

}
