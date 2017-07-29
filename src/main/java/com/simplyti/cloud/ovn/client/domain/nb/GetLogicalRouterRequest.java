package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collections;

import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.criteria.Function;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBSelectRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;

public class GetLogicalRouterRequest extends OVSTransactRequest {

	public GetLogicalRouterRequest(int id, String name) {
		super(id, "OVN_Northbound",Collections.singleton(new OVSDBSelectRequest("Logical_Router", 
				Collections.singleton(new Criteria("name",Function.EQ,name)))));
	}

}
