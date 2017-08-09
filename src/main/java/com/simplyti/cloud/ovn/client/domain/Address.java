package com.simplyti.cloud.ovn.client.domain;

import com.google.common.base.Joiner;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(of={"host","port"})
public class Address {
	
	public Address(String host,Integer port){
		this(host,port,null);
	}
	
	private final String host;
	private final Integer port;
	private final String portName;
	
	@Override
	public String toString(){
		return Joiner.on(':').join(host, port);
	}

}
