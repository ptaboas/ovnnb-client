package com.simplyti.cloud.ovn.client.routers;

import com.simplyti.cloud.ovn.client.NamedResourceApi;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalRouter;

public interface RoutersApi extends NamedResourceApi<LogicalRouter> {

	RouterBuilder builder();

}
