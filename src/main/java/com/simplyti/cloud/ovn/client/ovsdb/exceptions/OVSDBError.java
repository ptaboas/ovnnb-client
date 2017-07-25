package com.simplyti.cloud.ovn.client.ovsdb.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OVSDBError {
	
	private final String error;

}
