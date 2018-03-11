package com.simplyti.cloud.ovn.client;

import java.nio.channels.ClosedChannelException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.google.common.collect.Maps;
import com.jsoniter.spi.TypeLiteral;
import com.simplyti.cloud.ovn.client.domain.wire.OVSMethod;
import com.simplyti.cloud.ovn.client.domain.wire.OVSRequest;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;

public class InternalClient {
	
	private final InternalLogger log = InternalLoggerFactory.getInstance(getClass());
	
	public static final AttributeKey<Map<Integer,ResourceConsumer<?>>> CONSUMERS = AttributeKey.valueOf("handlers");
	private static final AttributeKey<AtomicInteger> RPC_ID_GEN = AttributeKey.valueOf("rpcIdGen");
	private static final AttributeKey<LocalDateTime> LAST_CHECK = AttributeKey.valueOf("lastCheck");
	private static final TypeLiteral<Void> VOID = new TypeLiteral<Void>(){};
	
	private final EventLoopGroup eventLoopGroup;
	
	private final Bootstrap bootstrap;
	
	private final EventLoop acquireChannelExecutor;
	private final AtomicReference<Channel> acquiredChannel;
	private final AtomicReference<Promise<Channel>> acquiringChannel;
	private final AtomicReference<Instant> lastEcho;
	
	public InternalClient(EventLoopGroup eventLoopGroup, String host, int port, boolean verbose){
		InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
		this.eventLoopGroup = eventLoopGroup;
		this.acquireChannelExecutor = eventLoopGroup.next();
		this.acquiredChannel = new AtomicReference<>();
		this.acquiringChannel = new AtomicReference<>();
		this.lastEcho = new AtomicReference<>();
		this.bootstrap = new Bootstrap().group(eventLoopGroup)
			.channel(channelClass())
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
			.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
			.remoteAddress(host,port)
			.handler(new OVNNbClientChanneInitializer(verbose,this));
	}
	
	private Class<? extends Channel> channelClass() {
		return NioSocketChannel.class;
	}
	
	public <T> Future<T> call(TypeLiteral<T> resourceClass, Function<Integer,OVSRequest> requestSupplier) {
		Promise<T> promise = eventLoopGroup.next().newPromise();
		call(promise, resourceClass, requestSupplier);
		return promise;
	}
	
	public <T> Future<T> call(Promise<T> promise,TypeLiteral<T> resourceClass, Function<Integer,OVSRequest> requestSupplier) {
		acquireChannel().addListener(future->{
			if(future.isSuccess()){
				Channel channel = (Channel) future.getNow();
				int reqId = channel.attr(RPC_ID_GEN).get().getAndIncrement();
				ResourceConsumer<T> consumer = new ResourceConsumer<T>(promise,resourceClass,results->{
					promise.setSuccess(results);
				});
				channel.attr(CONSUMERS).get().put(reqId,consumer);
				channel.writeAndFlush(requestSupplier.apply(reqId)).addListener(f->setFailWhenFutureFail(f,consumer));
			}else{
				promise.setFailure(future.cause());
			}
		});
		return promise;
	}
	
	private void setFailWhenFutureFail(Future<?> f, ResourceConsumer<?> consumer) {
		if(!f.isSuccess()){
			consumer.setFailure(f.cause());
		}
	}

	private Future<Channel> acquireChannel() {
		Channel currrentChannel = acquiredChannel.get();
		if(currrentChannel!=null){
			if(currrentChannel.isActive() && currrentChannel.attr(LAST_CHECK).get().isAfter(LocalDateTime.now().minusSeconds(5))){
				return eventLoopGroup.next().newSucceededFuture(currrentChannel);
			}else{
				return checkChannelState(currrentChannel);
			}
		}else{
			return acquireNewChannel();
		}
	}
	
