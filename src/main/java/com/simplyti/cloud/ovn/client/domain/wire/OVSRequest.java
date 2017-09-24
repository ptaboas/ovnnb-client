package com.simplyti.cloud.ovn.client.domain.wire;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OVSRequest {
	
	private final int id;
	private final OVSMethod method;
	private final Collection<Object> params;

}
