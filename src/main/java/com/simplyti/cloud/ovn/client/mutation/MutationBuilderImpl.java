package com.simplyti.cloud.ovn.client.mutation;

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

}
