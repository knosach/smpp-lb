package com.mobiussoftware.smpplb.core;

import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.channel.SmppChannelConstants;
import com.cloudhopper.smpp.impl.DefaultSmppServerCounters;
import com.mobiussoftware.smpplb.impl.ServerChannelConnector;

public class LbServer{

	private static final Logger logger = LoggerFactory.getLogger(LbServer.class);

    private final ChannelGroup channels;
    private final ServerChannelConnector serverConnector;
    private final SmppServerConfiguration configuration;
	private ExecutorService bossThreadPool;
    private ChannelFactory channelFactory;
    private ServerBootstrap serverBootstrap;
    private final AtomicLong sessionIdSequence;
    private DefaultSmppServerCounters counters;
    
	public LbServer (final SmppServerConfiguration configuration,ExecutorService executor, Properties properties) {
        this.configuration = configuration;
        this.channels = new DefaultChannelGroup();
        this.bossThreadPool = Executors.newCachedThreadPool();
        if (configuration.isNonBlockingSocketsEnabled()) 
            this.channelFactory = new NioServerSocketChannelFactory(this.bossThreadPool, executor, configuration.getMaxConnectionSize());
        else 
            this.channelFactory = new OioServerSocketChannelFactory(this.bossThreadPool, executor);
        
        this.serverBootstrap = new ServerBootstrap(this.channelFactory);
        this.serverBootstrap.setOption("reuseAddress", configuration.isReuseAddress());
        this.serverConnector = new ServerChannelConnector(channels, this, properties);
        this.serverBootstrap.getPipeline().addLast(SmppChannelConstants.PIPELINE_SERVER_CONNECTOR_NAME, this.serverConnector);
        this.sessionIdSequence = new AtomicLong(0);        
        this.counters = new DefaultSmppServerCounters();
        
    }
	    
	public void start() 
	{
	        try 
	        {
	            this.serverBootstrap.bind(new InetSocketAddress(configuration.getHost(), configuration.getPort()));
	            logger.info("{} started at {}:{}", configuration.getName(), configuration.getHost(), configuration.getPort());
	        } 
	        catch (ChannelException e) 
	        {
	        	logger.error("Smpp Channel Exception:", e);
	            //throw new SmppChannelException(e.getMessage(), e);
	        }
	}
	
	public SmppServerConfiguration getConfiguration()
	{
		return configuration;
	}
	
	public DefaultSmppServerCounters getCounters() 
	{
		return counters;
	}
    public Long nextSessionId() 
    {
    	this.sessionIdSequence.compareAndSet(Long.MAX_VALUE, 0);
        return this.sessionIdSequence.getAndIncrement();
    }
}