	private Future<Channel> checkChannelState(Channel channel) {
		Promise<Channel> futureChannel = channel.eventLoop().newPromise();
		int reqId = channel.attr(RPC_ID_GEN).get().getAndIncrement();
		Promise<Void> checkFuture = channel.eventLoop().newPromise();
		ResourceConsumer<Void> consumer = new ResourceConsumer<Void>(checkFuture,VOID,results->checkFuture.setSuccess(results));
		channel.attr(CONSUMERS).get().put(reqId,consumer);
		channel.writeAndFlush(new OVSRequest(reqId,OVSMethod.ECHO,Collections.emptyList())).addListener(f->setFailWhenFutureFail(f,consumer));
		checkFuture.addListener(future->{
			if(future.isSuccess()){
				channel.attr(LAST_CHECK).set(LocalDateTime.now());
				futureChannel.setSuccess(channel);
			}else{
				acquireNewChannel(futureChannel);
			}
		});
		return futureChannel;
	}

	private Future<Channel> acquireNewChannel() {
		return acquireNewChannel(acquireChannelExecutor.newPromise());
	}

	private Future<Channel> acquireNewChannel(Promise<Channel> futureChannel) {
		if(acquireChannelExecutor.inEventLoop()){
			acquireNewChannel0(futureChannel);
		}else{
			acquireChannelExecutor.submit(()->acquireNewChannel0(futureChannel));
		}
		return futureChannel;
	}

	private void acquireNewChannel0(Promise<Channel> channelPromise) {
		if(acquiringChannel.get()!=null){
			acquiringChannel.get().addListener(channelFuture->{
				if(channelFuture.isSuccess()){
					channelPromise.setSuccess((Channel) channelFuture.getNow());
				}else{
					channelPromise.setFailure(channelFuture.cause());
				}
			});
			return;
		}
		bootstrap.connect().addListener(channelFuture->{
			if(channelFuture.isSuccess()){
				Channel channel = ((ChannelFuture)channelFuture).channel();
				channel.attr(LAST_CHECK).set(LocalDateTime.now());
				channel.attr(CONSUMERS).set(Maps.newHashMap());
				channel.attr(RPC_ID_GEN).set(new AtomicInteger());
				acquiredChannel.set(channel);
				if(acquireChannelExecutor.inEventLoop()){
					this.acquiringChannel.set(null);
				}else{
					acquireChannelExecutor.submit(()->this.acquiringChannel.set(null));
				}
				channel.closeFuture().addListener(f->setFailure(channel));
				channelPromise.setSuccess(channel);
			}else{
				log.error("Error connectig to ovn db: {}",channelFuture.cause().getMessage());
				if(acquireChannelExecutor.inEventLoop()){
					this.acquiringChannel.set(null);
				}else{
					acquireChannelExecutor.submit(()->this.acquiringChannel.set(null));
				}
				channelPromise.setFailure(channelFuture.cause());
			}
		});
		acquiringChannel.set(channelPromise);
	}
	
	private void setFailure(Channel channel) {
		channel.attr(CONSUMERS).get().values().forEach(consumer->consumer.setFailure(new ClosedChannelException()));
	}

	public <T> Promise<T> newPromise() {
		return eventLoopGroup.next().newPromise();
	}
	
	public <T> Future<T> newSucceededFuture(T object) {
		return eventLoopGroup.next().newSucceededFuture(object);
	}
	
	public <T> Future<T> newFailedFuture(Throwable cause) {
		return eventLoopGroup.next().newFailedFuture(cause);
	}

	public Future<Void> close() {
		Promise<Void> promise = acquireChannelExecutor.newPromise();
		if(acquireChannelExecutor.inEventLoop()){
			close0(acquiredChannel.getAndSet(null),promise);
		}else{
			acquireChannelExecutor.submit(()->close0(acquiredChannel.getAndSet(null),promise));
		}
		return promise;
	}

	private void close0(Channel channel, Promise<Void> promise) {
		if(channel!=null){
			channel.close().addListener(f->promise.setSuccess(null));
		}
	}

	public void lastEcho(Instant instant) {
		this.lastEcho.set(instant);
	}

	public Instant lastEcho() {
		return this.lastEcho.get();
	}

}
