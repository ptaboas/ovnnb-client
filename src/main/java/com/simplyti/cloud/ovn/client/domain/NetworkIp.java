package com.simplyti.cloud.ovn.client.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NetworkIp {
	
	private final String value;

	public static NetworkIp valueOf(String value) {
		return new NetworkIp(value);
	}
	
	@Override
	public String toString(){
		return value;
	}

}
