package com.simplyti.cloud.ovn.client.criteria;

public class CriteriaBuilderImpl implements CriteriaBuilder {
	
	private String field;
	

	public CriteriaBuilderImpl(String field) {
		this.field=field;
	}
	
	@Override
	public Criteria eq(Object value) {
		return new Criteria(field, Function.EQ, value);
	}

	@Override
	public Criteria includes(Object value) {
		return new Criteria(field, Function.INCLUDES, value);
	}
	

}
