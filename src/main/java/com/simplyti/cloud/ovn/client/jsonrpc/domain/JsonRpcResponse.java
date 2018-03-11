package com.simplyti.cloud.ovn.client.jsonrpc.domain;

import java.util.List;

import com.jsoniter.annotation.JsonCreator;
import com.jsoniter.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class JsonRpcResponse extends JsonRpcMessage{
	
	private List<Object> result;
	private String error;
	
	@JsonCreator
	public JsonRpcResponse(@JsonProperty("id") Object id,
			@JsonProperty("result") List<Object> result, 
			@JsonProperty("error") String error) {
		super(id);
		this.result=result;
		this.error=error;
	}

}
