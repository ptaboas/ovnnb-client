package com.simplyti.cloud.ovn.client.routers;

import java.util.Map;

import com.simplyti.cloud.ovn.client.AbstractNamedApiBuilder;
import com.simplyti.cloud.ovn.client.NamedResourceApi;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalRouter;

public class RouterBuilder extends AbstractNamedApiBuilder<LogicalRouter,RouterBuilder>{

	public RouterBuilder(NamedResourceApi<LogicalRouter> namedApi) {
		super(namedApi);
	}

	@Override
	protected LogicalRouter create(String name,Map<String,String> externalIds) {
		return new LogicalRouter(null,name,null,externalIds);
	}

}
