package com.simplyti.cloud.ovn.client.domain.db;

import java.util.Collection;

import com.simplyti.cloud.ovn.client.criteria.Criteria;

import lombok.Getter;

@Getter
public class OVSDBDeleteRequest extends OVSDBOperationRequest {
	
	private final Collection<Criteria> criteria;

	public OVSDBDeleteRequest(String table,Collection<Criteria> criteria) {
		super(OVSDBOperation.DELETE,table);
		this.criteria=criteria;
	}

}
