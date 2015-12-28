package com.mobiussoftware.smpplb.api;

import com.cloudhopper.smpp.pdu.Pdu;

public interface LbClientListener {

	void bindSuccesfull(long sessionID, Pdu packet);
	void bindFailed(long sessionID, Pdu packet);
	void unbindSuccesfull(long sessionID, Pdu packet);
	void smppEntityResponse(Long sessionId, Pdu packet);
	void smppEntityRequestFromServer(Long sessionId, Pdu packet);
	void connectionLost(Long sessionId, Pdu packet);
	void reconnectSuccesful(Long sessionId, Pdu packet);

}
