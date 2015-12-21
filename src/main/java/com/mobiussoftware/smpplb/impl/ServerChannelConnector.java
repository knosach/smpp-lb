package com.mobiussoftware.smpplb.impl;

import java.util.Properties;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.ssl.SslHandler;

import com.cloudhopper.smpp.channel.SmppChannelConstants;
import com.cloudhopper.smpp.channel.SmppSessionPduDecoder;
import com.cloudhopper.smpp.ssl.SslConfiguration;
import com.cloudhopper.smpp.ssl.SslContextFactory;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoder;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoderContext;
import com.mobiussoftware.smpplb.api.LbServerListener;
import com.mobiussoftware.smpplb.core.LbDispatcher;

public class ServerChannelConnector extends SimpleChannelUpstreamHandler {

    private ChannelGroup channels;
    private SmppServer server;
    private LbServerListener lbServerListener;

    public ServerChannelConnector(ChannelGroup channels, SmppServer smppServer, Properties properties) 
    {
        this.channels = channels;
        this.server = smppServer;
        this.lbServerListener = new LbDispatcher(properties);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception 
    {
        Channel channel = e.getChannel();
        channels.add(channel);       

        if (server.getConfiguration().isUseSsl()) 
        {
		    SslConfiguration sslConfig = server.getConfiguration().getSslConfiguration();
		    if (sslConfig == null) throw new IllegalStateException("sslConfiguration must be set");
		    SslContextFactory factory = new SslContextFactory(sslConfig);
		    SSLEngine sslEngine = factory.newSslEngine();
		    sslEngine.setUseClientMode(false);
		    channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_SSL_NAME, new SslHandler(sslEngine));
        }

        channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_PDU_DECODER_NAME, new SmppSessionPduDecoder(new DefaultPduTranscoder(new DefaultPduTranscoderContext())));

        ServerConnectionImpl serverConnectionImpl = new ServerConnectionImpl(server.nextSessionId(),channel,lbServerListener);
        channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_WRAPPER_NAME, new ServerConnectionHandlerImpl(serverConnectionImpl));
  
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

    	channels.remove(e.getChannel());
    	this.server.getCounters().incrementChannelDisconnectsAndGet();
    }

}
