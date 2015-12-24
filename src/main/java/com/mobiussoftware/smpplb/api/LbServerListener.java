package com.mobiussoftware.smpplb.api;

import com.cloudhopper.smpp.pdu.Pdu;
import com.mobiussoftware.smpplb.impl.ServerConnectionImpl;

public interface LbServerListener {

	void bindRequested(Long sessionId, ServerConnectionImpl serverConnectionImpl, Pdu packet);
	void unbindRequested(Long sessionId, Pdu packet);
	void smppEntityRequested(Long sessionId, Pdu packet);
	void smppEntityResponseFromClient(Long sessionId, Pdu packet);
	
	
}
