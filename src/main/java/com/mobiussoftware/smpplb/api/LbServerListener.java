package com.mobiussoftware.smpplb.api;

import com.cloudhopper.smpp.pdu.Pdu;
import com.mobiussoftware.smpplb.impl.ServerConnectionImpl;

public interface LbServerListener {

	
	void bindRequested(Long sessionId, ServerConnectionImpl serverConnectionImpl, Pdu packet);
	void unbindRequested(Long sessionId, Pdu packet);
	void smppEntityRequested(Long sessionId, Pdu packet);
	void smppEntityResponseFromClient(Long sessionId, Pdu packet);
	void checkConnection(Long sessionId, int i);
	void closeConnection(Long sessionId);
	void unbindSuccesfullFromServer(Long sessionId, Pdu packet);
	
	
}
