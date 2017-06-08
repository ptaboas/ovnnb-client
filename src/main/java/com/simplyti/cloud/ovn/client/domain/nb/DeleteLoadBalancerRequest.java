package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collections;

import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBDeleteRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;

public class DeleteLoadBalancerRequest extends OVSTransactRequest {

	public DeleteLoadBalancerRequest(int id, Criteria criteria) {
		super(id, "OVN_Northbound",Collections.singleton(new OVSDBDeleteRequest("Load_Balancer",
				Collections.singleton(criteria))));
	}

}