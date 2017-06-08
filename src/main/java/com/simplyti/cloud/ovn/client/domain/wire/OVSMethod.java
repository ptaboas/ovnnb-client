package com.simplyti.cloud.ovn.client.domain.wire;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum OVSMethod {
	
	LIST_DATABASE("list_dbs"),
	TRANSACT("transact");
	
	@Getter
	private final String name;

}
