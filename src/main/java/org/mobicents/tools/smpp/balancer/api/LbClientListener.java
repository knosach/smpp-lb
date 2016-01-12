package org.mobicents.tools.smpp.balancer.api;

import com.cloudhopper.smpp.pdu.Pdu;

public interface LbClientListener {

	void bindSuccesfull(long sessionID, Pdu packet);
	void bindFailed(long sessionID, Pdu packet);
	void unbindSuccesfull(long sessionID, Pdu packet);
	void smppEntityResponse(Long sessionId, Pdu packet);
	void smppEntityRequestFromServer(Long sessionId, Pdu packet);
	void connectionLost(Long sessionId, Pdu packet, int serverIndex);
	void reconnectSuccesful(Long sessionId);
	void enquireLinkReceivedFromServer(Long sessionId);
	void unbindRequestedFromServer(Long sessionId, Pdu packet);

}
