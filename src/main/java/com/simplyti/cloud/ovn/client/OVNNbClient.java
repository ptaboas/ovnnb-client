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
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.Address;
import com.simplyti.cloud.ovn.client.domain.Vip;
import com.simplyti.cloud.ovn.client.domain.db.OVSDBOperationResult;
import com.simplyti.cloud.ovn.client.domain.nb.AddLoadBalancerVipRequest;
import com.simplyti.cloud.ovn.client.domain.nb.AttachLoadBalancerToRouterRequest;
import com.simplyti.cloud.ovn.client.domain.nb.AttachLoadBalancerToSwitchRequest;
import com.simplyti.cloud.ovn.client.domain.nb.CreateLoadBalancerRequest;
import com.simplyti.cloud.ovn.client.domain.nb.CreateLogicalRouterRequest;
import com.simplyti.cloud.ovn.client.domain.nb.CreateLogicalSwitchRequest;
import com.simplyti.cloud.ovn.client.domain.nb.DeleteLoadBalancerRequest;
import com.simplyti.cloud.ovn.client.domain.nb.DeleteLoadBalancerVipRequest;
import com.simplyti.cloud.ovn.client.domain.nb.DeleteLogicalRouterRequest;
import com.simplyti.cloud.ovn.client.domain.nb.DeleteLogicalSwitchRequest;
import com.simplyti.cloud.ovn.client.domain.nb.DettachLoadBalancerRequest;
import com.simplyti.cloud.ovn.client.domain.nb.GetLoadBalancerRequest;
import com.simplyti.cloud.ovn.client.domain.nb.GetLoadBalancersRequest;
import com.simplyti.cloud.ovn.client.domain.nb.GetLogicalRouterRequest;
import com.simplyti.cloud.ovn.client.domain.nb.GetLogicalRoutersRequest;
import com.simplyti.cloud.ovn.client.domain.nb.GetLogicalSwitchRequest;
import com.simplyti.cloud.ovn.client.domain.nb.GetLogicalSwitchesRequest;
import com.simplyti.cloud.ovn.client.domain.nb.LoadBalancer;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalRouter;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalSwitch;
import com.simplyti.cloud.ovn.client.domain.nb.Protocol;
import com.simplyti.cloud.ovn.client.domain.nb.UpdateLoadBalancerVipRequest;
import com.simplyti.cloud.ovn.client.domain.wire.OVSRequest;
import com.simplyti.cloud.ovn.client.ovsdb.exceptions.OVSDBError;
import com.simplyti.cloud.ovn.client.ovsdb.exceptions.OVSDBException;

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
	private final EventLoopGroup eventLoopGroup;
	private final EventLoop acquireChannelExecutor;
	
	private Channel activeChannel;
	private AtomicReference<Promise<Channel>> acquiringChannel = new AtomicReference<Promise<Channel>>();
	
	private AtomicInteger requestId = new AtomicInteger();
	
	public static final AttributeKey<Map<Integer,Consumer<List<OVSDBOperationResult>>>> CONSUMERS = AttributeKey.valueOf("handlers");
	
	private final Map<String,Future<LoadBalancer>> checkingLbExists = Maps.newConcurrentMap();
	
	public OVNNbClient(EventLoopGroup eventLoopGroup, Address server,boolean verbose) {
		InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
		this.eventLoopGroup = eventLoopGroup;
		this.acquireChannelExecutor = eventLoopGroup.next();
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
	
	public Future<List<LogicalRouter>> getlogicalRouters() {
		return getList(new GetLogicalRoutersRequest(requestId.getAndIncrement()),new TypeReference<List<LogicalRouter>>(){});
	}
	
	public Future<LogicalSwitch> getLogicalSwitch(String name) {
		return getItem(new GetLogicalSwitchRequest(requestId.getAndIncrement(),name),LogicalSwitch.class);
	}
	
	public Future<LogicalRouter> getLogicalRouter(String name) {
		return getItem(new GetLogicalRouterRequest(requestId.getAndIncrement(),name),LogicalRouter.class);
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
		Promise<List<T>> promise = eventLoopGroup.next().newPromise();
		acquireChannel().addListener(future->{
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(request.getId(), results->{
				onSuccess(promise,results,ovsRes->mapper.convertValue(ovsRes.get(0).results(), clazz));
			});
			channel.writeAndFlush(request);
		});
		return promise;
	}

	private <T> Future<T> getItem(OVSRequest request,Class<T> clazz) {
		Promise<T> promise = eventLoopGroup.next().newPromise();
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
		Promise<UUID> promise = eventLoopGroup.next().newPromise();
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
				onSuccess(promise,results,ovsRes->(UUID) ovsRes.get(0).result().get("uuid"));
			});
			channel.writeAndFlush(new CreateLogicalSwitchRequest(id,name,new LogicalSwitch(name,externalIds)));
		});
	}
	
	public Future<UUID> createLogicalRouter(String name) {
		return createLogicalRouter(name,Collections.emptyMap());
	}
	
	public Future<UUID> createLogicalRouter(String name,  Map<String,String> externalIds) {
		Promise<UUID> promise = eventLoopGroup.next().newPromise();
		getLogicalRouter(name).addListener(future->{
			if(future.getNow()==null){
				createLogicalRouter(promise,name,externalIds);
			}else{
				LogicalRouter existing = (LogicalRouter) future.getNow();
				promise.setSuccess(existing.getUuid());
			}
		});
		return promise;
	}
	
	private void createLogicalRouter(Promise<UUID> promise, String name, Map<String, String> externalIds) {
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				onSuccess(promise,results,ovsRes->(UUID) ovsRes.get(0).result().get("uuid"));
			});
			channel.writeAndFlush(new CreateLogicalRouterRequest(id,name,new LogicalRouter(name,externalIds)));
		});
	}

	public Future<Void> deleteLogicalSwitch(String name) {
		return deleteLogicalSwitchs(Criteria.field("name").eq(name));
	}
	
	public Future<Void> deleteLogicalSwitchs(Criteria criteria) {
		Promise<Void> promise = eventLoopGroup.next().newPromise();
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				onSuccess(promise,results,ovsRes->null);
			});
			channel.writeAndFlush(new DeleteLogicalSwitchRequest(id,criteria));
		});
		return promise;
	}
	
	public Future<Void> deleteLogicalRouter(String name) {
		return deleteLogicalRouters(Criteria.field("name").eq(name));
	}
	
	public Future<Void> deleteLogicalRouters(Criteria criteria) {
		Promise<Void> promise = eventLoopGroup.next().newPromise();
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				onSuccess(promise,results,ovsRes->null);
			});
			channel.writeAndFlush(new DeleteLogicalRouterRequest(id,criteria));
		});
		return promise;
	}
	
	public Future<Void> deleteLogicalSwitches() {
		Promise<Void> promise = eventLoopGroup.next().newPromise();
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				onSuccess(promise,results,ovsRes->null);
			});
			channel.writeAndFlush(new DeleteLogicalSwitchRequest(id));
		});
		return promise;
	}
	
	public Future<Void> deleteLogicalRouters() {
		Promise<Void> promise = eventLoopGroup.next().newPromise();
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				onSuccess(promise,results,ovsRes->null);
			});
			channel.writeAndFlush(new DeleteLogicalRouterRequest(id));
		});
		return promise;
	}
	
	public Future<Void> deleteLoadBalancers(boolean force) {
		Promise<Void> promise = eventLoopGroup.next().newPromise();
		getLoadBalancers().addListener(futureLb->onSuccess(futureLb,promise,result->{
				List<?> lbs =  (List<?>) result;
				PromiseCombiner combiner = new PromiseCombiner();
				lbs.forEach(lb->{
					Promise<Void> deletePromise = eventLoopGroup.next().newPromise();
					deleteLoadBalancer(deletePromise,((LoadBalancer)lb).getUuid(),force);
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

	public Future<Void> deleteLoadBalancer(String name,Protocol protocol,boolean force) {
		return deleteLoadBalancer(lbId(name, protocol), force);
	}
	
	public Future<Void> deleteLoadBalancer(String name) {
		return deleteLoadBalancer(name,false);
	}
	
	public Future<Void> deleteLoadBalancer(String name,boolean force) {
		Promise<Void> promise = eventLoopGroup.next().newPromise();
		getLoadBalancer(name).addListener(futureLb->onSuccess(futureLb,promise,result->{
				if(result==null){
					promise.setSuccess(null);
				}else{
					deleteLoadBalancer(promise,((LoadBalancer)result).getUuid(),force);
				}
		}));
		return promise;
	}
	
	private void deleteLoadBalancer(Promise<Void> promise, UUID uuid,boolean force) {
		if(force){
			dettachLoadBalancer(uuid).addListener(futureDettach->onSuccess(futureDettach,promise,result->{
				deleteLoadBalancers(promise,Criteria.field("_uuid").eq(uuid));
			}));
		}else{
			deleteLoadBalancers(promise,Criteria.field("_uuid").eq(uuid));
		}
	}

	public Future<Void> deleteLoadBalancerVip(String name, Vip vip) {
		Promise<Void> promise = eventLoopGroup.next().newPromise();
		deleteLoadBalancerVip(promise,name,vip);
		return promise;
	}

	public Future<Void> deleteLoadBalancerVip(Promise<Void> promise,String name, Vip vip) {
		log.info("Delete load balancer vip {} of {}",vip,name);
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				onSuccess(promise,results,ovsRes->null);
			});
			channel.writeAndFlush(new DeleteLoadBalancerVipRequest(id,lbId(name, vip.getProtocol()),vip.toString()));
		});
		return promise;
	}
	
	public Future<Void> dettachLoadBalancer(UUID lbuuid) {
		Promise<Void> promise = eventLoopGroup.next().newPromise();
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				onSuccess(promise,results,ovsRes->null);
			});
			channel.writeAndFlush(new DettachLoadBalancerRequest(id,lbuuid));
		});
		return promise;
	}

	public Future<Void> deleteLoadBalancers(Criteria criteria,boolean force) {
		Promise<Void> promise = eventLoopGroup.next().newPromise();
		getLoadBalancers(criteria).addListener(futureLbs->onSuccess(futureLbs,promise,result->{
				List<?> lbs = (List<?>) result;
				PromiseCombiner combiner =new PromiseCombiner();
				lbs.forEach(lb->{
					Promise<Void> item = eventLoopGroup.next().newPromise();
					combiner.add((Future<?>)item);
					deleteLoadBalancer(item,((LoadBalancer)lb).getUuid(),force);
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
				onSuccess(promise,results,ovsRes->null);
			});
			channel.writeAndFlush(new DeleteLoadBalancerRequest(id,criteria));
		});
		return promise;
	}

	private <T> void onSuccess(Promise<T> promise, List<OVSDBOperationResult> results, Function<List<OVSDBOperationResult>,T> supplier) {
		if(results.stream().anyMatch(result->result.getError()!=null)){
			promise.setFailure(new OVSDBException(results.stream()
					.filter(result->result.getError()!=null)
					.map(result->new OVSDBError(result.getError()))
					.collect(Collectors.toList())
					));
		}else{
			promise.setSuccess(supplier.apply(results));
		}
	}

	private Future<Void> attachLoadBalancerToSwitch(Promise<Void> promise, UUID lbUUID, Collection<String> logicalSwitches) {
		acquireChannel().addListener(future->{
			AttachLoadBalancerToSwitchRequest req = new AttachLoadBalancerToSwitchRequest(requestId.getAndIncrement(), lbUUID,logicalSwitches);
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(req.getId(), results->{
				onSuccess(promise,results,ovsRes->null);
			});
			channel.writeAndFlush(req);
		});
		return promise;
	}
	
	private Future<Void> attachLoadBalancerToRouter(Promise<Void> promise, UUID lbUUID, String logicalRouter) {
		acquireChannel().addListener(future->{
			AttachLoadBalancerToRouterRequest req = new AttachLoadBalancerToRouterRequest(requestId.getAndIncrement(), lbUUID,logicalRouter);
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(req.getId(), results->{
				onSuccess(promise,results,ovsRes->null);
			});
			channel.writeAndFlush(req);
		});
		return promise;
	}
	
	private String lbId(String name,Protocol protocol) {
		return Joiner.on(':').join(name,protocol.getValue());
	}
	
	public Future<LoadBalancer> createLoadBalancer(String name, Collection<String> attachedSwitches, Vip vip,  Collection<Address> ips) {
		return createLoadBalancer(name, attachedSwitches, vip, ips, null);
	}
	
	public Future<LoadBalancer> createLoadBalancer(String name, String gatewaySwitch, Vip vip,  Collection<Address> ips) {
		return createLoadBalancer(name, null, gatewaySwitch, vip, ips, null);
	}
	
	public Future<LoadBalancer> createLoadBalancer(String name, Collection<String> attachedSwitches, Vip vip,  Collection<Address> ips, Map<String, String> externalIds) {
		return createLoadBalancer(name,attachedSwitches,null,vip,ips,externalIds);
	}
	
	public Future<LoadBalancer> createLoadBalancer(String name, Collection<String> attachedSwitches, String gatewaySwitch, Vip vip,  Collection<Address> ips, Map<String, String> externalIds) {
		Promise<LoadBalancer> promise = eventLoopGroup.next().newPromise();
		String lbName = Joiner.on(':').join(name, vip.getProtocol().getValue());
		
		Promise<LoadBalancer> createLoadBalancerPromise = eventLoopGroup.next().newPromise();
		Future<LoadBalancer> existing = checkingLbExists.computeIfAbsent(lbName, (k)->createLoadBalancerIfNotExist(createLoadBalancerPromise,lbName,attachedSwitches,gatewaySwitch,vip,ips,externalIds));
		
		if(existing!=createLoadBalancerPromise){
			createLoadBalancerPromise.cancel(false);
			existing.addListener(futureLoadBalancer->{
				onSuccess(futureLoadBalancer,promise,result->{
					LoadBalancer loadBalancer = (LoadBalancer) result;
					createOrUpdateVirtualIp(promise,loadBalancer,vip,ips);
				});
			});
		}else{
			existing.addListener(futureLoadBalancer->
				onSuccess(futureLoadBalancer,promise,result->promise.setSuccess(((LoadBalancer) result))
			));
		}
		return promise;
	}

	private void createOrUpdateVirtualIp(Promise<LoadBalancer> promise, LoadBalancer loadBalancer, Vip vip, Collection<Address> ips) {
		String existingTargets = loadBalancer.getVips().get(Joiner.on(':').join(vip.getHost(),vip.getPort()));
		if(existingTargets!=null){
			if(sameTargets(existingTargets, ips)){
				log.info("Load balancer {}[{}] contains vip {} and targets are updated",loadBalancer.getName(),vip.getProtocol(),vip);
				promise.setSuccess(loadBalancer);
			}else{
				log.info("Load balancer {}[{}] contains vip {} but targets must be updated",loadBalancer.getName(),vip.getProtocol(),vip);
				updateLoadBalancerVip(loadBalancer.getUuid(),vip,ips).addListener(updateFuture->
					onSuccess(updateFuture,promise,update->promise.setSuccess(loadBalancer)));
			}
		}else{
			log.info("Load balancer {}[{}] does not contain vip {}, creating",loadBalancer.getName(),vip.getProtocol(),vip);
			addLoadBalancerVip(promise,loadBalancer,vip,ips);
		}
		
	}

	private Future<LoadBalancer> createLoadBalancerIfNotExist(Promise<LoadBalancer> promise, String name, Collection<String> attachedSwitches, String gatewayRouter, Vip vip,
			Collection<Address> ips, Map<String, String> externalIds) {
		log.info("Create load balancer {}[{}]: {}={}",name,vip.getProtocol(),vip,ips);
		getLoadBalancer(name).addListener(future->{
			if(future.getNow()!=null){
				checkingLbExists.remove(name);
				LoadBalancer existingLb = (LoadBalancer) future.getNow();
				createOrUpdateVirtualIp(promise,existingLb,vip,ips);
			}else{
				createLoadBalancer(eventLoopGroup.next().newPromise(),name, vip,ips,externalIds).addListener(futureLb->{
					checkingLbExists.remove(name);
					onSuccess(futureLb,promise,result->{
						UUID lbUid = (UUID) result;
						EventLoop loop = eventLoopGroup.next();
						PromiseCombiner combiner = new PromiseCombiner();
						Promise<Void> combinedPromise = loop.newPromise();
						combinedPromise.addListener(futureAttach->
							onSuccess(futureAttach,promise,attach->promise.setSuccess(loadBalancer(lbUid,name, vip, ips, externalIds)))
						);
						if(attachedSwitches!=null){
							Promise<Void> switchPromise = loop.newPromise();
							combiner.add((Future<?>)switchPromise);
							attachLoadBalancerToSwitch(switchPromise, lbUid, attachedSwitches);
						}
						if(gatewayRouter!=null){
							Promise<Void> routerPromise = loop.newPromise();
							combiner.add((Future<?>)routerPromise);
							attachLoadBalancerToRouter(routerPromise, lbUid, gatewayRouter);
						}
						combiner.finish(combinedPromise);
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
				onSuccess(promise,results,ovsRes->(UUID) ovsRes.get(0).result().get("uuid"));
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

	private Future<LoadBalancer> addLoadBalancerVip(Promise<LoadBalancer> promise, LoadBalancer lb, Vip vip, Collection<Address> ips) {
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->onSuccess(promise,results,ovsRes->
				new LoadBalancer(
						lb.getUuid(),
						lb.getName(),
						lb.getProtocol(),
						ImmutableMap.<String,String>builder().putAll(lb.getVips().entrySet()).put(toEndpoint(vip), ips.stream().map(this::toEndpoint).collect(Collectors.joining( "," ))).build(),
						lb.getExternalIds())));
			channel.writeAndFlush(new AddLoadBalancerVipRequest(id,lb.getUuid(),
					Collections.singletonMap(toEndpoint(vip), 
							ips.stream().map(this::toEndpoint).collect(Collectors.joining( "," )))));
		});
		return promise;
	}
	
	public Future<Void> updateLoadBalancerVip(UUID lbId, Vip vip, Collection<Address> ips) {
		Promise<Void> promise = eventLoopGroup.next().newPromise();
		updateLoadBalancerVip(promise,lbId, vip, ips);
		return promise;
	}
	
	private void updateLoadBalancerVip(Promise<Void> promise, UUID lbId, Vip vip, Collection<Address> ips) {
		acquireChannel().addListener(future->{
			int id = requestId.getAndIncrement();
			Channel channel = (Channel) future.getNow();
			channel.attr(CONSUMERS).get().put(id, results->{
				onSuccess(promise,results,ovsRes->null);
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
				return acquireChannelExecutor.newSucceededFuture(this.activeChannel);
			}
		}
		
		Promise<Channel> newAcquiringChannel = eventLoopGroup.next().newPromise();
		if(acquiringChannel.compareAndSet(null, newAcquiringChannel)){
			acquireChannelExecutor.submit(()->acquireChannel(newAcquiringChannel));
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
				acquireChannelExecutor.schedule(()->acquireChannel(acquiringChannel), 1, TimeUnit.SECONDS);
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
