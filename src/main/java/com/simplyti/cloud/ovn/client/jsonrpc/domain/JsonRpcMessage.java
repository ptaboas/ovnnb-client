package com.simplyti.cloud.ovn.client.jsonrpc.domain;

import lombok.Getter;

@Getter
public abstract class JsonRpcMessage {
	
	private final Object id;
	
	public JsonRpcMessage(Object id) {
		this.id=id;
	}

	

}
