package com.simplyti.cloud.ovn.client;


import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.criteria.Function;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBDeleteRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBInsertRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBOperationRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBSelectRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;
import com.simplyti.cloud.ovn.client.exception.OvnException;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

public abstract class AbstractNamedResourceApi<T extends NamedOvsResource> extends AbstractOvsResourceApi<T> implements NamedResourceApi<T>, ResourceApi<T> {
	
	private static final TypeReference<UUID> UUID_TYPE = new TypeReference<UUID>(){};
	private static final String CREATED_ROW = "created";
	
	public AbstractNamedResourceApi(InternalClient client, String resourceTable){
		super(client,resourceTable);
	}

	@Override
	public Future<T> get(String name) {
		return client.call(resourceClass,id->selectByNameRequest(id, name));
	}
	
	@Override
	public Future<Void> delete(String name) {
		return delete(name,false);
	}
	
	@Override
	public Future<Void> delete(String name, boolean force) {
		if(force){
			Promise<Void >promise = client.newPromise();
			Future<T> resourceFuture = get(name);
			resourceFuture.addListener(future->{
				if(future.isSuccess()){
					if(resourceFuture.getNow()==null){
						promise.setSuccess(null);
					}else {
						Collection<OVSDBOperationRequest> cleanOperations = getRemoveReferencesMutations(resourceFuture.getNow().getUuid());
						client.call(promise, VOID_TYPE, id->deleteByNameRequest(id, name, cleanOperations));
					}
				}else{
					promise.setFailure(future.cause());
				}
			});
			return promise;
		}else{
		 return client.call(VOID_TYPE, id->deleteByNameRequest(id, name));
		}
	}
	
	private OVSRequest deleteByNameRequest(Integer id,String name) {
		return new OVSTransactRequest(id,db(),Collections.singleton(
				new OVSDBDeleteRequest(resourceTable, Collections.singleton(new Criteria("name",Function.EQ,name)))));
	}
	
	private OVSRequest deleteByNameRequest(Integer id,String name,Collection<OVSDBOperationRequest> additionalOperations) {
		Collection<OVSDBOperationRequest> ops = ImmutableList.<OVSDBOperationRequest>builder()
			.addAll(additionalOperations)
			.add(new OVSDBDeleteRequest(resourceTable, Collections.singleton(new Criteria("name",Function.EQ,name)))).build();
		return new OVSTransactRequest(id,db(),ops);
	}
	
	private OVSRequest selectByNameRequest(Integer id,String name) {
		return new OVSTransactRequest(id,db(),Collections.singleton(
				new OVSDBSelectRequest(resourceTable, Collections.singleton(new Criteria("name",Function.EQ,name)))));
	}
	
	@Override
	public Future<UUID> create(T resource,Collection<OVSDBOperationRequest> additionalOperations) {
		Promise<UUID> promise = client.newPromise();
		get(resource.getName()).addListener(future->{
			if(future.isSuccess()){
				if(future.getNow()==null){
					client.call(promise,UUID_TYPE,id->createResourceRequest(id,resource,additionalOperations));
				}else{
					promise.setFailure(new OvnException("Resource with name "+resource.getName()+" already exist"));
				}
			}else{
				promise.setFailure(future.cause());
			}
		});
		
		return promise;
	}
	
	private OVSRequest createResourceRequest(Integer id, T resource, Collection<OVSDBOperationRequest> additionalOperations) {
		Collection<OVSDBOperationRequest> operations;
		if(additionalOperations==null){
			operations = Collections.singleton(new OVSDBInsertRequest(resourceTable,resource,CREATED_ROW));
		}else{
			operations = ImmutableList.<OVSDBOperationRequest>builder()
				.add(new OVSDBInsertRequest(resourceTable,resource,CREATED_ROW))
				.addAll(additionalOperations).build();
		}
		return new OVSTransactRequest(id,db(),operations);
	}
	
}
