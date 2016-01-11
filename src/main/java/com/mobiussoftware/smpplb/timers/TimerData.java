package com.mobiussoftware.smpplb.timers;

import java.util.concurrent.ScheduledFuture;

import com.cloudhopper.smpp.pdu.Pdu;

public class TimerData {
	
	Pdu paket;
	ScheduledFuture <?> scheduledFuture;
	CancellableRunnable runnable;
	
	public TimerData(Pdu paket,ScheduledFuture <?> scheduledFuture,CancellableRunnable runnable)
	{
		this.paket = paket;
		this.runnable=runnable;
		this.scheduledFuture = scheduledFuture;
	}

	public CancellableRunnable getRunnable()
	{
		return runnable;
	}
	
	public Pdu getPaket() {
		return paket;
	}

	public ScheduledFuture<?> getScheduledFuture() {
		return scheduledFuture;
	}


	

}
