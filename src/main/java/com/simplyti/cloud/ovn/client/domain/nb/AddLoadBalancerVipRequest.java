package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBMutateRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;
import com.simplyti.cloud.ovn.client.mutation.Mutation;

public class AddLoadBalancerVipRequest extends OVSTransactRequest {

	public AddLoadBalancerVipRequest(int id, UUID loadBalander, Map<String,String> vip) {
		super(id, "OVN_Northbound",Collections.singleton(new OVSDBMutateRequest("Load_Balancer",
				Collections.singleton(Criteria.field("_uuid").eq(loadBalander)),
				Collections.singleton(Mutation.field("vips").insert(vip)))));
	}

}