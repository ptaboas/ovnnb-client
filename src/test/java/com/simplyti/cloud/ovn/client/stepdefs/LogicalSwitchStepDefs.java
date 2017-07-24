package com.simplyti.cloud.ovn.client.stepdefs;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.google.common.base.Splitter;
import com.simplyti.cloud.ovn.client.OVNNbClient;
import com.simplyti.cloud.ovn.client.criteria.Criteria;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalSwitch;

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
		client.deleteLogicalSwitches().await();
	}
	
	@When("^I create a logical switch with name \"([^\"]*)\"$")
	public UUID iCreateALogicalSwitchWithName(String name) throws Throwable {
		Future<UUID> result = client.createLogicalSwitch(name).await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
		return result.getNow();
	}

	@When("^I create a logical switch with name \"([^\"]*)\" and external ids \"([^\"]*)\"$")
	public void iCreateALogicalSwitchWithNameAndExternalIds(String name, String externalIds) throws Throwable {
		Map<String, String> externalIdsMap = Splitter.on(',').withKeyValueSeparator('=').split(externalIds);
		Future<UUID> result = client.createLogicalSwitch(name,externalIdsMap).await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
	}
	
	@When("^I create a logical switch with name \"([^\"]*)\" getting the created uuid \"([^\"]*)\"$")
	public void iCreateALogicalSwitchWithNameGettingTheCreatedUuid(String name, String key) throws Throwable {
		this.scenarioData.put(key, iCreateALogicalSwitchWithName(name)) ;
	}
	
	@Then("^I check that uuid \"([^\"]*)\" is equals to \"([^\"]*)\"$")
	public void iCheckThatUuidIsEqualsTo(String key1, String key2) throws Throwable {
		assertThat(scenarioData.get(key1),equalTo(scenarioData.get(key2)));
	}
	
	@Then("^I check that exist a logical switch with name \"([^\"]*)\"$")
	public void iCheckThatExistALogicalSwitchWithName(String name) throws Throwable {
		Future<LogicalSwitch> result = client.getLogicalSwitch(name).await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
		assertThat(result.getNow().getName(),equalTo(name));
	}
	
	@Then("^I check that exist (\\d+) logical switches$")
	public void iCheckThatExistLogicalSwitches(int size) throws Throwable {
		Future<List<LogicalSwitch>> result = client.getlogicalSwitches().await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),hasSize(size));
	}
	
	@When("^I get the logical switch with name \"([^\"]*)\" as \"([^\"]*)\"$")
	public void iGetTheLogicalSwitchWithNameAs(String name, String key) throws Throwable {
		Future<LogicalSwitch> result = client.getLogicalSwitch(name).await();
		this.scenarioData.put(key, result.getNow());
	}
	
	@Then("^I check that logical switch \"([^\"]*)\" contains external ids \"([^\"]*)\"$")
	public void iCheckThatLogicalSwitchContainsExternalIds(String key, String externalIds) throws Throwable {
		Map<String, String> externalIdsMap = Splitter.on(',').withKeyValueSeparator('=').split(externalIds);
		LogicalSwitch ls = (LogicalSwitch) scenarioData.get(key);
		assertThat(ls.getExternalIds().entrySet(),hasSize(externalIdsMap.size()));
		externalIdsMap.forEach((k,v)->assertThat(ls.getExternalIds(),hasEntry(k,v)));
	}
	
	@Then("^I check that does not exist logical switch with name \"([^\"]*)\"$")
	public void iCheckThatDoesNotExistLogicalSwitchWithName(String name) throws Throwable {
		Future<LogicalSwitch> result = client.getLogicalSwitch(name).await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),nullValue());
	}
	
	@When("^I delete the logical switch with name \"([^\"]*)\"$")
	public void iDeleteTheLogicalSwitchWithName(String name) throws Throwable {
		Future<Void> result = client.deleteLogicalSwitch(name).await();
		assertTrue(result.isSuccess());
	}
	
	@When("^I delete logical switches with external ids \"([^\"]*)\"$")
	public void iDeleteLogicalSwitchesWithExternalIds(String externalIds) throws Throwable {
		Map<String, String> externalIdsMap = Splitter.on(',').withKeyValueSeparator('=').split(externalIds);
		Future<Void> result = client.deleteLogicalSwitchs(Criteria.field("external_ids").includes(externalIdsMap)).await();
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
	

}
