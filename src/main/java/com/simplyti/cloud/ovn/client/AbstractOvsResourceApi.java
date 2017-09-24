package com.simplyti.cloud.ovn.client;


import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.criteria.Function;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBDeleteRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBOperationRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBSelectRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

public abstract class AbstractOvsResourceApi<T extends OvsResource> implements ResourceApi<T> {
	
	private static final String NORHBOUND = "OVN_Northbound";
	protected static final TypeReference<Void> VOID_TYPE = new TypeReference<Void>(){};
	
	protected final InternalClient client;
	
	protected final String resourceTable;
	protected final TypeReference<T> resourceClass;
	private final TypeReference<Collection<T>> resourceListClass;
	
	
	public AbstractOvsResourceApi(InternalClient client, String resourceTable){
		this.client=client;
		this.resourceTable=resourceTable;
		Type resourceType = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		this.resourceClass = new TypeReferenceLiteral<T>(resourceType);
		this.resourceListClass =  new TypeReferenceLiteral<Collection<T>>(new ParameterizedTypeImpl(Collection.class, resourceType));
	}
	
	@Override
	public Future<Collection<T>> list() {
		return client.call(resourceListClass,id->selectAll(id));
	}
	
	@Override
	public Future<Collection<T>> list(Criteria criteria) {
		return client.call(resourceListClass,id->selectWhere(id,criteria));
	}

	@Override
	public Future<T> get(UUID uuid) {
		return client.call(resourceClass,id->selectByIdRequest(id, uuid));
	}
	
	@Override
	public Future<Void> delete(UUID uuid) {
		return client.call(VOID_TYPE,id->deleteByIdRequest(id, uuid));
	}
	
	@Override
	public Future<Void> deleteAll() {
		return client.call(VOID_TYPE,id->deleteAllRequest(id));
	}
	
	@Override
	public Future<Void> delete(Criteria criteria) {
		return delete(criteria,false);
	}
	
	public Future<Void> delete(Criteria criteria, boolean forced) {
		if(forced){
			Promise<Void> promise = client.newPromise();
			Future<Collection<T>> futureResources = list(criteria);
			futureResources.addListener(future->{
				if(future.isSuccess()){
					List<UUID> resources = futureResources.getNow().stream().map(OvsResource::getUuid).collect(Collectors.toList());
					Collection<OVSDBOperationRequest> cleanOperations = getRemoveReferencesMutations(resources);
					client.call(promise, VOID_TYPE, id->deleteRequest(id,criteria, cleanOperations));
				}else{
					promise.setFailure(future.cause());
				}
			});
			return promise;
		}else{
			return client.call(VOID_TYPE,id->deleteRequest(id,criteria));
		}
		
	}
	
	private OVSRequest selectAll(Integer id) {
		return new OVSTransactRequest(id,NORHBOUND,Collections.singleton(
				new OVSDBSelectRequest(resourceTable, Collections.emptyList())));
	}
	
	private OVSRequest selectWhere(Integer id,Criteria critera) {
		return new OVSTransactRequest(id,NORHBOUND,Collections.singleton(
				new OVSDBSelectRequest(resourceTable, Collections.singleton(critera))));
	}
	
	private OVSRequest deleteAllRequest(Integer id) {
		return new OVSTransactRequest(id,NORHBOUND,Collections.singleton(
				new OVSDBDeleteRequest(resourceTable, Collections.emptyList())));
	}
	
	private OVSRequest deleteRequest(Integer id,Criteria critera) {
		return new OVSTransactRequest(id,NORHBOUND,Collections.singleton(
				new OVSDBDeleteRequest(resourceTable, Collections.singleton(critera))));
	}
	
	private OVSRequest deleteRequest(Integer id,Criteria criteria,Collection<OVSDBOperationRequest> additionalOperations) {
		Collection<OVSDBOperationRequest> ops = ImmutableList.<OVSDBOperationRequest>builder()
				.addAll(additionalOperations)
				.add(new OVSDBDeleteRequest(resourceTable, Collections.singleton(criteria))).build();
		return new OVSTransactRequest(id,NORHBOUND,ops);
	}
	
	
	private OVSRequest deleteByIdRequest(Integer id,UUID uuid) {
		return new OVSTransactRequest(id,NORHBOUND,Collections.singleton(
				new OVSDBDeleteRequest(resourceTable, Collections.singleton(new Criteria("_uuid",Function.EQ,uuid)))));
	}
	
	
	private OVSRequest selectByIdRequest(Integer id,UUID uuid) {
		return new OVSTransactRequest(id,NORHBOUND,Collections.singleton(
				new OVSDBSelectRequest(resourceTable, Collections.singleton(new Criteria("_uuid",Function.EQ,uuid)))));
	}
	
	public OvnCriteriaBuilder<T> where(){
		return new OvnCriteriaBuilder<T>(this);
	}

	protected String db() {
		return NORHBOUND;
	}
	
	protected abstract Collection<OVSDBOperationRequest> getRemoveReferencesMutations(UUID uuid) ;
	protected abstract Collection<OVSDBOperationRequest> getRemoveReferencesMutations(Collection<UUID> uuid) ;

}
