package com.simplyti.cloud.ovn.client.mutation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Mutator {
	
	INSERT("insert");
	
	private final String operand;

}
