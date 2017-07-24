package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collections;

import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBDeleteRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;

public class DeleteLogicalSwitchRequest extends OVSTransactRequest {
	
	public DeleteLogicalSwitchRequest(int id) {
		super(id, "OVN_Northbound",Collections.singleton(new OVSDBDeleteRequest("Logical_Switch",
				Collections.emptyList())));
	}

	public DeleteLogicalSwitchRequest(int id, Criteria criteria) {
		super(id, "OVN_Northbound",Collections.singleton(new OVSDBDeleteRequest("Logical_Switch",
				Collections.singleton(criteria))));
	}

}
