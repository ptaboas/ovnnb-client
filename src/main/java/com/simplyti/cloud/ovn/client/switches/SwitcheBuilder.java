package com.simplyti.cloud.ovn.client.switches;

import java.util.Map;

import com.simplyti.cloud.ovn.client.AbstractNamedApiBuilder;
import com.simplyti.cloud.ovn.client.NamedResourceApi;
import com.simplyti.cloud.ovn.client.domain.NetworkIp;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalSwitch;


public class SwitcheBuilder extends AbstractNamedApiBuilder<LogicalSwitch,SwitcheBuilder>{

	private NetworkIp subnet;

	public SwitcheBuilder(NamedResourceApi<LogicalSwitch> namedApi) {
		super(namedApi);
	}

	@Override
	protected LogicalSwitch create(String name,Map<String,String> externalIds) {
		return new LogicalSwitch(null,name, subnet, null, externalIds);
	}

	public SwitcheBuilder withSubnet(String subnet) {
		this.subnet=NetworkIp.valueOf(subnet);
		return this;
	}



}
