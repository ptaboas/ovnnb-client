package com.simplyti.cloud.ovn.client.stepdefs;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.cthul.matchers.CthulMatchers.matchesPattern;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.base.Splitter;
import com.simplyti.cloud.ovn.client.OVNNbClient;
import com.simplyti.cloud.ovn.client.OvnCriteriaBuilder;
import com.simplyti.cloud.ovn.client.domain.Endpoint;
import com.simplyti.cloud.ovn.client.domain.nb.LoadBalancer;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalRouter;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalSwitch;
import com.simplyti.cloud.ovn.client.domain.nb.Protocol;
import com.simplyti.cloud.ovn.client.loadbalancers.EndpointUpdater;
import com.simplyti.cloud.ovn.client.loadbalancers.LoadBalancerBuilder;
import com.simplyti.cloud.ovn.client.loadbalancers.LoadBalancerEndpointBuilder;
import com.simplyti.cloud.ovn.client.loadbalancers.TargetEndpointBuilder;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.netty.util.concurrent.Future;

public class LoadBalancerStepDefs {
	
	@Inject
	private OVNNbClient client;
	
	@Inject
	private Map<String,Object> scenarioData;
	
	
	@Given("^there not exist any load balancer$")
	public void thereNotExistAnyLoadBalancer() throws Throwable {
		client.loadBalancers().deleteAll().await();
	}
	
	@When("^I create a \"([^\"]*)\" load balancer with name \"([^\"]*)\", virtual ip \"([^\"]*)\", targets \"([^\"]*)\" attached to logical switch \"([^\"]*)\"$")
	public void iCreateALoadBalancerWithNameVirtualIpTargetsAttachedToLogicalSwitch(Protocol protocol, String name, String vip, List<String> targets, String ls) throws InterruptedException{
		Future<UUID> uuid = createALoadBalancer(protocol,name,vip,targets,ls);
		assertTrue(uuid.isSuccess());
		assertThat(uuid.getNow(),notNullValue());
	}
	
	@When("^I try to create a \"([^\"]*)\" load balancer with name \"([^\"]*)\", virtual ip \"([^\"]*)\", targets \"([^\"]*)\" attached to logical switch \"([^\"]*)\" getting error \"([^\"]*)\"$")
	public void iTryToCreateALoadBalancerWithNameVirtualIpTargetsAttachedToLogicalSwitchGettingError(Protocol protocol, String name, String vip, List<String> targets, String ls, String errorKey) throws Throwable {
		Future<UUID> uuid = createALoadBalancer(protocol,name,vip,targets,ls);
		assertThat(uuid.isSuccess(),equalTo(false));
		scenarioData.put(errorKey, uuid.cause());
	}
	
	@Given("^I create a load balancer with name \"([^\"]*)\", virtual ip \"([^\"]*)\", targets \"([^\"]*)\"$")
	public void iCreateALoadBalancerWithNameVirtualIpTargets(String name, String vip, List<String> targets) throws Throwable {
		Endpoint endpointVip = toEndpoint(vip);
		LoadBalancerEndpointBuilder builder = client.loadBalancers().builder()
			.withName(name)
			.withVirtualIp()
				.ip(endpointVip.getIp());
		
		if(endpointVip.getPort()!=null){
			builder.port(endpointVip.getPort());
		}
		
		for(String targetStrp:targets){
			Endpoint target = toEndpoint(targetStrp);
			TargetEndpointBuilder targetBuilder = builder.addTarget()
				.ip(target.getIp());
			
			if(target.getPort()!=null){
				builder = targetBuilder.port(target.getPort())
						.add();
			}else{
				builder = targetBuilder.add();
			}
		}
		builder.create().create().await();
	}
	
	private Future<UUID> createALoadBalancer(Protocol protocol, String name, String vip, List<String> targets, String ls) throws InterruptedException {
		Endpoint endpointVip = toEndpoint(vip);
		LoadBalancerEndpointBuilder builder = client.loadBalancers().builder()
			.withName(name)
			.withProtocol(protocol)
			.inLogicalSwitch(ls)
			.withVirtualIp()
				.ip(endpointVip.getIp());
		
		if(endpointVip.getPort()!=null){
			builder.port(endpointVip.getPort());
		}
		
		for(String targetStrp:targets){
			Endpoint target = toEndpoint(targetStrp);
			TargetEndpointBuilder targetBuilder = builder.addTarget()
				.ip(target.getIp());
			
			if(target.getPort()!=null){
				builder = targetBuilder.port(target.getPort())
						.add();
			}else{
				builder = targetBuilder.add();
			}
		}
		return builder.create().create().await();
	}

