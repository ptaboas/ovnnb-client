package com.simplyti.cloud.ovn.client.switches;

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
import com.simplyti.cloud.ovn.client.domain.nb.LogicalSwitch;
import com.simplyti.cloud.ovn.client.mutation.Mutation;


public class DefaultSwitchesApi extends AbstractNamedResourceApi<LogicalSwitch> implements SwitchesApi {

	public DefaultSwitchesApi(InternalClient client) {
		super(client, "Logical_Switch");
	}

	@Override
	public SwitcheBuilder builder() {
		return new SwitcheBuilder(this);
	}

	@Override
	public SwitchUpdater update(String name) {
		return new SwitchUpdater(client,this,db(),resourceTable,name,get(name));
	}

	public OVSDBMutateRequest addLoadBalancerOperation(UUID lsid, UUID lbid) {
		return new OVSDBMutateRequest(resourceTable, 
				Collections.singleton(new Criteria("_uuid",Function.EQ,lsid)),
				Collections.singleton(addLoadBalancerMutate(lbid)));
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

	private Mutation deleteLoadBalancerMutate(Object lb) {
		return Mutation.field("load_balancer").delete(lb);
	}
	
	private Mutation deleteLoadBalancerMutate(Collection<UUID> lb) {
		return Mutation.field("load_balancer").delete(lb);
	}

	public Mutation addLoadBalancerMutate(Object lb) {
		return Mutation.field("load_balancer").insert(lb);
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
