package com.simplyti.cloud.ovn.client.jsonrpc.domain;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class JsonRpcResponse extends JsonRpcMessage{
	
	private Collection<Object> result;
	private String error;
	
	@JsonCreator
	public JsonRpcResponse(@JsonProperty("id") Object id,
			@JsonProperty("result") Collection<Object> result, 
			@JsonProperty("error") String error) {
		super(id);
		this.result=result;
		this.error=error;
	}

}
