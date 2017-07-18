package com.simplyti.cloud.ovn.client;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.Address;
import com.simplyti.cloud.ovn.client.domain.Vip;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBOperationResult;
import com.simplyti.cloud.ovn.client.domain.nb.AddLoadBalancerVipRequest;
import com.simplyti.cloud.ovn.client.domain.nb.AttachLoadBalancerToSwitchRequest;
import com.simplyti.cloud.ovn.client.domain.nb.CreateLoadBalancerRequest;
import com.simplyti.cloud.ovn.client.domain.nb.CreateLogicalSwitchRequest;
import com.simplyti.cloud.ovn.client.domain.nb.DeleteLoadBalancerRequest;
import com.simplyti.cloud.ovn.client.domain.nb.DeleteLoadBalancerVipRequest;
import com.simplyti.cloud.ovn.client.domain.nb.DeleteLogicalSwitchRequest;
import com.simplyti.cloud.ovn.client.domain.nb.DettachLoadBalancerRequest;
import com.simplyti.cloud.ovn.client.domain.nb.GetLoadBalancerRequest;
import com.simplyti.cloud.ovn.client.domain.nb.GetLogicalSwitchRequest;
import com.simplyti.cloud.ovn.client.domain.nb.LoadBalancer;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalSwitch;
import com.simplyti.cloud.ovn.client.domain.nb.Protocol;
import com.simplyti.cloud.ovn.client.domain.nb.UpdateLoadBalancerVipRequest;

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
public class OVNNbClient {
	
	private final ObjectMapper mapper;
	
	private final Bootstrap bootstrap;
	private final EventLoop executor;
	
	private Channel activeChannel;
	private Promise<Channel> acquiringChannel;
	
	private AtomicInteger requestId = new AtomicInteger();
	
	public static final AttributeKey<Map<Integer,Consumer<List<OVSDBOperationResult>>>> CONSUMERS = AttributeKey.valueOf("handlers");
	
	public OVNNbClient(EventLoopGroup eventLoopGroup, Address server,boolean verbose) {
		InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
		this.executor = eventLoopGroup.next();
		this.bootstrap = new Bootstrap().group(eventLoopGroup)
			.channel(channelClass())
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
			.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
			.remoteAddress(server.getHost(),server.getPort())
			.handler(new OVNNbClientChanneInitializer(verbose));
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
	}

	private Class<? extends Channel> channelClass() {
		return NioSocketChannel.class;
	}

	public static OVNNbClientBuilder builder(){
		return new OVNNbClientBuilder();
	}
	
	public Future<LogicalSwitch> getLogicalSwitch(String name) {
		Promise<LogicalSwitch> promise = executor.next().newPromise();
		acquireChannel().addListener(future->{
			GetLogicalSwitchRequest request = new GetLogicalSwitchRequest(requestId.getAndIncrement(),name);
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(request.getId(), results->{
				if(results.get(0).results().isEmpty()){
					promise.setSuccess(null);
				}else{
					promise.setSuccess(mapper.convertValue(results.get(0).result(), LogicalSwitch.class));
				}
			});
			channel.writeAndFlush(request);
		});
		return promise;
	}
	
	public Future<LoadBalancer> getLoadBalancer(String name,Protocol protocol) {
		Promise<LoadBalancer> promise = executor.next().newPromise();
		acquireChannel().addListener(future->{
			GetLoadBalancerRequest request = new GetLoadBalancerRequest(requestId.getAndIncrement(),lbId(name, protocol));
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(request.getId(), results->{
				if(results.get(0).results().isEmpty()){
					promise.setSuccess(null);
				}else{
					promise.setSuccess(mapper.convertValue(results.get(0).result(), LoadBalancer.class));
				}
			});
			channel.writeAndFlush(request);
		});
		return promise;
	}
	
	public Future<UUID> createLogicalSwitch(String name) {
		return createLogicalSwitch(name,Collections.emptyMap());
	}

	public Future<UUID> createLogicalSwitch(String name,  Map<String,String> externalIds) {
		Promise<UUID> promise = executor.next().newPromise();
		getLogicalSwitch(name).addListener(future->{
			if(future.getNow()==null){
				createLogicalSwitch(promise,name,externalIds);
			}else{
				promise.setFailure(new IllegalStateException("Logical switch "+name+" already exists"));
			}
		});
		return promise;
	}
	
