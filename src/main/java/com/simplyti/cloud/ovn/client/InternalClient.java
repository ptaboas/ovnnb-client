package com.simplyti.cloud.ovn.client;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
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
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class InternalClient {
	
	public static final AttributeKey<Map<Integer,ResourceConsumer<?>>> CONSUMERS = AttributeKey.valueOf("handlers");
	private static final AttributeKey<AtomicInteger> RPC_ID_GEN = AttributeKey.valueOf("rpcIdGen");
	
	private final EventLoopGroup eventLoopGroup;
	
	private final Bootstrap bootstrap;
	private final ObjectMapper mapper;
	
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
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
	}
	
	private Class<? extends Channel> channelClass() {
		return NioSocketChannel.class;
	}
	
	public <T> Future<T> call(TypeReference<T> resourceClass, Function<Integer,OVSRequest> requestSupplier) {
		Promise<T> promise = eventLoopGroup.next().newPromise();
		call(promise, resourceClass, requestSupplier);
		return promise;
	}
	
	public <T> Future<T> call(Promise<T> promise,TypeReference<T> resourceClass, Function<Integer,OVSRequest> requestSupplier) {
		acquireChannel().addListener(future->{
			if(future.isSuccess()){
				Channel channel = (Channel) future.getNow();
				int reqId = channel.attr(RPC_ID_GEN).get().getAndIncrement();
				channel.attr(CONSUMERS).get().put(reqId,new ResourceConsumer<T>(promise,resourceClass,results->{
					promise.setSuccess(results);
				}));
				channel.writeAndFlush(requestSupplier.apply(reqId));
			}else{
				promise.setFailure(future.cause());
			}
		});
		return promise;
	}

	private Future<Channel> acquireChannel() {
		Channel currrentChannel = acquiredChannel.get();
		if(currrentChannel!=null && currrentChannel.isActive()){
			return eventLoopGroup.next().newSucceededFuture(currrentChannel);
		}
		
		Promise<Channel> futureChannel = eventLoopGroup.next().newPromise();
		if(acquireChannelExecutor.inEventLoop()){
			acquireChannel(futureChannel);
		}else{
			acquireChannelExecutor.submit(()->acquireChannel(futureChannel));
		}
		return futureChannel;
	}
	
	private void acquireChannel(Promise<Channel> channelPromise) {
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
				channel.attr(CONSUMERS).set(Maps.newHashMap());
				channel.attr(RPC_ID_GEN).set(new AtomicInteger());
				acquiredChannel.set(channel);
				if(acquireChannelExecutor.inEventLoop()){
					this.acquiringChannel.set(null);
				}else{
					acquireChannelExecutor.submit(()->this.acquiringChannel.set(null));
				}
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
	
	public <T> Promise<T> newPromise() {
		return eventLoopGroup.next().newPromise();
	}

	public void close() {
		if(acquireChannelExecutor.inEventLoop()){
			close0(acquiredChannel.getAndSet(null));
		}else{
			acquireChannelExecutor.submit(()->close0(acquiredChannel.getAndSet(null)));
		}
	}

	private void close0(Channel channel) {
		if(channel!=null){
			channel.close();
		}
	}

	public void lastEcho(Instant instant) {
		this.lastEcho.set(instant);
	}

	public Instant lastEcho() {
		return this.lastEcho.get();
	}

}
