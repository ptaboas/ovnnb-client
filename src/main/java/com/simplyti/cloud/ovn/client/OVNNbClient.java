package com.simplyti.cloud.ovn.client;

import java.util.Collections;
import java.util.List;

import com.jsoniter.spi.TypeLiteral;
import com.simplyti.cloud.ovn.client.domain.wire.OVSMethod;
import com.simplyti.cloud.ovn.client.domain.wire.OVSRequest;
import com.simplyti.cloud.ovn.client.loadbalancers.DefaultLoadBalancersApi;
import com.simplyti.cloud.ovn.client.loadbalancers.LoadBalancersApi;
import com.simplyti.cloud.ovn.client.routers.DefaultRoutersApi;
import com.simplyti.cloud.ovn.client.routers.RoutersApi;
import com.simplyti.cloud.ovn.client.switches.DefaultSwitchesApi;
import com.simplyti.cloud.ovn.client.switches.SwitchesApi;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;

public class OVNNbClient {
	
	protected static final TypeLiteral<List<String>> STRING_LIST_TYPE = new TypeLiteral<List<String>>(){};
	
	private final InternalClient internalClient;
	private final DefaultSwitchesApi switches;
	private final DefaultRoutersApi routers;
	private final DefaultLoadBalancersApi loadBalancers;

	public OVNNbClient(EventLoopGroup eventLoopGroup, String host, int port, boolean verbose) {
		this.internalClient=new InternalClient(eventLoopGroup, host, port , verbose);
		this.switches = new DefaultSwitchesApi(internalClient);
		this.routers =  new DefaultRoutersApi(internalClient);
		this.loadBalancers = new DefaultLoadBalancersApi(internalClient, switches,routers);
	}

	public static OVNNbClientBuilder builder(){
		return new OVNNbClientBuilder();
	}

	public SwitchesApi switches() {
		return switches;
	}
	
	public RoutersApi routers() {
		return routers;
	}
	
	public LoadBalancersApi loadBalancers(){
		return loadBalancers;
	}
	
	public Future<List<String>> dbs() {
		return internalClient.call(STRING_LIST_TYPE, id->new OVSRequest(id,OVSMethod.LIST_DATABASE,Collections.emptyList()));
	}

	public Future<Void> close() {
		return this.internalClient.close();
	}

	public InternalClient internalClient() {
		return internalClient;
	}

}
