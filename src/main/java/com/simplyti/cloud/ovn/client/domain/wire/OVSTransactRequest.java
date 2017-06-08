package com.simplyti.cloud.ovn.client.domain.wire;

import java.util.Collection;

import com.simplyti.cloud.ovn.client.domain.db.OVSDBOperationRequest;

public class OVSTransactRequest extends OVSRequest {

	public OVSTransactRequest(int id,String dbName, Collection<OVSDBOperationRequest> operations) {
		super(id,OVSMethod.TRANSACT,dbName, operations);
	}

}
