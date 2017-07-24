package com.simplyti.cloud.ovn.client;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
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
import com.simplyti.cloud.ovn.client.domain.nb.GetLoadBalancersRequest;
import com.simplyti.cloud.ovn.client.domain.nb.GetLogicalSwitchRequest;
import com.simplyti.cloud.ovn.client.domain.nb.GetLogicalSwitchesRequest;
import com.simplyti.cloud.ovn.client.domain.nb.LoadBalancer;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalSwitch;
import com.simplyti.cloud.ovn.client.domain.nb.Protocol;
import com.simplyti.cloud.ovn.client.domain.nb.UpdateLoadBalancerVipRequest;
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
import io.netty.util.concurrent.PromiseCombiner;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class OVNNbClient {
	
	private final ObjectMapper mapper;
	
	private final Bootstrap bootstrap;
	private final EventLoop executor;
	
	private Channel activeChannel;
	private AtomicReference<Promise<Channel>> acquiringChannel = new AtomicReference<Promise<Channel>>();
	
	private AtomicInteger requestId = new AtomicInteger();
	
	public static final AttributeKey<Map<Integer,Consumer<List<OVSDBOperationResult>>>> CONSUMERS = AttributeKey.valueOf("handlers");
	
	private final Map<String,Future<LoadBalancer>> checkingLbExists = Maps.newConcurrentMap();
	
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
	
	public Future<List<LogicalSwitch>> getlogicalSwitches() {
		return getList(new GetLogicalSwitchesRequest(requestId.getAndIncrement()),new TypeReference<List<LogicalSwitch>>(){});
	}
	
	public Future<LogicalSwitch> getLogicalSwitch(String name) {
		return getItem(new GetLogicalSwitchRequest(requestId.getAndIncrement(),name),LogicalSwitch.class);
	}
	
	public Future<List<LoadBalancer>> getLoadBalancers(Criteria criteria) {
		return getList(new GetLoadBalancersRequest(requestId.getAndIncrement(),criteria), new TypeReference<List<LoadBalancer>>(){});
	}
	
	public Future<List<LoadBalancer>> getLoadBalancers() {
		return getList(new GetLoadBalancersRequest(requestId.getAndIncrement()), new TypeReference<List<LoadBalancer>>(){});
	}
	
	public Future<LoadBalancer> getLoadBalancer(String name,Protocol protocol) {
		return getItem(new GetLoadBalancerRequest(requestId.getAndIncrement(),lbId(name, protocol)),LoadBalancer.class);
	}
	
	public Future<LoadBalancer> getLoadBalancer(String name) {
		return getItem(new GetLoadBalancerRequest(requestId.getAndIncrement(),name),LoadBalancer.class);
	}
	
	private <T> Future<List<T>> getList(OVSRequest request,TypeReference<List<T>> clazz) {
		Promise<List<T>> promise = executor.next().newPromise();
		acquireChannel().addListener(future->{
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(request.getId(), results->{
				promise.setSuccess(mapper.convertValue(results.get(0).results(), clazz));
			});
			channel.writeAndFlush(request);
		});
		return promise;
	}

	private <T> Future<T> getItem(OVSRequest request,Class<T> clazz) {
		Promise<T> promise = executor.next().newPromise();
		acquireChannel().addListener(future->{
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(request.getId(), results->{
				if(results.get(0).results().isEmpty()){
					promise.setSuccess(null);
				}else{
					promise.setSuccess(mapper.convertValue(results.get(0).result(), clazz));
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
				LogicalSwitch existing = (LogicalSwitch) future.getNow();
				promise.setSuccess(existing.getUuid());
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
			channel.writeAndFlush(new CreateLogicalSwitchRequest(id,name,new LogicalSwitch(name,externalIds)));
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
	
	public Future<Void> deleteLogicalSwitches() {
		Promise<Void> promise = executor.next().newPromise();
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				promise.setSuccess(null);
			});
			channel.writeAndFlush(new DeleteLogicalSwitchRequest(id));
		});
		return promise;
	}
	
	public Future<Void> deleteLoadBalancers() {
		Promise<Void> promise = executor.next().newPromise();
		getLoadBalancers().addListener(futureLb->onSuccess(futureLb,promise,result->{
				List<?> lbs =  (List<?>) result;
				PromiseCombiner combiner = new PromiseCombiner();
				lbs.forEach(lb->{
					Promise<Void> deletePromise = executor.next().newPromise();
					deleteLoadBalancer(deletePromise,((LoadBalancer)lb).getUuid());
					combiner.add((Future<?>)deletePromise);
				});
				combiner.finish(promise);
		}));
		return promise;
	}
	
	private <T> void onSuccess(Future<T> future,Promise<?> promise, Consumer<T> consumer) {
		if(future.isSuccess()){
			consumer.accept(future.getNow());
		}else{
			promise.setFailure(future.cause());
		}
	}

	public Future<Void> deleteLoadBalancer(String name,Protocol protocol) {
		return deleteLoadBalancer(lbId(name, protocol));
	}
	
	public Future<Void> deleteLoadBalancer(String name) {
		Promise<Void> promise = executor.next().newPromise();
		getLoadBalancer(name).addListener(futureLb->onSuccess(futureLb,promise,result->{
				if(result==null){
					promise.setSuccess(null);
				}else{
					deleteLoadBalancer(promise,((LoadBalancer)result).getUuid());
				}
		}));
		return promise;
	}
	
	private void deleteLoadBalancer(Promise<Void> promise, UUID uuid) {
		dettachLoadBalancer(uuid).addListener(futureDettach->onSuccess(futureDettach,promise,result->{
				deleteLoadBalancers(promise,Criteria.field("_uuid").eq(uuid));
		}));
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
		getLoadBalancers(criteria).addListener(futureLbs->onSuccess(futureLbs,promise,result->{
				List<?> lbs = (List<?>) result;
				PromiseCombiner combiner =new PromiseCombiner();
				lbs.forEach(lb->{
					Promise<Void> item = executor.next().newPromise();
					combiner.add((Future<?>)item);
					deleteLoadBalancer(item,((LoadBalancer)lb).getUuid());
				});
				combiner.finish(promise);
		}));
		return promise;
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
	
	public Future<UUID> createLoadBalancer(String name, Collection<String> attachedSwitches, Vip vip,  Collection<Address> ips) {
		return createLoadBalancer(name, attachedSwitches, vip, ips, null);
	}

	public Future<UUID> createLoadBalancer(String name, Collection<String> attachedSwitches, Vip vip,  Collection<Address> ips, Map<String, String> externalIds) {
		Promise<UUID> promise = executor.next().newPromise();
		String lbName = Joiner.on(':').join(name, vip.getProtocol().getValue());
		
		Promise<LoadBalancer> createLoadBalancerPromise = executor.next().newPromise();
		Future<LoadBalancer> existing = checkingLbExists.computeIfAbsent(lbName, (k)->createLoadBalancerIfNotExist(createLoadBalancerPromise,lbName,attachedSwitches,vip,ips,externalIds));
		
		if(existing!=createLoadBalancerPromise){
			log.info("Load balancer {}[{}] is being created by other",name,vip.getProtocol());
			createLoadBalancerPromise.cancel(false);
			existing.addListener(futureLoadBalancer->{
				onSuccess(futureLoadBalancer,promise,result->{
					LoadBalancer loadBalancer = (LoadBalancer) result;
					String existingTargets = loadBalancer.getVips().get(Joiner.on(':').join(vip.getHost(),vip.getPort()));
					if(existingTargets!=null){
						if(sameTargets(existingTargets, ips)){
							log.info("Load balancer {}[{}] has vip {} updated",name,vip.getProtocol(),vip);
							promise.setSuccess(loadBalancer.getUuid());
						}else{
							log.info("Load balancer {}[{}] vip {} must be updated",name,vip.getProtocol(),vip);
							updateLoadBalancerVip(loadBalancer.getUuid(),vip,ips).addListener(updateFuture->
								onSuccess(updateFuture,promise,update->promise.setSuccess(loadBalancer.getUuid())));
						}
					}else{
						log.info("Load balancer {}[{}] does not contains vip {}, add it",name,vip.getProtocol(),vip);
						addLoadBalancerVip(promise,loadBalancer.getUuid(),vip,ips);
					}
				});
			});
		}else{
			log.info("Load balancer {}[{}] is being created by this",name,vip.getProtocol(),vip);
			existing.addListener(futureLoadBalancer->
				onSuccess(futureLoadBalancer,promise,result->promise.setSuccess(((LoadBalancer) result).getUuid())
			));
		}
		return promise;
	}

	private Future<LoadBalancer> createLoadBalancerIfNotExist(Promise<LoadBalancer> promise, String name, Collection<String> attachedSwitches, Vip vip,
			Collection<Address> ips, Map<String, String> externalIds) {
		log.info("Create load balancer {}[{}]: {}={}",name,vip.getProtocol(),vip,ips);
		getLoadBalancer(name).addListener(future->{
			if(future.getNow()!=null){
				checkingLbExists.remove(name);
				LoadBalancer existingLb = (LoadBalancer) future.getNow();
				addLoadBalancerVip(executor.next().newPromise(),existingLb.getUuid(),vip,ips).addListener(futureAddVip->
					onSuccess(futureAddVip,promise,result->promise.setSuccess(existingLb)));
			}else{
				createLoadBalancer(executor.next().newPromise(),name, vip,ips,externalIds).addListener(futureLb->{
					checkingLbExists.remove(name);
					onSuccess(futureLb,promise,result->{
						UUID lbUid = (UUID) result;
						attachLoadBalancerToSwitch(executor.next().newPromise(), lbUid, attachedSwitches).addListener(futureAttach->
							onSuccess(futureAttach,promise,attach->promise.setSuccess(loadBalancer(lbUid,name, vip, ips, externalIds)))
						);
					});
				});
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
					loadBalancer(name,vip,ips,externalIds)));
		});
		return promise;
	}
	
	private LoadBalancer loadBalancer(UUID uuid, String name, Vip vip, Collection<Address> ips, Map<String, String> externalIds) {
		return new LoadBalancer(uuid,
				lbId(name, vip.getProtocol()),
				vip.getProtocol().getValue(),
				Collections.singletonMap(toEndpoint(vip),ips.stream().map(this::toEndpoint).collect(Collectors.joining( "," ))),
				externalIds);
	}
	
	private LoadBalancer loadBalancer(String name, Vip vip, Collection<Address> ips, Map<String, String> externalIds) {
		return new LoadBalancer(
				name,
				vip.getProtocol(),
				Collections.singletonMap(toEndpoint(vip),ips.stream().map(this::toEndpoint).collect(Collectors.joining( "," ))),
				externalIds);
	}

	private Future<UUID> addLoadBalancerVip(Promise<UUID> promise, UUID lbId, Vip vip, Collection<Address> ips) {
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
		return promise;
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
		
		Promise<Channel> newAcquiringChannel = executor.newPromise();
		if(acquiringChannel.compareAndSet(null, newAcquiringChannel)){
			executor.submit(()->acquireChannel(newAcquiringChannel));
			return newAcquiringChannel;
		}else{
			newAcquiringChannel.cancel(false);
			return acquiringChannel.get();
		}
		
	}
	
	private void acquireChannel(Promise<Channel> acquiringChannel) {
		bootstrap.connect().addListener(channelFuture->{
			if(channelFuture.isSuccess()){
				activeChannel = ((ChannelFuture)channelFuture).channel();
				activeChannel.attr(CONSUMERS).set(Maps.newHashMap());
				acquiringChannel.setSuccess(activeChannel);
				this.acquiringChannel.set(null);
			}else{
				log.error("Error connectig to ovn db: {}. Retrying",channelFuture.cause().getMessage());
				executor.schedule(()->acquireChannel(acquiringChannel), 1, TimeUnit.SECONDS);
			}
		});
	}

	public void close() {
		if(this.activeChannel!=null){
			this.activeChannel.close();
			this.activeChannel=null;
		}
	}

}
