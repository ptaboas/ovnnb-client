package com.simplyti.cloud.ovn.client;


import io.netty.channel.EventLoopGroup;

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

}
