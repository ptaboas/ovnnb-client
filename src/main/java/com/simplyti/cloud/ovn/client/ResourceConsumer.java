package com.simplyti.cloud.ovn.client;

import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;

import io.netty.util.concurrent.Promise;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ResourceConsumer<T> {
	
	private Promise<T> promise;
	@Getter
	private final TypeReference<T> resourceClass;

	private final Consumer<T> consumer;
	
	public void accept(T results) {
		consumer.accept(results);
	}

	public void setFailure(Throwable error) {
		promise.setFailure(error);
	}

}
