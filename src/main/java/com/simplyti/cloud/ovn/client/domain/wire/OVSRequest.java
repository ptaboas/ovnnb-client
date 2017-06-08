package com.simplyti.cloud.ovn.client.domain.wire;

import java.util.Collection;

import com.simplyti.cloud.ovn.client.domain.db.OVSDBOperationRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OVSRequest {
	
	private final int id;
	private final OVSMethod method;
	private final String dbName;
	private final Collection<OVSDBOperationRequest> operations;

}
