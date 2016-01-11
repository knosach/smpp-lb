package com.mobiussoftware.smpplb.timers;

public interface CancellableRunnable extends Runnable 
{
	public void cancel();
}
