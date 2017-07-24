package com.simplyti.cloud.ovn.client.stepdefs;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.simplyti.cloud.ovn.client.OVNNbClient;
import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.Address;
import com.simplyti.cloud.ovn.client.domain.Vip;
import com.simplyti.cloud.ovn.client.domain.nb.LoadBalancer;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalSwitch;
import com.simplyti.cloud.ovn.client.domain.nb.Protocol;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

public class LoadBalancerStepDefs {
	
	@Inject
	private OVNNbClient client;
	
	@Inject
	private Map<String,Object> scenarioData;
	
	
	@Given("^there not exist any load balancer$")
	public void thereNotExistAnyLoadBalancer() throws Throwable {
		client.deleteLoadBalancers().await();
	}
	
	@When("^I create a \"([^\"]*)\" load balancer with name \"([^\"]*)\", virtual ip \"([^\"]*)\", targets \"([^\"]*)\" attached to logical switch \"([^\"]*)\"$")
	public void iCreateALoadBalancerWithNameVirtualIpTargetsAttachedToLogicalSwitch(Protocol protocol, String name, String vipStr, List<String> targets, String ls) throws InterruptedException{
		Future<UUID> result = createLoadBalancer(protocol,name,vipStr,targets,ls,null).await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
	}
	
	@When("^I create a \"([^\"]*)\" load balancer with name \"([^\"]*)\", virtual ip \"([^\"]*)\", targets \"([^\"]*)\" and external ids \"([^\"]*)\" attached to logical switch \"([^\"]*)\"$")
	public void iCreateALoadBalancerWithNameVirtualIpTargetsAndExternalIdsAttachedToLogicalSwitch(Protocol protocol, String name, String vipStr, List<String> targets, String externalIds, String ls) throws Throwable {
		Map<String, String> externalIdsMap = Splitter.on(',').withKeyValueSeparator('=').split(externalIds);
		Future<UUID> result = createLoadBalancer(protocol,name,vipStr,targets,ls,externalIdsMap).await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
	}
	
	@When("^I asynchronously create a \"([^\"]*)\" load balancer with name \"([^\"]*)\", virtual ip \"([^\"]*)\", targets \"([^\"]*)\" attached to logical switch \"([^\"]*)\" getting \"([^\"]*)\"$")
	public void iAsynchronouslyCreateALoadBalancerWithNameVirtualIpTargetsAttachedToLogicalSwitchGetting(Protocol protocol, String name, String vipStr, List<String> targets, String ls, String promisekey) throws Throwable {
		scenarioData.put(promisekey, createLoadBalancer(protocol,name,vipStr,targets,ls,null));
	}

	private Future<UUID> createLoadBalancer(Protocol protocol, String name, String vipStr, List<String> targets, String ls, Map<String,String> externalIds) {
		Iterable<String> vipIt = Splitter.on(':').split(vipStr);
		String port = Iterables.get(vipIt, 1,null);
		Vip vip = new Vip(Iterables.get(vipIt, 0),port!=null?Integer.parseInt(port):null,protocol);
		List<Address> ips = targets.stream().map(str->{
			Iterable<String> addressIt = Splitter.on(':').split(str);
			String ipPort = Iterables.get(addressIt, 1,null);
			return new Address(Iterables.get(addressIt, 0),ipPort!=null?Integer.parseInt(ipPort):null);
		}).collect(Collectors.toList());
		if(externalIds==null){
			return client.createLoadBalancer(name, Collections.singleton(ls), vip, ips);
		}else{
			return client.createLoadBalancer(name, Collections.singleton(ls), vip, ips,externalIds);
		}
		
	}
	
	@Then("^I check that promises \"([^\"]*)\" are success$")
	public void iCheckThatPromisesAreSuccess(List<String> keys) throws Throwable {
		for(String key:keys){
			Promise<?> promise = (Promise<?>) scenarioData.get(key);
			promise.await();
			assertTrue(promise.isSuccess());
		}
	}

