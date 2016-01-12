package org.mobicents.tools.smpp.balancer.timers;

public interface CancellableRunnable extends Runnable 
{
	public void cancel();
}
