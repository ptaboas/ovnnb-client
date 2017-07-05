package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBUpdateRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;

public class UpdateLoadBalancerVipRequest extends OVSTransactRequest {

	public UpdateLoadBalancerVipRequest(int id, UUID loadBalander, Map<String,String> vip) {
		super(id, "OVN_Northbound",Collections.singleton(new OVSDBUpdateRequest("Load_Balancer",
				Collections.singleton(Criteria.field("_uuid").eq(loadBalander)),Collections.singletonMap("vips", vip))));
	}

}