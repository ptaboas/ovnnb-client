package com.simplyti.cloud.ovn.client;

import io.netty.channel.EventLoopGroup;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;

public class OVNNbClientBuilder {

	private boolean vervose;
	private EventLoopGroup eventLoop;
	private String host;
	private int port;

	public OVNNbClientBuilder server(String host, int port) {
		this.host=host;
		this.port=port;
		return this;
	}
	
	public OVNNbClientBuilder verbose(boolean verbose) {
		this.vervose=verbose;
		return this;
	}
	
	public OVNNbClientBuilder eventLoop(EventLoopGroup eventLoop){
		this.eventLoop=eventLoop;
		return this;
	}

	public OVNNbClient build() {
		return new OVNNbClient(eventLoop,host,port,vervose);
	}
	
	public OVNNbClientBuilder withLog4J2Logger() {
		InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
		return this;
	}

}
