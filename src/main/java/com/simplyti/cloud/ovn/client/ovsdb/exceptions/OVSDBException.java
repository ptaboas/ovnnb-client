package com.simplyti.cloud.ovn.client.ovsdb.exceptions;

import lombok.Getter;

@Getter
public class OVSDBException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4061320327050109874L;
	
	public OVSDBException(String message) {
		super(message);
	}

}
