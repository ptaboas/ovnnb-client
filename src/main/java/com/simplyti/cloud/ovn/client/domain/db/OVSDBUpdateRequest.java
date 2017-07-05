package com.simplyti.cloud.ovn.client.domain.db;

import java.util.Collection;

import com.simplyti.cloud.ovn.client.criteria.Criteria;

import lombok.Getter;

@Getter
public class OVSDBUpdateRequest extends OVSDBOperationRequest {
	
	private final Collection<Criteria> criteria;
	
	private final Object row;

	public OVSDBUpdateRequest(String table, Collection<Criteria> criteria, Object row) {
		super(OVSDBOperation.UPDATE, table);
		this.criteria=criteria;
		this.row=row;
	}

}
