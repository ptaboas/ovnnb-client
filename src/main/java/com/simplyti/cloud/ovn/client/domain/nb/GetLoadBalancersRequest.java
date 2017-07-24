package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collections;

import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBSelectRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;

public class GetLoadBalancersRequest extends OVSTransactRequest {

	public GetLoadBalancersRequest(int id, Criteria criteria) {
		super(id, "OVN_Northbound",Collections.singleton(new OVSDBSelectRequest("Load_Balancer", 
				Collections.singleton(criteria))));
	}
	
	public GetLoadBalancersRequest(int id) {
		super(id, "OVN_Northbound",Collections.singleton(new OVSDBSelectRequest("Load_Balancer", 
				Collections.emptyList())));
	}
	
}
