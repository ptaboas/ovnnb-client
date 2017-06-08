package com.simplyti.cloud.ovn.client.mutation;

import java.util.Set;

public interface MutationBuilder {

	Mutation insert(Set<?> values);

}
