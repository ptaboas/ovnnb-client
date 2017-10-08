package com.simplyti.cloud.ovn.client;

public class ExternalIdConditionBuilder<T extends OvsResource> {

	private final OvnCriteriaBuilder<T> parent;
	private final String key;
	
	public ExternalIdConditionBuilder(OvnCriteriaBuilder<T> parent, String key){
		this.parent=parent;
		this.key=key;
	}

	public OvnCriteriaBuilder<T> equal(String value) {
		parent.addExternalId(key,value);
		return parent;
	}

}
