package com.simplyti.cloud.ovn.client.stepdefs;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.google.common.base.Splitter;
import com.simplyti.cloud.ovn.client.OVNNbClient;
import com.simplyti.cloud.ovn.client.OvnCriteriaBuilder;
import com.simplyti.cloud.ovn.client.domain.NetworkIp;
import com.simplyti.cloud.ovn.client.domain.nb.LoadBalancer;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalSwitch;
import com.simplyti.cloud.ovn.client.switches.SwitcheBuilder;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.netty.util.concurrent.Future;

public class LogicalSwitchStepDefs {
	
	@Inject
	private OVNNbClient client;
	
	@Inject
	private Map<String,Object> scenarioData;
	
	@Given("^there not exist any logical switch$")
	public void thereNotExistAnyLogicalSwitch() throws Throwable {
		client.switches().deleteAll().await();
	}
	
	@When("^I create a logical switch with name \"([^\"]*)\"$")
	public UUID iCreateALogicalSwitchWithName(String name) throws Throwable {
		Future<UUID> result = client.switches().builder().withName(name).create().await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
		return result.getNow();
	}
	
	@When("^I create a logical switch with name \"([^\"]*)\" as \"([^\"]*)\"$")
	public void iCreateALogicalSwitchWithNameAs(String name, String key) throws Throwable {
		Future<UUID> result = client.switches().builder().withName(name).create().await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
		scenarioData.put(key, result.getNow());
	}

	@When("^I create a logical switch with name \"([^\"]*)\" and external ids \"([^\"]*)\"$")
	public void iCreateALogicalSwitchWithNameAndExternalIds(String name, String externalIds) throws Throwable {
		SwitcheBuilder builder = client.switches().builder().withName(name);
		Splitter.on(',').withKeyValueSeparator('=').split(externalIds).forEach((k,v)->builder.withExternalId(k,v));
		Future<UUID> result = builder.create().await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
	}
	
	@When("^I create a logical switch with name \"([^\"]*)\" and subnet \"([^\"]*)\"$")
	public void iCreateALogicalSwitchWithNameAndSubnet(String name, String subnet) throws Throwable {
		Future<UUID> result = client.switches().builder()
			.withName(name)
			.withSubnet(subnet)
			.create().await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
	}
	
	@When("^I create a logical switch with name \"([^\"]*)\" getting the created uuid \"([^\"]*)\"$")
	public void iCreateALogicalSwitchWithNameGettingTheCreatedUuid(String name, String key) throws Throwable {
		this.scenarioData.put(key, iCreateALogicalSwitchWithName(name)) ;
	}
	
	@When("^I try to create a logical switch with name \"([^\"]*)\" getting error \"([^\"]*)\"$")
	public void iTryToCreateALogicalSwitchWithNameGettingError(String name, String key) throws Throwable {
	    Future<UUID> result = client.switches().builder().withName(name).create().await();
	    scenarioData.put(key, result.cause());
	}
	
	@Then("^I check that exist a logical switch with name \"([^\"]*)\"$")
	public void iCheckThatExistALogicalSwitchWithName(String name) throws Throwable {
		Future<LogicalSwitch> result = client.switches().get(name).await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
		assertThat(result.getNow().getName(),equalTo(name));
	}
	