	private Endpoint toEndpoint(String vip) {
		String[] ipPort = vip.split(":");
		if(ipPort.length==2){
			return new Endpoint(ipPort[0],Integer.parseInt(ipPort[1]));
		}else{
			return new Endpoint(ipPort[0],null);
		}
	}
	
	@When("^I add virtual ip \"([^\"]*)\" with targets \"([^\"]*)\" to load balancer \"([^\"]*)\"$")
	public void iAddVirtualIpWithTargetsToLoadBalancer(String vip, List<String> targets,String lbKey) throws Throwable {
		Endpoint endpointVip = toEndpoint(vip);
		LoadBalancer lb = (LoadBalancer) scenarioData.get(lbKey);
		
		EndpointUpdater updater = client.loadBalancers().update(lb.getUuid())
			.virtualIp()
				.ip(endpointVip.getIp())
				.port(endpointVip.getPort());
		
		for(String targetStrp:targets){
			Endpoint target = toEndpoint(targetStrp);
			updater = updater.target()
				.ip(target.getIp())
				.port(target.getPort())
				.add();
		}
		
		Future<Void> result = updater.uptate().update().await();
		assertTrue(result.isSuccess());
	}
	
	@When("^I set virtual ip \"([^\"]*)\" targets to \"([^\"]*)\" to load balancer \"([^\"]*)\"$")
	public void iSetVirtualIpTargetsToToLoadBalancer(String vip, List<String> targets, String lbKey) throws Throwable {
		Endpoint endpointVip = toEndpoint(vip);
		LoadBalancer lb = (LoadBalancer) scenarioData.get(lbKey);
		
		Future<Void> result = client.loadBalancers().update(lb.getUuid())
				.virtualIp()
					.ip(endpointVip.getIp())
					.port(endpointVip.getPort())
						.setTargets(targets.stream().map(this::toEndpoint).collect(Collectors.toList()))
						.uptate().update().await();
			
		assertTrue(result.isSuccess());
	}
	
	@When("^I add targets \"([^\"]*)\" to virtual ip \"([^\"]*)\" in load balancer \"([^\"]*)\"$")
	public void iAddTargetsToVirtualIpInLoadBalancer(List<String> targets, String vip, String lbKey) throws Throwable {
		Endpoint endpointVip = toEndpoint(vip);
		LoadBalancer lb = (LoadBalancer) scenarioData.get(lbKey);
		
		EndpointUpdater updater = client.loadBalancers().update(lb.getUuid())
			.virtualIp()
				.ip(endpointVip.getIp())
				.port(endpointVip.getPort());
		
		for(String targetStrp:targets){
			Endpoint target = toEndpoint(targetStrp);
			updater = updater.target()
				.ip(target.getIp())
				.port(target.getPort())
				.add();
		}
		
		Future<Void> result = updater.uptate().update().await();
		assertTrue(result.isSuccess());
	}
	
	@When("^I update load balancer \"([^\"]*)\" adding virtual ip \"([^\"]*)\" with targets \"([^\"]*)\" getting result \"([^\"]*)\"$")
	public void iUpdateLoadBalancerAddingVirtualIpWithTargetsGettingResult(String name, String vip, List<String> targets, String key) throws Throwable {
		Future<Void> result = updateLb(name,vip,targets);
		scenarioData.put(key, result);
	}
	
	private Future<Void> updateLb(String name, String vip, List<String> targets) throws InterruptedException {
		Endpoint endpointVip = toEndpoint(vip);
		EndpointUpdater updater = client.loadBalancers().update(name).virtualIp()
			.ip(endpointVip.getIp())
			.port(endpointVip.getPort());
		
		for(String targetStrp:targets){
			Endpoint target = toEndpoint(targetStrp);
			updater = updater.target()
				.ip(target.getIp())
				.port(target.getPort())
				.add();
		}
		
		return updater.uptate().update().await();
	}

