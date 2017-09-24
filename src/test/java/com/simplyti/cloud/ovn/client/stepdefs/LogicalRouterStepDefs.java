package com.simplyti.cloud.ovn.client.stepdefs;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.google.common.base.Splitter;
import com.simplyti.cloud.ovn.client.OVNNbClient;
import com.simplyti.cloud.ovn.client.OvnCriteriaBuilder;
import com.simplyti.cloud.ovn.client.domain.nb.LogicalRouter;
import com.simplyti.cloud.ovn.client.routers.RouterBuilder;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.netty.util.concurrent.Future;

public class LogicalRouterStepDefs {
	
	@Inject
	private OVNNbClient client;
	
	@Inject
	private Map<String,Object> scenarioData;
	
	@Given("^there not exist any logical router$")
	public void thereNotExistAnyLogicalRouter() throws Throwable {
		client.routers().deleteAll().await();
	}
	
	@When("^I create a logical router with name \"([^\"]*)\"$")
	public UUID iCreateALogicalRouterWithName(String name) throws Throwable {
		Future<UUID> result = client.routers().builder().withName(name).create().await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
		return result.getNow();
	}

	@When("^I create a logical router with name \"([^\"]*)\" and external ids \"([^\"]*)\"$")
	public void iCreateALogicalRouterWithNameAndExternalIds(String name, String externalIds) throws Throwable {
		RouterBuilder builder = client.routers().builder().withName(name);
		Splitter.on(',').withKeyValueSeparator('=').split(externalIds).forEach((k,v)->builder.withExternalId(k,v));
		Future<UUID> result = builder.create().await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
	}
	
	@Then("^I check that exist a logical router with name \"([^\"]*)\"$")
	public void iCheckThatExistALogicalRouterWithName(String name) throws Throwable {
		Future<LogicalRouter> result = client.routers().get(name).await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),notNullValue());
		assertThat(result.getNow().getName(),equalTo(name));
	}
	
	@Then("^I check that exist (\\d+) logical routers$")
	public void iCheckThatExistLogicalRouters(int size) throws Throwable {
		Future<Collection<LogicalRouter>> result = client.routers().list().await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),hasSize(size));
	}
	
	@When("^I get the logical router with name \"([^\"]*)\" as \"([^\"]*)\"$")
	public void iGetTheLogicalRouterWithNameAs(String name, String key) throws Throwable {
		Future<LogicalRouter> result = client.routers().get(name).await();
		this.scenarioData.put(key, result.getNow());
	}
	
	@Then("^I check that logical router \"([^\"]*)\" contains external ids \"([^\"]*)\"$")
	public void iCheckThatLogicalRouterContainsExternalIds(String key, String externalIds) throws Throwable {
		Map<String, String> externalIdsMap = Splitter.on(',').withKeyValueSeparator('=').split(externalIds);
		LogicalRouter ls = (LogicalRouter) scenarioData.get(key);
		assertThat(ls.getExternalIds().entrySet(),hasSize(externalIdsMap.size()));
		externalIdsMap.forEach((k,v)->assertThat(ls.getExternalIds(),hasEntry(k,v)));
	}
	
	@Then("^I check that does not exist logical router with name \"([^\"]*)\"$")
	public void iCheckThatDoesNotExistLogicalRouterWithName(String name) throws Throwable {
		Future<LogicalRouter> result = client.routers().get(name).await();
		assertTrue(result.isSuccess());
		assertThat(result.getNow(),nullValue());
	}
	
	@When("^I delete the logical router with name \"([^\"]*)\"$")
	public void iDeleteTheLogicalRouterWithName(String name) throws Throwable {
		Future<Void> result = client.routers().delete(name).await();
		assertTrue(result.isSuccess());
	}
	
	@When("^I delete logical routers with external ids \"([^\"]*)\"$")
	public void iDeleteLogicalRoutersWithExternalIds(String externalIds) throws Throwable {
		OvnCriteriaBuilder<?> builder = client.routers().where();
		Splitter.on(',').withKeyValueSeparator('=').split(externalIds).forEach((k,v)->builder.externalId(k).equal(v));
		Future<Void> result = builder.delete().await();
		assertTrue(result.isSuccess());
	}
	
	@Then("^I check that logical router \"([^\"]*)\" has (\\d+) load balancer$")
	public void iCheckThatLogicalRouterHasLoadBalancer(String key, int size) throws Throwable {
		LogicalRouter ls = (LogicalRouter) scenarioData.get(key);
		assertThat(ls.getLoadBalancers(),hasSize(size));
	}
	
}
