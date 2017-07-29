package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collections;

import com.simplyti.cloud.ovn.client.domain.db.OVSDBSelectRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;

public class GetLogicalRoutersRequest extends OVSTransactRequest {

	public GetLogicalRoutersRequest(int id) {
		super(id, "OVN_Northbound",Collections.singleton(new OVSDBSelectRequest("Logical_Router", 
				Collections.emptyList())));
	}
}