	@When("^I delete targets \"([^\"]*)\" from virtual ip \"([^\"]*)\" in load balancer \"([^\"]*)\"$")
	public void iDeleteTargetsFromVirtualIpInLoadBalancer(List<String> targets, String vip, String lbKey) throws Throwable {
		Endpoint endpointVip = toEndpoint(vip);
		LoadBalancer lb = (LoadBalancer) scenarioData.get(lbKey);
		
		EndpointUpdater updater = client.loadBalancers().update(lb.getUuid())
			.virtualIp()
				.ip(endpointVip.getIp())
				.port(endpointVip.getPort());
		
		for(String targetStrp:targets){
			Endpoint target = toEndpoint(targetStrp);
			updater = updater.target()
				.ip(target.getIp())
				.port(target.getPort())
				.remove();
		}
		
		Future<Void> result = updater.uptate().update().await();
		assertTrue(result.isSuccess());
	}
	
	
	@When("^I create a \"([^\"]*)\" load balancer with name \"([^\"]*)\", virtual ip \"([^\"]*)\", targets \"([^\"]*)\" attached to logical router \"([^\"]*)\"$")
	public void iCreateALoadBalancerWithNameVirtualIpTargetsAttachedToLogicalRouter(Protocol protocol, String name, String vip, List<String> targets, String lr) throws InterruptedException{
		Endpoint endpointVip = toEndpoint(vip);
		LoadBalancerEndpointBuilder builder = client.loadBalancers().builder()
			.withName(name)
			.withProtocol(protocol)
			.inLogicalRouter(lr)
			.withVirtualIp()
				.ip(endpointVip.getIp())
				.port(endpointVip.getPort());
		
		for(String targetStrp:targets){
			Endpoint target = toEndpoint(targetStrp);
			builder = builder.addTarget()
				.ip(target.getIp())
				.port(target.getPort())
				.add();
		}
		
		Future<UUID> uuid = builder.create().create().await();
		assertTrue(uuid.isSuccess());
		assertThat(uuid.getNow(),notNullValue());
	}
	
	@When("^I create a \"([^\"]*)\" load balancer with name \"([^\"]*)\", virtual ip \"([^\"]*)\", targets \"([^\"]*)\" and external ids \"([^\"]*)\" attached to logical switch \"([^\"]*)\"$")
	public void iCreateALoadBalancerWithNameVirtualIpTargetsAndExternalIdsAttachedToLogicalSwitch(Protocol protocol, String name, String vip, List<String> targets, String externalIds, String ls) throws Throwable {
		Endpoint endpointVip = toEndpoint(vip);
		LoadBalancerBuilder builder = client.loadBalancers().builder()
			.withName(name);
		
		Splitter.on(',').withKeyValueSeparator('=').split(externalIds).forEach((k,v)->builder.withExternalId(k,v));
		
		LoadBalancerEndpointBuilder endpointBuilder = builder.withProtocol(protocol)
			.inLogicalSwitch(ls)
			.withVirtualIp()
				.ip(endpointVip.getIp())
				.port(endpointVip.getPort());
		
		for(String targetStrp:targets){
			Endpoint target = toEndpoint(targetStrp);
			endpointBuilder = endpointBuilder.addTarget()
				.ip(target.getIp())
				.port(target.getPort())
				.add();
		}
		
		Future<UUID> uuid = endpointBuilder.create().create().await();
		assertTrue(uuid.isSuccess());
		assertThat(uuid.getNow(),notNullValue());
	}
	
	@When("^I attach load balancer \"([^\"]*)\" to switch \"([^\"]*)\"$")
	public void iAttachLoadBalancerToSwitch(String lbKey, String lsKey) throws Throwable {
		LoadBalancer lb = (LoadBalancer) scenarioData.get(lbKey);
		UUID lsid = (UUID) scenarioData.get(lsKey);
	    Future<Void> result = client.loadBalancers().update(lb.getUuid())
	    	.attachToSwitch(lsid)
	    	.update().await();
	    assertTrue(result.isSuccess());
	}
	

	@Then("^I check that exist a load balancer with name \"([^\"]*)\"$")
	public LoadBalancer iCheckThatExistALoadBalancerWithName(String name) throws Throwable {
		Future<LoadBalancer> result = client.loadBalancers().get(name).await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
		assertThat(result.getNow().getName(),equalTo(name));
		return result.getNow();
	}

