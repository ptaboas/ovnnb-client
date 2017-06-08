package com.simplyti.cloud.ovn.client.domain.db;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class OVSDBOperationRequest {

	private final OVSDBOperation operation;
	private final String table;
	
}
