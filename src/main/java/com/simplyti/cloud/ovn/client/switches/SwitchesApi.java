package com.simplyti.cloud.ovn.client.switches;

import com.simplyti.cloud.ovn.client.NamedResourceApi;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalSwitch;

public interface SwitchesApi extends NamedResourceApi<LogicalSwitch> {

	SwitcheBuilder builder();

	SwitchUpdater update(String name);

}
