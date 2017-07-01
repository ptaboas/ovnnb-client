package com.simplyti.cloud.ovn.client.domain.nb;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Protocol {
	
	TCP("tcp"),UDP("udp");
	
	private final String value;

}
