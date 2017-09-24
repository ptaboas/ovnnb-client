package com.simplyti.cloud.ovn.client;

import java.util.Collection;
import java.util.UUID;

import com.simplyti.cloud.ovn.client.criteria.Criteria;

import io.netty.util.concurrent.Future;

public interface ResourceApi<T extends OvsResource> {
	
	Future<Collection<T>> list();
	
	Future<Collection<T>> list(Criteria criteria);
	
	Future<T> get(UUID uuid);
	
	Future<Void> delete(UUID now);
	
	Future<Void> deleteAll();
	
	Future<Void> delete(Criteria critera);

}
