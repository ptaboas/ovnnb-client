package com.simplyti.cloud.ovn.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.criteria.Function;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBMutateRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBOperationRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSTransactRequest;
import com.simplyti.cloud.ovn.client.mutation.Mutation;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.PromiseCombiner;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;

public abstract class Updater<T> {
	
	private static final TypeReference<Void> VOID_TYPE = new TypeReference<Void>() {};
	
	protected final InternalClient client;
	private final String db;
	private final String table;
	
	protected final UUID uuid;
	private final String name;
	private final Collection<Mutation> mutations;
	private final Collection<OVSDBOperationRequest> operations;
	private final Future<T> currentFuture;
	
	private final PromiseCombiner resourceDependantPatchesCombiner;
	
	
	public Updater(InternalClient client,String db, String table,String name,Future<T> currentFuture){
		this.db=db;
		this.table=table;
		this.uuid=null;
		this.name=name;
		this.client=client;
		this.currentFuture=currentFuture;
		this.mutations=new ArrayList<>();
		this.operations=new ArrayList<>();
		this.resourceDependantPatchesCombiner = new PromiseCombiner();
	}
	
	public Updater(InternalClient client,String db, String table,UUID uuid,Future<T> currentFuture){
		this.db=db;
		this.table=table;
		this.uuid=uuid;
		this.name=null;
		this.client=client;
		this.currentFuture=currentFuture;
		this.mutations=new ArrayList<>();
		this.operations=new ArrayList<>();
		this.resourceDependantPatchesCombiner = new PromiseCombiner();
	}

	public Future<Void> update() {
		addMutation();
		Promise<Void> promise = client.newPromise();
		Promise<Void> lazyPatchesPromise = client.newPromise();
		resourceDependantPatchesCombiner.finish(lazyPatchesPromise);
		lazyPatchesPromise.addListener(future->{
			if(future.isSuccess()){
				if(operations.isEmpty()){
					client.call(promise,VOID_TYPE,id->updateByNameRequest(id));
				}else{
					client.call(promise,VOID_TYPE,id->updateOperationsRequest(id));
				}
			}else{
				promise.setFailure(future.cause());
			}
		});
		return promise;
	}
	
	protected abstract void addMutation();

	private OVSRequest updateOperationsRequest(Integer id) {
		return new OVSTransactRequest(id,db,operations);
	}

	private OVSRequest updateByNameRequest(Integer id) {
		return new OVSTransactRequest(id,db,Collections.singleton(
				new OVSDBMutateRequest(table, 
						Collections.singleton(new Criteria("name",Function.EQ,name)),
						mutations)));
	}
	
	protected void addMutation(Mutation mutation) {
		this.mutations.add(mutation);
	}
	
	protected void addOperation(OVSDBOperationRequest operation) {
		this.operations.add(operation);
	}
	
	protected void onCurrentResource(Consumer<T> consumer) {
		if(currentFuture.isSuccess()){
			consumer.accept(currentFuture.getNow());
		}else{
			Promise<Void> lazyPatch = client.newPromise();
			resourceDependantPatchesCombiner.add((Future<?>)lazyPatch);
			currentFuture.addListener(future->{
				if(currentFuture.isSuccess()){
					consumer.accept(currentFuture.getNow());
					lazyPatch.setSuccess(null);
				}else{
					lazyPatch.setFailure(currentFuture.cause());
				}
			});
		}
	}
}
