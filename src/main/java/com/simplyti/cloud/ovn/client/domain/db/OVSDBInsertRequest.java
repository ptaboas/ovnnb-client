package com.simplyti.cloud.ovn.client.domain.db;

import lombok.Getter;

@Getter
public class OVSDBInsertRequest extends OVSDBOperationRequest {
	
	private final Object row;

	public OVSDBInsertRequest(String table, Object row) {
		super(OVSDBOperation.INSERT,table);
		this.row=row;
	}

}
