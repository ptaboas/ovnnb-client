package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collections;

import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBDeleteRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;

public class DeleteLogicalRouterRequest extends OVSTransactRequest {
	
	public DeleteLogicalRouterRequest(int id) {
		super(id, "OVN_Northbound",Collections.singleton(new OVSDBDeleteRequest("Logical_Router",
				Collections.emptyList())));
	}

	public DeleteLogicalRouterRequest(int id, Criteria criteria) {
		super(id, "OVN_Northbound",Collections.singleton(new OVSDBDeleteRequest("Logical_Router",
				Collections.singleton(criteria))));
	}

}
