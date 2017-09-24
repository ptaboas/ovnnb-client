package com.simplyti.cloud.ovn.client;

import java.lang.reflect.Type;

import com.fasterxml.jackson.core.type.TypeReference;

public class TypeReferenceLiteral<T> extends TypeReference<T> {

	private Type type;

	public TypeReferenceLiteral(Type type) {
		this.type=type;
	}
	
	@Override
	public Type getType(){
		return type;
	}

}
