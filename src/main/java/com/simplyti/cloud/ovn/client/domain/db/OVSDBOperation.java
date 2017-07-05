package com.simplyti.cloud.ovn.client.domain.db;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum OVSDBOperation {
	
	INSERT("insert"),
	SELECT("select"), 
	DELETE("delete"), 
	MUTATE("mutate"), 
	UPDATE("update");
	
	@Getter
	private final String name;

}
