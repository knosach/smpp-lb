package com.mobiussoftware.smpplb.impl;

import java.util.concurrent.ScheduledFuture;

import com.cloudhopper.smpp.pdu.Pdu;

public class TimerData {
	
	Pdu paket;
	ScheduledFuture <?> scheduledFuture;
	
	public TimerData(Pdu paket,ScheduledFuture <?> scheduledFuture){
		this.paket = paket;
		this.scheduledFuture = scheduledFuture;
	}

	public Pdu getPaket() {
		return paket;
	}

	public ScheduledFuture<?> getScheduledFuture() {
		return scheduledFuture;
	}


	

}
