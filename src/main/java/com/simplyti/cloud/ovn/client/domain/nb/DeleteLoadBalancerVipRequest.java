package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collections;

import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBMutateRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;
import com.simplyti.cloud.ovn.client.mutation.Mutation;

public class DeleteLoadBalancerVipRequest extends OVSTransactRequest {
	
	public DeleteLoadBalancerVipRequest(int id, String name, String vip) {
		super(id, "OVN_Northbound",
				Collections.singleton(new OVSDBMutateRequest(
							"Load_Balancer",
							Collections.singleton(Criteria.field("name").eq(name)),
							Collections.singleton(Mutation.field("vips").delete(Collections.singleton(vip)))))
				);
	}

}
