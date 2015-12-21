package com.mobiussoftware.smpplb.api;

import com.cloudhopper.smpp.pdu.Pdu;

public interface ServerConnection {

	public void packetReceived(Pdu packet);

	public void sendBindResponse(Pdu packet);

	public void sendUnbindResponse(Pdu packet);

	public void sendResponse(Pdu packet);

}
