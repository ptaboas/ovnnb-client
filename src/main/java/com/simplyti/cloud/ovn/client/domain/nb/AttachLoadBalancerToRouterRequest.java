package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collections;
import java.util.UUID;

import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBMutateRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;
import com.simplyti.cloud.ovn.client.mutation.Mutation;

public class AttachLoadBalancerToRouterRequest extends OVSTransactRequest {
	
	public AttachLoadBalancerToRouterRequest(int id, UUID lbUUID, String lrName) {
		super(id, "OVN_Northbound",
				Collections.singleton(new OVSDBMutateRequest(
					"Logical_Router",
					Collections.singleton(Criteria.field("name").eq(lrName)),
					Collections.singleton(Mutation.field("load_balancer").insert(Collections.singleton(lbUUID))))));
	}

}
