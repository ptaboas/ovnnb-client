package com.simplyti.cloud.ovn.client.domain.db;

import java.util.Collection;

import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.mutation.Mutation;

import lombok.Getter;

@Getter
public class OVSDBMutateRequest extends OVSDBOperationRequest {
	
	private final Collection<Criteria> criteria;
	
	private final Collection<Mutation> mutations;

	public OVSDBMutateRequest(String table, Collection<Criteria> criteria,Collection<Mutation> mutations) {
		super(OVSDBOperation.MUTATE,table);
		this.criteria=criteria;
		this.mutations=mutations;
	}

}
