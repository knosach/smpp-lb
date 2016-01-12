package org.mobicents.tools.smpp.balancer.api;

import org.mobicents.tools.smpp.balancer.impl.ServerConnectionImpl;

import com.cloudhopper.smpp.pdu.Pdu;

public interface LbServerListener {
	
	void bindRequested(Long sessionId, ServerConnectionImpl serverConnectionImpl, Pdu packet);
	void unbindRequested(Long sessionId, Pdu packet);
	void smppEntityRequested(Long sessionId, Pdu packet);
	void smppEntityResponseFromClient(Long sessionId, Pdu packet);
	void checkConnection(Long sessionId);
	void closeConnection(Long sessionId);
	void unbindSuccesfullFromServer(Long sessionId, Pdu packet);

}
