package com.simplyti.cloud.ovn.client.switches;

import java.util.UUID;

import com.simplyti.cloud.ovn.client.InternalClient;
import com.simplyti.cloud.ovn.client.Updater;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalSwitch;

import io.netty.util.concurrent.Future;

public class SwitchUpdater extends Updater<LogicalSwitch>{

	private DefaultSwitchesApi switches;

	public SwitchUpdater(InternalClient client,DefaultSwitchesApi switches, String db,String table,String name,
			Future<LogicalSwitch> currentFuture) {
		super(client,db,table,name,currentFuture);
		this.switches=switches;
	}

	public SwitchUpdater addLoadBalancer(UUID lbId) {
		addMutation(switches.addLoadBalancerMutate(lbId));
		return this;
	}

	@Override
	protected void addMutation() {}

}
