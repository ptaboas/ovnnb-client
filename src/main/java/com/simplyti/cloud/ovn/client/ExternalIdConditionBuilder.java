package com.simplyti.cloud.ovn.client;

public class ExternalIdConditionBuilder {

	private final OvnCriteriaBuilder<?> parent;
	private final String key;
	
	public ExternalIdConditionBuilder(OvnCriteriaBuilder<?> parent, String key){
		this.parent=parent;
		this.key=key;
	}

	public OvnCriteriaBuilder<?> equal(String value) {
		parent.addExternalId(key,value);
		return parent;
	}

}