	@Then("^I check that does not exist load balancer with name \"([^\"]*)\"$")
	public void iCheckThatDoesNotExistLoadBalancerWithName(String name) throws Throwable {
		Future<LoadBalancer> result = client.loadBalancers().get(name).await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),nullValue());
	}
	
	@When("^I get the load balancer with name \"([^\"]*)\" as \"([^\"]*)\"$")
	public void iGetTheLoadBalancerWithNameAs(String name, String key) throws Throwable {
		this.scenarioData.put(key, iCheckThatExistALoadBalancerWithName(name)) ;
	}

	@Then("^I check that load balancer \"([^\"]*)\" has protocol \"([^\"]*)\"$")
	public void iCheckThatLoadBalancerHasProtocol(String key, Protocol protocol) throws Throwable {
		LoadBalancer lb = (LoadBalancer) scenarioData.get(key);
		assertThat(lb.getProtocol(),equalTo(protocol));
	}
	
	@Then("^I check that load balancer \"([^\"]*)\" has (\\d+) virtual ips$")
	public void iCheckThatLoadBalancerHasVirtualIps(String key, int size) throws Throwable {
		LoadBalancer lb = (LoadBalancer) scenarioData.get(key);
		assertThat(lb.getVips().entrySet(),hasSize(size));
	}
	
	@Then("^I check that load balancer \"([^\"]*)\" contains virtual ip \"([^\"]*)\" with targets \"([^\"]*)\"$")
	public void iCheckThatLoadBalancerContainsVirtualIpWithTargets(String key, String vip, List<String> ips) throws Throwable {
		LoadBalancer lb = (LoadBalancer) scenarioData.get(key);
		Endpoint endpointVip = toEndpoint(vip);
		List<Endpoint> targets = ips.stream().map(this::toEndpoint).collect(Collectors.toList());
		assertThat(lb.getVips(),hasKey(endpointVip));
		Collection<Endpoint> vipTargets = lb.getVips().get(endpointVip);
		
		assertThat(vipTargets,containsInAnyOrder(targets.stream().toArray(Endpoint[]::new)));
	}
	
	@Then("^I check that logical switch \"([^\"]*)\" contains load balancer \"([^\"]*)\"$")
	public void iCheckThatLogicalSwitchContainsLoadBalancer(String lsKey, String lbKey) throws Throwable {
		LogicalSwitch ls = (LogicalSwitch) scenarioData.get(lsKey);
		LoadBalancer lb = (LoadBalancer) scenarioData.get(lbKey);
		assertThat(ls.getLoadBalancers(),hasItem(lb.getUuid()));
	}
	
	@Then("^I check that logical router \"([^\"]*)\" contains load balancer \"([^\"]*)\"$")
	public void iCheckThatLogicalRouterContainsLoadBalancer(String lsKey, String lbKey) throws Throwable {
		LogicalRouter ls = (LogicalRouter) scenarioData.get(lsKey);
		LoadBalancer lb = (LoadBalancer) scenarioData.get(lbKey);
		assertThat(ls.getLoadBalancers(),hasItem(lb.getUuid()));
	}
	
	@When("^I delete load balancer with name \"([^\"]*)\" with forced option$")
	public void iDeleteLoadBalancerWithNameForced(String name) throws Throwable {
		Future<Void> result = client.loadBalancers().delete(name,true).await();
		assertTrue(result.isSuccess());
	}
	
	
	@When("^I try to delete load balancer with name \"([^\"]*)\" getting promise \"([^\"]*)\"$")
	public void iTryToDeleteLoadBalancerWithNameGettingPromise(String name, String resultKey) throws Throwable {
		Future<Void> result = client.loadBalancers().delete(name).await();
		scenarioData.put(resultKey, result);
	}

	
	@When("^I delete load balancer with name \"([^\"]*)\"$")
	public void iDeleteLoadBalancerWithName(String name) throws Throwable {
		Future<Void> result = client.loadBalancers().delete(name);
		assertTrue(result.isSuccess());
	}
	
	
	@When("^I delete \"([^\"]*)\" virtual ip \"([^\"]*)\" of load balancer \"([^\"]*)\"$")
	public void iDeleteVirtualIpOfLoadBalancer(Protocol protocol, String vip, String name) throws Throwable {
		Endpoint endpointVip = toEndpoint(vip);
		Future<Void> result = client.loadBalancers().update(name)
			.virtualIp()
				.ip(endpointVip.getIp())
				.port(endpointVip.getPort())
				.remove()
			.update().await();
		assertTrue(result.isSuccess());
	}
	
	@When("^I delete load balancers with external ids \"([^\"]*)\" with forced option$")
	public void iDeleteLoadBalancersWithExternalIdsForced(String externalIds) throws Throwable {
		OvnCriteriaBuilder<?> builder = client.loadBalancers().where();
		Splitter.on(',').withKeyValueSeparator('=').split(externalIds).forEach((k,v)->builder.externalId(k).equal(v));
		Future<Void> result = builder.delete(true).await();
		assertTrue(result.isSuccess());
	}
	
	@Then("^I check that exist (\\d+) load balancers$")
	public void iCheckThatExistLoadBalancers(int size) throws Throwable {
		Future<Collection<LoadBalancer>> result = client.loadBalancers().list().await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),hasSize(size));
	}
	
	@Then("^I check that result promise \"([^\"]*)\" has failed$")
	public void iCheckThatResultPromiseHasFailed(String key) throws Throwable {
	   Future<?> result = (Future<?>) scenarioData.get(key);
	   assertThat(result.isSuccess(),equalTo(false));
	}
	
	@Then("^I check that result ptomise \"([^\"]*)\" has failure message that match with \"([^\"]*)\"$")
	public void iCheckThatResultPtomiseHasFailureMessageThatMatchWith(String key, String message) throws Throwable {
		Future<?> result = (Future<?>) scenarioData.get(key);
		assertThat(result.cause().getMessage(),matchesPattern(message));
	}
	


}
