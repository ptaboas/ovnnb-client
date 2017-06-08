package com.simplyti.cloud.ovn.client.domain.db;

import java.util.Collection;

import com.simplyti.cloud.ovn.client.criteria.Criteria;

import lombok.Getter;

@Getter
public class OVSDBSelectRequest extends OVSDBOperationRequest {
	
	private final Collection<Criteria> criteria;

	public OVSDBSelectRequest(String table, Collection<Criteria> criteria) {
		super(OVSDBOperation.SELECT,table);
		this.criteria=criteria;
	}

}
