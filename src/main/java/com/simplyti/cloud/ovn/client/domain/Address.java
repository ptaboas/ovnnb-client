package com.simplyti.cloud.ovn.client.domain;

import com.google.common.base.Joiner;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class Address {
	
	private final String host;
	private final Integer port;
	
	@Override
	public String toString(){
		return Joiner.on(':').join(host, port);
	}

}
