package com.simplyti.cloud.ovn.client.mutation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Mutation {
	
	private final String field;
	private final Mutator mutator;
	private final Object value;
	
	public static MutationBuilder field(String field) {
		return new MutationBuilderImpl(field);
	}

}
