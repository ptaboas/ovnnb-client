package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collections;

import com.simplyti.cloud.ovn.client.domain.db.OVSDBInsertRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;

public class CreateLogicalRouterRequest extends OVSTransactRequest {

	public CreateLogicalRouterRequest(int id, String name, LogicalRouter lSwitch) {
		super(id, "OVN_Northbound",Collections.singleton(new OVSDBInsertRequest("Logical_Router",lSwitch)));
	}

}
