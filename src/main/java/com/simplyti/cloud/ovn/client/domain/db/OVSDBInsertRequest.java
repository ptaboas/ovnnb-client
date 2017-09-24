package com.simplyti.cloud.ovn.client.domain.db;


import lombok.Getter;

@Getter
public class OVSDBInsertRequest extends OVSDBOperationRequest {
	
	private final Object row;
	private final String uuidName;
	
	public OVSDBInsertRequest(String table, Object row, String uuidName) {
		super(OVSDBOperation.INSERT,table);
		this.row=row;
		this.uuidName=uuidName;
	}

}
