package com.simplyti.cloud.ovn.client.loadbalancers;

import com.simplyti.cloud.ovn.client.domain.Endpoint;

public class EndpointBuilder<T extends EndpointBuilder<T>> {

	private String ip;
	private Integer port;

	@SuppressWarnings("unchecked")
	public T ip(String ip) {
		this.ip=ip;
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T port(int port) {
		this.port=port;
		return (T) this;
	}
	
	protected Endpoint endpoint() {
		return new Endpoint(ip,port);
	}

}
