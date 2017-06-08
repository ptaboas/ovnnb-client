package com.simplyti.cloud.ovn.client.domain.nb;

import java.util.Collections;

import com.simplyti.cloud.ovn.client.domain.db.OVSDBInsertRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;

public class CreateLogicalSwitchRequest extends OVSTransactRequest {

	public CreateLogicalSwitchRequest(int id, String name, LogicalSwitch lSwitch) {
		super(id, "OVN_Northbound",Collections.singleton(new OVSDBInsertRequest("Logical_Switch",lSwitch)));
	}

}
