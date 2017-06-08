package com.simplyti.cloud.ovn.client.criteria;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Criteria {
	
	private final String column;
	private final Function condition;
	private final Object value;
	
	public static CriteriaBuilder field(String field){
		return new CriteriaBuilderImpl(field);
	}

}
