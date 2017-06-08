package com.simplyti.cloud.ovn.client;

import com.simplyti.cloud.ovn.client.domain.Address;

import io.netty.channel.nio.NioEventLoopGroup;

public class OVNNbClientBuilder {

	private Address serverAddress;
	private boolean vervose;

	public OVNNbClientBuilder server(String host, int port) {
		this.serverAddress = new Address(host,port);
		return this;
	}
	
	public OVNNbClientBuilder verbose() {
		this.vervose=true;
		return this;
	}

	public OVNNbClient build() {
		return new OVNNbClient(new NioEventLoopGroup(1),serverAddress,vervose);
	}

}
