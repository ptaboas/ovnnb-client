package com.simplyti.cloud.ovn.client.domain;

import com.google.common.base.Joiner;
import com.simplyti.cloud.ovn.client.domain.nb.Protocol;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class Vip {
	
	private final String host;
	private final Integer port;
	private final Protocol protocol;
	
	@Override
	public String toString(){
		return Joiner.on(':').join(host, port);
	}

}