	@Then("^I check that exist a load balancer with name \"([^\"]*)\"$")
	public LoadBalancer iCheckThatExistALoadBalancerWithName(String name) throws Throwable {
 		Future<LoadBalancer> result = client.getLoadBalancer(name).await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
		assertThat(result.getNow().getName(),equalTo(name));
		return result.getNow();
	}
	

	@Then("^I check that exist a \"([^\"]*)\" load balancer with name \"([^\"]*)\"$")
	public void iCheckThatExistALoadBalancerWithName(Protocol protocol, String name) throws Throwable {
		Future<LoadBalancer> result = client.getLoadBalancer(name,protocol).await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
	}
	
	@Then("^I check that does not exist load balancer with name \"([^\"]*)\"$")
	public void iCheckThatDoesNotExistLoadBalancerWithName(String name) throws Throwable {
		Future<LoadBalancer> result = client.getLoadBalancer(name).await();
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
		assertThat(lb.getProtocol(),equalTo(protocol.name().toLowerCase()));
	}
	
	@Then("^I check that load balancer \"([^\"]*)\" has (\\d+) virtual ips$")
	public void iCheckThatLoadBalancerHasVirtualIps(String key, int size) throws Throwable {
		LoadBalancer lb = (LoadBalancer) scenarioData.get(key);
		assertThat(lb.getVips().entrySet(),hasSize(size));
	}
	
	@Then("^I check that load balancer \"([^\"]*)\" contains virtual ip \"([^\"]*)\" with targets \"([^\"]*)\"$")
	public void iCheckThatLoadBalancerContainsVirtualIpWithTargets(String key, String vip, String ips) throws Throwable {
		LoadBalancer lb = (LoadBalancer) scenarioData.get(key);
		assertThat(lb.getVips(),hasEntry(vip,ips));
	}
	
	@Then("^I check that logical switch \"([^\"]*)\" contains load balancer \"([^\"]*)\"$")
	public void iCheckThatLogicalSwitchContainsLoadBalancer(String lsKey, String lbKey) throws Throwable {
		LogicalSwitch ls = (LogicalSwitch) scenarioData.get(lsKey);
		LoadBalancer lb = (LoadBalancer) scenarioData.get(lbKey);
		assertThat(ls.getLoadBalancers(),hasItem(lb.getUuid()));
	}
	
	@When("^I delete load balancer with name \"([^\"]*)\"$")
	public void iDeleteLoadBalancerWithName(String name) throws Throwable {
		Future<Void> result = client.deleteLoadBalancer(name).await();
		assertTrue(result.isSuccess());
	}
	
	@When("^I delete \"([^\"]*)\" load balancer with name \"([^\"]*)\"$")
	public void iDeleteLoadBalancerWithName(Protocol protocol, String name) throws Throwable {
		Future<Void> result = client.deleteLoadBalancer(name,protocol).await();
		assertTrue(result.isSuccess());
	}
	
	@When("^I delete \"([^\"]*)\" virtual ip \"([^\"]*)\" of load balancer \"([^\"]*)\"$")
	public void iDeleteVirtualIpOfLoadBalancer(Protocol protocol, String vipStr, String name) throws Throwable {
		Iterable<String> vipIt = Splitter.on(':').split(vipStr);
		Vip vip = new Vip(Iterables.get(vipIt, 0),Integer.parseInt(Iterables.get(vipIt, 1)),protocol);
		Future<Void> result = client.deleteLoadBalancerVip(name, vip).await();
		assertTrue(result.isSuccess());
	}
	
	@When("^I delete load balancers with external ids \"([^\"]*)\"$")
	public void iDeleteLoadBalancersWithExternalIds(String externalIds) throws Throwable {
		Map<String, String> externalIdsMap = Splitter.on(',').withKeyValueSeparator('=').split(externalIds);
		Future<Void> result =client.deleteLoadBalancers(Criteria.field("external_ids").includes(externalIdsMap)).await();
		assertTrue(result.isSuccess());
	}
	
	@Then("^I check that exist (\\d+) load balancers$")
	public void iCheckThatExistLoadBalancers(int size) throws Throwable {
		Future<List<LoadBalancer>> result = client.getLoadBalancers().await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),hasSize(size));
	}


}
