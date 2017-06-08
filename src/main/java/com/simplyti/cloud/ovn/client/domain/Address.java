package com.simplyti.cloud.ovn.client.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Address {
	
	private final String host;
	private final Integer port;

}
