package com.simplyti.cloud.ovn.client;

import java.util.Collection;
import java.util.UUID;

import com.simplyti.cloud.ovn.client.domain.db.OVSDBOperationRequest;

import io.netty.util.concurrent.Future;

public interface NamedResourceApi<T extends NamedOvsResource> extends ResourceApi<T> {
	
	public Future<T> get(String name);

	public Future<UUID> create(T create, Collection<OVSDBOperationRequest> additionalOperations);
	
	public Future<Void> delete(String name);
	
	public Future<Void> delete(String name, boolean force);
	
	public OvnCriteriaBuilder<T> where();
	
}