	private void createLogicalSwitch(Promise<UUID> promise, String name, Map<String, String> externalIds) {
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				promise.setSuccess((UUID) results.get(0).result().get("uuid"));
			});
			channel.writeAndFlush(new CreateLogicalSwitchRequest(id,name,
					new LogicalSwitch(name,externalIds)));
		});
	}

	public Future<Void> deleteLogicalSwitch(String name) {
		return deleteLogicalSwitchs(Criteria.field("name").eq(name));
	}
	
	public Future<Void> deleteLogicalSwitchs(Criteria criteria) {
		Promise<Void> promise = executor.next().newPromise();
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				promise.setSuccess(null);
			});
			channel.writeAndFlush(new DeleteLogicalSwitchRequest(id,criteria));
		});
		return promise;
	}
	
	public Future<Void> deleteLoadBalancer(String name,Protocol protocol) {
		Promise<Void> promise = executor.next().newPromise();
		getLoadBalancer(name,protocol).addListener(futureLb->{
			if(futureLb.isSuccess()){
				if(futureLb.getNow()==null){
					promise.setSuccess(null);
				}else{
					LoadBalancer lb = (LoadBalancer) futureLb.getNow();
					dettachLoadBalancer(lb.getUuid()).addListener(futureDettach->{
						if(futureDettach.isSuccess()){
							deleteLoadBalancers(promise,Criteria.field("name").eq(lbId(name, protocol)));
						}else{
							promise.setFailure(futureDettach.cause());
						}
					});
				}
			}else{
				promise.setFailure(futureLb.cause());
			}
		});
		return promise;
	}
	
	public Future<Void> deleteLoadBalancerVip(String name, Vip vip) {
		Promise<Void> promise = executor.next().newPromise();
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				promise.setSuccess(null);
			});
			channel.writeAndFlush(new DeleteLoadBalancerVipRequest(id,lbId(name, vip.getProtocol()),vip.toString()));
		});
		return promise;
	}
	
	public Future<Void> dettachLoadBalancer(UUID lbuuid) {
		Promise<Void> promise = executor.next().newPromise();
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				promise.setSuccess(null);
			});
			channel.writeAndFlush(new DettachLoadBalancerRequest(id,lbuuid));
		});
		return promise;
	}

	public Future<Void> deleteLoadBalancers(Criteria criteria) {
		Promise<Void> promise = executor.next().newPromise();
		return deleteLoadBalancers(promise,criteria);
	}
	
	private Future<Void> deleteLoadBalancers(Promise<Void> promise, Criteria criteria) {
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				promise.setSuccess(null);
			});
			channel.writeAndFlush(new DeleteLoadBalancerRequest(id,criteria));
		});
		return promise;
	}

	public Future<Void> attachLoadBalancerToSwitch(String name, Protocol protocol, String logicalSwitch) {
		Promise<Void> promise = executor.next().newPromise();
		getLoadBalancer(name,protocol).addListener(future->{
			LoadBalancer lb = (LoadBalancer) future.getNow();
			attachLoadBalancerToSwitch(promise,lb.getUuid(),Collections.singleton(logicalSwitch));
		});
		return promise;
	}
	
	private Future<Void> attachLoadBalancerToSwitch(Promise<Void> promise, UUID lbUUID, Collection<String> logicalSwitches) {
		acquireChannel().addListener(future->{
			AttachLoadBalancerToSwitchRequest req = new AttachLoadBalancerToSwitchRequest(requestId.getAndIncrement(), lbUUID,logicalSwitches);
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(req.getId(), results->{
				promise.setSuccess(null);
			});
			channel.writeAndFlush(req);
		});
		return promise;
	}
	
	private String lbId(String name,Protocol protocol) {
		return Joiner.on(':').join(name,protocol.getValue());
	}

	public Future<UUID> createLoadBalancer(String name, Collection<String> attachedSwitches, Vip vip,  Collection<Address> ips, Map<String, String> externalIds) {
		log.info("Create load balancer {}[{}]: {}={}",name,vip.getProtocol(),vip,ips);
		Promise<UUID> promise = executor.next().newPromise();
		getLoadBalancer(name,vip.getProtocol()).addListener(future->{
			if(future.getNow()==null){
				createLoadBalancer(executor.next().newPromise(),name, vip,ips,externalIds).addListener(futureLb->{
						if(futureLb.isSuccess()){
							UUID lbUid = (UUID) futureLb.get();
							attachLoadBalancerToSwitch(executor.next().newPromise(), lbUid, attachedSwitches).addListener(futureAttach->{
								if(futureAttach.isSuccess()){
									promise.setSuccess(lbUid);
								}else{
									promise.setFailure(futureAttach.cause());
								}
							});
						}else{
							promise.setFailure(futureLb.cause());
						}
					});
			}else{
				LoadBalancer existingLoadBalancer = (LoadBalancer) future.getNow();
				String existingTargets = existingLoadBalancer.getVips().get(Joiner.on(':').join(vip.getHost(),vip.getPort()));
				if(existingTargets!=null){
					if(sameTargets(existingTargets, ips)){
						log.info("Load balancer {}[{}] for vip {} already exist, do nothing",name,vip.getProtocol(),vip);
						promise.setSuccess(existingLoadBalancer.getUuid());
					}else{
						updateLoadBalancerVip(existingLoadBalancer.getUuid(),vip,ips)
							.addListener(updateFuture->{
								if(updateFuture.isSuccess()){
									promise.setSuccess(existingLoadBalancer.getUuid());
								}else{
									promise.setFailure(updateFuture.cause());
								}
							});
					}
				}else{
					log.info("New vip for lb {}[{}]: {}={}",name,vip.getProtocol(),vip,ips);
					addLoadBalancerVip(promise,existingLoadBalancer.getUuid(),vip,ips);
				}
			}
		});
		return promise;
	}

	private boolean sameTargets(String existingTargets, Collection<Address> ips) {
		Iterable<String> existingTargetList = Splitter.on(',').split(existingTargets);
		if(Iterables.size(existingTargetList)==ips.size()){
			return ips.stream().map(ip->Joiner.on(':').join(ip.getHost(),ip.getPort()))
				.anyMatch(requiredTarget->Iterables.contains(existingTargetList, requiredTarget));
		}else{
			return false;
		}
		
	}
	
	private Future<UUID> createLoadBalancer(Promise<UUID> promise, String name, Vip vip, Collection<Address> ips, Map<String, String> externalIds) {
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				promise.setSuccess((UUID) results.get(0).result().get("uuid"));
			});
			channel.writeAndFlush(new CreateLoadBalancerRequest(id,
					new LoadBalancer(lbId(name, vip.getProtocol()),vip.getProtocol(),Collections.singletonMap(toEndpoint(vip), 
							ips.stream().map(this::toEndpoint).collect(Collectors.joining( "," ))),
							externalIds)));
		});
		return promise;
	}
	
	private void addLoadBalancerVip(Promise<UUID> promise, UUID lbId, Vip vip, Collection<Address> ips) {
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				promise.setSuccess(lbId);
			});
			channel.writeAndFlush(new AddLoadBalancerVipRequest(id,lbId,
					Collections.singletonMap(toEndpoint(vip), 
							ips.stream().map(this::toEndpoint).collect(Collectors.joining( "," )))));
		});
	}
	
	public Future<Void> updateLoadBalancerVip(UUID lbId, Vip vip, Collection<Address> ips) {
		Promise<Void> promise = executor.next().newPromise();
		updateLoadBalancerVip(promise,lbId, vip, ips);
		return promise;
	}
	
	private void updateLoadBalancerVip(Promise<Void> promise, UUID lbId, Vip vip, Collection<Address> ips) {
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				promise.setSuccess(null);
			});
			channel.writeAndFlush(new UpdateLoadBalancerVipRequest(id,lbId,
					Collections.singletonMap(toEndpoint(vip), 
							ips.stream().map(this::toEndpoint).collect(Collectors.joining( "," )))));
		});
	}

	private String toEndpoint(Vip vip) {
		if(vip.getPort()==null){
			return vip.getHost();
		}else{
			return Joiner.on(':').join(vip.getHost(), vip.getPort());
		}
	}
	
	private String toEndpoint(Address vip) {
		if(vip.getPort()==null){
			return vip.getHost();
		}else{
			return Joiner.on(':').join(vip.getHost(), vip.getPort());
		}
	}

	private Future<Channel> acquireChannel() {
		if(this.activeChannel!=null){
			if(this.activeChannel.isActive()){
				return executor.newSucceededFuture(this.activeChannel);
			}
		}
		if(acquiringChannel==null){
			this.acquiringChannel = executor.newPromise();
			executor.submit(()->acquireChannel(this.acquiringChannel));
		}
		return this.acquiringChannel;
	}
	
	private void acquireChannel(Promise<Channel> acquiringChannel) {
		bootstrap.connect().addListener(channelFuture->{
			if(channelFuture.isSuccess()){
				Channel channel = ((ChannelFuture)channelFuture).channel();
				channel.attr(CONSUMERS).set(Maps.newHashMap());
				acquiringChannel.setSuccess(channel);
			}else{
				log.error("Error connectig to ovn db: {}. Retrying",channelFuture.cause().getMessage());
				executor.schedule(()->acquireChannel(acquiringChannel), 1, TimeUnit.SECONDS);
			}
		});
	}

	public void close() {
		if(this.activeChannel!=null){
			this.activeChannel.close();
		}
	}

}
