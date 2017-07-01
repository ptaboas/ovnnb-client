package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collections;

import com.simplyti.cloud.ovn.client.domain.db.OVSDBInsertRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;

public class CreateLoadBalancerRequest extends OVSTransactRequest {

	public CreateLoadBalancerRequest(int id, LoadBalancer lBalancer) {
		super(id, "OVN_Northbound",Collections.singleton(new OVSDBInsertRequest("Load_Balancer",lBalancer)));
	}

}