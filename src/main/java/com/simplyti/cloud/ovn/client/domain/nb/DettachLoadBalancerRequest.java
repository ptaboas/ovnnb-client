package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collections;
import java.util.UUID;

import com.simplyti.cloud.ovn.client.domain.db.OVSDBMutateRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;
import com.simplyti.cloud.ovn.client.mutation.Mutation;

public class DettachLoadBalancerRequest extends OVSTransactRequest {
	
	public DettachLoadBalancerRequest(int id, UUID lbUUID) {
		super(id, "OVN_Northbound",
				Collections.singleton(new OVSDBMutateRequest(
							"Logical_Switch",
							Collections.emptyList(),
							Collections.singleton(Mutation.field("load_balancer").delete(Collections.singleton(lbUUID)))))
				);
	}

}
