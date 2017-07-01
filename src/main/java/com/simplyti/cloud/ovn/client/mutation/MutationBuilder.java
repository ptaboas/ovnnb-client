package com.simplyti.cloud.ovn.client.mutation;

import java.util.Map;
import java.util.Set;

public interface MutationBuilder {

	Mutation insert(Set<?> values);
	
	Mutation insert(Map<?,?> entry);

}
