package com.simplyti.cloud.ovn.client.ovsdb.exceptions;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OVSDBException extends RuntimeException {

	private final List<OVSDBError> errors;

	/**
	 * 
	 */
	private static final long serialVersionUID = 4061320327050109874L;

}
