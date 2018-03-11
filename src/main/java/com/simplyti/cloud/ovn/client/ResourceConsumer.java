package com.simplyti.cloud.ovn.client;

import java.util.function.Consumer;

import com.jsoniter.spi.TypeLiteral;

import io.netty.util.concurrent.Promise;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ResourceConsumer<T> {
	
	private Promise<T> promise;
	@Getter
	private final TypeLiteral<T> resourceClass;

	private final Consumer<T> consumer;
	
	public void accept(T results) {
		consumer.accept(results);
	}

	public void setFailure(Throwable error) {
		promise.setFailure(error);
	}

}
