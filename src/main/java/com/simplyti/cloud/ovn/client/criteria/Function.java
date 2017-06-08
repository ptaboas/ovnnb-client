package com.simplyti.cloud.ovn.client.criteria;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Function {

	EQ("=="), NEQ("!="), INCLUDES("includes");
	
	private final String operand;
}
