package com.simplyti.cloud.ovn.client.mutation;

import java.util.Map;
import java.util.Set;

public class MutationBuilderImpl implements MutationBuilder {
	
	private String field;
	
	public MutationBuilderImpl(String field) {
		this.field=field;
	}

	@Override
	public Mutation insert(Set<?> values) {
		return new Mutation(field,Mutator.INSERT,values);
	}
	
	@Override
	public Mutation insert(Map<?,?> entry) {
		return new Mutation(field,Mutator.INSERT,entry);
	}

}
