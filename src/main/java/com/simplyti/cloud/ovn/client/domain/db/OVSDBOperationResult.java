package com.simplyti.cloud.ovn.client.domain.db;

import java.util.List;
import java.util.Map;

import lombok.Getter;

public class OVSDBOperationResult {
	
	private final List<Map<String,Object>> results;
	
	@Getter
	private final String error;

	public static OVSDBOperationResult error(String msg) {
		return new OVSDBOperationResult(msg);
	}

	public static OVSDBOperationResult result(List<Map<String, Object>> results) {
		return new OVSDBOperationResult(results);
	}
	
	public static OVSDBOperationResult ignored(Object object) {
		return new OVSDBOperationResult();
	}
	
	private OVSDBOperationResult() {
		this.results=null;
		this.error=null;
	}
	
	private OVSDBOperationResult(List<Map<String, Object>> results) {
		this.results=results;
		this.error=null;
	}
	
	private OVSDBOperationResult(String error) {
		this.error=error;
		this.results=null;
	}

	public Map<String,Object> result() {
		return results.get(0);
	}

	public List<Map<String, Object>> results() {
		return results;
	}


}
