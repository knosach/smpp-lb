package com.mobiussoftware.smpplb.api;

import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

public interface ClientConnection {

	public void bind() throws UnrecoverablePduException, RecoverablePduException;
	
	public Boolean connect();
	
	public void packetReceived(Pdu packet);

	public void sendUnbindRequest(Pdu packet);

	public void sendSmppRequest(Pdu packet);
	
	public void requestTimeout(Pdu packet);
	
}
