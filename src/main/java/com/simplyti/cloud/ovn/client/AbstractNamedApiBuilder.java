package com.simplyti.cloud.ovn.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBMutateRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBOperationRequest;

import io.netty.util.concurrent.Future;

public abstract class AbstractNamedApiBuilder<T extends NamedOvsResource,B extends AbstractNamedApiBuilder<T,B>> {
	
	private final NamedResourceApi<T> namedApi;
	
	private String name;
	private Map<String,String> externalId;
	
	private Collection<OVSDBOperationRequest> additionalOperations;
	
	public AbstractNamedApiBuilder(NamedResourceApi<T> namedApi){
		this.namedApi=namedApi;
	}

	@SuppressWarnings("unchecked")
	public B withName(String name) {
		this.name=name;
		return (B) this;
	}
	
	@SuppressWarnings("unchecked")
	public B withExternalId(String key, String value) {
		if(externalId==null){
			this.externalId=Maps.newHashMap();
		}
		this.externalId.put(key, value);
		return (B) this;
	}
	
	public Future<UUID> create() {
		return namedApi.create(create(name,externalId),additionalOperations);
	}
	
	protected void addOperation(OVSDBMutateRequest operation) {
		if(additionalOperations==null){
			this.additionalOperations=new ArrayList<>();
		}
		this.additionalOperations.add(operation);
	}

	protected abstract T create(String name,Map<String,String> externalIds);

}
