package com.simplyti.cloud.ovn.client.domain;

import com.google.common.base.Joiner;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class Endpoint {
	
	private final String ip;
	private final Integer port;
	
	@Override
	public String toString(){
		if(port==null){
			return ip;
		}else{
			return Joiner.on(':').join(ip,port);
		}
	}
	
	public static Endpoint valueOf(String value){
		String[] parts = value.split(":");
		if(parts.length==2){
			return new Endpoint(parts[0],Integer.parseInt(parts[1]));
		}else{
			return new Endpoint(value, null);
		}
		
	}

}
