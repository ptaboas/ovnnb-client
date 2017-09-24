package com.simplyti.cloud.ovn.client.routers;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import com.simplyti.cloud.ovn.client.AbstractNamedResourceApi;
import com.simplyti.cloud.ovn.client.InternalClient;
import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.criteria.Function;
import com.simplyti.cloud.ovn.client.domain.db.NamedUUID;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBMutateRequest;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBOperationRequest;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalRouter;
import com.simplyti.cloud.ovn.client.mutation.Mutation;

public class DefaultRoutersApi extends AbstractNamedResourceApi<LogicalRouter> implements RoutersApi {

	public DefaultRoutersApi(InternalClient client) {
		super(client, "Logical_Router");
	}

	@Override
	public RouterBuilder builder() {
		return new RouterBuilder(this);
	}
	
	public OVSDBMutateRequest addLoadNamedUUIDBalancerOperation(String name, String namedUUID) {
		return new OVSDBMutateRequest(resourceTable, 
				Collections.singleton(new Criteria("name",Function.EQ,name)),
				Collections.singleton(addLoadBalancerMutate(new NamedUUID(namedUUID))));
	}
	
	public OVSDBMutateRequest deleteLoadBalancerOperation(UUID lbid) {
		return new OVSDBMutateRequest(resourceTable, 
				Collections.emptyList(),
				Collections.singleton(deleteLoadBalancerMutate(lbid)));
	}
	
	public OVSDBMutateRequest deleteLoadBalancersOperation(Collection<UUID> lbids) {
		return new OVSDBMutateRequest(resourceTable, 
				Collections.emptyList(),
				Collections.singleton(deleteLoadBalancerMutate(lbids)));
	}
	
	public Mutation addLoadBalancerMutate(Object lb) {
		return Mutation.field("load_balancer").insert(lb);
	}
	
	public Mutation deleteLoadBalancerMutate(Object lb) {
		return Mutation.field("load_balancer").delete(lb);
	}
	
	public Mutation deleteLoadBalancerMutate(Collection<UUID> lb) {
		return Mutation.field("load_balancer").delete(lb);
	}

	@Override
	protected Collection<OVSDBOperationRequest> getRemoveReferencesMutations(UUID uuid) {
		return Collections.emptyList();
	}

	@Override
	protected Collection<OVSDBOperationRequest> getRemoveReferencesMutations(Collection<UUID> uuid) {
		return Collections.emptyList();
	}

	

}
