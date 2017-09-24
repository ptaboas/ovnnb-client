package com.simplyti.cloud.ovn.client;

import java.util.Map;

import com.google.common.collect.Maps;
import com.simplyti.cloud.ovn.client.criteria.Criteria;

import io.netty.util.concurrent.Future;

public class OvnCriteriaBuilder<T extends OvsResource> {
	
	private final AbstractOvsResourceApi<T> api;

	private Map<String,String> externalIds;

	
	public OvnCriteriaBuilder(AbstractOvsResourceApi<T> api){
		this.api=api;
	}

	public ExternalIdConditionBuilder externalId(String key) {
		return new ExternalIdConditionBuilder(this,key);
	}

	public Future<Void> delete() {
		return delete(false);
	}

	public void addExternalId(String key, String value) {
		if(externalIds==null){
			externalIds=Maps.newConcurrentMap();
		}
		externalIds.put(key, value);
	}

	public Future<Void> delete(boolean forced) {
		return api.delete(Criteria.field("external_ids").includes(externalIds),forced);
	}

}