	@Then("^I check that exist (\\d+) logical switches$")
	public void iCheckThatExistLogicalSwitches(int size) throws Throwable {
		Future<Collection<LogicalSwitch>> result = client.switches().list().await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),hasSize(size));
	}
	
	@Then("^I check that exist (\\d+) logical switches with external ids \"([^\"]*)\"$")
	public void iCheckThatExistLogicalSwitchesWithExternalIds(int size, String externalIds) throws Throwable {
		OvnCriteriaBuilder<LogicalSwitch> criteriaBuilder = client.switches().where();
		Splitter.on(',').withKeyValueSeparator('=').split(externalIds).forEach((k,v)->criteriaBuilder.externalId(k).equal(v));
		Future<Collection<LogicalSwitch>> result = criteriaBuilder.list().await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),hasSize(size));
	}
	
	@When("^I get the logical switch with name \"([^\"]*)\" as \"([^\"]*)\"$")
	public void iGetTheLogicalSwitchWithNameAs(String name, String key) throws Throwable {
		Future<LogicalSwitch> result = client.switches().get(name).await();
		this.scenarioData.put(key, result.getNow());
	}
	
	@Then("^I check that logical switch \"([^\"]*)\" contains external ids \"([^\"]*)\"$")
	public void iCheckThatLogicalSwitchContainsExternalIds(String key, String externalIds) throws Throwable {
		Map<String, String> externalIdsMap = Splitter.on(',').withKeyValueSeparator('=').split(externalIds);
		LogicalSwitch ls = (LogicalSwitch) scenarioData.get(key);
		assertThat(ls.getExternalIds().entrySet(),hasSize(externalIdsMap.size()));
		externalIdsMap.forEach((k,v)->assertThat(ls.getExternalIds(),hasEntry(k,v)));
	}
	
	@Then("^I check that logical switch \"([^\"]*)\" has subnet \"([^\"]*)\"$")
	public void iCheckThatLogicalSwitchHasSubnet(String key, String subnet) throws Throwable {
		LogicalSwitch ls = (LogicalSwitch) scenarioData.get(key);
		assertThat(ls.getSubnet().getValue(),equalTo(NetworkIp.valueOf(subnet).getValue()));
	}
	
	@Then("^I check that does not exist logical switch with name \"([^\"]*)\"$")
	public void iCheckThatDoesNotExistLogicalSwitchWithName(String name) throws Throwable {
		Future<LogicalSwitch> result = client.switches().get(name).await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),nullValue());
	}
	
	@When("^I delete the logical switch with name \"([^\"]*)\"$")
	public void iDeleteTheLogicalSwitchWithName(String name) throws Throwable {
		Future<Void> result = client.switches().delete(name).await();
		assertTrue(result.isSuccess());
	}
	
	@When("^I delete logical switches with external ids \"([^\"]*)\"$")
	public void iDeleteLogicalSwitchesWithExternalIds(String externalIds) throws Throwable {
		OvnCriteriaBuilder<?> builder = client.switches().where();
		Splitter.on(',').withKeyValueSeparator('=').split(externalIds).forEach((k,v)->builder.externalId(k).equal(v));
		Future<Void> result = builder.delete().await();
		assertTrue(result.isSuccess());
	}
	
	@Then("^I check that logical switch \"([^\"]*)\" has (\\d+) load balancer$")
	public void iCheckThatLogicalSwitchHasLoadBalancer(String key, int size) throws Throwable {
		LogicalSwitch ls = (LogicalSwitch) scenarioData.get(key);
		assertThat(ls.getLoadBalancers(),hasSize(size));
	}
	
	@When("^I close ovn northbound client$")
	public void iCloseOvnNorthboundClient() throws Throwable {
		client.close();
	}
	
	@Then("^I check that error \"([^\"]*)\" is instance of \"([^\"]*)\"$")
	public void iCheckThatErrorIsInstanceOf(String key, Class<?> errorClass) throws Throwable {
		assertThat(scenarioData.get(key),instanceOf(errorClass));
	}

	@Then("^I check that error \"([^\"]*)\" contains message \"([^\"]*)\"$")
	public void iCheckThatErrorContainsMessage(String key, String message) throws Throwable {
		Throwable err = (Throwable) scenarioData.get(key);
		assertThat(err.getMessage(),equalTo(message));
	}
	
	@When("^I update switch \"([^\"]*)\" attaching load balancer \"([^\"]*)\"$")
	public void iUpdateSwitchAttachingLoadBalancer(String name, String lbKey) throws Throwable {
		LoadBalancer lb = (LoadBalancer) scenarioData.get(lbKey);
	    Future<Void> result = client.switches().update(name)
	    	.addLoadBalancer(lb.getUuid())
	    	.update().await();
	    assertTrue(result.isSuccess());
	}
	

}
