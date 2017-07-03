package com.simplyti.cloud.ovn.client.mutation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Mutator {
	
	INSERT("insert"),
	DELETE("delete");
	
	private final String operand;

}
