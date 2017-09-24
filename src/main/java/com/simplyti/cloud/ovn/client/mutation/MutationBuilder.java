package com.simplyti.cloud.ovn.client.mutation;

import java.util.Collection;

public interface MutationBuilder {

	Mutation insert(Object value);

	Mutation delete(Collection<?> values);

	Mutation delete(Object value);

}
