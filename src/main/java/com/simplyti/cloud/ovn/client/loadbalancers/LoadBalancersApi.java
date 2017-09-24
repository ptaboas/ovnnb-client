package com.simplyti.cloud.ovn.client.loadbalancers;

import java.util.UUID;

import com.simplyti.cloud.ovn.client.NamedResourceApi;
import com.simplyti.cloud.ovn.client.domain.nb.LoadBalancer;

public interface LoadBalancersApi extends NamedResourceApi<LoadBalancer> {

	LoadBalancerBuilder builder();

	LoadBalancerUpdater update(UUID uuid);

	LoadBalancerUpdater update(String name);

}
