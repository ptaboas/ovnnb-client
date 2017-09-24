package com.simplyti.cloud.ovn.client.mutation;

import java.util.Collection;
import java.util.Collections;

public class MutationBuilderImpl implements MutationBuilder {
	
	private String field;
	
	public MutationBuilderImpl(String field) {
		this.field=field;
	}

	@Override
	public Mutation insert(Object item) {
		return new Mutation(field,Mutator.INSERT,Collections.singleton(item));
	}

	@Override
	public Mutation delete(Collection<?> values) {
		return new Mutation(field,Mutator.DELETE,values);
	}

	@Override
	public Mutation delete(Object item) {
		return new Mutation(field,Mutator.DELETE,Collections.singleton(item));
	}

}
