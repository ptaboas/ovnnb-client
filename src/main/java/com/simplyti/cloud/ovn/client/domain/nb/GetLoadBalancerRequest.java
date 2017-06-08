package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collections;

import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.criteria.Function;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBSelectRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;

public class GetLoadBalancerRequest extends OVSTransactRequest {

	public GetLoadBalancerRequest(int id, String name) {
		super(id, "OVN_Northbound",Collections.singleton(new OVSDBSelectRequest("Load_Balancer", 
				Collections.singleton(new Criteria("name",Function.EQ,name)))));
	}
}
