package com.simplyti.cloud.ovn.client;

import com.simplyti.cloud.ovn.client.domain.Address;

import io.netty.channel.EventLoopGroup;

public class OVNNbClientBuilder {

	private Address serverAddress;
	private boolean vervose;
	private EventLoopGroup eventLoop;

	public OVNNbClientBuilder server(String host, int port) {
		this.serverAddress = new Address(host,port);
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
		return new OVNNbClient(eventLoop,serverAddress,vervose);
	}

}
