package com.simplyti.cloud.ovn.client.loadbalancers;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableSet;
import com.simplyti.cloud.ovn.client.AbstractNamedResourceApi;
import com.simplyti.cloud.ovn.client.InternalClient;
import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.Endpoint;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBOperationRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBUpdateRequest;
import com.simplyti.cloud.ovn.client.domain.nb.LoadBalancer;
import com.simplyti.cloud.ovn.client.routers.DefaultRoutersApi;
import com.simplyti.cloud.ovn.client.switches.DefaultSwitchesApi;

public class DefaultLoadBalancersApi extends AbstractNamedResourceApi<LoadBalancer> implements LoadBalancersApi {

	private final DefaultSwitchesApi switches;
	private final DefaultRoutersApi routers;

	public DefaultLoadBalancersApi(InternalClient client, DefaultSwitchesApi switches,DefaultRoutersApi routers) {
		super(client, "Load_Balancer");
		this.switches=switches;
		this.routers=routers;
	}

	@Override
	public LoadBalancerBuilder builder() {
		return new LoadBalancerBuilder(this,switches,this.routers);
	}

	@Override
	public LoadBalancerUpdater update(UUID uuid) {
		return new LoadBalancerUpdater(client,db(),resourceTable,switches,this,uuid,get(uuid));
	}
	
	@Override
	public LoadBalancerUpdater update(String name) {
		return new LoadBalancerUpdater(client,db(),resourceTable,switches,this,name,get(name));
	}

	public OVSDBOperationRequest updateVipsOperation(UUID uuid, Map<Endpoint, Collection<Endpoint>> vips) {
		return new OVSDBUpdateRequest(resourceTable, 
				Collections.singleton(Criteria.field("_uuid").eq(uuid)),
				new LoadBalancer(null, null, null, vips,null));
	}

	@Override
	protected Collection<OVSDBOperationRequest> getRemoveReferencesMutations(UUID uuid) {
		return ImmutableSet.of(switches.deleteLoadBalancerOperation(uuid),
				routers.deleteLoadBalancerOperation(uuid));
	}

	@Override
	protected Collection<OVSDBOperationRequest> getRemoveReferencesMutations(Collection<UUID> uuids) {
		return ImmutableSet.of(switches.deleteLoadBalancersOperation(uuids),
				routers.deleteLoadBalancersOperation(uuids));
	}

}
