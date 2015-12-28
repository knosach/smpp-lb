package com.mobiussoftware.smpplb.impl;

public class RemoteServer{
	private String ip;
	private int port;
	
	public RemoteServer(String ip,int port){
		this.ip = ip;
		this.port = port;
	}

	public String getIP() {
		return ip;
	}

	public int getPort() {
		return port;
	}
	public String toString()
	{
		return ip + "  " + port;
		
	}
	
}
