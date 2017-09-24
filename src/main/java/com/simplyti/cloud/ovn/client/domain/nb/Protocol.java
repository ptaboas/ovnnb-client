package com.simplyti.cloud.ovn.client.domain.nb;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Protocol {
	
	TCP("tcp"),UDP("udp");
	
	private final String value;
	
	@Override
	public String toString(){
		return value;
	}
	
}
