package com.simplyti.cloud.ovn.client;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


public class ParameterizedTypeImpl implements ParameterizedType {

	private final Type raw;
	private final Type[] typeArguments;

	public ParameterizedTypeImpl(Class<?> raw, Type... typeArguments) {
		this.raw=raw;
		this.typeArguments=typeArguments;
	}

	@Override
	public Type[] getActualTypeArguments() {
		return typeArguments;
	}

	@Override
	public Type getRawType() {
		return raw;
	}

	@Override
	public Type getOwnerType() {
		return null;
	}

}
